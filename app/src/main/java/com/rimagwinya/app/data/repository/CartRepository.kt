package com.rimagwinya.app.data.repository

import com.rimagwinya.app.core.database.CartDao
import com.rimagwinya.app.core.database.CartLineEntity
import com.rimagwinya.app.domain.model.Cart
import com.rimagwinya.app.domain.model.CartLine
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.pricing.PriceCalculator
import com.rimagwinya.app.domain.pricing.Selection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The cart, kept in Room so it survives the app being killed halfway
 * through an order — which on a cheap phone in a crowded break is not rare.
 *
 * A line stores the item id and the [Selection] as JSON: choices, never
 * prices. The prices are worked out again from the cached menu, so a price
 * change between adding and ordering shows up rather than being remembered
 * wrongly, and the server prices it once more when the order is placed.
 */
@Singleton
class CartRepository @Inject constructor(
    private val dao: CartDao,
    private val menu: MenuRepository,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val _slotId = MutableStateFlow<String?>(null)

    /** The collection break chosen on the cart screen, carried to checkout. */
    val slotId: StateFlow<String?> = _slotId.asStateFlow()

    val cart: StateFlow<Cart> = combine(dao.observe(), menu.observe()) { rows, items ->
        val byId = items.associateBy { it.id }
        Cart(
            lines = rows.mapNotNull { row ->
                // An item removed from the menu drops out of the cart with it.
                val item = byId[row.itemId] ?: return@mapNotNull null
                CartLine(item, json.decodeFromString<Selection>(row.selection))
            }
        )
    }.stateIn(scope, SharingStarted.Eagerly, Cart())

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
        val existing = cart.value.lines.firstOrNull { it.item.id == item.id && it.selection == selection }

        scope.launch {
            if (existing != null && existing.hasOuterQuantity) {
                // The key includes the quantity, so the old row has to go
                // with it — otherwise "2 Cokes" and "4 Cokes" both sit there.
                write(
                    item = item,
                    selection = existing.selection.copy(
                        quantity = existing.selection.quantity + selection.quantity
                    ),
                    replacing = existing.key,
                )
            } else if (existing == null) {
                write(item, selection)
            }
            // A build item cannot be "two of" — its steppers are the count —
            // so adding the identical build again changes nothing.
        }
    }

    /** Sets a plain item's outer quantity. Zero removes the line. */
    fun setQuantity(key: String, quantity: Int) {
        val line = cart.value.lines.firstOrNull { it.key == key } ?: return
        if (!line.hasOuterQuantity) return
        scope.launch {
            if (quantity <= 0) dao.remove(key) else write(line.item, line.selection.copy(quantity = quantity), key)
        }
    }

    /** Replaces a line's whole selection, for editing a build from the cart. */
    fun replace(key: String, selection: Selection) {
        val line = cart.value.lines.firstOrNull { it.key == key } ?: return
        scope.launch {
            dao.remove(key)
            write(line.item, selection)
        }
    }

    fun remove(key: String) {
        scope.launch { dao.remove(key) }
    }

    fun clear() {
        scope.launch { dao.clear() }
    }

    private suspend fun write(item: MenuItem, selection: Selection, replacing: String? = null) {
        val line = CartLine(item, selection)
        if (replacing != null && replacing != line.key) dao.remove(replacing)
        dao.upsert(
            CartLineEntity(
                key = line.key,
                itemId = item.id,
                selection = json.encodeToString(selection),
                addedAt = System.currentTimeMillis(),
            )
        )
    }
}
