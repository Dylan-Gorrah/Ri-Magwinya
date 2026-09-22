package com.rimagwinya.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/** Material 3 shapes, mapped onto the brief's radius scale. */
val RmShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.r8),
    small = RoundedCornerShape(Radius.r10),
    medium = RoundedCornerShape(Radius.r12),
    large = RoundedCornerShape(Radius.r16),
    extraLarge = RoundedCornerShape(Radius.r20),
)

/** Sheets are rounded on the top corners only. */
val SheetShape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)
