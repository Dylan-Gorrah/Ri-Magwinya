package com.rimagwinya.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rimagwinya.app.R

/**
 * Inter, subset to the Latin ranges English, Afrikaans and Sesotho need, and
 * instanced to three static weights. Static rather than variable on purpose:
 * variable weight axes only work from API 26, and minSdk here is 24.
 */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** The type scale from the brief. Nothing outside this set. */
@Immutable
data class RmTypography(
    val largeTitle: TextStyle,
    val screenTitle: TextStyle,
    val bodyStrong: TextStyle,
    val body: TextStyle,
    val secondary: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
    /** Prices and any column of figures. Tabular so digits line up. */
    val price: TextStyle,
)

val RmType = RmTypography(
    largeTitle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.03).em,
    ),
    screenTitle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.025).em,
    ),
    bodyStrong = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.01).em,
    ),
    body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    secondary = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
    ),
    caption = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
    ),
    label = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.12.em,
    ),
    price = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = "tnum",
        textAlign = TextAlign.End,
    ),
)

val LocalRmType = staticCompositionLocalOf { RmType }
