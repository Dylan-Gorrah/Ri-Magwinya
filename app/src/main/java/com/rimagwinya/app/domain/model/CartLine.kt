package com.rimagwinya.app.domain.model

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.pricing.PriceCalculator
import com.rimagwinya.app.domain.pricing.Selection

/**
 * One line in the cart: an item plus the exact thing that was chosen.
 *
 * Identity is the item **and** the selection, which is why
 * "Vetkoek · 2 Polony" and "Vetkoek · Snoek" stay separate lines. Adding the
 * same build twice bumps the existing line instead.
 */
data class CartLine(
    val item: MenuItem,
    val selection: Selection,
) {
    /** Stable across recomposition and safe as a LazyColumn key. */
    val key: String get() = "${item.id}#${selection.hashCode()}"

    val unitPrice: Money get() = PriceCalculator.unitPrice(item, selection)
    val quantity: Int get() = PriceCalculator.quantity(item, selection)
    val total: Money get() = PriceCalculator.lineTotal(item, selection)
    val unitsConsumed: Int get() = PriceCalculator.unitsConsumed(item, selection)

    /** What staff read at the counter. Frozen onto the order when placed. */
    val optionsLabel: String get() = PriceCalculator.label(item, selection)

    /**
     * Build items carry their count in the steppers, so the cart shows their
     * quantity stepper only for plain items.
     */
    val hasOuterQuantity: Boolean get() = !item.isBuildItem
}

/** The cart as a whole. */
data class Cart(
    val lines: List<CartLine> = emptyList(),
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    val itemCount: Int get() = lines.sumOf { it.quantity }

    /**
     * Display only. The server recomputes every line when the order is
     * placed and charges that. They agree because both run the same formula,
     * but this number never decides anything.
     */
    val total: Money get() = Money(lines.sumOf { it.total.cents })
}
