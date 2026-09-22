package com.rimagwinya.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmTextField
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Space.screen)
            .padding(top = Space.x32, bottom = Space.x32),
        verticalArrangement = Arrangement.spacedBy(Space.x20),
    ) {
        Text(
            text = stringResource(R.string.title_login),
            style = RmTheme.type.largeTitle,
            color = RmTheme.colors.text,
        )

        RmTextField(
            value = state.email,
            onValueChange = viewModel::onEmail,
            label = stringResource(R.string.auth_email),
            placeholder = stringResource(R.string.auth_email_hint),
            error = state.emailError?.let { stringResource(it) },
            keyboardType = KeyboardType.Email,
            leadingIcon = R.drawable.ic_mail,
            modifier = Modifier.onFocusChanged { if (!it.isFocused) viewModel.onEmailBlur() },
        )

        RmTextField(
            value = state.password,
            onValueChange = viewModel::onPassword,
            label = stringResource(R.string.auth_password),
            error = state.passwordError?.let { stringResource(it) },
            isPassword = true,
            keyboardType = KeyboardType.Password,
            leadingIcon = R.drawable.ic_lock,
            modifier = Modifier.onFocusChanged { if (!it.isFocused) viewModel.onPasswordBlur() },
        )

        if (state.formError != null) {
            Banner(tone = BannerTone.Error, text = stringResource(state.formError!!))
        }

        RmButton(
            text = stringResource(
                if (state.isSubmitting) R.string.auth_signing_in else R.string.auth_sign_in
            ),
            onClick = viewModel::submit,
            enabled = state.canSubmit,
        )

        GoogleSignInButton()

        RmButton(
            text = stringResource(R.string.auth_no_account),
            onClick = onRegister,
            style = RmButtonStyle.Quiet,
        )
    }
}

@Composable
fun RegisterScreen(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.registered) {
        // Registering signs you in, so there is nothing to navigate to — the
        // session observer swaps the whole graph underneath this screen.
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Space.screen)
            .padding(top = Space.x32, bottom = Space.x32),
        verticalArrangement = Arrangement.spacedBy(Space.x20),
    ) {
        Text(
            text = stringResource(R.string.title_register),
            style = RmTheme.type.largeTitle,
            color = RmTheme.colors.text,
        )

        RmTextField(
            value = state.fullName,
            onValueChange = { viewModel.onChange(RegisterField.Name, it) },
            label = stringResource(R.string.auth_name),
            placeholder = stringResource(R.string.auth_name_hint),
            error = state.errorFor(RegisterField.Name)?.let { stringResource(it) },
            leadingIcon = R.drawable.ic_user,
            modifier = Modifier.onFocusChanged {
                if (!it.isFocused) viewModel.onBlur(RegisterField.Name)
            },
        )

        RmTextField(
            value = state.email,
            onValueChange = { viewModel.onChange(RegisterField.Email, it) },
            label = stringResource(R.string.auth_email),
            placeholder = stringResource(R.string.auth_email_hint),
            error = state.errorFor(RegisterField.Email)?.let { stringResource(it) },
            keyboardType = KeyboardType.Email,
            leadingIcon = R.drawable.ic_mail,
            modifier = Modifier.onFocusChanged {
                if (!it.isFocused) viewModel.onBlur(RegisterField.Email)
            },
        )

        RmTextField(
            value = state.studentNumber,
            onValueChange = { viewModel.onChange(RegisterField.StudentNumber, it) },
            label = stringResource(R.string.auth_student_number),
            placeholder = stringResource(R.string.auth_student_number_hint),
            error = state.errorFor(RegisterField.StudentNumber)?.let { stringResource(it) },
            leadingIcon = R.drawable.ic_sig,
            modifier = Modifier.onFocusChanged {
                if (!it.isFocused) viewModel.onBlur(RegisterField.StudentNumber)
            },
        )

        RmTextField(
            value = state.password,
            onValueChange = { viewModel.onChange(RegisterField.Password, it) },
            label = stringResource(R.string.auth_password),
            placeholder = stringResource(R.string.auth_password_hint),
            error = state.errorFor(RegisterField.Password)?.let { stringResource(it) },
            isPassword = true,
            keyboardType = KeyboardType.Password,
            leadingIcon = R.drawable.ic_lock,
            modifier = Modifier.onFocusChanged {
                if (!it.isFocused) viewModel.onBlur(RegisterField.Password)
            },
        )

        RmTextField(
            value = state.confirmPassword,
            onValueChange = { viewModel.onChange(RegisterField.ConfirmPassword, it) },
            label = stringResource(R.string.auth_confirm_password),
            error = state.errorFor(RegisterField.ConfirmPassword)?.let { stringResource(it) },
            isPassword = true,
            keyboardType = KeyboardType.Password,
            leadingIcon = R.drawable.ic_lock,
            modifier = Modifier.onFocusChanged {
                if (!it.isFocused) viewModel.onBlur(RegisterField.ConfirmPassword)
            },
        )

        if (state.formError != null) {
            Banner(tone = BannerTone.Error, text = stringResource(state.formError!!))
        }

        // The account exists but Supabase is holding it. Without this the
        // screen would just sit there looking broken.
        if (state.awaitingEmailConfirmation) {
            Banner(
                tone = BannerTone.Success,
                title = stringResource(R.string.auth_check_email_title),
                text = stringResource(R.string.auth_check_email_body),
            )
        }

        // Consent belongs before the account is made, not in a settings
        // screen nobody opens.
        Banner(tone = BannerTone.Info, text = stringResource(R.string.popia_notice))

        RmButton(
            text = stringResource(
                if (state.isSubmitting) R.string.auth_creating else R.string.auth_create_account
            ),
            onClick = viewModel::submit,
            enabled = state.canSubmit,
        )

        GoogleSignInButton()

        RmButton(
            text = stringResource(R.string.auth_have_account),
            onClick = onSignIn,
            style = RmButtonStyle.Quiet,
        )
    }
}

@Preview(name = "Login", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun LoginPreview() = RimagwinyaTheme {
    Column(
        Modifier.padding(Space.screen),
        verticalArrangement = Arrangement.spacedBy(Space.x20),
    ) {
        Text("Sign in", style = RmTheme.type.largeTitle, color = RmTheme.colors.text)
        RmTextField(value = "thabo@gmail.com", onValueChange = {}, label = "Email",
            leadingIcon = R.drawable.ic_mail)
        RmTextField(value = "wrongpass", onValueChange = {}, label = "Password",
            isPassword = true, leadingIcon = R.drawable.ic_lock)
        Banner(tone = BannerTone.Error, text = "Email or password is incorrect.")
        RmButton("Sign in", {})
    }
}
