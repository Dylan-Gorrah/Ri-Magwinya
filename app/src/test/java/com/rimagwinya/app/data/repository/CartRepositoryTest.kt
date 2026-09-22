package com.rimagwinya.app.data.repository

import com.rimagwinya.app.core.database.CartDao
import com.rimagwinya.app.core.database.CartLineEntity
import com.rimagwinya.app.core.network.NetworkModule
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.pricing.MenuFixtures.coke
import com.rimagwinya.app.domain.pricing.MenuFixtures.optionId
import com.rimagwinya.app.domain.pricing.MenuFixtures.sweets
import com.rimagwinya.app.domain.pricing.MenuFixtures.vetkoek
import com.rimagwinya.app.domain.pricing.Selection
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The cart, over a fake of the Room DAO.
 *
 * Prices are never stored: each line keeps the item id and the selection,
 * and the price is worked out from the menu every time — which is why the
 * fake menu is what the totals come from here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CartRepositoryTest {

    private class FakeCartDao : CartDao {
        val rows = MutableStateFlow<List<CartLineEntity>>(emptyList())
        override fun observe(): Flow<List<CartLineEntity>> = rows
        override suspend fun upsert(line: CartLineEntity) =
            rows.update { current -> current.filterNot { it.key == line.key } + line }
        override suspend fun remove(key: String) = rows.update { it.filterNot { row -> row.key == key } }
        override suspend fun clear() = rows.update { emptyList() }
    }

    private val dao = FakeCartDao()
    private val menu = mockk<MenuRepository>().also {
        every { it.observe() } returns MutableStateFlow(listOf<MenuItem>(vetkoek, coke, sweets))
    }
    private val cart = CartRepository(
        dao = dao,
        menu = menu,
        json = NetworkModule.json(),
        scope = CoroutineScope(UnconfinedTestDispatcher()),
    )

    @Test
    fun `starts empty`() {
        assertTrue(cart.cart.value.isEmpty)
        assertEquals("R0.00", cart.cart.value.total.format())
    }

    @Test
    fun `different builds of the same item are separate lines`() {
        cart.add(vetkoek, Selection(baseQty = 1).withCount(optionId("polony"), 2))
        cart.add(vetkoek, Selection(baseQty = 0).withCount(optionId("snoek"), 1))

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
        cart.setQuantity(cart.cart.value.lines.single().key, 0)
        assertTrue(cart.cart.value.isEmpty)
    }

    @Test
    fun `a build item has no outer quantity to step`() {
        cart.add(vetkoek, Selection(baseQty = 2).withCount(optionId("polony"), 2))
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
        cart.add(
            vetkoek,
            Selection(baseQty = 2).withCount(optionId("polony"), 2).withCount(optionId("cheese"), 1),
        )
        cart.add(coke, Selection(quantity = 1))

        // R17.00 + R16.00
        assertEquals("R33.00", cart.cart.value.total.format())
        assertEquals(2, cart.cart.value.itemCount)
    }

    @Test
    fun `editing a line replaces its selection`() {
        cart.add(vetkoek, Selection(baseQty = 1))
        cart.replace(cart.cart.value.lines.single().key, Selection(baseQty = 3))
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
        cart.add(
            vetkoek,
            Selection(baseQty = 2).withCount(optionId("polony"), 2).withCount(optionId("cheese"), 1),
        )

        assertEquals("2 vetkoeks, 2 Polony, Cheese slice", cart.cart.value.lines.single().optionsLabel)
    }

    @Test
    fun `a stored line survives being rebuilt from the database`() {
        cart.add(
            vetkoek,
            Selection(baseQty = 2).withCount(optionId("polony"), 2).withCount(optionId("cheese"), 1),
        )
        val stored = dao.rows.value.single()

        // What a restart does: build a fresh repository over the same rows.
        val reopened = CartRepository(dao, menu, NetworkModule.json(), CoroutineScope(UnconfinedTestDispatcher()))

        assertEquals(stored.key, reopened.cart.value.lines.single().key)
        assertEquals("R17.00", reopened.cart.value.total.format())
    }

    @Test
    fun `a line whose item has left the menu drops out`() {
        cart.add(coke, Selection(quantity = 1))
        every { menu.observe() } returns MutableStateFlow(listOf<MenuItem>(vetkoek))

        val reopened = CartRepository(dao, menu, NetworkModule.json(), CoroutineScope(UnconfinedTestDispatcher()))
        assertTrue(reopened.cart.value.isEmpty)
    }
}
