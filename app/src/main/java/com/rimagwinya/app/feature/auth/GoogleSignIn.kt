package com.rimagwinya.app.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rimagwinya.app.core.config.AppConfig

/** What came back from the Google sheet. */
sealed interface GoogleResult {
    data class Token(val idToken: String) : GoogleResult

    /** The sheet was dismissed. Not an error — say nothing. */
    data object Cancelled : GoogleResult

    /** No Google account on the phone, Play Services missing, and so on. */
    data class Failed(val cause: Throwable) : GoogleResult
}

/**
 * Credential Manager, asking for a Google ID token.
 *
 * Any Google account is accepted — there is no college domain to check
 * (section 12). The token is then handed to Supabase, which verifies it
 * against Google and creates or finds the account.
 */
suspend fun requestGoogleIdToken(context: Context): GoogleResult {
    if (!AppConfig.isGoogleSignInConfigured) {
        return GoogleResult.Failed(IllegalStateException("Google sign-in is not configured"))
    }

    val option = GetGoogleIdOption.Builder()
        // false, so someone signing in for the first time on this phone
        // still sees their accounts rather than an empty sheet.
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(AppConfig.googleWebClientId)
        .build()

    return try {
        val response = CredentialManager.create(context)
            .getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
        GoogleResult.Token(credential.idToken)
    } catch (e: GetCredentialCancellationException) {
        GoogleResult.Cancelled
    } catch (e: GetCredentialException) {
        GoogleResult.Failed(e)
    } catch (e: Exception) {
        GoogleResult.Failed(e)
    }
}
