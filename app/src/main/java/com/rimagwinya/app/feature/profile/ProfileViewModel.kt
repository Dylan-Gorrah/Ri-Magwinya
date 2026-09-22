package com.rimagwinya.app.feature.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.R
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.datastore.Settings
import com.rimagwinya.app.core.datastore.SettingsRepository
import com.rimagwinya.app.core.designsystem.theme.ThemeChoice
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.core.util.FieldResult
import com.rimagwinya.app.core.util.UiText
import com.rimagwinya.app.core.util.Validation
import com.rimagwinya.app.data.repository.AuthRepository
import com.rimagwinya.app.domain.model.Profile
import com.rimagwinya.app.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The change-password sheet, when it is open. */
data class PasswordForm(
    val password: String = "",
    val repeat: String = "",
    val saving: Boolean = false,
    val error: UiText? = null,
) {
    val canSave: Boolean
        get() = !saving &&
            Validation.password(password) is FieldResult.Valid &&
            password == repeat
}

/** The edit-profile sheet, when it is open. */
data class EditProfileForm(
    val fullName: String = "",
    val phone: String = "",
    val saving: Boolean = false,
    val error: UiText? = null,
) {
    val nameError: Boolean get() = Validation.fullName(fullName) is FieldResult.Invalid

    /** A blank phone number is fine; a malformed one is not. */
    val phoneError: Boolean get() = Validation.phone(phone) is FieldResult.Invalid

    val canSave: Boolean get() = !saving && !nameError && !phoneError && fullName.isNotBlank()
}

data class ProfileUiState(
    val profile: Profile? = null,
    val settings: Settings = Settings(),
    val stamps: Int = 0,
    val passwordForm: PasswordForm? = null,
    val editForm: EditProfileForm? = null,
    val message: UiText? = null,
) {
    val isStaff: Boolean get() = profile?.role == UserRole.Staff

    /** Progress round the current card. Display only — nothing redeems it. */
    val stampProgress: Int get() = stamps % AppConfig.LOYALTY_TARGET
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val local = MutableStateFlow(Local())

    val state: StateFlow<ProfileUiState> = combine(
        auth.currentProfile,
        settingsRepository.settings,
        local,
    ) { profile, settings, l ->
        ProfileUiState(
            profile = profile,
            settings = settings,
            stamps = l.stamps,
            passwordForm = l.passwordForm,
            editForm = l.editForm,
            message = l.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { auth.profile() }
        viewModelScope.launch {
            auth.loyaltyStamps().onSuccess { count -> local.update { it.copy(stamps = count) } }
        }
    }

    fun setTheme(choice: ThemeChoice) = viewModelScope.launch { settingsRepository.setTheme(choice) }

    fun setNotifications(on: Boolean) = viewModelScope.launch { settingsRepository.setOrderNotifications(on) }

    fun setBiometric(on: Boolean) = viewModelScope.launch { settingsRepository.setBiometricUnlock(on) }

    /**
     * Switches the app's language immediately and remembers it.
     *
     * Three places, on purpose: AppCompat applies it now and restores it on
     * the next launch, DataStore is what the settings screen reads, and
     * `profiles.language` is what a push notification is written in.
     */
    fun setLanguage(code: String) = viewModelScope.launch {
        settingsRepository.setLanguage(code)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
        auth.updateLanguage(code)
    }

    fun openEditSheet() {
        val profile = state.value.profile ?: return
        local.update {
            it.copy(editForm = EditProfileForm(fullName = profile.fullName, phone = profile.phone.orEmpty()))
        }
    }

    fun closeEditSheet() = local.update { it.copy(editForm = null) }

    fun editProfile(change: (EditProfileForm) -> EditProfileForm) =
        local.update { l -> l.copy(editForm = l.editForm?.let { change(it).copy(error = null) }) }

    fun saveProfile() {
        val form = local.value.editForm ?: return
        if (!form.canSave) return
        local.update { it.copy(editForm = form.copy(saving = true)) }
        viewModelScope.launch {
            auth.updateProfile(form.fullName, form.phone)
                .onSuccess {
                    local.update { it.copy(editForm = null, message = UiText(R.string.profile_edit_saved)) }
                }
                .onFailure { e ->
                    local.update {
                        it.copy(editForm = form.copy(saving = false, error = UiText(e.asApiError().messageRes())))
                    }
                }
        }
    }

    fun openPasswordSheet() = local.update { it.copy(passwordForm = PasswordForm()) }

    fun closePasswordSheet() = local.update { it.copy(passwordForm = null) }

    fun editPassword(change: (PasswordForm) -> PasswordForm) =
        local.update { l -> l.copy(passwordForm = l.passwordForm?.let { change(it).copy(error = null) }) }

    fun savePassword() {
        val form = local.value.passwordForm ?: return
        if (!form.canSave) {
            local.update { it.copy(passwordForm = form.copy(error = UiText(R.string.password_mismatch))) }
            return
        }
        local.update { it.copy(passwordForm = form.copy(saving = true)) }
        viewModelScope.launch {
            auth.changePassword(form.password)
                .onSuccess {
                    local.update { it.copy(passwordForm = null, message = UiText(R.string.password_changed)) }
                }
                .onFailure { e ->
                    local.update {
                        it.copy(passwordForm = form.copy(saving = false, error = UiText(e.asApiError().messageRes())))
                    }
                }
        }
    }

    fun dismissMessage() = local.update { it.copy(message = null) }

    fun signOut() = viewModelScope.launch { auth.signOut() }

    private data class Local(
        val stamps: Int = 0,
        val passwordForm: PasswordForm? = null,
        val editForm: EditProfileForm? = null,
        val message: UiText? = null,
    )
}
