package com.rimagwinya.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

/**
 * Minus, a number, plus. Used for cart quantities, vetkoek counts, fillings
 * and staff stock adjustments, so it has to work at 48dp touch targets.
 */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 99,
    enabled: Boolean = true,
) {
    val c = RmTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.r10))
            .background(c.surfaceVariant)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(R.drawable.ic_minus, enabled && value > min) { onChange(value - 1) }
        Text(
            text = value.toString(),
            style = RmTheme.type.bodyStrong,
            // A non-zero count is the thing worth noticing on a long option list
            color = if (value > 0) c.navy else c.text2,
            modifier = Modifier.width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepperButton(R.drawable.ic_plus, enabled && value < max) { onChange(value + 1) }
    }
}

@Composable
private fun StepperButton(
    @androidx.annotation.DrawableRes icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = RmTheme.colors
    Box(
        modifier = Modifier
            .size(Space.touchTarget - Space.x8)
            .clip(RoundedCornerShape(Radius.r10))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (enabled) c.text else c.text3,
            modifier = Modifier.size(15.dp),
        )
    }
}

/** A horizontally scrolling row of single-select chips. Menu categories, filters. */
@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.x8),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.screen),
    ) {
        items(options) { option ->
            Chip(
                text = label(option),
                selected = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = RmTheme.colors
    Text(
        text = text,
        style = RmTheme.type.body,
        color = if (selected) Color.White else c.text2,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.r10))
            .background(if (selected) c.navy else c.surface)
            .then(
                if (selected) Modifier
                else Modifier.border(Stroke.hairline, c.border, RoundedCornerShape(Radius.r10))
            )
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.x16, vertical = Space.x8),
    )
}

/** A labelled row with a switch. Settings screen. */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    ListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        enabled = enabled,
        showChevron = false,
        onClick = { onCheckedChange(!checked) },
        trailing = { RmToggle(checked = checked, enabled = enabled) },
    )
}

/** The switch itself. Springs rather than sliding linearly. */
@Composable
fun RmToggle(checked: Boolean, enabled: Boolean = true) {
    val c = RmTheme.colors
    val offset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = Motion.springy(),
        label = "toggle",
    )
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(if (checked) c.navy else c.surfaceVariant)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = 18.dp * offset)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Preview(name = "Inputs", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun InputsPreview() = RimagwinyaTheme {
    var qty by remember { mutableIntStateOf(2) }
    var category by remember { mutableStateOf("Meals") }
    var notify by remember { mutableStateOf(true) }
    var biometric by remember { mutableStateOf(false) }

    Column(
        Modifier.padding(vertical = Space.x20),
        verticalArrangement = Arrangement.spacedBy(Space.x16),
    ) {
        ChipRow(
            options = listOf("All", "Meals", "Snacks", "Drinks"),
            selected = category,
            onSelect = { category = it },
            label = { it },
        )
        Row(
            Modifier.padding(horizontal = Space.screen),
            horizontalArrangement = Arrangement.spacedBy(Space.x16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Stepper(value = qty, onChange = { qty = it })
            Stepper(value = 0, onChange = {})
            Stepper(value = 5, onChange = {}, enabled = false)
        }
        Column(Modifier.padding(horizontal = Space.screen)) {
            GroupedList {
                ToggleRow(
                    title = "Order notifications",
                    subtitle = "Tell me when my order is ready",
                    checked = notify,
                    onCheckedChange = { notify = it },
                )
                ListDivider()
                ToggleRow(
                    title = "Biometric unlock",
                    checked = biometric,
                    onCheckedChange = { biometric = it },
                )
            }
        }
    }
}
