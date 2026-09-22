package com.rimagwinya.app.data.repository

import com.rimagwinya.app.domain.pricing.MenuFixtures.coke
import com.rimagwinya.app.domain.pricing.MenuFixtures.optionId
import com.rimagwinya.app.domain.pricing.MenuFixtures.sweets
import com.rimagwinya.app.domain.pricing.MenuFixtures.vetkoek
import com.rimagwinya.app.domain.pricing.Selection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartRepositoryTest {

    private val cart = CartRepository()

    @Test
    fun `starts empty`() {
        assertTrue(cart.cart.value.isEmpty)
        assertEquals("R0.00", cart.cart.value.total.format())
    }

    @Test
    fun `different builds of the same item are separate lines`() {
        val polony = Selection(baseQty = 1).withCount(optionId("polony"), 2)
        val snoek = Selection(baseQty = 0).withCount(optionId("snoek"), 1)

        cart.add(vetkoek, polony)
        cart.add(vetkoek, snoek)

        assertEquals(2, cart.cart.value.lines.size)
        // R3 + R6 = R9, and R10
        assertEquals("R19.00", cart.cart.value.total.format())
    }

    @Test
    fun `the same plain item merges and adds up`() {
        cart.add(coke, Selection(quantity = 2))
        cart.add(coke, Selection(quantity = 2))

        assertEquals(1, cart.cart.value.lines.size)
        assertEquals(4, cart.cart.value.lines.single().quantity)
        assertEquals("R64.00", cart.cart.value.total.format())
    }

    @Test
    fun `an empty selection is refused`() {
        // An untouched sweets sheet costs nothing and must not be orderable.
        cart.add(sweets, Selection())
        assertTrue(cart.cart.value.isEmpty)
    }

    @Test
    fun `stepping a line to zero removes it`() {
        cart.add(coke, Selection(quantity = 2))
        val key = cart.cart.value.lines.single().key

        cart.setQuantity(key, 0)
        assertTrue(cart.cart.value.isEmpty)
    }

    @Test
    fun `a build item has no outer quantity to step`() {
        val build = Selection(baseQty = 2).withCount(optionId("polony"), 2)
        cart.add(vetkoek, build)
        val line = cart.cart.value.lines.single()

        assertEquals(false, line.hasOuterQuantity)
        // The steppers are the quantity, so this is ignored rather than
        // silently creating a second way to say the same thing.
        cart.setQuantity(line.key, 5)
        assertEquals(1, cart.cart.value.lines.single().quantity)
        assertEquals("R12.00", cart.cart.value.total.format())
    }

    @Test
    fun `the cart total is the sum of its lines`() {
        cart.add(vetkoek, Selection(baseQty = 2)
            .withCount(optionId("polony"), 2)
            .withCount(optionId("cheese"), 1))
        cart.add(coke, Selection(quantity = 1))

        // R17.00 + R16.00
        assertEquals("R33.00", cart.cart.value.total.format())
        assertEquals(2, cart.cart.value.itemCount)
    }

    @Test
    fun `editing a line replaces its selection`() {
        cart.add(vetkoek, Selection(baseQty = 1))
        val key = cart.cart.value.lines.single().key

        cart.replace(key, Selection(baseQty = 3))
        assertEquals("R9.00", cart.cart.value.total.format())
    }

    @Test
    fun `clearing empties the cart`() {
        cart.add(coke, Selection(quantity = 3))
        cart.clear()
        assertTrue(cart.cart.value.isEmpty)
    }

    @Test
    fun `line labels carry through to the cart`() {
        cart.add(vetkoek, Selection(baseQty = 2)
            .withCount(optionId("polony"), 2)
            .withCount(optionId("cheese"), 1))

        assertEquals(
            "2 vetkoeks, 2 Polony, Cheese slice",
            cart.cart.value.lines.single().optionsLabel,
        )
    }
}
