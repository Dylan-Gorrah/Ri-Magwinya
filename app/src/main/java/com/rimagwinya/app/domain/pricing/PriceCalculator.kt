package com.rimagwinya.app.domain.pricing

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.OptionGroupType

/**
 * What a configured item costs.
 *
 * This is the hardest logic in the app and the only place it lives on the
 * client. The **same formula is implemented again in `place_order`**, and the
 * server's answer is the one that decides what a student is charged — this
 * one exists so the button can show a number that is already correct.
 *
 * If the two ever disagree, the server wins and this is the bug.
 *
 * The formula, from section 9.1 of the brief:
 *
 * ```
 * unit = if the item has a base step:          item.price x baseQty
 *        else if a group replaces the price:   the chosen option's price
 *        else:                                 item.price
 *      + for every qty group:                  sum(option.price x count)
 *      + for every non-replacing single group: the chosen option's price
 *
 * build item     = has a base step OR any qty group
 * quantity       = 1 for build items, otherwise the outer stepper
 * line total     = unit x quantity
 * units consumed = baseQty for base-step items (may be 0), else quantity
 * add to cart    = only when unit > 0
 * ```
 */
object PriceCalculator {

    /** The price of one configured item, before the outer quantity. */
    fun unitPrice(item: MenuItem, selection: Selection): Money {
        var total = basePrice(item, selection)

        item.optionGroups.forEach { group ->
            when (group.type) {
                OptionGroupType.Qty ->
                    group.options.forEach { option ->
                        val count = selection.countOf(option.id)
                        if (count > 0) total += option.price * count
                    }

                OptionGroupType.Single -> {
                    // A replacing group already set the base above, so adding
                    // it again here would charge for the size twice.
                    if (!group.replacesPrice) {
                        chosenOption(group, selection)?.let { total += it.price }
                    }
                }
            }
        }

        return total
    }

    private fun basePrice(item: MenuItem, selection: Selection): Money {
        item.baseStep?.let { return item.price * selection.baseQty }

        val replacing = item.optionGroups.firstOrNull { it.replacesPrice }
        if (replacing != null) {
            // Large chips is R40, not R28 + R40.
            return chosenOption(replacing, selection)?.price ?: item.price
        }

        return item.price
    }

    private fun chosenOption(
        group: com.rimagwinya.app.domain.model.OptionGroup,
        selection: Selection,
    ) = selection.singles[group.id]?.let { id -> group.options.firstOrNull { it.id == id } }

    /**
     * On a build item the steppers **are** the quantity, so there is no outer
     * count. Two ways to say the same thing would only ever be an argument
     * about which one won.
     */
    fun quantity(item: MenuItem, selection: Selection): Int =
        if (item.isBuildItem) 1 else selection.quantity.coerceAtLeast(1)

    fun lineTotal(item: MenuItem, selection: Selection): Money =
        unitPrice(item, selection) * quantity(item, selection)

    /**
     * How much stock this takes off the shelf.
     *
     * Different from [quantity] for base-step items, and that difference is
     * the whole point: zero vetkoeks with a snoek filling is a real R10 order
     * that consumes no vetkoek.
     */
    fun unitsConsumed(item: MenuItem, selection: Selection): Int =
        if (item.baseStep != null) selection.baseQty else quantity(item, selection)

    /**
     * Sweets are priced at R0 with everything in a qty group, so an untouched
     * sheet costs nothing. The button stays disabled until something is
     * actually chosen.
     */
    fun canAddToCart(item: MenuItem, selection: Selection): Boolean =
        unitPrice(item, selection).isPositive

    /**
     * The cheapest thing that could be built, for the "from R…" label on the
     * menu. Mirrors the prototype's minPrice().
     */
    fun fromPrice(item: MenuItem): Money {
        item.optionGroups.firstOrNull { it.replacesPrice }?.let { group ->
            return group.options.minOfOrNull { it.price } ?: item.price
        }

        // A vetkoek starts at one vetkoek, not at zero.
        if (item.baseStep != null) return item.price

        // Sweets: the item itself is free, so the floor is the cheapest sweet.
        if (item.price.isZero) {
            val cheapest = item.optionGroups
                .filter { it.type == OptionGroupType.Qty }
                .flatMap { it.options }
                .minOfOrNull { it.price }
            if (cheapest != null) return cheapest
        }

        return item.price
    }

    /** True when the menu row should read "from R…" rather than a flat price. */
    fun hasVariablePrice(item: MenuItem): Boolean =
        item.isBuildItem || item.optionGroups.any { it.replacesPrice }

    /**
     * The human-readable description of a selection, frozen onto the order
     * when it is placed so staff read exactly what the student saw.
     *
     * Mirrors the prototype's sheetLabel() and the server's version:
     * "2 vetkoeks, 2 Polony, Cheese slice", "No vetkoek, Snoek".
     */
    fun label(item: MenuItem, selection: Selection): String {
        val parts = mutableListOf<String>()

        item.baseStep?.let { step ->
            parts += when (selection.baseQty) {
                0 -> "No ${step.singular}"
                1 -> "1 ${step.singular}"
                else -> "${selection.baseQty} ${step.label.lowercase()}"
            }
        }

        item.optionGroups.forEach { group ->
            when (group.type) {
                OptionGroupType.Qty ->
                    group.options.forEach { option ->
                        val count = selection.countOf(option.id)
                        if (count > 0) {
                            parts += if (count == 1) option.name else "$count ${option.name}"
                        }
                    }

                OptionGroupType.Single ->
                    chosenOption(group, selection)?.let { parts += it.name }
            }
        }

        return parts.joinToString(", ")
    }
}
