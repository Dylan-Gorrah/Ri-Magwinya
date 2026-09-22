package com.rimagwinya.app.data.sync

import com.rimagwinya.app.core.database.PendingActionDao
import com.rimagwinya.app.core.database.PendingActionEntity
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.network.NetworkModule
import com.rimagwinya.app.data.remote.AdjustStockBody
import com.rimagwinya.app.data.remote.FunctionsApi
import com.rimagwinya.app.data.remote.OrderDto
import com.rimagwinya.app.data.remote.PlaceOrderBody
import com.rimagwinya.app.data.remote.SupabaseApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The offline queue: what is kept, what is sent, and what happens when the
 * server refuses something that was queued hours ago.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryTest {

    private class FakeDao : PendingActionDao {
        val rows = MutableStateFlow<List<PendingActionEntity>>(emptyList())
        private var nextId = 1L
        override fun observe(): Flow<List<PendingActionEntity>> = rows
        override suspend fun due() = rows.value.filter { it.failureCode == null }.sortedBy { it.createdAt }
        override suspend fun insert(action: PendingActionEntity): Long {
            val id = nextId++
            rows.update { it + action.copy(id = id) }
            return id
        }
        override suspend fun update(action: PendingActionEntity) =
            rows.update { list -> list.map { if (it.id == action.id) action else it } }
        override suspend fun delete(action: PendingActionEntity) =
            rows.update { list -> list.filterNot { it.id == action.id } }
        override suspend fun deleteById(id: Long) = rows.update { list -> list.filterNot { it.id == id } }
        override suspend fun clear() = rows.update { emptyList() }
    }

    private val dao = FakeDao()
    private val functions = mockk<FunctionsApi>()
    private val api = mockk<SupabaseApi>()
    private val sync = SyncRepository(dao, functions, api, NetworkModule.json(), UnconfinedTestDispatcher())

    private val order = PlaceOrderBody(
        slotId = "96e2fddc-39dc-4ee2-8db7-e9ec69671711",
        paymentMethod = "wallet",
        clientRef = "0b8e8d5e-4c55-4e0e-9a7a-111111111111",
        lines = emptyList(),
    )

    @Test
    fun `an order waits with its client ref, so replaying cannot charge twice`() = runTest {
        sync.queueOrder(order, "Vetkoek")

        val stored = dao.rows.value.single()
        assertEquals(PendingActionEntity.PLACE_ORDER, stored.type)
        assertTrue(order.clientRef in stored.payload)
        assertEquals("Vetkoek", stored.summary)
    }

    @Test
    fun `sending clears the queue`() = runTest {
        coEvery { functions.placeOrder(any()) } returns mockk<OrderDto>()
        sync.queueOrder(order, "Vetkoek")

        assertEquals(SyncOutcome.Done, sync.replay())
        assertTrue(dao.rows.value.isEmpty())
        coVerify(exactly = 1) { functions.placeOrder(match { it.clientRef == order.clientRef }) }
    }

    @Test
    fun `still no signal leaves the queue exactly as it was`() = runTest {
        coEvery { functions.placeOrder(any()) } throws ApiError.Offline()
        sync.queueOrder(order, "Vetkoek")

        assertEquals(SyncOutcome.Offline, sync.replay())
        assertEquals(1, dao.rows.value.size)
        assertEquals(null, dao.rows.value.single().failureCode)
    }

    @Test
    fun `a refusal is kept with its reason rather than retried for ever`() = runTest {
        coEvery { functions.placeOrder(any()) } throws
            ApiError.Conflict(ConflictCode.OUT_OF_STOCK, "Score Energy 500ml")
        sync.queueOrder(order, "Score Energy")

        assertEquals(SyncOutcome.Refused, sync.replay())
        val stored = dao.rows.value.single()
        assertEquals("OUT_OF_STOCK", stored.failureCode)
        assertEquals("Score Energy 500ml", stored.failureDetail)

        // A second run leaves it alone: it would fail the same way.
        assertEquals(SyncOutcome.Done, sync.replay())
        coVerify(exactly = 1) { functions.placeOrder(any()) }
    }

    @Test
    fun `the queue goes out oldest first`() = runTest {
        coEvery { functions.placeOrder(any()) } returns mockk<OrderDto>()
        coEvery { api.adjustStock(any()) } returns mockk()

        sync.queueOrder(order, "first")
        Thread.sleep(2)
        sync.queueStockChange(AdjustStockBody("item", -1), "second")

        sync.replay()
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            functions.placeOrder(any())
            api.adjustStock(any())
        }
    }

    @Test
    fun `a stock change travels as a change, not a total`() = runTest {
        sync.queueStockChange(AdjustStockBody("item-coke", -3), "Coke -3")

        val stored = dao.rows.value.single()
        assertEquals(PendingActionEntity.ADJUST_STOCK, stored.type)
        assertTrue("\"p_delta\":-3" in stored.payload)
    }

    @Test
    fun `a read refusal can be dismissed`() = runTest {
        val id = sync.queueOrder(order, "Vetkoek")
        sync.dismiss(id)
        assertTrue(dao.rows.value.isEmpty())
    }
}
