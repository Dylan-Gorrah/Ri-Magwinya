package com.rimagwinya.app.domain.model

import com.rimagwinya.app.core.money.Money

/**
 * A live change to the menu, as it arrives from the realtime socket.
 */
sealed interface MenuChange {

    /**
     * One row changed. Only the fields a student can see move mid-break —
     * stock, availability and price — are carried; null means "not in the
     * payload, keep what you have".
     */
    data class Updated(
        val id: String,
        val stockQuantity: Int?,
        val isAvailable: Boolean?,
        val price: Money?,
    ) : MenuChange

    /**
     * Fetch the whole menu again, quietly. Sent when an item is added or
     * removed (the payload has no option groups to build it from) and each
     * time the socket (re)connects, since changes made while it was down
     * were never delivered.
     */
    data object Reload : MenuChange
}

/** The list with [change] applied to the one item it names. */
fun List<MenuItem>.applying(change: MenuChange.Updated): List<MenuItem> = map { item ->
    if (item.id != change.id) {
        item
    } else {
        item.copy(
            stockQuantity = change.stockQuantity ?: item.stockQuantity,
            isAvailable = change.isAvailable ?: item.isAvailable,
            price = change.price ?: item.price,
        )
    }
}
