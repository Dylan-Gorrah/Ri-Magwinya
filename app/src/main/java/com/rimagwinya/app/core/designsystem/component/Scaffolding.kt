package com.rimagwinya.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space

/**
 * The top of every screen except the menu: an optional back button, the
 * title, and whatever sits on the right (a count, a live pill, an action).
 */
@Composable
fun RmAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Space.x12)
            .padding(top = Space.x12, bottom = Space.x8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x8),
    ) {
        if (onBack != null) {
            IconAction(
                icon = R.drawable.ic_back,
                label = stringResource(R.string.action_back),
                onClick = onBack,
            )
        } else {
            Box(Modifier.size(Space.x8))
        }
        Text(
            text = title,
            style = RmTheme.type.screenTitle,
            color = RmTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
        Box(Modifier.size(Space.x4))
    }
}

/** A 48dp tappable icon with a spoken label. */
@Composable
fun IconAction(
    @androidx.annotation.DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
) {
    val c = RmTheme.colors
    Box(
        modifier = modifier
            .size(Space.touchTarget)
            .clip(RoundedCornerShape(Radius.r12))
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = c.text,
            modifier = Modifier.size(20.dp),
        )
        if (badge > 0) {
            Text(
                text = if (badge > 9) "9+" else badge.toString(),
                style = RmTheme.type.label,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = Space.x8, end = Space.x8)
                    .clip(RoundedCornerShape(Radius.r8))
                    .background(c.error)
                    .padding(horizontal = Space.x4),
            )
        }
    }
}

/** The small uppercase heading above a group. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = RmTheme.type.label,
        color = RmTheme.colors.text3,
        modifier = modifier.padding(start = Space.x4, bottom = Space.x8),
    )
}

/** A plain surface with a hairline border. Shadows are for sheets. */
@Composable
fun RmCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = RmTheme.colors
    val shape = RoundedCornerShape(Radius.r16)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(
                width = if (highlighted) 1.5.dp else 1.dp,
                color = if (highlighted) c.navy else c.border,
                shape = shape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Space.x16),
        verticalArrangement = Arrangement.spacedBy(Space.x8),
        content = content,
    )
}

/** "Subtotal ... R67.00". [strong] for the total line. */
@Composable
fun AmountRow(label: String, amount: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (strong) RmTheme.type.bodyStrong else RmTheme.type.secondary,
            color = if (strong) RmTheme.colors.text else RmTheme.colors.text2,
            modifier = Modifier.weight(1f),
        )
        Text(
            amount,
            style = if (strong) RmTheme.type.price.copy(fontSize = RmTheme.type.screenTitle.fontSize) else RmTheme.type.price,
            color = RmTheme.colors.text,
        )
    }
}

@Preview(name = "Scaffolding", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun ScaffoldingPreview() = RimagwinyaTheme {
    Column(verticalArrangement = Arrangement.spacedBy(Space.x16)) {
        RmAppBar(title = "Checkout", onBack = {})
        Column(Modifier.padding(horizontal = Space.screen)) {
            SectionLabel("Payment")
            RmCard(highlighted = true) {
                AmountRow("Subtotal", "R67.00")
                AmountRow("Total", "R67.00", strong = true)
            }
        }
    }
}
