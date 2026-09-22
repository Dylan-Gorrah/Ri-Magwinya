package com.rimagwinya.app.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.designsystem.theme.Stroke

enum class BannerTone { Info, Warning, Error, Success }

/**
 * An inline message attached to the content it is about. Not a dialog, not a
 * toast, and never covering anything up.
 */
@Composable
fun Banner(
    text: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Info,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val c = RmTheme.colors
    val (accent, icon) = when (tone) {
        BannerTone.Info -> c.sky to R.drawable.ic_sig
        BannerTone.Warning -> c.warning to R.drawable.ic_warn
        BannerTone.Error -> c.error to R.drawable.ic_warn
        BannerTone.Success -> c.success to R.drawable.ic_check
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.r12))
            .background(accent.copy(alpha = 0.10f))
            .border(Stroke.hairline, accent.copy(alpha = 0.25f), RoundedCornerShape(Radius.r12))
            .padding(Space.x12),
        horizontalArrangement = Arrangement.spacedBy(Space.x12),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.x4)) {
            if (title != null) {
                Text(title, style = RmTheme.type.bodyStrong, color = c.text)
            }
            Text(text, style = RmTheme.type.secondary, color = c.text2)
            if (action != null) {
                Box(Modifier.padding(top = Space.x4)) { action() }
            }
        }
    }
}

/**
 * The styled snackbar. One message at a time, sliding up from the bottom.
 * Deliberately singular: stacked toasts are how people stop reading them.
 */
@Composable
fun Hud(
    message: String?,
    modifier: Modifier = Modifier,
    @androidx.annotation.DrawableRes icon: Int = R.drawable.ic_check,
) {
    val c = RmTheme.colors
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .padding(Space.x16)
                .clip(RoundedCornerShape(Radius.r12))
                .background(c.text)
                .padding(horizontal = Space.x16, vertical = Space.x12),
            horizontalArrangement = Arrangement.spacedBy(Space.x8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (c.isDark) c.background else Color.White,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = message.orEmpty(),
                style = RmTheme.type.body,
                color = if (c.isDark) c.background else Color.White,
            )
        }
    }
}

/** Nothing here, and why. Never a blank screen. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    @androidx.annotation.DrawableRes icon: Int = R.drawable.ic_box,
    action: (@Composable () -> Unit)? = null,
) {
    val c = RmTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.screen, vertical = Space.x48),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.x12),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(Radius.r16))
                .background(c.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = c.text3,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(title, style = RmTheme.type.bodyStrong, color = c.text, textAlign = TextAlign.Center)
        if (body != null) {
            Text(body, style = RmTheme.type.secondary, color = c.text2, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Box(Modifier.padding(top = Space.x8)) { action() }
        }
    }
}

@Preview(name = "Feedback", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun FeedbackPreview() = RimagwinyaTheme {
    Column(
        Modifier.padding(Space.x20),
        verticalArrangement = Arrangement.spacedBy(Space.x12),
    ) {
        Banner(
            tone = BannerTone.Info,
            text = "Rain expected. Order ahead so you are not queueing outside.",
        )
        Banner(
            tone = BannerTone.Warning,
            title = "Running low",
            text = "Score Energy (4), MoFaya (3)",
        )
        Banner(
            tone = BannerTone.Error,
            text = "Vetkoek just sold out. Take it out of your cart to carry on.",
        )
        Banner(tone = BannerTone.Success, text = "R50.00 added to your wallet.")
        Hud(message = "Signed in as Thabo Mokoena")
        EmptyState(
            title = "Nothing in your cart yet",
            body = "Add something from the menu and it will show up here.",
            icon = R.drawable.ic_cart,
        )
    }
}
