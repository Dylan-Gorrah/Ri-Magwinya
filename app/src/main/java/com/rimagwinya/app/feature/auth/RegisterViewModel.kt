package com.rimagwinya.app.feature.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.R
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

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val studentNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val touched: Set<RegisterField> = emptySet(),
    val isSubmitting: Boolean = false,
    @StringRes val formError: Int? = null,
    val registered: Boolean = false,
    /**
     * The account was made but Supabase is holding it until the email is
     * confirmed, so there is no session and nothing to navigate to. The
     * screen says so instead of looking like it failed.
     */
    val awaitingEmailConfirmation: Boolean = false,
) {
    val nameResult: FieldResult get() = Validation.fullName(fullName)
    val emailResult: FieldResult get() = Validation.email(email)
    val numberResult: FieldResult get() = Validation.studentNumber(studentNumber)
    val passwordResult: FieldResult get() = Validation.password(password)

    /** Typed twice, to catch the typo before the account exists. */
    val passwordsMatch: Boolean get() = password == confirmPassword

    @StringRes
    fun errorFor(field: RegisterField): Int? {
        if (field !in touched) return null
        return when (field) {
            RegisterField.Name -> nameResult
            RegisterField.Email -> emailResult
            RegisterField.StudentNumber -> numberResult
            RegisterField.Password -> passwordResult
            RegisterField.ConfirmPassword ->
                return if (passwordsMatch) null else R.string.password_mismatch
        }.problemOrNull?.messageRes()
    }

    val canSubmit: Boolean
        get() = nameResult.isValid && emailResult.isValid &&
            numberResult.isValid && passwordResult.isValid &&
            passwordsMatch && !isSubmitting
}

enum class RegisterField { Name, Email, StudentNumber, Password, ConfirmPassword }

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onChange(field: RegisterField, value: String) = _state.update {
        when (field) {
            RegisterField.Name -> it.copy(fullName = value, formError = null)
            RegisterField.Email -> it.copy(email = value, formError = null)
            RegisterField.StudentNumber -> it.copy(studentNumber = value, formError = null)
            RegisterField.Password -> it.copy(password = value, formError = null)
            RegisterField.ConfirmPassword -> it.copy(confirmPassword = value, formError = null)
        }
    }

    fun onBlur(field: RegisterField) = _state.update {
        it.copy(touched = it.touched + field)
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return

        _state.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            auth.register(
                email = current.email,
                password = current.password,
                fullName = current.fullName,
                studentNumber = current.studentNumber,
            )
                .onSuccess { signedIn ->
                    // Everyone who registers is a student. A staff account is
                    // made by hand in Supabase and promoted with SQL, because
                    // "sign up as staff" would be the first thing anyone tried.
                    //
                    // signedIn is false when "Confirm email" is on in the
                    // Supabase dashboard: the account exists, but there is no
                    // session until the link is clicked.
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            registered = signedIn,
                            awaitingEmailConfirmation = !signedIn,
                        )
                    }
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
