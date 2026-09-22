package com.rimagwinya.app.feature.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.util.FieldResult
import com.rimagwinya.app.core.util.Validation
import com.rimagwinya.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    /** Only shown once the field has been left, so it does not nag mid-typing. */
    val emailTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val isSubmitting: Boolean = false,
    @StringRes val formError: Int? = null,
    val signedIn: Boolean = false,
) {
    val emailResult: FieldResult get() = Validation.email(email)
    val passwordResult: FieldResult get() = Validation.password(password)

    @get:StringRes
    val emailError: Int?
        get() = if (emailTouched) emailResult.problemOrNull?.messageRes() else null

    @get:StringRes
    val passwordError: Int?
        get() = if (passwordTouched) passwordResult.problemOrNull?.messageRes() else null

    /** The button stays disabled until the form could actually succeed. */
    val canSubmit: Boolean
        get() = emailResult.isValid && passwordResult.isValid && !isSubmitting
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmail(value: String) = _state.update {
        it.copy(email = value, formError = null)
    }

    fun onPassword(value: String) = _state.update {
        it.copy(password = value, formError = null)
    }

    fun onEmailBlur() = _state.update { it.copy(emailTouched = true) }
    fun onPasswordBlur() = _state.update { it.copy(passwordTouched = true) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return

        _state.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            auth.signIn(current.email, current.password)
                .onSuccess {
                    // Where they land is decided by the profile's role, which
                    // the session observer reads. Nothing here chooses it.
                    _state.update { it.copy(isSubmitting = false, signedIn = true) }
                }
                .onFailure { throwable ->
                    val error = (throwable as? ApiError) ?: throwable.asApiError()
                    _state.update {
                        it.copy(isSubmitting = false, formError = error.authMessageRes())
                    }
                }
        }
    }
}
