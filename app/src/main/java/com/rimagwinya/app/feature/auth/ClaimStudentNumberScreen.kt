package com.rimagwinya.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmTextField
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.core.util.FieldResult
import com.rimagwinya.app.core.util.UiText
import com.rimagwinya.app.core.util.Validation
import com.rimagwinya.app.core.util.resolve
import com.rimagwinya.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClaimUiState(
    val number: String = "",
    val saving: Boolean = false,
    val error: UiText? = null,
) {
    val canSave: Boolean get() = !saving && Validation.studentNumber(number) is FieldResult.Valid
}

/**
 * The gap Google sign-in leaves: an account exists, with no student number.
 *
 * `claim_student_number` in the database sets it only while it is still
 * null, so nobody can rewrite theirs later to take a number that belongs to
 * someone else.
 */
@HiltViewModel
class ClaimStudentNumberViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ClaimUiState())
    val state: StateFlow<ClaimUiState> = _state.asStateFlow()

    fun onNumber(value: String) = _state.update { it.copy(number = value, error = null) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            auth.claimStudentNumber(current.number)
                .onSuccess { _state.update { it.copy(saving = false) } }
                .onFailure { e ->
                    val error = e.asApiError()
                    val message = if ((error as? ApiError.Conflict)?.code == ConflictCode.STUDENT_NUMBER_TAKEN) {
                        UiText(R.string.claim_taken)
                    } else {
                        UiText(error.messageRes())
                    }
                    _state.update { it.copy(saving = false, error = message) }
                }
        }
    }

    fun signOut() = viewModelScope.launch { auth.signOut() }
}

@Composable
fun ClaimStudentNumberScreen(
    modifier: Modifier = Modifier,
    viewModel: ClaimStudentNumberViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Space.screen)
            .padding(top = Space.x48, bottom = Space.x32),
        verticalArrangement = Arrangement.spacedBy(Space.x20),
    ) {
        Text(stringResource(R.string.claim_title), style = RmTheme.type.largeTitle, color = RmTheme.colors.text)
        Text(stringResource(R.string.claim_body), style = RmTheme.type.body, color = RmTheme.colors.text2)

        RmTextField(
            value = state.number,
            onValueChange = viewModel::onNumber,
            label = stringResource(R.string.auth_student_number),
            keyboardType = KeyboardType.Ascii,
            leadingIcon = R.drawable.ic_user,
        )

        state.error?.let { Banner(text = it.resolve(), tone = BannerTone.Error) }

        RmButton(
            text = stringResource(if (state.saving) R.string.claim_saving else R.string.claim_save),
            onClick = viewModel::save,
            enabled = state.canSave,
        )
        RmButton(stringResource(R.string.auth_sign_out), { viewModel.signOut() }, style = RmButtonStyle.Quiet)
    }
}
