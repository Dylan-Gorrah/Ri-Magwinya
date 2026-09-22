package com.rimagwinya.app.core.network

import com.rimagwinya.app.BuildConfig
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.data.remote.SupabaseApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        // The server will grow columns we do not know about yet. Ignoring
        // them means adding a column is not a client release.
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /**
     * Attaches the two headers every Supabase request needs.
     *
     * `apikey` is the anon key, which is public and does nothing on its own —
     * Row Level Security decides what it can see. `Authorization` carries the
     * signed-in user's token, and that is what the policies actually read.
     */
    @Provides
    @Singleton
    fun authInterceptor(tokens: TokenProvider): Interceptor = Interceptor { chain ->
        val token = tokens.accessToken() ?: AppConfig.supabaseAnonKey
        val request = chain.request().newBuilder()
            .header("apikey", AppConfig.supabaseAnonKey)
            .header("Authorization", "Bearer $token")
            .build()
        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun okHttp(
        auth: Interceptor,
        errors: ErrorMappingInterceptor,
        json: Json,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(auth)
        .addInterceptor(errors)
        .apply {
            if (BuildConfig.DEBUG) {
                // Body logging in debug only. Note that this logs request
                // bodies but the interceptor above runs first, so tokens are
                // never in the body — and they are never logged as headers.
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                        redactHeader("Authorization")
                        redactHeader("apikey")
                    }
                )
            }
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        // Trailing slash matters to Retrofit's URL resolution. When the URL
        // is not configured yet this is a harmless placeholder that fails as
        // an ordinary network error rather than crashing at startup.
        .baseUrl(
            AppConfig.supabaseUrl.takeIf { it.isNotBlank() }?.trimEnd('/')?.plus("/")
                ?: "http://localhost/"
        )
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun supabaseApi(retrofit: Retrofit): SupabaseApi = retrofit.create(SupabaseApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TokenModule {
    /** Phase 3 rebinds this to the supabase-kt session. */
    @Binds
    @Singleton
    abstract fun tokenProvider(impl: AnonTokenProvider): TokenProvider
}
