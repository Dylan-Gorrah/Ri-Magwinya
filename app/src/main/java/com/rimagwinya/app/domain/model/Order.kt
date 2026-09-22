package com.rimagwinya.app.domain.model

import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.money.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** One of the day's three collection breaks. */
data class Slot(
    val id: String,
    val name: String,
    val startsAt: LocalTime,
    val endsAt: LocalTime,
    val capacity: Int,
    val ordersTaken: Int,
    val serviceDate: LocalDate,
) {
    val isFull: Boolean get() = ordersTaken >= capacity

    /** "11:00–11:20". */
    val timeRange: String get() = "${startsAt.format(HHMM)}–${endsAt.format(HHMM)}"

    /**
     * Still taking orders at [now]: until [AppConfig.SLOT_CUTOFF_MINUTES]
     * before it ends, so a break that is under way can still be ordered for
     * while the kitchen has time.
     */
    fun isOpenAt(now: LocalTime): Boolean =
        now.isBefore(endsAt.minusMinutes(AppConfig.SLOT_CUTOFF_MINUTES))

    /** Has ended — used for the staff no-show action. */
    fun hasEndedAt(now: LocalTime): Boolean = !now.isBefore(endsAt)

    companion object {
        val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

enum class OrderStatus(val serverName: String) {
    Placed("placed"),
    Preparing("preparing"),
    Ready("ready"),
    Collected("collected"),
    Cancelled("cancelled"),
    NoShow("no_show");

    /** Still on its way to the counter. */
    val isActive: Boolean get() = this == Placed || this == Preparing || this == Ready

    companion object {
        fun from(raw: String): OrderStatus =
            entries.firstOrNull { it.serverName == raw } ?: Placed
    }
}

enum class PaymentMethod(val serverName: String) {
    Wallet("wallet"),
    Counter("counter");

    companion object {
        fun from(raw: String): PaymentMethod = if (raw == "counter") Counter else Wallet
    }
}

data class OrderLine(
    val id: String,
    val itemId: String?,
    val name: String,
    /** Frozen at placing time: what the student chose, as staff read it. */
    val optionsLabel: String?,
    val quantity: Int,
    val unitPrice: Money,
    val subtotal: Money,
)

data class Order(
    val id: String,
    /** The #1047 people see. */
    val number: Long,
    val studentId: String,
    val code: String,
    val slotId: String,
    val total: Money,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod,
    val placedAt: Instant,
    val preparedAt: Instant? = null,
    val readyAt: Instant? = null,
    val completedAt: Instant? = null,
    val cancelledAt: Instant? = null,
    val lines: List<OrderLine> = emptyList(),
    /** Embedded when the query asked for it. */
    val slotName: String? = null,
    val slotStart: LocalTime? = null,
    val slotEnd: LocalTime? = null,
    /** Staff queries embed who the order is for. */
    val studentName: String? = null,
) {
    /** A student may cancel only before the kitchen starts. */
    val canCancel: Boolean get() = status == OrderStatus.Placed

    val itemCount: Int get() = lines.sumOf { it.quantity }

    val slotTimeRange: String?
        get() = if (slotStart != null && slotEnd != null) {
            "${slotStart.format(Slot.HHMM)}–${slotEnd.format(Slot.HHMM)}"
        } else {
            null
        }
}

/** "09:41", in the tuckshop's time zone. */
fun Instant.clockTime(): String = atZone(AppConfig.zone).toLocalTime().format(Slot.HHMM)
