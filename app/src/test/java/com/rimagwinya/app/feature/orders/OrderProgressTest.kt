package com.rimagwinya.app.feature.orders

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.core.network.NetworkModule
import com.rimagwinya.app.data.remote.OrderDto
import com.rimagwinya.app.data.remote.toDomain
import com.rimagwinya.app.domain.model.OrderStatus
import com.rimagwinya.app.domain.model.PaymentMethod
import com.rimagwinya.app.domain.model.clockTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * Reading an order back from PostgREST, and turning its timestamps into the
 * progress rail the student watches.
 */
class OrderProgressTest {

    private val json = NetworkModule.json()

    private fun order(
        status: String,
        method: String = "wallet",
        prepared: String? = null,
        ready: String? = null,
        completed: String? = null,
    ) = json.decodeFromString<OrderDto>(
        """{"id":"o1","order_number":1047,"student_id":"s","collection_code":"7306",
        "slot_id":"sl","total_amount":67,"status":"$status","payment_method":"$method",
        "client_ref":"c","placed_at":"2026-09-22T07:41:05.123+00:00",
        "prepared_at":${prepared?.let { "\"$it\"" }},"ready_at":${ready?.let { "\"$it\"" }},
        "completed_at":${completed?.let { "\"$it\"" }},"cancelled_at":null,
        "order_items":[{"id":"a","item_id":"v","item_name":"Vetkoek","options_label":"No vetkoek, Snoek",
          "quantity":1,"unit_price":10,"units_consumed":0,"subtotal":10}],
        "collection_slots":{"name":"Second break","starts_at":"11:00:00","ends_at":"11:20:00"},
        "profiles":{"full_name":"Test Student","student_number":"TEST001"}}"""
    ).toDomain()

    @Test
    fun `an order reads back with its slot, student and lines`() {
        val o = order("placed")
        assertEquals(1047L, o.number)
        assertEquals(Money.ofRands(67), o.total)
        assertEquals("Second break", o.slotName)
        assertEquals(LocalTime.of(11, 0), o.slotStart)
        assertEquals("11:00–11:20", o.slotTimeRange)
        assertEquals("Test Student", o.studentName)
        assertEquals("No vetkoek, Snoek", o.lines.single().optionsLabel)
    }

    @Test
    fun `times show in Bloemfontein, not UTC`() {
        // 07:41 UTC is 09:41 at the tuckshop.
        assertEquals("09:41", order("placed").placedAt.clockTime())
    }

    @Test
    fun `a new wallet order is paid and waiting`() {
        val rail = railStages(order("placed"))
        assertEquals(RailStage.Kind.Paid, rail[1].kind)
        assertEquals(RailStage.State.Current, rail[1].state)
        assertEquals(RailStage.State.Pending, rail[2].state)
        assertTrue(order("placed").canCancel)
    }

    @Test
    fun `a counter order says it is paid on collection`() {
        val rail = railStages(order("placed", method = "counter"))
        assertEquals(RailStage.Kind.PayOnCollection, rail[1].kind)
        assertEquals(null, rail[1].time)
        assertEquals(PaymentMethod.Counter, order("placed", method = "counter").paymentMethod)
    }

    @Test
    fun `preparing moves the ring and stops the cancel`() {
        val o = order("preparing", prepared = "2026-09-22T07:45:00+00:00")
        val rail = railStages(o)
        assertEquals(RailStage.State.Done, rail[1].state)
        assertEquals(RailStage.State.Current, rail[2].state)
        assertEquals("09:45", rail[2].time)
        assertFalse(o.canCancel)
    }

    @Test
    fun `ready has the ring on ready`() {
        val o = order("ready", prepared = "2026-09-22T07:45:00+00:00", ready = "2026-09-22T07:52:00+00:00")
        assertEquals(RailStage.State.Current, railStages(o)[3].state)
        assertEquals(OrderStatus.Ready, o.status)
    }

    @Test
    fun `collected is all done with no current step`() {
        val o = order(
            "collected",
            prepared = "2026-09-22T07:45:00+00:00",
            ready = "2026-09-22T07:52:00+00:00",
            completed = "2026-09-22T09:05:00+00:00",
        )
        val rail = railStages(o)
        assertTrue(rail.all { it.state == RailStage.State.Done })
        assertFalse(o.status.isActive)
    }

    @Test
    fun `history splits live from finished`() {
        val state = OrdersUiState(
            orders = listOf(order("ready"), order("collected"), order("placed"), order("cancelled"), order("no_show")),
            loading = false,
        )
        assertEquals(2, state.inProgress.size)
        assertEquals(3, state.past.size)
    }
}
