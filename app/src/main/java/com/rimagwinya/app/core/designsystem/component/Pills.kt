package com.rimagwinya.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space

/**
 * Status is never colour alone. Every pill carries a word, because a colour
 * on its own is invisible to a colourblind student and hard work for
 * everyone else in the sun.
 */
@Composable
private fun Pill(text: String, fg: Color, bg: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RmTheme.type.label,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.r8))
            .background(bg)
            .padding(horizontal = Space.x8, vertical = Space.x4),
    )
}

/** What the menu shows against each item. */
sealed interface StockState {
    data object InStock : StockState
    data class Low(val remaining: Int) : StockState
    data object SoldOut : StockState

    companion object {
        fun of(quantity: Int, reorderLevel: Int, available: Boolean): StockState = when {
            !available || quantity <= 0 -> SoldOut
            quantity <= reorderLevel -> Low(quantity)
            else -> InStock
        }
    }
}

@Composable
fun StockPill(state: StockState, modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    when (state) {
        StockState.InStock ->
            Pill(stringResource(R.string.stock_in), c.success, c.success.copy(alpha = 0.12f), modifier)
        is StockState.Low ->
            Pill(stringResource(R.string.stock_low, state.remaining), c.warning, c.warning.copy(alpha = 0.14f), modifier)
        StockState.SoldOut ->
            Pill(stringResource(R.string.stock_sold_out), c.error, c.error.copy(alpha = 0.12f), modifier)
    }
}

/** Where an order has got to. Mirrors the order_status enum. */
enum class OrderState { Placed, Preparing, Ready, Collected, Cancelled, NoShow }

@Composable
fun StatusPill(state: OrderState, modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    val (labelRes, fg) = when (state) {
        OrderState.Placed -> R.string.status_placed to c.sky
        OrderState.Preparing -> R.string.status_preparing to c.warning
        OrderState.Ready -> R.string.status_ready to c.success
        OrderState.Collected -> R.string.status_collected to c.text2
        OrderState.Cancelled -> R.string.status_cancelled to c.text2
        OrderState.NoShow -> R.string.status_no_show to c.error
    }
    Pill(stringResource(labelRes), fg, fg.copy(alpha = 0.12f), modifier)
}

@Preview(name = "Pills", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun PillsPreview() = RimagwinyaTheme {
    Column(
        Modifier.padding(Space.x20),
        verticalArrangement = Arrangement.spacedBy(Space.x12),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.x8)) {
            StockPill(StockState.InStock)
            StockPill(StockState.Low(4))
            StockPill(StockState.SoldOut)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.x8)) {
            StatusPill(OrderState.Placed)
            StatusPill(OrderState.Preparing)
            StatusPill(OrderState.Ready)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.x8)) {
            StatusPill(OrderState.Collected)
            StatusPill(OrderState.Cancelled)
            StatusPill(OrderState.NoShow)
        }
    }
}
