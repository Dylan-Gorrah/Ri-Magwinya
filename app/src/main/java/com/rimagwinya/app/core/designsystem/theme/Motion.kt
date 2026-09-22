package com.rimagwinya.app.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion from the brief. Everything eases on the same curve so the app moves
 * as one thing rather than a collection of screens.
 */
object Motion {
    const val FAST = 180
    const val NORMAL = 300
    const val SLOW = 460

    val Smooth = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

    fun <T> fast() = tween<T>(FAST, easing = Smooth)
    fun <T> normal() = tween<T>(NORMAL, easing = Smooth)
    fun <T> slow() = tween<T>(SLOW, easing = Smooth)

    /** Sheets, toggles, button press, selection indicators. */
    fun <T> springy() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Press feedback. 0.96 for large surfaces, 0.975 for small ones. */
    const val PRESS_SCALE_LARGE = 0.96f
    const val PRESS_SCALE_SMALL = 0.975f
}
