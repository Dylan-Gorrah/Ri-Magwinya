package com.rimagwinya.app.core.network

import com.rimagwinya.app.core.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * supabase-kt, used for **auth and realtime only**.
 *
 * Every REST call goes through Retrofit instead, so each endpoint is an
 * explicit, typed, testable interface rather than a query chain built at the
 * call site. What supabase-kt is genuinely better at is the two things kept
 * here: session storage with token refresh, and the realtime socket.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun supabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = AppConfig.supabaseUrl.ifBlank { "https://localhost" },
        supabaseKey = AppConfig.supabaseAnonKey,
    ) {
        install(Auth) {
            // The session is kept and refreshed in the background, so nobody
            // signs into a tuckshop app twice.
            autoLoadFromStorage = true
            alwaysAutoRefresh = true
        }
        install(Realtime)
    }
}

/**
 * Hands the live access token to the OkHttp interceptor.
 *
 * This is the seam Phase 2 left behind: the networking layer never changed,
 * only what it asks for a token. When nobody is signed in this returns null
 * and the interceptor falls back to the anon key, which can read the menu
 * and nothing else.
 */
@Singleton
class SupabaseTokenProvider @Inject constructor(
    private val client: SupabaseClient,
) : TokenProvider {
    override fun accessToken(): String? =
        client.auth.currentAccessTokenOrNull()
}
