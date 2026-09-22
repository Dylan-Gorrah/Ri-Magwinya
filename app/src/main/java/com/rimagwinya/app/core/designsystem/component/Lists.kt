package com.rimagwinya.app.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

/**
 * One container, hairline dividers between rows, no per-row shadow.
 *
 * The whole point: if every row were its own elevated card, nothing would
 * read as elevated. One border around the group, lines inside it.
 */
@Composable
fun GroupedList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = RmTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.r16))
            .background(c.surface)
            .border(Stroke.hairline, c.border, RoundedCornerShape(Radius.r16)),
        content = content,
    )
}

/** A hairline between two rows, inset so it clears the container edge. */
@Composable
fun ListDivider(inset: Boolean = true) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = if (inset) Space.x16 else 0.dp)
            .height(Stroke.hairline)
            .background(RmTheme.colors.border)
    )
}

/**
 * A row inside a [GroupedList]. Dimmed and unclickable when disabled, which
 * is how sold-out menu items read.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    showChevron: Boolean = onClick != null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val c = RmTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .alpha(if (enabled) 1f else 0.45f)
            .defaultMinSize(minHeight = Space.touchTarget + Space.x8)
            .padding(horizontal = Space.x16, vertical = Space.x12),
        horizontalArrangement = Arrangement.spacedBy(Space.x12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.x4)) {
            Text(title, style = RmTheme.type.bodyStrong, color = c.text)
            if (subtitle != null) {
                Text(subtitle, style = RmTheme.type.secondary, color = c.text2)
            }
        }
        trailing?.invoke(this)
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chev),
                contentDescription = null,
                tint = c.text3,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Placeholder row while real content loads. Pulses rather than spinning. */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.SLOW, easing = Motion.Smooth),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x16, vertical = Space.x16),
        horizontalArrangement = Arrangement.spacedBy(Space.x12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Radius.r12))
                .background(c.surfaceVariant.copy(alpha = alpha))
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.x8)) {
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(Radius.r8))
                    .background(c.surfaceVariant.copy(alpha = alpha))
            )
            Box(
                Modifier
                    .fillMaxWidth(0.3f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(Radius.r8))
                    .background(c.surfaceVariant.copy(alpha = alpha))
            )
        }
        Box(
            Modifier
                .width(48.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(Radius.r8))
                .background(c.surfaceVariant.copy(alpha = alpha))
        )
    }
}

@Preview(name = "Grouped list", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun GroupedListPreview() = RimagwinyaTheme {
    Column(
        Modifier.padding(Space.x20),
        verticalArrangement = Arrangement.spacedBy(Space.x16),
    ) {
        GroupedList {
            ListRow(
                title = "Vetkoek",
                subtitle = "Golden, crisp, filled how you like it",
                leading = { FoodIcon(R.drawable.ic_food_vetkoek) },
                trailing = {
                    Text("from R3.00", style = RmTheme.type.price, color = RmTheme.colors.text)
                },
                onClick = {},
                showChevron = false,
            )
            ListDivider()
            ListRow(
                title = "Score Energy 500ml",
                subtitle = "Ice cold",
                leading = { FoodIcon(R.drawable.ic_food_energy) },
                trailing = { StockPill(StockState.Low(4)) },
                onClick = {},
                showChevron = false,
            )
            ListDivider()
            ListRow(
                title = "Doritos",
                subtitle = "Sold out for today",
                leading = { FoodIcon(R.drawable.ic_food_triangle) },
                trailing = { StockPill(StockState.SoldOut) },
                enabled = false,
                showChevron = false,
            )
        }
        GroupedList {
            SkeletonRow()
            ListDivider()
            SkeletonRow()
        }
    }
}
