package com.rimagwinya.app.feature.staff

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.core.network.NetworkModule
import com.rimagwinya.app.data.remote.SalesSummaryDto
import com.rimagwinya.app.data.remote.slugOf
import com.rimagwinya.app.data.remote.toDomain
import com.rimagwinya.app.data.remote.toJson
import com.rimagwinya.app.domain.model.ItemDraft
import com.rimagwinya.app.domain.model.MenuCategory
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.OrderStatus
import com.rimagwinya.app.domain.model.PaymentMethod
import com.rimagwinya.app.domain.model.Temperature
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** The staff side: the queue's next step, the editor, sales and the CSV. */
class StaffRulesTest {

    private fun order(
        id: String = "o",
        status: OrderStatus = OrderStatus.Ready,
        slotEnd: LocalTime? = LocalTime.of(11, 20),
    ) = Order(
        id = id, number = 1, studentId = "s", code = "7306", slotId = "sl",
        total = Money.ofRands(30), status = status, paymentMethod = PaymentMethod.Wallet,
        placedAt = Instant.parse("2026-09-22T07:41:00Z"), slotEnd = slotEnd,
    )

    // --- Queue ----------------------------------------------------------------

    @Test
    fun `each status has one next step`() {
        assertEquals(OrderStatus.Preparing, QueueViewModel.nextStatus(OrderStatus.Placed))
        assertEquals(OrderStatus.Ready, QueueViewModel.nextStatus(OrderStatus.Preparing))
        assertEquals(OrderStatus.Collected, QueueViewModel.nextStatus(OrderStatus.Ready))
        assertNull(QueueViewModel.nextStatus(OrderStatus.Collected))
        assertNull(QueueViewModel.nextStatus(OrderStatus.Cancelled))
    }

    @Test
    fun `no-show only on a ready order whose break has ended`() {
        val state = QueueUiState(now = LocalTime.of(11, 25))
        assertTrue(state.canMarkNoShow(order(status = OrderStatus.Ready)))
        assertFalse("still preparing", state.canMarkNoShow(order(status = OrderStatus.Preparing)))
        assertFalse(
            "break still running",
            state.copy(now = LocalTime.of(11, 10)).canMarkNoShow(order(status = OrderStatus.Ready)),
        )
    }

    @Test
    fun `filters and counters split the queue`() {
        val state = QueueUiState(
            orders = listOf(
                order("a", OrderStatus.Placed),
                order("b", OrderStatus.Placed),
                order("c", OrderStatus.Preparing),
                order("d", OrderStatus.Ready),
            ),
            filter = QueueFilter.New,
        )
        assertEquals(2, state.newCount)
        assertEquals(1, state.preparingCount)
        assertEquals(1, state.readyCount)
        assertEquals(2, state.visible.size)
        assertEquals(4, state.copy(filter = QueueFilter.All).visible.size)
    }

    // --- Item editor ----------------------------------------------------------

    @Test
    fun `prices are read the way people type them`() {
        assertEquals(1600L, EditorForm.parseRands("16"))
        assertEquals(1650L, EditorForm.parseRands("16.5"))
        assertEquals(1650L, EditorForm.parseRands("16,50"))
        assertEquals(1650L, EditorForm.parseRands("R16.50"))
        assertEquals(0L, EditorForm.parseRands("0"))
        assertNull(EditorForm.parseRands(""))
        assertNull(EditorForm.parseRands("abc"))
        assertNull("three decimals is not a price", EditorForm.parseRands("16.505"))
    }

    @Test
    fun `the form refuses to save what the database would reject`() {
        assertTrue(EditorForm(name = " ", price = "16").validate().isFailure)
        assertTrue(EditorForm(name = "Pie", price = "free").validate().isFailure)
        assertTrue(EditorForm(name = "Pie", price = "16", reorderLevel = "-2").validate().isFailure)
        assertTrue(EditorForm(name = "Pie", price = "16").validate().isSuccess)
    }

    @Test
    fun `slugs are made from the name`() {
        assertEquals("sparletta-creme-soda-440ml", slugOf("Sparletta Creme Soda 440ml"))
        assertEquals("item", slugOf("!!!"))
    }

    private val draft = ItemDraft(
        name = "Steak pie", description = null, category = MenuCategory.Meals,
        price = Money.ofCents(1850), iconKey = "wors", openingStock = 12,
        reorderLevel = 4, temperature = Temperature.Hot, isAvailable = true,
    )

    @Test
    fun `a new item carries its slug and opening stock`() {
        val json = draft.toJson(isNew = true)
        assertEquals("steak-pie", json["slug"]!!.jsonPrimitive.content)
        assertEquals(12, json["stock_quantity"]!!.jsonPrimitive.content.toInt())
        assertEquals(18.5, json["price"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals("hot", json["temperature_tag"]!!.jsonPrimitive.content)
    }

    @Test
    fun `an edit never touches stock, and can clear a field`() {
        val json = draft.copy(temperature = null).toJson(isNew = false)
        assertFalse("stock moves through adjust_stock only", "stock_quantity" in json)
        assertFalse("slug is fixed once created", "slug" in json)
        // Explicit null, so "Either" actually clears the old value.
        assertEquals(JsonNull, json["temperature_tag"])
        assertEquals(JsonNull, json["description"])
    }

    // --- Sales ----------------------------------------------------------------

    private val summary = NetworkModule.json().decodeFromString<SalesSummaryDto>(
        """{"from":"2026-09-22","to":"2026-09-22","revenue":67.00,"order_count":1,
        "average_order":67.00,"orders_by_hour":[{"hour":9,"orders":1},{"hour":11,"orders":3}],
        "top_items":[{"item_name":"Vetkoek","quantity":2,"revenue":27.00},
                     {"item_name":"Fried chips","quantity":1,"revenue":40.00}]}"""
    ).toDomain()

    @Test
    fun `the chart has no gaps and knows its peak`() {
        assertEquals(SalesSummary_TRADING_HOURS, summary.byHour.map { it.hour })
        assertEquals(0, summary.byHour.first { it.hour == 10 }.orders)
        assertEquals(11, summary.peak!!.hour)
        assertEquals("11:00", summary.peak!!.label)
    }

    @Test
    fun `money comes back as cents`() {
        assertEquals(Money.ofRands(67), summary.revenue)
        assertEquals(Money.ofRands(27), summary.topItems.first().revenue)
    }

    @Test
    fun `the CSV has the totals, the hours and the items`() {
        val csv = SalesViewModel.csv(LocalDate.of(2026, 9, 22), summary)
        assertTrue(csv.startsWith("Ri-magwinya sales,2026-09-22"))
        assertTrue("Revenue,67.00" in csv)
        assertTrue("Collected orders,1" in csv)
        assertTrue("11:00,3" in csv)
        assertTrue("Vetkoek,2,27.00" in csv)
        assertFalse("amounts must sum as numbers", "R67" in csv)
    }

    @Test
    fun `an item name with a comma stays one cell`() {
        val tricky = summary.copy(
            topItems = listOf(com.rimagwinya.app.domain.model.TopItem("Chips, large", 1, Money.ofRands(40)))
        )
        assertTrue("\"Chips, large\",1,40.00" in SalesViewModel.csv(LocalDate.of(2026, 9, 22), tricky))
    }

    private companion object {
        val SalesSummary_TRADING_HOURS = (7..16).toList()
    }
}
