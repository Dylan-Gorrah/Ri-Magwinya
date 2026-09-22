package com.rimagwinya.app.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The cache.
 *
 * Everything here is a copy of something the server owns, except
 * [PendingActionEntity] and [CartLineEntity], which are the phone's own.
 * Nothing in here is ever the authority on a price or a balance — it is
 * what the app shows while it cannot ask.
 */
@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val name: String,
    val description: String?,
    val category: String,
    val priceCents: Long,
    val iconKey: String,
    val imageUrl: String?,
    val stockQuantity: Int,
    val reorderLevel: Int,
    val isAvailable: Boolean,
    val temperature: String?,
    val baseStepLabel: String?,
    val baseStepSingular: String?,
    val baseStepMin: Int?,
    val sortOrder: Int,
)

@Entity(tableName = "option_groups", indices = [Index("itemId")])
data class OptionGroupEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val key: String,
    val label: String,
    val type: String,
    val replacesPrice: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "options", indices = [Index("groupId")])
data class OptionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val key: String,
    val name: String,
    val priceCents: Long,
    val isDefault: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "slots")
data class SlotEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startsAt: String,
    val endsAt: String,
    val capacity: Int,
    val ordersTaken: Int,
    val serviceDate: String,
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val number: Long,
    val studentId: String,
    val code: String,
    val slotId: String,
    val totalCents: Long,
    val status: String,
    val paymentMethod: String,
    val placedAt: String,
    val preparedAt: String?,
    val readyAt: String?,
    val completedAt: String?,
    val cancelledAt: String?,
    val slotName: String?,
    val slotStart: String?,
    val slotEnd: String?,
    val studentName: String?,
)

@Entity(tableName = "order_lines", indices = [Index("orderId")])
data class OrderLineEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val itemId: String?,
    val name: String,
    val optionsLabel: String?,
    val quantity: Int,
    val unitPriceCents: Long,
    val subtotalCents: Long,
)

/** The cart, so it survives the app being killed mid-order. */
@Entity(tableName = "cart_lines")
data class CartLineEntity(
    @PrimaryKey val key: String,
    val itemId: String,
    /** The Selection, as JSON. Choices only — never a price. */
    val selection: String,
    val addedAt: Long,
)

/**
 * Something the phone did while it had no signal, waiting to be sent.
 *
 * The payload is the request body, so replaying is "send this again" rather
 * than "work out what they meant". An order carries its `client_ref`, which
 * is what stops a replay becoming a second order.
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val payload: String,
    /** Shown while it waits: "Vetkoek, Coke · R27.00". */
    val summary: String,
    val createdAt: Long,
    val attempts: Int = 0,
    /** Set when the server refused for good; the user is told why. */
    val failureCode: String? = null,
    val failureDetail: String? = null,
) {
    companion object {
        const val PLACE_ORDER = "place_order"
        const val ADJUST_STOCK = "adjust_stock"
    }
}

/** One row, replaced on every fetch. Phase 14 reads it when offline. */
@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey val id: Int = 1,
    val temperatureC: Double,
    val weatherCode: Int,
    val rainProbabilityToday: Int,
    val tomorrowMaxC: Double,
    val tomorrowMinC: Double,
    val tomorrowRainProbability: Int,
    val tomorrowCode: Int,
    val fetchedAt: Long,
)
