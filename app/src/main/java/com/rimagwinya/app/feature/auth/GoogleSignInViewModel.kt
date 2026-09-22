package com.rimagwinya.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoogleSignInUiState(val busy: Boolean = false, val failed: Boolean = false)

/**
 * Hands a Google ID token to Supabase.
 *
 * Nothing navigates from here: signing in changes the session, and the
 * session observer swaps the whole graph — the same path as email sign-in.
 */
@HiltViewModel
class GoogleSignInViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GoogleSignInUiState())
    val state: StateFlow<GoogleSignInUiState> = _state.asStateFlow()

    fun signIn(result: GoogleResult) {
        when (result) {
            // Dismissing the sheet is not a failure and gets no message.
            GoogleResult.Cancelled -> _state.update { it.copy(busy = false, failed = false) }
            is GoogleResult.Failed -> _state.update { it.copy(busy = false, failed = true) }
            is GoogleResult.Token -> {
                _state.update { it.copy(busy = true, failed = false) }
                viewModelScope.launch {
                    auth.signInWithGoogle(result.idToken)
                        .onSuccess { _state.update { s -> s.copy(busy = false) } }
                        .onFailure { _state.update { s -> s.copy(busy = false, failed = true) } }
                }
            }
        }
    }
}
