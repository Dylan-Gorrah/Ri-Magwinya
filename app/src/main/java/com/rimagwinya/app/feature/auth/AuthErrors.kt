package com.rimagwinya.app.feature.auth

import androidx.annotation.StringRes
import com.rimagwinya.app.R
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.util.Problem

/**
 * Failures as string resources, not sentences.
 *
 * ViewModels hold the resource id and the screen resolves it, which keeps
 * every user-facing word in strings.xml and Phase 13 a translation job.
 */
@StringRes
fun Problem.messageRes(): Int = when (this) {
    Problem.EmailMalformed -> R.string.error_email_malformed
    Problem.PasswordTooShort -> R.string.error_password_short
    Problem.NameTooShort -> R.string.error_name_short
    Problem.StudentNumberMalformed -> R.string.error_student_number
    Problem.PhoneMalformed -> R.string.error_phone
}

@StringRes
fun ApiError.authMessageRes(): Int = when (this) {
    is ApiError.Offline -> R.string.error_network
    // A wrong password and an unknown email give the same message on
    // purpose. "No account with that email" tells anyone who asks which
    // addresses have accounts here.
    ApiError.Unauthorised -> R.string.error_credentials
    is ApiError.Conflict -> when (code) {
        ConflictCode.BAD_CREDENTIALS -> R.string.error_credentials
        ConflictCode.EMAIL_TAKEN -> R.string.error_email_taken
        ConflictCode.STUDENT_NUMBER_TAKEN -> R.string.error_student_number_taken
        else -> R.string.error_generic
    }
    else -> R.string.error_generic
}
