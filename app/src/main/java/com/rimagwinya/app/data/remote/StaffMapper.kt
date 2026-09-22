package com.rimagwinya.app.data.remote

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.HourCount
import com.rimagwinya.app.domain.model.ItemDraft
import com.rimagwinya.app.domain.model.SalesSummary
import com.rimagwinya.app.domain.model.StudentAccount
import com.rimagwinya.app.domain.model.Temperature
import com.rimagwinya.app.domain.model.TopItem
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun SalesSummaryDto.toDomain(): SalesSummary {
    val counts = ordersByHour.associate { it.hour to it.orders }
    // Hours outside the trading day still count if orders happened then.
    val hours = (SalesSummary.TRADING_HOURS + counts.keys).toSortedSet()
    return SalesSummary(
        revenue = Money.fromDecimal(revenue),
        orderCount = orderCount,
        average = Money.fromDecimal(averageOrder),
        byHour = hours.map { HourCount(it, counts[it] ?: 0) },
        topItems = topItems.map { TopItem(it.itemName, it.quantity, Money.fromDecimal(it.revenue)) },
    )
}

fun ProfileDto.toStudentAccount(): StudentAccount = StudentAccount(
    id = id,
    fullName = fullName,
    studentNumber = studentNumber.orEmpty(),
    balance = Money.fromDecimal(walletBalance),
)

/** "Creme Soda 440ml" -> "creme-soda-440ml". */
fun slugOf(name: String): String =
    name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "item" }

/**
 * An editor draft as the JSON PostgREST expects. Built by hand rather than
 * serialised so that clearing a field (no description, either temperature)
 * sends an explicit null instead of leaving the old value in place.
 *
 * Stock goes only on a new item: after that it moves through adjust_stock,
 * as a change, so it cannot overwrite a sale.
 */
fun ItemDraft.toJson(isNew: Boolean, slugSuffix: String = ""): JsonObject = buildJsonObject {
    if (isNew) {
        put("slug", slugOf(name) + slugSuffix)
        put("stock_quantity", openingStock)
    }
    put("name", name.trim())
    put("description", description?.trim()?.takeIf { it.isNotEmpty() }?.let(::JsonPrimitive) ?: JsonNull)
    put("category", category.serverName)
    // numeric(10,2) on the wire; cents everywhere else.
    put("price", JsonPrimitive(price.cents / 100.0))
    put("icon_key", iconKey)
    put("reorder_level", reorderLevel)
    put("is_available", isAvailable)
    put(
        "temperature_tag",
        when (temperature) {
            Temperature.Hot -> JsonPrimitive("hot")
            Temperature.Cold -> JsonPrimitive("cold")
            null -> JsonNull
        },
    )
}
