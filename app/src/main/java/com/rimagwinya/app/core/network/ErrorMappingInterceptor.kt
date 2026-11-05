package com.rimagwinya.app.core.network

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns HTTP failures into [ApiError] before they reach a repository.
 *
 * Doing it in an interceptor rather than in each Retrofit call means there is
 * exactly one place that knows what a 409 body looks like, and repositories
 * only ever catch typed errors.
 */
@Singleton
class ErrorMappingInterceptor @Inject constructor(
    private val json: Json,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response

        // peekBody leaves the stream intact, so this does not consume the
        // body that a caller might still want.
        val raw = runCatching { response.peekBody(MAX_ERROR_BODY).string() }.getOrNull()
        val parsed = raw?.let {
            runCatching { json.decodeFromString<ApiErrorBody>(it) }.getOrNull()
        }

        response.close()

        // Edge Functions put the reason in `code`. PostgREST puts a SQL
        // error's own code there (P0001 for `raise exception`) and the
        // reason in `message`, so take the first field that is a reason we
        // know rather than the first field that is present.
        val code = listOf(parsed?.code, parsed?.error, parsed?.message)
            .map(ConflictCode::from)
            .firstOrNull { it != ConflictCode.UNKNOWN } ?: ConflictCode.UNKNOWN
        val detail = parsed?.detail ?: parsed?.hint

        throw when {
            response.code == 401 -> ApiError.Unauthorised
            response.code == 403 -> ApiError.Forbidden
            // A known reason wins over the status: STUDENT_NOT_FOUND is a 404
            // and AMOUNT_OUT_OF_RANGE a 400, and the screen needs the reason.
            code != ConflictCode.UNKNOWN -> ApiError.Conflict(code, detail)
            response.code == 409 -> ApiError.Conflict(ConflictCode.UNKNOWN, detail)
            response.code == 404 -> ApiError.NotFound
            else -> ApiError.Server(response.code, raw)
        }
    }

    private companion object {
        const val MAX_ERROR_BODY = 8L * 1024
    }
}
