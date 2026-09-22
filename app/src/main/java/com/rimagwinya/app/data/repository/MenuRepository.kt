package com.rimagwinya.app.data.repository

import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.data.remote.SupabaseApi
import com.rimagwinya.app.data.remote.toDomain
import com.rimagwinya.app.domain.model.MenuItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the menu.
 *
 * Network only for now. Phase 10 puts Room in front of it and turns this
 * into a Flow, but the shape the screens see — call it, get items or a typed
 * error — does not change.
 */
@Singleton
class MenuRepository @Inject constructor(
    private val api: SupabaseApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun menu(): Result<List<MenuItem>> = withContext(io) {
        runCatching { api.menu().map { it.toDomain() } }
            .recoverCatching { throw it.asApiError() }
    }
}

/** Human-readable text for a failure, chosen by type rather than by message. */
fun ApiError.friendly(): String = when (this) {
    is ApiError.Offline -> "No connection. Check your signal and try again."
    ApiError.Unauthorised -> "Please sign in again."
    ApiError.Forbidden -> "You do not have access to that."
    ApiError.NotFound -> "We could not find that."
    is ApiError.Conflict -> "That did not go through. Please try again."
    is ApiError.Server -> "The tuckshop system is having a moment. Try again shortly."
    is ApiError.Unexpected -> "Something went wrong. Try again."
}
