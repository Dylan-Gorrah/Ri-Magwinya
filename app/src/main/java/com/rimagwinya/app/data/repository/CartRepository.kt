package com.rimagwinya.app.data.repository

import com.rimagwinya.app.domain.model.Cart
import com.rimagwinya.app.domain.model.CartLine
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.pricing.PriceCalculator
import com.rimagwinya.app.domain.pricing.Selection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The cart.
 *
 * In memory for now. Phase 10 backs it with Room so it survives the app
 * being killed, but the shape the screens see — a `StateFlow<Cart>` and a
 * handful of operations — does not change, so nothing above this has to
 * know the difference.
 */
@Singleton
class CartRepository @Inject constructor() {

    private val _cart = MutableStateFlow(Cart())
    val cart: StateFlow<Cart> = _cart.asStateFlow()

    /** The collection break chosen on the cart screen, carried to checkout. */
    private val _slotId = MutableStateFlow<String?>(null)
    val slotId: StateFlow<String?> = _slotId.asStateFlow()

    fun selectSlot(id: String?) {
        _slotId.value = id
    }

    /**
     * Adds a configured item.
     *
     * A line is identified by the item **and** the exact selection, so the
     * same build merges and a different one becomes its own line.
     */
    fun add(item: MenuItem, selection: Selection) {
        if (!PriceCalculator.canAddToCart(item, selection)) return

        _cart.update { cart ->
            val existing = cart.lines.indexOfFirst {
                it.item.id == item.id && it.selection == selection
            }

            val lines = if (existing >= 0) {
                cart.lines.toMutableList().also { list ->
                    val line = list[existing]
                    list[existing] = if (line.hasOuterQuantity) {
                        line.copy(
                            selection = line.selection.copy(
                                quantity = line.selection.quantity + selection.quantity
                            )
                        )
                    } else {
                        // A build item cannot be "two of" — adding the same
                        // build again is a second identical line, so the
                        // steppers stay the single source of the count.
                        line
                    }
                }
            } else {
                cart.lines + CartLine(item, selection)
            }

            cart.copy(lines = lines)
        }
    }

    /** Sets a plain item's outer quantity. Zero removes the line. */
    fun setQuantity(key: String, quantity: Int) {
        _cart.update { cart ->
            if (quantity <= 0) return@update cart.copy(lines = cart.lines.filterNot { it.key == key })

            cart.copy(
                lines = cart.lines.map { line ->
                    if (line.key == key && line.hasOuterQuantity) {
                        line.copy(selection = line.selection.copy(quantity = quantity))
                    } else {
                        line
                    }
                }
            )
        }
    }

    /** Replaces a line's whole selection, for editing a build from the cart. */
    fun replace(key: String, selection: Selection) {
        _cart.update { cart ->
            cart.copy(
                lines = cart.lines.map { line ->
                    if (line.key == key) line.copy(selection = selection) else line
                }
            )
        }
    }

    fun remove(key: String) {
        _cart.update { cart -> cart.copy(lines = cart.lines.filterNot { it.key == key }) }
    }

    fun clear() {
        _cart.value = Cart()
    }
}
