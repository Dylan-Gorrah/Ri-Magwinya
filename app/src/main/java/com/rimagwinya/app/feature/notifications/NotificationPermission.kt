package com.rimagwinya.app.feature.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.RmBottomSheet
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.notifications.Notifications

/**
 * Asks for notification permission the way people should be asked: a
 * sentence saying what it is for, then the system dialog — never the system
 * dialog cold at launch, which is how you get a permanent "no".
 *
 * Shown once per run, only to someone signed in, and only when the phone
 * needs asking (Android 13+).
 */
@Composable
fun NotificationPermissionGate(enabled: Boolean, onGranted: () -> Unit) {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(!Notifications.hasPermission(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted()
    }

    if (!showRationale || asked) return

    RmBottomSheet(onDismiss = { asked = true; showRationale = false }) {
        Column(Modifier.padding(Space.screen), verticalArrangement = Arrangement.spacedBy(Space.x16)) {
            Text(
                stringResource(R.string.notif_permission_title),
                style = RmTheme.type.screenTitle,
                color = RmTheme.colors.text,
            )
            Text(
                stringResource(R.string.notif_permission_body),
                style = RmTheme.type.body,
                color = RmTheme.colors.text2,
            )
            RmButton(stringResource(R.string.notif_permission_allow), {
                asked = true
                showRationale = false
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
            RmButton(
                stringResource(R.string.notif_permission_skip),
                { asked = true; showRationale = false },
                style = RmButtonStyle.Quiet,
            )
        }
    }
}
