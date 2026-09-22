package com.rimagwinya.app.feature.menu

import app.cash.turbine.test
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.data.repository.CartRepository
import com.rimagwinya.app.data.repository.MenuRepository
import com.rimagwinya.app.data.repository.OrderRepository
import com.rimagwinya.app.data.repository.WeatherRepository
import com.rimagwinya.app.domain.model.Cart
import com.rimagwinya.app.domain.model.MenuChange
import com.rimagwinya.app.domain.model.applying
import com.rimagwinya.app.domain.model.Sky
import com.rimagwinya.app.domain.model.Weather
import com.rimagwinya.app.domain.pricing.MenuFixtures
import com.rimagwinya.app.domain.pricing.Selection
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The menu screen's state: what the student sees while it loads, when it
 * fails, when the weather arrives, and when stock changes under them.
 *
 * `AppConfig.isBackendConfigured` is false in unit tests (no keys in
 * BuildConfig), so the realtime and weather paths stay switched off and
 * these tests exercise the state machine rather than the network.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val menu = mockk<MenuRepository>(relaxed = true)
    private val cart = mockk<CartRepository>(relaxed = true)
    private val orders = mockk<OrderRepository>(relaxed = true)
    private val weather = mockk<WeatherRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { menu.changes() } returns emptyFlow()
        every { cart.cart } returns MutableStateFlow(Cart())
        every { orders.orderChanges() } returns emptyFlow()
        coEvery { orders.orders(any()) } returns Result.success(emptyList())
        coEvery { weather.weather() } returns null
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = MenuViewModel(menu, cart, orders, weather)

    @Test
    fun `it starts loading, then shows the menu`() = runTest {
        coEvery { menu.menu() } returns Result.success(listOf(MenuFixtures.coke, MenuFixtures.tea))

        viewModel().state.test {
            assertTrue("starts loading", awaitItem().isLoading)
            val loaded = awaitItem()
            assertEquals(2, loaded.items.size)
            assertNull(loaded.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no signal shows a message, not an empty menu`() = runTest {
        coEvery { menu.menu() } returns Result.failure(ApiError.Offline())

        viewModel().state.test {
            awaitItem()
            val failed = awaitItem()
            assertEquals(com.rimagwinya.app.R.string.error_network, failed.error)
            assertTrue(failed.items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search and category narrow what is shown`() = runTest {
        coEvery { menu.menu() } returns
            Result.success(listOf(MenuFixtures.coke, MenuFixtures.tea, MenuFixtures.vetkoek))
        val model = viewModel()

        model.state.test {
            awaitItem()
            awaitItem()

            model.onQuery("tea")
            assertEquals(listOf("tea"), awaitItem().visible.map { it.slug })

            model.onQuery("")
            awaitItem()
            model.onCategory(CategoryFilter.Meals)
            assertEquals(listOf("vetkoek"), awaitItem().visible.map { it.slug })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a sold-out item will not open its sheet`() = runTest {
        val soldOut = MenuFixtures.coke.copy(stockQuantity = 0)
        coEvery { menu.menu() } returns Result.success(listOf(soldOut))
        val model = viewModel()

        model.state.test {
            awaitItem()
            awaitItem()
            model.openItem(soldOut)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `adding to the cart closes the sheet and tells the cart`() = runTest {
        coEvery { menu.menu() } returns Result.success(listOf(MenuFixtures.coke))
        val model = viewModel()
        val selection = Selection(quantity = 2)

        model.state.test {
            awaitItem()
            awaitItem()
            model.openItem(MenuFixtures.coke)
            assertEquals(MenuFixtures.coke, awaitItem().openItem)

            model.addToCart(MenuFixtures.coke, selection)
            assertNull(awaitItem().openItem)
            verify { cart.add(MenuFixtures.coke, selection) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the weather reorders the menu without refetching it`() = runTest {
        coEvery { menu.menu() } returns
            Result.success(listOf(MenuFixtures.coke, MenuFixtures.tea, MenuFixtures.vetkoek))

        val cold = Weather(
            temperatureC = 9.0, sky = Sky.Rain, rainProbabilityToday = 80,
            tomorrowMaxC = 12.0, tomorrowMinC = 4.0, tomorrowRainProbability = 60,
            tomorrowSky = Sky.Rain, fetchedAt = 0,
        )
        val state = MenuUiState(items = listOf(MenuFixtures.coke, MenuFixtures.tea, MenuFixtures.vetkoek))

        // Cold and wet: the hot things come first.
        assertEquals(
            listOf("vetkoek", "tea", "coke"),
            state.copy(weather = cold).visible.map { it.slug },
        )
        assertEquals(
            listOf("vetkoek", "tea", "coke").sorted(),
            state.visible.map { it.slug }.sorted(),
        )
    }

    @Test
    fun `a live stock change patches only that item`() = runTest {
        val state = MenuUiState(items = listOf(MenuFixtures.coke, MenuFixtures.tea))
        val patched = state.items.applying(
            MenuChange.Updated(MenuFixtures.coke.id, stockQuantity = 0, isAvailable = null, price = null)
        )

        assertTrue(patched.first { it.slug == "coke" }.isSoldOut)
        assertEquals(50, patched.first { it.slug == "tea" }.stockQuantity)
    }
}
