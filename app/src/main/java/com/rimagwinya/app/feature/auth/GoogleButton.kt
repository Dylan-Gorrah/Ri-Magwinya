package com.rimagwinya.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import kotlinx.coroutines.launch

/**
 * "Continue with Google", on both the sign-in and the create-account screens.
 *
 * Hidden entirely when there is no client id, so the app before Phase 12 is
 * set up simply does not offer something that cannot work.
 */
@Composable
fun GoogleSignInButton(
    modifier: Modifier = Modifier,
    viewModel: GoogleSignInViewModel = hiltViewModel(),
) {
    if (!AppConfig.isGoogleSignInConfigured) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(Space.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.google_or), style = RmTheme.type.caption, color = RmTheme.colors.text3)
        RmButton(
            text = stringResource(if (state.busy) R.string.google_signing_in else R.string.google_continue),
            onClick = { scope.launch { viewModel.signIn(requestGoogleIdToken(context)) } },
            style = RmButtonStyle.Secondary,
            enabled = !state.busy,
        )
        if (state.failed) {
            Banner(text = stringResource(R.string.google_failed), tone = BannerTone.Error)
        }
    }
}
