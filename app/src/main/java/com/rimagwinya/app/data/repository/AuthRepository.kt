package com.rimagwinya.app.data.repository

import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.util.Validation
import com.rimagwinya.app.data.remote.SupabaseApi
import com.rimagwinya.app.data.remote.toDomain
import com.rimagwinya.app.domain.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/** Whether anyone is signed in, and who. */
sealed interface AuthState {
    /** Still restoring a stored session. The splash stays up for this. */
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val profile: Profile) : AuthState
}

/**
 * Sign up, sign in, sign out, and the profile that decides which half of the
 * app loads.
 *
 * Passwords are hashed by Supabase with bcrypt, on their servers. The app
 * never hashes anything and never stores a password.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient,
    private val api: SupabaseApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    /**
     * Emits whenever the session changes, including the silent refresh that
     * keeps someone signed in for weeks.
     */
    val sessionStatus: Flow<SessionStatus> = client.auth.sessionStatus

    val isSignedIn: Boolean
        get() = client.auth.currentSessionOrNull() != null

    private val _currentProfile = MutableStateFlow<Profile?>(null)

    /**
     * The last profile fetched. Screens that show the balance watch this, so
     * placing an order or a top-up updates the hero, the checkout and the
     * profile together after one [profile] call.
     */
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    /**
     * Creates the account. The profile row is created by the
     * `handle_new_user` trigger from the metadata sent here — which is why
     * the name and student number travel as user metadata rather than as a
     * second insert the app would have to keep consistent.
     */
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        studentNumber: String,
    ): Result<Unit> = withContext(io) {
        runCatching {
            client.auth.signUpWith(Email) {
                this.email = Validation.normaliseEmail(email)
                this.password = password
                data = JsonObject(
                    mapOf(
                        "full_name" to JsonPrimitive(fullName.trim()),
                        "student_number" to JsonPrimitive(
                            Validation.normaliseStudentNumber(studentNumber)
                        ),
                    )
                )
            }
            Unit
        }.recoverCatching { throw it.toAuthError() }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> =
        withContext(io) {
            runCatching {
                client.auth.signInWith(Email) {
                    this.email = Validation.normaliseEmail(email)
                    this.password = password
                }
                Unit
            }.recoverCatching { throw it.toAuthError() }
        }

    /**
     * Signs in with a Google ID token from Credential Manager.
     *
     * Supabase verifies the token with Google, so nothing here has to. A
     * first sign-in creates the account with no student number, which the
     * app then asks for — see [claimStudentNumber].
     */
    suspend fun signInWithGoogle(idToken: String): Result<Unit> = withContext(io) {
        runCatching {
            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
            }
            Unit
        }.recoverCatching { throw it.toAuthError() }
    }

    suspend fun signOut(): Result<Unit> = withContext(io) {
        runCatching {
            client.auth.signOut()
            _currentProfile.value = null
            Unit
        }.recoverCatching { throw it.asApiError() }
        // Phase 11 also clears profiles.fcm_token here. On a shared or
        // handed-down phone, leaving it behind means the next person gets
        // the last person's order notifications.
    }

    /**
     * The profile, which carries the role. Row Level Security scopes this to
     * the caller, so there is no user id in the query — asking for "the
     * profile" can only ever return your own.
     */
    suspend fun profile(): Result<Profile> = withContext(io) {
        runCatching {
            val id = currentUserId() ?: throw ApiError.Unauthorised
            api.profile("eq.$id").firstOrNull()?.toDomain()
                ?: throw ApiError.NotFound
        }.recoverCatching { throw it.asApiError() }
            .onSuccess { _currentProfile.value = it }
    }

    /**
     * Changes the password of the signed-in account. Supabase checks the
     * session, not the old password, so the screen asks for the new one only.
     */
    suspend fun changePassword(newPassword: String): Result<Unit> = withContext(io) {
        runCatching {
            client.auth.updateUser { password = newPassword }
            Unit
        }.recoverCatching { throw it.toAuthError() }
    }

    /** Stamps earned, one per collected order. Display only — no redemption. */
    suspend fun loyaltyStamps(): Result<Int> = withContext(io) {
        runCatching { api.loyaltyStamps().size }.recoverCatching { throw it.asApiError() }
    }

    /** Sets the student number once, for accounts created by Google sign-in. */
    suspend fun claimStudentNumber(number: String): Result<Profile> =
        withContext(io) {
            runCatching {
                api.claimStudentNumber(
                    com.rimagwinya.app.data.remote.ClaimStudentNumberBody(
                        Validation.normaliseStudentNumber(number)
                    )
                ).toDomain()
            }.recoverCatching { throw it.asApiError() }
                .onSuccess { _currentProfile.value = it }
        }

    fun currentUserId(): String? = client.auth.currentSessionOrNull()?.user?.id
}

/**
 * Supabase returns 400 for a wrong password, which is not what 400 usually
 * means, so it is translated here rather than leaking into the screens.
 */
private fun Throwable.toAuthError(): Throwable {
    val message = message.orEmpty().lowercase()
    return when {
        this is ApiError -> this
        message.contains("student_number_taken") ->
            ApiError.Conflict(ConflictCode.STUDENT_NUMBER_TAKEN)
        message.contains("already registered") || message.contains("already been registered") ->
            ApiError.Conflict(ConflictCode.EMAIL_TAKEN)
        message.contains("invalid login") || message.contains("invalid credentials") ->
            ApiError.Conflict(ConflictCode.BAD_CREDENTIALS)
        this is RestException -> ApiError.Server(-1, message)
        else -> asApiError()
    }
}
