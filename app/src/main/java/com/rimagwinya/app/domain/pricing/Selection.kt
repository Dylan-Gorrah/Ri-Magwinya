package com.rimagwinya.app.domain.pricing

import com.rimagwinya.app.domain.model.MenuItem

/**
 * What somebody has chosen on an item sheet, before it becomes a cart line.
 *
 * Deliberately holds **choices, not prices**. The price is derived from it by
 * [PriceCalculator], and the server derives it again from the same choices
 * when the order is placed. Nothing here can be tampered with into a
 * discount, because there is no money in it.
 */
data class Selection(
    /** The count of the item itself. Only meaningful for base-step items. */
    val baseQty: Int = 0,
    /** Chosen option id per single-select group id. */
    val singles: Map<String, String> = emptyMap(),
    /** Count per option id, for qty groups. Zeroes are not kept. */
    val counts: Map<String, Int> = emptyMap(),
    /** The outer stepper. Always 1 for build items. */
    val quantity: Int = 1,
) {
    fun withSingle(groupId: String, optionId: String) =
        copy(singles = singles + (groupId to optionId))

    fun withCount(optionId: String, count: Int) = copy(
        counts = if (count <= 0) counts - optionId else counts + (optionId to count)
    )

    fun countOf(optionId: String): Int = counts[optionId] ?: 0

    companion object {
        /**
         * How a sheet opens: the base step at its start value and every
         * single-select group on its default, so the common case is already
         * chosen and the total is never zero for no reason.
         */
        fun initial(item: MenuItem): Selection {
            val defaults = item.optionGroups
                .filter { it.type == com.rimagwinya.app.domain.model.OptionGroupType.Single }
                .mapNotNull { group ->
                    val option = group.options.firstOrNull { it.isDefault }
                        ?: group.options.firstOrNull().takeIf { group.replacesPrice }
                    option?.let { group.id to it.id }
                }
                .toMap()

            return Selection(
                baseQty = item.baseStep?.start ?: 0,
                singles = defaults,
                quantity = 1,
            )
        }
    }
}
