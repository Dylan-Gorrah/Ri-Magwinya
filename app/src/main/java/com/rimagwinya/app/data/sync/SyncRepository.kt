package com.rimagwinya.app.data.sync

import com.rimagwinya.app.core.database.PendingActionDao
import com.rimagwinya.app.core.database.PendingActionEntity
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.data.remote.AdjustStockBody
import com.rimagwinya.app.data.remote.FunctionsApi
import com.rimagwinya.app.data.remote.PlaceOrderBody
import com.rimagwinya.app.data.remote.SupabaseApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** What a replay attempt ended up doing. */
enum class SyncOutcome {
    /** Everything queued went through, or there was nothing to send. */
    Done,

    /** Still no signal. The queue is untouched; try again later. */
    Offline,

    /** Something was refused for good. It stays, with its reason, to be shown. */
    Refused,
}

/**
 * The offline queue.
 *
 * Actions are replayed in the order they were made, and a replay is exactly
 * the request that failed — for an order that means the same `client_ref`,
 * which is what makes sending it twice safe.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val dao: PendingActionDao,
    private val functions: FunctionsApi,
    private val api: SupabaseApi,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val pending: Flow<List<PendingActionEntity>> = dao.observe()

    suspend fun queueOrder(body: PlaceOrderBody, summary: String): Long = withContext(io) {
        dao.insert(
            PendingActionEntity(
                type = PendingActionEntity.PLACE_ORDER,
                payload = json.encodeToString(body),
                summary = summary,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun queueStockChange(body: AdjustStockBody, summary: String): Long = withContext(io) {
        dao.insert(
            PendingActionEntity(
                type = PendingActionEntity.ADJUST_STOCK,
                payload = json.encodeToString(body),
                summary = summary,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    /** Drops a refusal the user has read. */
    suspend fun dismiss(id: Long) = withContext(io) { dao.deleteById(id) }

    /**
     * Sends everything waiting, oldest first.
     *
     * Stops at the first sign of no signal, so the order of the queue is
     * kept. A refusal (out of stock, slot full) is not retried — it would
     * fail the same way for ever — and is kept with its reason instead.
     */
    suspend fun replay(): SyncOutcome = withContext(io) {
        var outcome = SyncOutcome.Done
        for (action in dao.due()) {
            try {
                when (action.type) {
                    PendingActionEntity.PLACE_ORDER ->
                        functions.placeOrder(json.decodeFromString<PlaceOrderBody>(action.payload))
                    PendingActionEntity.ADJUST_STOCK ->
                        api.adjustStock(json.decodeFromString<AdjustStockBody>(action.payload))
                    else -> Unit
                }
                dao.delete(action)
            } catch (e: Throwable) {
                when (val error = e.asApiError()) {
                    is ApiError.Offline -> return@withContext SyncOutcome.Offline
                    is ApiError.Conflict -> {
                        dao.update(
                            action.copy(
                                attempts = action.attempts + 1,
                                failureCode = error.code.name,
                                failureDetail = error.detail,
                            )
                        )
                        outcome = SyncOutcome.Refused
                    }
                    is ApiError.Server -> {
                        // Could be temporary. Count the attempt and move on;
                        // WorkManager brings us back with a longer gap.
                        dao.update(action.copy(attempts = action.attempts + 1))
                        return@withContext SyncOutcome.Offline
                    }
                    else -> {
                        dao.update(
                            action.copy(
                                attempts = action.attempts + 1,
                                failureCode = "UNKNOWN",
                                failureDetail = null,
                            )
                        )
                        outcome = SyncOutcome.Refused
                    }
                }
            }
        }
        outcome
    }
}
