package com.rimagwinya.app.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Reads the design tokens. Every composable in the app goes through this
 * rather than MaterialTheme, so the brand colours and the type scale are
 * always to hand.
 */
object RmTheme {
    val colors: RmColors
        @Composable get() = LocalRmColors.current
    val type: RmTypography
        @Composable get() = LocalRmType.current
}

/**
 * Which theme the app should use. Stored in DataStore from Phase 9; until
 * then it always follows the system.
 */
enum class ThemeChoice { System, Light, Dark }

@Composable
fun RimagwinyaTheme(
    choice: ThemeChoice = ThemeChoice.System,
    content: @Composable () -> Unit,
) {
    val dark = when (choice) {
        ThemeChoice.System -> isSystemInDarkTheme()
        ThemeChoice.Light -> false
        ThemeChoice.Dark -> true
    }
    val colors = if (dark) DarkRmColors else LightRmColors

    // No dynamic colour. The palette is the brand; letting the wallpaper
    // repaint it would undo the whole design system.
    val material = if (dark) {
        darkColorScheme(
            primary = colors.navy,
            onPrimary = Color.White,
            secondary = colors.sky,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.text2,
            error = colors.error,
            outline = colors.borderStrong,
        )
    } else {
        lightColorScheme(
            primary = colors.navy,
            onPrimary = Color.White,
            secondary = colors.sky,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.text2,
            error = colors.error,
            outline = colors.borderStrong,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // The menu hero is navy and sets this to light itself.
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !dark
        }
    }

    CompositionLocalProvider(
        LocalRmColors provides colors,
        LocalRmType provides RmType,
    ) {
        MaterialTheme(
            colorScheme = material,
            shapes = RmShapes,
            content = content,
        )
    }
}
