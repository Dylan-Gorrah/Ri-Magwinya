package com.rimagwinya.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.theme.Motion
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.designsystem.theme.Stroke
import kotlinx.coroutines.delay

/**
 * The 4-digit collection code, one digit per tile.
 *
 * This is the thing a student holds up at the counter, so it is the biggest
 * element on the order screen. Tiles spring in one after another.
 */
@Composable
fun CodeTiles(
    code: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.x8),
    ) {
        code.forEachIndexed { index, digit ->
            CodeTile(digit = digit, index = index, animate = animate)
        }
    }
}

@Composable
private fun CodeTile(digit: Char, index: Int, animate: Boolean) {
    val c = RmTheme.colors
    var landed by remember { mutableStateOf(!animate) }
    LaunchedEffect(digit) {
        if (animate) {
            delay(60L * index)
            landed = true
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (landed) 1f else 0.6f,
        animationSpec = Motion.springy(),
        label = "codeTile",
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(width = 58.dp, height = 68.dp)
            .clip(RoundedCornerShape(Radius.r16))
            .background(c.surface)
            .border(Stroke.hairline, c.border, RoundedCornerShape(Radius.r16)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit.toString(),
            style = RmTheme.type.largeTitle.copy(fontSize = 32.sp),
            color = c.navy,
            textAlign = TextAlign.Center,
        )
    }
}

/** One step on a [ProgressRail]. */
data class RailStep(
    val label: String,
    val timestamp: String? = null,
    val done: Boolean = false,
    val current: Boolean = false,
)

/**
 * The vertical order timeline. Filled for done, ringed for current, grey for
 * not yet, with the real timestamp against each step that has happened.
 */
@Composable
fun ProgressRail(
    steps: List<RailStep>,
    modifier: Modifier = Modifier,
) {
    val c = RmTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.x12),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    step.done -> c.success
                                    step.current -> Color.Transparent
                                    else -> c.surfaceVariant
                                }
                            )
                            .then(
                                if (step.current) {
                                    Modifier.border(2.dp, c.navy, CircleShape)
                                } else Modifier
                            ),
                    )
                    if (index != steps.lastIndex) {
                        Box(
                            Modifier
                                .width(Stroke.hairline)
                                .height(Space.x32)
                                .background(if (step.done) c.success.copy(alpha = 0.4f) else c.border)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (index == steps.lastIndex) 0.dp else Space.x8),
                    verticalArrangement = Arrangement.spacedBy(Space.x4),
                ) {
                    Text(
                        text = step.label,
                        style = RmTheme.type.bodyStrong,
                        color = if (step.done || step.current) c.text else c.text3,
                    )
                    if (step.timestamp != null) {
                        Text(step.timestamp, style = RmTheme.type.caption, color = c.text3)
                    }
                }
            }
        }
    }
}

/**
 * A tab in the bottom bar. Holds the string resource rather than the text,
 * so the tab list can be a plain top-level val and still translate.
 */
data class RoleTab(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val icon: Int,
    val route: String,
)

/**
 * The bottom bar. Both roles use it; only the four tabs differ, which is why
 * it takes its items rather than knowing about them.
 */
@Composable
fun RoleTabBar(
    tabs: List<RoleTab>,
    selectedRoute: String?,
    onSelect: (RoleTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = RmTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface)
            .padding(top = Stroke.hairline)
            .padding(horizontal = Space.x8, vertical = Space.x8),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == selectedRoute
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.r12))
                    .clickable { onSelect(tab) }
                    .padding(vertical = Space.x8),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.x4),
            ) {
                Icon(
                    painter = painterResource(tab.icon),
                    contentDescription = null,
                    tint = if (selected) c.navy else c.text3,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(tab.labelRes),
                    style = RmTheme.type.caption,
                    color = if (selected) c.navy else c.text3,
                )
            }
        }
    }
}

@Preview(name = "Order pieces", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun OrderPiecesPreview() = RimagwinyaTheme {
    Column(
        Modifier.padding(Space.x20),
        verticalArrangement = Arrangement.spacedBy(Space.x24),
    ) {
        CodeTiles(code = "4729", animate = false)
        ProgressRail(
            steps = listOf(
                RailStep("Placed", "09:12", done = true),
                RailStep("Preparing", "09:24", done = true),
                RailStep("Ready", current = true),
                RailStep("Collected"),
            )
        )
    }
}
