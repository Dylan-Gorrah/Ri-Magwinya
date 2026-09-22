package com.rimagwinya.app.feature.staff

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.Chip
import com.rimagwinya.app.core.designsystem.component.EmptyState
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmCard
import com.rimagwinya.app.core.designsystem.component.SkeletonRow
import com.rimagwinya.app.core.designsystem.component.StatusPill
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.util.resolve
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.OrderStatus
import com.rimagwinya.app.domain.model.PaymentMethod
import com.rimagwinya.app.domain.model.clockTime
import com.rimagwinya.app.feature.orders.pill

@Composable
fun QueueScreen(
    onTopUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        RmAppBar(title = stringResource(R.string.queue_title)) {
            RmButton(
                text = stringResource(R.string.queue_top_up),
                onClick = onTopUp,
                style = RmButtonStyle.Secondary,
                small = true,
                fillWidth = false,
            )
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Space.screen, end = Space.screen, bottom = Space.x32,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x12),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x8)) {
                    Counter(stringResource(R.string.queue_new), state.newCount, Modifier.weight(1f))
                    Counter(stringResource(R.string.queue_preparing), state.preparingCount, Modifier.weight(1f))
                    Counter(stringResource(R.string.queue_ready), state.readyCount, Modifier.weight(1f))
                }
            }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Space.x8),
                ) {
                    QueueFilter.entries.forEach { filter ->
                        Chip(
                            text = stringResource(filter.labelRes()),
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                        )
                    }
                }
            }
            state.error?.let { error ->
                item { Banner(text = error.resolve(), tone = BannerTone.Error) }
            }

            when {
                state.loading && state.orders.isEmpty() -> item {
                    GroupedList { repeat(3) { i -> SkeletonRow(); if (i < 2) ListDivider() } }
                }

                state.visible.isEmpty() -> item {
                    EmptyState(
                        title = stringResource(R.string.queue_empty_title, stringResource(state.filter.labelRes())),
                        body = stringResource(R.string.queue_empty_body),
                        icon = R.drawable.ic_check,
                    )
                }

                else -> items(state.visible, key = { it.id }) { order ->
                    QueueCard(
                        order = order,
                        busy = order.id in state.busy,
                        canNoShow = state.canMarkNoShow(order),
                        onAdvance = { viewModel.advance(order) },
                        onNoShow = { viewModel.noShow(order) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Counter(label: String, value: Int, modifier: Modifier) {
    RmCard(modifier) {
        Text(label, style = RmTheme.type.caption, color = RmTheme.colors.text2)
        Text(value.toString(), style = RmTheme.type.largeTitle, color = RmTheme.colors.text)
    }
}

@Composable
private fun QueueCard(
    order: Order,
    busy: Boolean,
    canNoShow: Boolean,
    onAdvance: () -> Unit,
    onNoShow: () -> Unit,
) {
    RmCard(highlighted = order.status == OrderStatus.Ready) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.queue_card_header, "#${order.number}", order.placedAt.clockTime()) +
                    (order.slotName?.let { " · $it" } ?: ""),
                style = RmTheme.type.secondary,
                color = RmTheme.colors.text2,
                modifier = Modifier.weight(1f),
            )
            StatusPill(order.status.pill())
        }

        // The full option label, exactly as the student built it — this is
        // what the kitchen makes.
        order.lines.forEach { line ->
            Text(
                if (line.optionsLabel.isNullOrBlank()) {
                    stringResource(R.string.queue_line, line.quantity, line.name)
                } else {
                    stringResource(R.string.queue_line_with_options, line.quantity, line.name, line.optionsLabel)
                },
                style = RmTheme.type.body,
                color = RmTheme.colors.text,
            )
        }

        Text(
            stringResource(R.string.queue_code, order.code, order.studentName.orEmpty()),
            style = RmTheme.type.bodyStrong,
            color = RmTheme.colors.text,
        )
        if (order.paymentMethod == PaymentMethod.Counter) {
            Text(
                stringResource(R.string.queue_counter_due, order.total.format()),
                style = RmTheme.type.caption,
                color = RmTheme.colors.warning,
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.x8, Alignment.End),
        ) {
            if (canNoShow) {
                RmButton(
                    text = stringResource(R.string.queue_no_show),
                    onClick = onNoShow,
                    style = RmButtonStyle.Danger,
                    small = true,
                    fillWidth = false,
                    enabled = !busy,
                )
            }
            nextLabel(order.status)?.let { label ->
                RmButton(
                    text = stringResource(label),
                    onClick = onAdvance,
                    small = true,
                    fillWidth = false,
                    enabled = !busy,
                )
            }
        }
    }
}

private fun nextLabel(status: OrderStatus): Int? = when (status) {
    OrderStatus.Placed -> R.string.queue_start
    OrderStatus.Preparing -> R.string.queue_mark_ready
    OrderStatus.Ready -> R.string.queue_mark_collected
    else -> null
}

private fun QueueFilter.labelRes(): Int = when (this) {
    QueueFilter.New -> R.string.queue_new
    QueueFilter.Preparing -> R.string.queue_preparing
    QueueFilter.Ready -> R.string.queue_ready
    QueueFilter.All -> R.string.queue_all
}
