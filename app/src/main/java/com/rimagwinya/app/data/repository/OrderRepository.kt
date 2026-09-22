package com.rimagwinya.app.data.repository

import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.database.OrderDao
import com.rimagwinya.app.core.database.SlotDao
import com.rimagwinya.app.core.database.lineEntities
import com.rimagwinya.app.core.database.toDomain as cachedToDomain
import com.rimagwinya.app.core.database.toEntity
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.data.remote.EnsureSlotsBody
import com.rimagwinya.app.data.remote.FunctionsApi
import com.rimagwinya.app.data.remote.OrderStatusBody
import com.rimagwinya.app.data.remote.PlaceOrderBody
import com.rimagwinya.app.data.remote.SupabaseApi
import com.rimagwinya.app.data.remote.TableWatcher
import com.rimagwinya.app.data.sync.SyncRepository
import com.rimagwinya.app.data.remote.toBody
import com.rimagwinya.app.data.remote.toDomain
import com.rimagwinya.app.domain.model.Cart
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.OrderStatus
import com.rimagwinya.app.domain.model.PaymentMethod
import com.rimagwinya.app.domain.model.Slot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Slots, placing orders, and reading them back — for students and staff.
 *
 * Whose orders come back is decided by Row Level Security, not by a filter
 * here: the same call returns a student's own orders or, for staff, all of
 * them.
 */
@Singleton
open class OrderRepository @Inject constructor(
    private val api: SupabaseApi,
    private val functions: FunctionsApi,
    private val watcher: TableWatcher,
    private val orderDao: OrderDao,
    private val slotDao: SlotDao,
    private val sync: SyncRepository,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /** Today, in Bloemfontein — not in UTC, and not in the phone's zone. */
    fun today(): LocalDate = ZonedDateTime.now(clock.withZone(AppConfig.zone)).toLocalDate()

    /**
     * Creates today's breaks if missing, then returns them in order. Falls
     * back to the cached copy when there is no signal, so the cart can still
     * show which breaks exist.
     */
    open suspend fun todaysSlots(): Result<List<Slot>> = withContext(io) {
        val date = today()
        runCatching {
            val slots = api.ensureSlots(EnsureSlotsBody(date.toString()))
                .map { it.toDomain() }
                .sortedBy { it.startsAt }
            slotDao.replace(date.toString(), slots.map { it.toEntity() })
            slots
        }.recoverCatching { error ->
            val cached = slotDao.forDate(date.toString()).map { it.cachedToDomain() }
            if (cached.isNotEmpty()) cached else throw error.asApiError()
        }
    }

    /**
     * Places the cart. [clientRef] must stay the same across retries of the
     * same attempt — that is what makes a retry return the first order
     * instead of charging twice.
     */
    open suspend fun place(
        cart: Cart,
        slotId: String,
        method: PaymentMethod,
        clientRef: String,
    ): Result<PlaceResult> = withContext(io) {
        val body = PlaceOrderBody(
            slotId = slotId,
            paymentMethod = method.serverName,
            clientRef = clientRef,
            lines = cart.lines.map { it.toBody() },
        )
        runCatching {
            val order = functions.placeOrder(body).toDomain()
            orderDao.upsert(listOf(order.toEntity()), order.lineEntities())
            PlaceResult.Placed(order) as PlaceResult
        }.recoverCatching { error ->
            val mapped = error.asApiError()
            // No signal is not a failure here: the order waits, with its
            // client_ref, and goes as soon as there is a connection.
            if (mapped is ApiError.Offline) {
                sync.queueOrder(body, cart.lines.joinToString(", ") { it.item.name })
                PlaceResult.Queued
            } else {
                throw mapped
            }
        }
    }

    /** Pay-at-counter needs at least one top-up on record. */
    open suspend fun hasToppedUp(): Result<Boolean> = call { api.topUps().isNotEmpty() }

    /** The cached orders, updating as they are refreshed. */
    fun observeOrders(): Flow<List<Order>> = orderDao.observe().map { rows -> rows.map { it.cachedToDomain() } }

    fun observeOrder(id: String): Flow<Order?> = orderDao.observeOne(id).map { it?.cachedToDomain() }

    /** Newest first. A student's own; staff see everyone's. */
    open suspend fun orders(limit: Int = 50): Result<List<Order>> = withContext(io) {
        runCatching {
            val orders = api.orders(limit = limit).map { it.toDomain() }
            orderDao.upsert(orders.map { it.toEntity() }, orders.flatMap { it.lineEntities() })
            orders
        }.recoverCatching { error ->
            val cached = orderDao.load().map { it.cachedToDomain() }
            if (cached.isNotEmpty()) cached else throw error.asApiError()
        }
    }

    /** Today's orders that are still open, oldest first — the staff queue. */
    open suspend fun openOrdersToday(): Result<List<Order>> = call {
        val since = today().atStartOfDay(AppConfig.zone).toOffsetDateTime().toString()
        api.orders(
            order = "placed_at.asc",
            status = "in.(placed,preparing,ready)",
            placedSince = "gte.$since",
        ).map { it.toDomain() }
    }

    open suspend fun order(id: String): Result<Order> = withContext(io) {
        runCatching {
            val order = api.order("eq.$id").firstOrNull()?.toDomain() ?: throw ApiError.NotFound
            orderDao.upsert(listOf(order.toEntity()), order.lineEntities())
            order
        }.recoverCatching { error ->
            orderDao.load().firstOrNull { it.order.id == id }?.cachedToDomain() ?: throw error.asApiError()
        }
    }

    open suspend fun setStatus(id: String, status: OrderStatus): Result<Order> = call {
        functions.setOrderStatus(OrderStatusBody(id, status.serverName)).toDomain()
    }

    /** Fires when any visible order changes. Refetch on each. */
    fun orderChanges(): Flow<Unit> = watcher.changes("orders")

    fun orderChanges(id: String): Flow<Unit> = watcher.changes("orders", "id=eq.$id")

    fun slotChanges(): Flow<Unit> = watcher.changes("collection_slots")

    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(io) {
        runCatching { block() }.recoverCatching { throw it.asApiError() }
    }
}

/** What placing an order did. Offline it waits rather than failing. */
sealed interface PlaceResult {
    data class Placed(val order: com.rimagwinya.app.domain.model.Order) : PlaceResult

    /** Queued with its client_ref; [com.rimagwinya.app.data.sync.SyncWorker] sends it. */
    data object Queued : PlaceResult
}
