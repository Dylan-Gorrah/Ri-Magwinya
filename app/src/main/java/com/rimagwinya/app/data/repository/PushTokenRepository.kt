package com.rimagwinya.app.data.repository

import com.rimagwinya.app.BuildConfig
import com.rimagwinya.app.core.datastore.SettingsRepository
import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.data.remote.ProfilePatch
import com.rimagwinya.app.data.remote.SupabaseApi
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * The device token, which is what makes a push reach this phone.
 *
 * Every call is safe to make before Firebase exists: without
 * `google-services.json` the whole thing quietly does nothing, so the rest
 * of the app never has to ask whether push is set up.
 */
@Singleton
class PushTokenRepository @Inject constructor(
    private val api: SupabaseApi,
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /** Honours the settings toggle before anything is shown. */
    suspend fun notificationsWanted(): Boolean = settings.settings.first().orderNotifications

    /** Fetches the current token and saves it against the profile. */
    suspend fun syncToken(): Result<Unit> = withContext(io) {
        runCatching {
            if (!BuildConfig.PUSH_ENABLED) return@runCatching
            if (!notificationsWanted()) return@runCatching
            register(currentToken() ?: return@runCatching)
        }
    }

    /** Saves a token the messaging service was handed. */
    suspend fun register(token: String): Result<Unit> = withContext(io) {
        runCatching {
            val id = auth.currentUserId() ?: return@runCatching
            api.patchProfile("eq.$id", ProfilePatch(fcmToken = token))
            Unit
        }
    }

    /**
     * Clears the token on sign-out, so the next person to use this phone
     * does not get the last person's notifications.
     */
    suspend fun clear(): Result<Unit> = withContext(io) {
        runCatching {
            val id = auth.currentUserId() ?: return@runCatching
            api.patchProfile("eq.$id", ProfilePatch(fcmToken = ""))
            Unit
        }
    }

    private suspend fun currentToken(): String? = suspendCancellableCoroutine { continuation ->
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    continuation.resume(task.result.takeIf { task.isSuccessful })
                }
        }.onFailure { continuation.resume(null) }
    }
}
