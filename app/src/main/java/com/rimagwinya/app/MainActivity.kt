package com.rimagwinya.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
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
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RimagwinyaTheme {
                RootNavHost()
            }
        }
    }
}
