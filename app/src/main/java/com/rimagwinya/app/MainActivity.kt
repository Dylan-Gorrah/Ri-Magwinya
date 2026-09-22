package com.rimagwinya.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.core.datastore.Settings
import com.rimagwinya.app.core.datastore.SettingsRepository
import android.content.Intent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.notifications.Notifications
import javax.inject.Inject
import com.rimagwinya.app.navigation.RootNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity.
 *
 * It extends AppCompatActivity rather than ComponentActivity, in an app that
 * is otherwise entirely Compose, for one reason: per-app language in Phase 13
 * needs AppCompat to back-port it below Android 13, and minSdk here is 24.
 *
 * That also means the manifest theme must be an AppCompat DayNight theme.
 * AppCompatActivity crashes at startup without one.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.createChannels(this)

        setContent {
            // An order id arrives here when a notification was tapped.
            var deepLinkOrderId by remember { mutableStateOf(intent?.getStringExtra(Notifications.EXTRA_ORDER_ID)) }
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    deepLinkOrderId = newIntent.getStringExtra(Notifications.EXTRA_ORDER_ID)
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            // The chosen theme, from DataStore. System until it has loaded,
            // which is also what someone who never changed it wants.
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(Settings())
            RimagwinyaTheme(choice = settings.theme) {
                RootNavHost(
                    biometricUnlock = settings.biometricUnlock,
                    deepLinkOrderId = deepLinkOrderId,
                    onDeepLinkHandled = { deepLinkOrderId = null },
                )
            }
        }
    }
}
