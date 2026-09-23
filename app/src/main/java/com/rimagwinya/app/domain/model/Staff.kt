package com.rimagwinya.app.domain.model

import com.rimagwinya.app.core.money.Money

/** The day's sales, aggregated by the database. */
data class SalesSummary(
    val revenue: Money,
    val orderCount: Int,
    val average: Money,
    /** Every hour the tuckshop trades, zero-filled, so the chart has no gaps. */
    val byHour: List<HourCount>,
    val topItems: List<TopItem>,
) {
    /** The busiest hour, or null on a day with no sales. */
    val peak: HourCount? get() = byHour.filter { it.orders > 0 }.maxByOrNull { it.orders }

    companion object {
        /** 07:00 to 16:00 — the tuckshop's day. */
        val TRADING_HOURS = 7..16
    }
}

data class HourCount(val hour: Int, val orders: Int) {
    /** "10:00". */
    val label: String get() = "%02d:00".format(hour)
}

data class TopItem(val name: String, val quantity: Int, val revenue: Money)

/** A student found on the top-up screen, by number or by name. */
data class StudentAccount(
    val id: String,
    val fullName: String,
    val studentNumber: String,
    val balance: Money,
    val email: String = "",
    /** Optional; students add it on their profile whenever they like. */
    val phone: String? = null,
    val noShowCount: Int = 0,
)

/** One line of a student's wallet history, as staff see it. */
data class WalletEntry(
    /** Positive for a top-up or refund, negative for a payment. */
    val amount: Money,
    val type: WalletEntryType,
    val at: java.time.Instant,
)

enum class WalletEntryType {
    TopUp, Payment, Refund;

    companion object {
        fun from(raw: String): WalletEntryType = when (raw) {
            "topup" -> TopUp
            "refund" -> Refund
            else -> Payment
        }
    }
}

/** What the item editor produces. Money in cents, like everywhere else. */
data class ItemDraft(
    val name: String,
    val description: String?,
    val category: MenuCategory,
    val price: Money,
    val iconKey: String,
    val openingStock: Int,
    val reorderLevel: Int,
    val temperature: Temperature?,
    val isAvailable: Boolean,
)
