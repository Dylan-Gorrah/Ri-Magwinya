package com.rimagwinya.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException

/**
 * Every failure the app can show a student, as a type rather than a string.
 *
 * The point of mapping these here is that screens never parse an HTTP status
 * or match on a message. They match on a case and pick a sentence.
 */
sealed class ApiError(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** No signal, DNS failure, timeout. The one the cache answers. */
    class Offline(cause: Throwable? = null) : ApiError("No connection", cause)

    /** Signed out, or the token expired and could not be refreshed. */
    data object Unauthorised : ApiError("Not signed in")

    /** Signed in, but the wrong role, or someone else's data. */
    data object Forbidden : ApiError("Not allowed")

    data object NotFound : ApiError("Not found")

    /** A 409 from an Edge Function, carrying one of the codes below. */
    class Conflict(val code: ConflictCode, val detail: String? = null) :
        ApiError("Conflict: $code")

    class Server(val status: Int, val body: String?) :
        ApiError("Server error $status")

    class Unexpected(cause: Throwable) : ApiError("Unexpected failure", cause)
}

/**
 * The reason codes the server sends with a 409. Each one becomes a specific,
 * actionable sentence on the screen rather than "something went wrong".
 */
enum class ConflictCode {
    OUT_OF_STOCK,
    SLOT_FULL,
    INSUFFICIENT_FUNDS,
    COUNTER_BLOCKED,
    INVALID_TRANSITION,
    STUDENT_NOT_FOUND,
    STUDENT_NUMBER_TAKEN,
    STUDENT_NUMBER_REQUIRED,
    STUDENT_NUMBER_ALREADY_SET,
    EMAIL_TAKEN,
    BAD_CREDENTIALS,
    AMOUNT_OUT_OF_RANGE,
    EMPTY_SELECTION,
    UNKNOWN;

    companion object {
        fun from(raw: String?): ConflictCode =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

/** The error shape PostgREST and our Edge Functions both return. */
@Serializable
data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null,
    val detail: String? = null,
    val hint: String? = null,
    @SerialName("error") val error: String? = null,
)

/** Network-layer failures that are not HTTP responses at all. */
fun Throwable.asApiError(): ApiError = when (this) {
    is ApiError -> this
    is IOException -> ApiError.Offline(this)
    else -> ApiError.Unexpected(this)
}
