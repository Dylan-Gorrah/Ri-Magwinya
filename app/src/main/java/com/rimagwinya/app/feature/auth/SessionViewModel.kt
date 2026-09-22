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
                        // The refresh token is gone or rejected. Treat it as
                        // signed out rather than leaving the app in a state
                        // where every request quietly 401s.
                        _state.value = AuthState.SignedOut
                    }
                }
            }
        }
        viewModelScope.launch {
            // A newer profile (after an order or a top-up) replaces the one
            // on screen, but never signs anyone in by itself.
            auth.currentProfile.collect { profile ->
                if (profile != null && _state.value is AuthState.SignedIn) {
                    _state.value = AuthState.SignedIn(profile)
                }
            }
        }
    }

    private suspend fun loadProfile() {
        auth.profile()
            .onSuccess { _state.value = AuthState.SignedIn(it) }
            .onFailure {
                // A valid session with no readable profile is not a usable
                // state, so fall back to signed out rather than guessing a
                // role.
                _state.value = AuthState.SignedOut
            }
    }

    fun refreshProfile() {
        viewModelScope.launch { loadProfile() }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}
