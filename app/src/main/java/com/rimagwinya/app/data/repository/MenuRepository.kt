package com.rimagwinya.app.data.repository

import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.data.remote.MenuRealtime
import com.rimagwinya.app.data.remote.SupabaseApi
import com.rimagwinya.app.data.remote.toDomain
import com.rimagwinya.app.domain.model.MenuChange
import com.rimagwinya.app.domain.model.MenuItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the menu, and tells you when it changes.
 *
 * Network only for now. Phase 10 puts Room in front of it and turns this
 * into a Flow, but the shape the screens see — call it, get items or a typed
 * error — does not change.
 */
@Singleton
class MenuRepository @Inject constructor(
    private val api: SupabaseApi,
    private val realtime: MenuRealtime,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun menu(): Result<List<MenuItem>> = withContext(io) {
        runCatching { api.menu().map { it.toDomain() } }
            .recoverCatching { throw it.asApiError() }
    }

    /** Live stock, availability and price changes. See [MenuRealtime]. */
    fun changes(): Flow<MenuChange> = realtime.changes()
}
