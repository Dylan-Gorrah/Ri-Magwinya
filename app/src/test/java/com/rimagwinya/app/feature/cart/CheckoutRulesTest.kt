package com.rimagwinya.app.feature.cart

import com.rimagwinya.app.R
import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.util.UiText
import com.rimagwinya.app.data.remote.toBody
import com.rimagwinya.app.domain.model.Cart
import com.rimagwinya.app.domain.model.CartLine
import com.rimagwinya.app.domain.model.PaymentMethod
import com.rimagwinya.app.domain.model.Profile
import com.rimagwinya.app.domain.model.Slot
import com.rimagwinya.app.domain.model.UserRole
import com.rimagwinya.app.domain.pricing.MenuFixtures
import com.rimagwinya.app.domain.pricing.Selection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * The checkout's rules: which breaks are offered, when each payment method
 * is available, what goes to the server, and what each refusal says.
 */
class CheckoutRulesTest {

    private fun slot(name: String, start: String, end: String, taken: Int = 0, cap: Int = 40) = Slot(
        id = name, name = name,
        startsAt = LocalTime.parse(start), endsAt = LocalTime.parse(end),
        capacity = cap, ordersTaken = taken, serviceDate = LocalDate.of(2026, 9, 22),
    )

    private val slots = listOf(
        slot("Afternoon", "14:30", "14:50", cap = 25),
        slot("First break", "09:40", "10:00"),
        slot("Second break", "11:00", "11:20"),
    )

    private fun open(at: String) = CartViewModel.openSlots(slots, LocalTime.parse(at)).map { it.name }

    @Test
    fun `early morning offers all three, in time order`() {
        assertEquals(listOf("First break", "Second break", "Afternoon"), open("07:30"))
    }

    @Test
    fun `a break that has ended is hidden`() {
        assertEquals(listOf("Second break", "Afternoon"), open("10:30"))
    }

    @Test
    fun `a break under way is offered until five minutes before it ends`() {
        assertTrue("Second break" in open("11:05"))
        assertTrue("Second break" in open("11:14"))
        assertFalse("Second break" in open("11:15"))
    }

    @Test
    fun `after the last break there is nothing`() {
        assertEquals(emptyList<String>(), open("14:46"))
    }

    // --- Payment ------------------------------------------------------------

    private fun profile(balanceRands: Int, noShows: Int = 0) = Profile(
        id = "p", fullName = "Test Student", email = "t@x.co", studentNumber = "TEST001",
        role = UserRole.Student, walletBalance = Money.ofRands(balanceRands),
        noShowCount = noShows, language = "en",
    )

    private val cokes = Cart(listOf(CartLine(MenuFixtures.coke, Selection(quantity = 3)))) // R48

    private fun state(
        balance: Int = 100,
        noShows: Int = 0,
        toppedUp: Boolean? = true,
        method: PaymentMethod = PaymentMethod.Wallet,
    ) = CheckoutUiState(
        cart = cokes,
        slot = slots[1],
        profile = profile(balance, noShows),
        hasToppedUp = toppedUp,
        method = method,
    )

    @Test
    fun `enough in the wallet can place`() {
        val s = state(balance = 48)
        assertNull(s.walletShortBy)
        assertTrue(s.canPlace)
    }

    @Test
    fun `short by fifteen rand blocks the wallet and says how much`() {
        val s = state(balance = 33)
        assertEquals(Money.ofRands(15), s.walletShortBy)
        assertFalse(s.canPlace)
    }

    @Test
    fun `counter needs a top-up first`() {
        val s = state(toppedUp = false, method = PaymentMethod.Counter)
        assertEquals(CounterBlock.NoTopUp, s.counterBlock)
        assertFalse(s.canPlace)
    }

    @Test
    fun `counter stays off while it is still unknown`() {
        assertFalse(state(toppedUp = null, method = PaymentMethod.Counter).canPlace)
    }

    @Test
    fun `two no-shows switch the counter off even after a top-up`() {
        val s = state(noShows = 2, method = PaymentMethod.Counter)
        assertEquals(CounterBlock.NoShows, s.counterBlock)
        assertFalse(s.canPlace)
    }

    @Test
    fun `counter with a top-up and one no-show is fine, even with an empty wallet`() {
        val s = state(balance = 0, noShows = 1, method = PaymentMethod.Counter)
        assertNull(s.counterBlock)
        assertTrue(s.canPlace)
    }

    @Test
    fun `nothing can be placed without a slot`() {
        assertFalse(state().copy(slot = null).canPlace)
    }

    // --- The wire -------------------------------------------------------------

    @Test
    fun `a fillings-only vetkoek sends base_qty 0 and no quantity`() {
        val sel = Selection(baseQty = 0).withCount(MenuFixtures.optionId("snoek"), 1)
        val body = CartLine(MenuFixtures.vetkoek, sel).toBody()

        assertEquals(0, body.baseQty)
        assertNull(body.quantity)
        assertEquals(listOf("opt-snoek" to 1), body.options.map { it.optionId to it.count })
    }

    @Test
    fun `large chips send the size with no count and the outer quantity`() {
        val sel = Selection(quantity = 2).withSingle("grp-size", MenuFixtures.optionId("l"))
        val body = CartLine(MenuFixtures.chips, sel).toBody()

        assertNull(body.baseQty)
        assertEquals(2, body.quantity)
        assertEquals(listOf("opt-l" to null), body.options.map { it.optionId to it.count })
    }

    @Test
    fun `a plain item sends only its quantity`() {
        val body = CartLine(MenuFixtures.coke, Selection(quantity = 3)).toBody()
        assertEquals(3, body.quantity)
        assertTrue(body.options.isEmpty())
    }

    // --- Refusals ---------------------------------------------------------------

    private fun conflict(code: ConflictCode, detail: String? = null) =
        CheckoutViewModel.messageFor(ApiError.Conflict(code, detail))

    @Test
    fun `sold out names the item`() {
        assertEquals(
            UiText(R.string.order_error_out_of_stock, "Score Energy 500ml"),
            conflict(ConflictCode.OUT_OF_STOCK, "Score Energy 500ml"),
        )
    }

    @Test
    fun `each counter reason gets its own sentence`() {
        assertEquals(UiText(R.string.order_error_counter_no_topup), conflict(ConflictCode.COUNTER_BLOCKED, "NO_TOPUP_YET"))
        assertEquals(
            UiText(R.string.order_error_counter_no_shows),
            conflict(ConflictCode.COUNTER_BLOCKED, "TOO_MANY_NO_SHOWS"),
        )
    }

    @Test
    fun `full and closed slots, and money, each explain themselves`() {
        assertEquals(UiText(R.string.order_error_slot_full), conflict(ConflictCode.SLOT_FULL))
        assertEquals(UiText(R.string.order_error_slot_closed), conflict(ConflictCode.SLOT_NOT_TODAY))
        assertEquals(UiText(R.string.order_error_funds), conflict(ConflictCode.INSUFFICIENT_FUNDS))
    }

    @Test
    fun `no signal is not a refusal`() {
        assertEquals(UiText(R.string.error_network), CheckoutViewModel.messageFor(ApiError.Offline()))
    }
}
