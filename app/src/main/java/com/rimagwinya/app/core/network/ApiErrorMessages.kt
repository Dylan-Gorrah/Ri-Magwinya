package com.rimagwinya.app.core.network

import androidx.annotation.StringRes
import com.rimagwinya.app.R

/**
 * A failure as a string resource, chosen by type rather than by message.
 *
 * ViewModels hold the id and the screen resolves it, so every word stays in
 * strings.xml. Screens with their own wording (sign-in) map it themselves.
 */
@StringRes
fun ApiError.messageRes(): Int = when (this) {
    is ApiError.Offline -> R.string.error_network
    ApiError.Unauthorised -> R.string.error_signed_out
    ApiError.Forbidden -> R.string.error_forbidden
    ApiError.NotFound -> R.string.error_not_found
    is ApiError.Conflict -> R.string.error_conflict
    is ApiError.Server -> R.string.error_server
    is ApiError.Unexpected -> R.string.error_generic
}
