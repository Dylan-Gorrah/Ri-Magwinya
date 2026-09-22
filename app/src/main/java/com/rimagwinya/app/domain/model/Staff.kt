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

/** A student found by number on the top-up screen. */
data class StudentAccount(
    val id: String,
    val fullName: String,
    val studentNumber: String,
    val balance: Money,
)

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
