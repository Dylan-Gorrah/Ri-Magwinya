package com.rimagwinya.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.data.repository.AuthRepository
import com.rimagwinya.app.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Who is signed in, for the whole app.
 *
 * This is the only place that decides which half of the app loads, and it
 * decides it from the **profile's role column on the server** — never from
 * anything the phone chose. That is why the welcome screen's two cards are
 * cosmetic.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val push: com.rimagwinya.app.data.repository.PushTokenRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Emits on sign-in, sign-out, and on the silent refresh that
            // keeps someone signed in for weeks.
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> loadProfile()
                    is SessionStatus.NotAuthenticated -> _state.value = AuthState.SignedOut
                    is SessionStatus.Initializing -> _state.value = AuthState.Loading
                    is SessionStatus.RefreshFailure -> {
                        // supabase-kt reports a rejected token as
                        // NotAuthenticated; RefreshFailure means the refresh
                        // could not reach the server, and it retries. Someone
                        // already in the app stays in it (offline mode), and
                        // only a cold start with nothing loaded falls back to
                        // sign-in.
                        if (_state.value !is AuthState.SignedIn) {
                            _state.value = AuthState.SignedOut
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            // A newer profile (after an order or a top-up) replaces the one
            // on screen, but never signs anyone in by itself.
            // It must be the same person, too: only loadProfile decides who
            // is signed in.
            auth.currentProfile.collect { profile ->
                val current = _state.value as? AuthState.SignedIn ?: return@collect
                if (profile != null && profile.id == current.profile.id) {
                    _state.value = AuthState.SignedIn(profile)
                }
            }
        }
    }

    /** Saves this phone's push token against the signed-in profile. */
    fun syncPushToken() {
        viewModelScope.launch { push.syncToken() }
    }

    private suspend fun loadProfile() {
        auth.profile()
            .onSuccess {
                _state.value = AuthState.SignedIn(it)
                push.syncToken()
            }
            .onFailure {
                // A token refresh re-runs this. If the same person is already
                // in and the network blipped, keep the profile they have.
                val current = _state.value as? AuthState.SignedIn
                if (current != null && current.profile.id == auth.currentUserId()) return@onFailure
                // Otherwise a valid session with no readable profile is not a
                // usable state, so fall back to signed out rather than
                // guessing a role.
                _state.value = AuthState.SignedOut
            }
    }

    fun refreshProfile() {
        viewModelScope.launch { loadProfile() }
    }

    fun signOut() {
        viewModelScope.launch {
            // Clear the token first: after sign-out the request would be
            // refused, and the next person on this phone would keep getting
            // the last person's notifications.
            push.clear()
            auth.signOut()
        }
    }
}
