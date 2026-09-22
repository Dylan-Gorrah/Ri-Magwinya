package com.rimagwinya.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.theme.Motion
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.designsystem.theme.Stroke

enum class RmButtonStyle {
    /** Navy fill. The one thing this screen is for. */
    Primary,

    /** Outlined. Secondary actions sitting next to a primary. */
    Secondary,

    /** No fill, no border. Tertiary actions and "cancel". */
    Quiet,

    /** Red. Destructive and irreversible. */
    Danger,
}

/**
 * The app's button. Press feedback is a scale, not a colour change, which is
 * what makes it feel like the surface is being pushed rather than repainted.
 */
@Composable
fun RmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RmButtonStyle = RmButtonStyle.Primary,
    enabled: Boolean = true,
    small: Boolean = false,
    @androidx.annotation.DrawableRes leadingIcon: Int? = null,
    fillWidth: Boolean = true,
) {
    val c = RmTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) {
            if (small) Motion.PRESS_SCALE_SMALL else Motion.PRESS_SCALE_LARGE
        } else 1f,
        animationSpec = Motion.springy(),
        label = "buttonPress",
    )

    val container = when (style) {
        RmButtonStyle.Primary -> c.navy
        RmButtonStyle.Danger -> c.error
        RmButtonStyle.Secondary, RmButtonStyle.Quiet -> Color.Transparent
    }
    val content = when (style) {
        RmButtonStyle.Primary, RmButtonStyle.Danger -> Color.White
        RmButtonStyle.Secondary -> c.text
        RmButtonStyle.Quiet -> c.navy
    }
    val border = if (style == RmButtonStyle.Secondary) {
        BorderStroke(Stroke.hairline, c.borderStrong)
    } else null

    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .scale(scale)
            .defaultMinSize(minHeight = if (small) 40.dp else Space.touchTarget)
            .clip(RoundedCornerShape(Radius.r12))
            .background(if (enabled) container else container.copy(alpha = 0.4f))
            .then(border?.let { Modifier.border(it, RoundedCornerShape(Radius.r12)) } ?: Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(
                horizontal = if (small) Space.x12 else Space.x20,
                vertical = if (small) Space.x8 else Space.x12,
            ),
        horizontalArrangement = Arrangement.spacedBy(Space.x8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                tint = if (enabled) content else content.copy(alpha = 0.5f),
                modifier = Modifier.size(if (small) 16.dp else 18.dp),
            )
        }
        Text(
            text = text,
            style = RmTheme.type.bodyStrong,
            color = if (enabled) content else content.copy(alpha = 0.5f),
        )
    }
}

@Preview(name = "Buttons", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun RmButtonPreview() = RimagwinyaTheme {
    Column(
        Modifier.padding(Space.x20),
        verticalArrangement = Arrangement.spacedBy(Space.x12),
    ) {
        RmButton("Place order · R27.00", {})
        RmButton("Add to cart", {}, style = RmButtonStyle.Secondary)
        RmButton("Choose something first", {}, enabled = false)
        RmButton("Cancel order", {}, style = RmButtonStyle.Danger)
        RmButton("Skip for now", {}, style = RmButtonStyle.Quiet)
        Box {
            RmButton("Top up", {}, small = true, fillWidth = false, leadingIcon = R.drawable.ic_wallet)
        }
    }
}

@Preview(name = "Buttons dark", showBackground = true, backgroundColor = 0xFF0B1220,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RmButtonDarkPreview() = RimagwinyaTheme {
    Column(
        Modifier.padding(Space.x20),
        verticalArrangement = Arrangement.spacedBy(Space.x12),
    ) {
        RmButton("Place order · R27.00", {})
        RmButton("Add to cart", {}, style = RmButtonStyle.Secondary)
        RmButton("Cancel order", {}, style = RmButtonStyle.Danger)
    }
}
