package com.rimagwinya.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The palette from the design brief.
 *
 * Material 3's ColorScheme has no idea what "navy" or "skyWash" mean, so the
 * brand tokens live here and reach composables through [LocalRmColors]. The
 * M3 scheme is still populated in Theme.kt so Material's own components
 * (ripples, text selection, the odd built-in) look right.
 */
@Immutable
data class RmColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val border: Color,
    val borderStrong: Color,
    /** The only action colour. Primary buttons, active tab, selected chip. */
    val navy: Color,
    /** Menu hero header only. */
    val navyDeep: Color,
    /** Information: status pills, links, focus rings. */
    val sky: Color,
    val skyWash: Color,
    /** Sparingly: staff badge, loyalty, top seller. Nothing else. */
    val gold: Color,
    val goldWash: Color,
    /** Brand mark fill only. */
    val dough: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val isDark: Boolean,
)

val LightRmColors = RmColors(
    background = Color(0xFFF2F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EEF5),
    text = Color(0xFF0F1B2E),
    text2 = Color(0xFF5A6779),
    text3 = Color(0xFF93A0B0),
    border = Color(0xFF0F1B2E).copy(alpha = 0.08f),
    borderStrong = Color(0xFF0F1B2E).copy(alpha = 0.16f),
    navy = Color(0xFF16305B),
    navyDeep = Color(0xFF0D1F3D),
    sky = Color(0xFF3E7FBF),
    skyWash = Color(0xFFE8F1FA),
    gold = Color(0xFFB8892C),
    goldWash = Color(0xFFFAF2E3),
    dough = Color(0xFFE9A84F),
    success = Color(0xFF2A7050),
    warning = Color(0xFF9A6B12),
    error = Color(0xFFA33A2E),
    isDark = false,
)

val DarkRmColors = RmColors(
    background = Color(0xFF0B1220),
    surface = Color(0xFF141C2B),
    surfaceVariant = Color(0xFF1E2739),
    text = Color(0xFFEEF2F7),
    text2 = Color(0xFF95A2B4),
    text3 = Color(0xFF68758A),
    border = Color(0xFFFFFFFF).copy(alpha = 0.09f),
    borderStrong = Color(0xFFFFFFFF).copy(alpha = 0.18f),
    navy = Color(0xFF2F5C9E),
    navyDeep = Color(0xFF1B3A6B),
    sky = Color(0xFF7FB3E0),
    skyWash = Color(0xFF7FB3E0).copy(alpha = 0.13f),
    gold = Color(0xFFD9AC55),
    goldWash = Color(0xFFD9AC55).copy(alpha = 0.13f),
    dough = Color(0xFFE9A84F),
    success = Color(0xFF5FBD8C),
    warning = Color(0xFFD9AC55),
    error = Color(0xFFE08476),
    isDark = true,
)

val LocalRmColors = staticCompositionLocalOf { LightRmColors }
