package com.rimagwinya.app.data.repository

import com.rimagwinya.app.core.database.MenuDao
import com.rimagwinya.app.core.database.groupEntities
import com.rimagwinya.app.core.database.optionEntities
import com.rimagwinya.app.core.database.toDomain
import com.rimagwinya.app.core.database.toEntity
import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.data.remote.MenuRealtime
import com.rimagwinya.app.data.remote.SupabaseApi
import com.rimagwinya.app.data.remote.toDomain
import com.rimagwinya.app.domain.model.MenuChange
import com.rimagwinya.app.domain.model.MenuItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The menu.
 *
 * Network first, cache second. [menu] asks the server and writes what it
 * gets to Room; if the request fails it answers from Room instead, so the
 * menu still opens in a dead spot — with yesterday's stock figures, which
 * the server checks again when the order is placed.
 */
@Singleton
open class MenuRepository @Inject constructor(
    private val api: SupabaseApi,
    private val realtime: MenuRealtime,
    private val dao: MenuDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /** The cached menu, updating as it is refreshed. */
    open fun observe(): Flow<List<MenuItem>> = dao.observe().map { rows -> rows.map { it.toDomain() } }

    open suspend fun menu(): Result<List<MenuItem>> = withContext(io) {
        runCatching {
            val items = api.menu().map { it.toDomain() }
            cache(items)
            items
        }.recoverCatching { error ->
            val cached = dao.load().map { it.toDomain() }
            // An empty cache has nothing to show, so the error stands.
            if (cached.isNotEmpty()) cached else throw error.asApiError()
        }
    }

    /** True when there is something to show without a connection. */
    suspend fun hasCache(): Boolean = withContext(io) { dao.count() > 0 }

    private suspend fun cache(items: List<MenuItem>) {
        dao.replace(
            items = items.map { it.toEntity() },
            groups = items.flatMap { it.groupEntities() },
            options = items.flatMap { it.optionEntities() },
        )
    }

    /** Live stock, availability and price changes. See [MenuRealtime]. */
    fun changes(): Flow<MenuChange> = realtime.changes()

    /** Keeps the cache in step with a live change, so offline sees it too. */
    suspend fun cacheStock(id: String, stock: Int?, available: Boolean?) = withContext(io) {
        val current = dao.load().firstOrNull { it.item.id == id } ?: return@withContext
        dao.updateStock(
            id = id,
            stock = stock ?: current.item.stockQuantity,
            available = available ?: current.item.isAvailable,
        )
    }
}
