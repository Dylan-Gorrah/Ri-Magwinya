package com.rimagwinya.app.feature.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.AmountRow
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmCard
import com.rimagwinya.app.core.designsystem.component.SectionLabel
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.util.resolve
import com.rimagwinya.app.domain.model.PaymentMethod

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onPlaced: (orderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is CheckoutEvent.Placed -> onPlaced(event.orderId)
                CheckoutEvent.PickAnotherSlot -> Unit // The banner explains; the cart is one tap back.
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        RmAppBar(title = stringResource(R.string.title_checkout), onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            verticalArrangement = Arrangement.spacedBy(Space.x24),
        ) {
            Column {
                SectionLabel(stringResource(R.string.checkout_summary))
                GroupedList {
                    state.cart.lines.forEachIndexed { index, line ->
                        ListRow(
                            title = "${line.quantity}× ${line.item.name}",
                            subtitle = line.optionsLabel.takeIf { it.isNotBlank() },
                            showChevron = false,
                            trailing = {
                                Text(line.total.format(), style = RmTheme.type.price, color = RmTheme.colors.text)
                            },
                        )
                        if (index < state.cart.lines.lastIndex) ListDivider(inset = false)
                    }
                }
            }

            Column {
                SectionLabel(stringResource(R.string.checkout_collection))
                RmCard(onClick = onBack) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                state.slot?.name.orEmpty(),
                                style = RmTheme.type.bodyStrong,
                                color = RmTheme.colors.text,
                            )
                            Text(
                                stringResource(R.string.checkout_today_at, state.slot?.timeRange.orEmpty()),
                                style = RmTheme.type.caption,
                                color = RmTheme.colors.text2,
                            )
                        }
                        Icon(
                            painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.cart_pick_slot),
                            tint = RmTheme.colors.text2,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Space.x12)) {
                SectionLabel(stringResource(R.string.checkout_payment))

                PaymentOption(
                    icon = R.drawable.ic_wallet,
                    title = stringResource(R.string.checkout_wallet),
                    subtitle = stringResource(
                        R.string.checkout_wallet_balance,
                        state.profile?.walletBalance?.format().orEmpty(),
                    ),
                    selected = state.method == PaymentMethod.Wallet,
                    enabled = true,
                    onClick = { viewModel.choose(PaymentMethod.Wallet) },
                )
                if (state.method == PaymentMethod.Wallet) {
                    val short = state.walletShortBy
                    if (short != null) {
                        Banner(
                            text = stringResource(R.string.checkout_wallet_short, short.format()),
                            tone = BannerTone.Error,
                        )
                        Text(
                            stringResource(R.string.checkout_topup_note),
                            style = RmTheme.type.caption,
                            color = RmTheme.colors.text3,
                        )
                    } else if (state.profile != null) {
                        Text(
                            stringResource(
                                R.string.checkout_wallet_after,
                                (state.profile!!.walletBalance - state.total).format(),
                            ),
                            style = RmTheme.type.caption,
                            color = RmTheme.colors.text2,
                        )
                    }
                }

                PaymentOption(
                    icon = R.drawable.ic_store,
                    title = stringResource(R.string.checkout_counter),
                    subtitle = when (state.counterBlock) {
                        CounterBlock.NoTopUp -> stringResource(R.string.checkout_counter_no_topup)
                        CounterBlock.NoShows -> stringResource(R.string.checkout_counter_no_shows)
                        null -> stringResource(R.string.checkout_counter_body)
                    },
                    selected = state.method == PaymentMethod.Counter,
                    enabled = state.counterAvailable,
                    onClick = { viewModel.choose(PaymentMethod.Counter) },
                )
                if (state.method == PaymentMethod.Counter) {
                    Banner(text = stringResource(R.string.checkout_counter_warning), tone = BannerTone.Warning)
                }
            }

            Banner(text = stringResource(R.string.checkout_cancel_notice), tone = BannerTone.Info)

            state.error?.let { Banner(text = it.resolve(), tone = BannerTone.Error) }

            Column(verticalArrangement = Arrangement.spacedBy(Space.x12)) {
                AmountRow(stringResource(R.string.cart_total), state.total.format(), strong = true)
                RmButton(
                    text = if (state.placing) {
                        stringResource(R.string.checkout_placing)
                    } else {
                        stringResource(R.string.checkout_place, state.total.format())
                    },
                    onClick = viewModel::place,
                    enabled = state.canPlace,
                )
            }
        }
    }
}

@Composable
private fun PaymentOption(
    icon: Int,
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    RmCard(highlighted = selected && enabled, onClick = if (enabled) onClick else null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.x12),
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = if (enabled) RmTheme.colors.navy else RmTheme.colors.text3,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = RmTheme.type.bodyStrong,
                    color = if (enabled) RmTheme.colors.text else RmTheme.colors.text3,
                )
                Text(subtitle, style = RmTheme.type.caption, color = RmTheme.colors.text2)
            }
            if (selected && enabled) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = RmTheme.colors.navy,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
