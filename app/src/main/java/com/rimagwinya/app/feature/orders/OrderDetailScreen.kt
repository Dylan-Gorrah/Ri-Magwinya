package com.rimagwinya.app.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.AmountRow
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.CodeTiles
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.ProgressRail
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.component.RmBottomSheet
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmCard
import com.rimagwinya.app.core.designsystem.component.SectionLabel
import com.rimagwinya.app.core.designsystem.component.SkeletonRow
import com.rimagwinya.app.core.designsystem.component.StatusPill
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.util.resolve
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.OrderStatus
import com.rimagwinya.app.domain.model.PaymentMethod

@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order

    Column(modifier.fillMaxSize()) {
        RmAppBar(
            title = order?.let { stringResource(R.string.order_title, it.number.toInt()) }
                ?: stringResource(R.string.title_order),
            onBack = onBack,
        ) {
            order?.let { StatusPill(it.status.pill()) }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            verticalArrangement = Arrangement.spacedBy(Space.x24),
        ) {
            when {
                order == null && state.loading -> GroupedList {
                    repeat(4) { i -> SkeletonRow(); if (i < 3) ListDivider() }
                }

                order == null -> Column(verticalArrangement = Arrangement.spacedBy(Space.x12)) {
                    Banner(text = state.error?.resolve().orEmpty(), tone = BannerTone.Error)
                    RmButton(stringResource(R.string.action_retry), viewModel::load, style = RmButtonStyle.Secondary)
                }

                else -> OrderBody(
                    order = order,
                    error = state.error?.resolve(),
                    cancelling = state.cancelling,
                    onCancel = viewModel::askCancel,
                )
            }
        }
    }

    if (state.confirmingCancel) {
        RmBottomSheet(onDismiss = viewModel::dismissCancel) {
            Column(
                Modifier.padding(Space.screen),
                verticalArrangement = Arrangement.spacedBy(Space.x16),
            ) {
                Text(stringResource(R.string.order_cancel_confirm_title), style = RmTheme.type.screenTitle, color = RmTheme.colors.text)
                Text(stringResource(R.string.order_cancel_confirm_body), style = RmTheme.type.body, color = RmTheme.colors.text2)
                RmButton(stringResource(R.string.order_cancel), viewModel::cancel, style = RmButtonStyle.Danger)
                RmButton(stringResource(R.string.order_cancel_keep), viewModel::dismissCancel, style = RmButtonStyle.Secondary)
            }
        }
    }
}

@Composable
private fun OrderBody(order: Order, error: String?, cancelling: Boolean, onCancel: () -> Unit) {
    when (order.status) {
        OrderStatus.Ready -> Banner(text = stringResource(R.string.order_ready_banner), tone = BannerTone.Success)
        OrderStatus.Cancelled -> Banner(
            text = if (order.paymentMethod == PaymentMethod.Wallet) {
                stringResource(R.string.order_cancelled_refund, order.total.format())
            } else {
                stringResource(R.string.order_cancelled_banner)
            },
            tone = BannerTone.Info,
        )
        OrderStatus.NoShow -> Banner(text = stringResource(R.string.order_no_show_banner), tone = BannerTone.Warning)
        OrderStatus.Collected -> Banner(text = stringResource(R.string.order_collected_banner), tone = BannerTone.Success)
        else -> Unit
    }

    if (order.status.isActive) {
        RmCard {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Space.x8),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.x16),
            ) {
                Text(
                    stringResource(R.string.order_show_code).uppercase(),
                    style = RmTheme.type.label,
                    color = RmTheme.colors.text3,
                )
                CodeTiles(code = order.code)
                Text(
                    stringResource(R.string.order_slot_line, order.slotName.orEmpty(), order.slotTimeRange.orEmpty()),
                    style = RmTheme.type.caption,
                    color = RmTheme.colors.text2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (order.status != OrderStatus.Cancelled && order.status != OrderStatus.NoShow) {
        ProgressRail(steps = railStages(order).map { it.toStep() })
    }

    Column {
        SectionLabel(stringResource(R.string.order_items))
        GroupedList {
            order.lines.forEachIndexed { index, line ->
                ListRow(
                    title = "${line.quantity}× ${line.name}",
                    subtitle = line.optionsLabel,
                    showChevron = false,
                    trailing = { Text(line.subtotal.format(), style = RmTheme.type.price, color = RmTheme.colors.text) },
                )
                if (index < order.lines.lastIndex) ListDivider(inset = false)
            }
        }
    }

    AmountRow(
        stringResource(
            if (order.paymentMethod == PaymentMethod.Counter && order.status.isActive) {
                R.string.order_total_due
            } else {
                R.string.order_total_paid
            }
        ),
        order.total.format(),
        strong = true,
    )

    error?.let { Banner(text = it, tone = BannerTone.Error) }

    when {
        order.canCancel -> RmButton(
            text = stringResource(if (cancelling) R.string.order_cancelling else R.string.order_cancel),
            onClick = onCancel,
            style = RmButtonStyle.Danger,
            enabled = !cancelling,
        )
        // "Collected" is set by staff after checking the code — the student
        // has no button for it.
        order.status.isActive -> Text(
            stringResource(R.string.order_cannot_cancel),
            style = RmTheme.type.caption,
            color = RmTheme.colors.text2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
