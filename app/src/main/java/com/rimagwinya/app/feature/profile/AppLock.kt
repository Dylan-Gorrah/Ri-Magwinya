package com.rimagwinya.app.feature.profile

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.BrandMark
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import androidx.compose.ui.unit.dp

private const val ALLOWED = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/** Whether this phone can actually ask for a fingerprint or a PIN. */
fun canUseBiometrics(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(ALLOWED) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * The gate over the whole app when biometric unlock is on and someone is
 * signed in.
 *
 * It protects what is on the screen, not the session: the token is already
 * on the device either way. Signing in with a password is always available
 * as the way past it, which is also what someone does on a phone whose
 * fingerprint reader has stopped working.
 */
@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    onUsePassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    // Read as resources, so a language or configuration change is picked up.
    val promptTitle = stringResource(R.string.lock_prompt_title)
    val promptSubtitle = stringResource(R.string.lock_prompt_subtitle)

    fun prompt() {
        if (activity == null) {
            onUnlocked()
            return
        }
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onUnlocked()
            },
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(promptTitle)
                .setSubtitle(promptSubtitle)
                .setAllowedAuthenticators(ALLOWED)
                .build()
        )
    }

    // Ask as soon as the lock appears, rather than making people tap first.
    LaunchedEffect(Unit) { prompt() }

    Column(
        modifier
            .fillMaxSize()
            .padding(Space.screen),
        verticalArrangement = Arrangement.spacedBy(Space.x16, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(size = 64.dp)
        Text(stringResource(R.string.lock_title), style = RmTheme.type.screenTitle, color = RmTheme.colors.text)
        Text(
            stringResource(R.string.lock_body),
            style = RmTheme.type.secondary,
            color = RmTheme.colors.text2,
            textAlign = TextAlign.Center,
        )
        RmButton(stringResource(R.string.lock_unlock), ::prompt, fillWidth = false)
        RmButton(
            stringResource(R.string.lock_use_password),
            onUsePassword,
            style = RmButtonStyle.Quiet,
            fillWidth = false,
        )
    }
}

private fun Context.findActivity(): FragmentActivity? {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
