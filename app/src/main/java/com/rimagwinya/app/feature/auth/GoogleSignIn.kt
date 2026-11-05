package com.rimagwinya.app.feature.auth

import android.util.Log
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rimagwinya.app.core.config.AppConfig


private const val TAG = "RmGoogle"

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
// Reference: Google. 2026. Authenticate users with Sign in with Google. [Online].
// Available at: <https://developer.android.com/identity/sign-in/credential-manager-siwg>
// [Accessed 5 November 2026].
suspend fun requestGoogleIdToken(context: Context): GoogleResult {
    if (!AppConfig.isGoogleSignInConfigured) {
        Log.e(TAG, "picker: GOOGLE_WEB_CLIENT_ID is not set, Google sign-in is off")
        return GoogleResult.Failed(IllegalStateException("Google sign-in is not configured"))
    }

    val option = GetGoogleIdOption.Builder()
        // false, so someone signing in for the first time on this phone
        // still sees their accounts rather than an empty sheet.
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(AppConfig.googleWebClientId)
        .build()

    Log.d(TAG, "picker: opening the Google account sheet")
    return try {
        val response = CredentialManager.create(context)
            .getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
        // The token itself is never logged: it signs the user in.
        Log.d(TAG, "picker: got an ID token")
        GoogleResult.Token(credential.idToken)
    } catch (e: GetCredentialCancellationException) {
        Log.d(TAG, "picker: cancelled by the user")
        GoogleResult.Cancelled
    } catch (e: GetCredentialException) {
        Log.e(TAG, "picker: failed (${e.type})", e)
        GoogleResult.Failed(e)
    } catch (e: Exception) {
        Log.e(TAG, "picker: unexpected failure", e)
        GoogleResult.Failed(e)
    }
}
