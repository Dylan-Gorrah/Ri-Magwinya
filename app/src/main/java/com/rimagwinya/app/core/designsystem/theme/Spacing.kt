package com.rimagwinya.app.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * The only spacing values allowed. If a gap needs a number that isn't here,
 * the layout is wrong, not the scale.
 */
object Space {
    val x4 = 4.dp
    val x8 = 8.dp
    val x12 = 12.dp
    val x16 = 16.dp
    val x20 = 20.dp
    val x24 = 24.dp
    val x32 = 32.dp
    val x40 = 40.dp
    val x48 = 48.dp
    val x64 = 64.dp

    /** Left and right padding on every screen. */
    val screen = 20.dp

    /** Minimum touch target. Nothing tappable is smaller than this. */
    val touchTarget = 48.dp
}

/** Corner radii. Buttons 12, chips 10, sheets 26 on the top corners. */
object Radius {
    val r8 = 8.dp
    val r10 = 10.dp
    val r12 = 12.dp
    val r16 = 16.dp
    val r20 = 20.dp
    val sheet = 26.dp
}

/** Hairlines first, shadows rarely. If everything is elevated, nothing is. */
object Stroke {
    val hairline = 1.dp
}
