package com.rimagwinya.app.feature.cart

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.AmountRow
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.Chip
import com.rimagwinya.app.core.designsystem.component.EmptyState
import com.rimagwinya.app.core.designsystem.component.FoodIcon
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.MenuIcons
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmCard
import com.rimagwinya.app.core.designsystem.component.SectionLabel
import com.rimagwinya.app.core.designsystem.component.Stepper
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.domain.model.CartLine
import com.rimagwinya.app.domain.model.Slot

@Composable
fun CartScreen(
    onBrowseMenu: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadSlots() }

    Column(modifier.fillMaxSize()) {
        RmAppBar(title = stringResource(R.string.title_cart)) {
            if (!state.cart.isEmpty) {
                Text(
                    pluralStringResource(R.plurals.cart_item_count, state.cart.itemCount, state.cart.itemCount),
                    style = RmTheme.type.secondary,
                    color = RmTheme.colors.text2,
                )
            }
        }

        if (state.cart.isEmpty) {
            EmptyState(
                title = stringResource(R.string.cart_empty_title),
                body = stringResource(R.string.cart_empty_body),
                icon = R.drawable.ic_cart,
                action = {
                    RmButton(
                        text = stringResource(R.string.cart_browse),
                        onClick = onBrowseMenu,
                        style = RmButtonStyle.Secondary,
                        fillWidth = false,
                    )
                },
            )
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            verticalArrangement = Arrangement.spacedBy(Space.x24),
        ) {
            GroupedList {
                state.cart.lines.forEachIndexed { index, line ->
                    CartLineRow(
                        line = line,
                        onQuantity = { viewModel.setQuantity(line.key, it) },
                        onRemove = { viewModel.remove(line.key) },
                    )
                    if (index < state.cart.lines.lastIndex) ListDivider()
                }
            }

            Column {
                SectionLabel(stringResource(R.string.cart_slot_label))
                SlotPicker(state = state, onSelect = viewModel::selectSlot, onRetry = viewModel::loadSlots)
            }

            RmCard {
                AmountRow(stringResource(R.string.cart_subtotal), state.cart.total.format())
                AmountRow(stringResource(R.string.cart_total), state.cart.total.format(), strong = true)
            }

            RmButton(
                text = stringResource(
                    if (state.selectedSlot == null) R.string.cart_pick_slot else R.string.cart_continue
                ),
                onClick = onCheckout,
                enabled = state.canContinue,
            )
        }
    }
}

@Composable
private fun CartLineRow(line: CartLine, onQuantity: (Int) -> Unit, onRemove: () -> Unit) {
    val subtitle = listOfNotNull(
        line.optionsLabel.takeIf { it.isNotBlank() },
        stringResource(R.string.cart_each, line.unitPrice.format()),
    ).joinToString("\n")

    ListRow(
        title = line.item.name,
        subtitle = subtitle,
        showChevron = false,
        leading = { FoodIcon(MenuIcons.drawableFor(line.item.iconKey)) },
        trailing = {
            if (line.hasOuterQuantity) {
                // Down to zero removes the line.
                Stepper(value = line.quantity, onChange = onQuantity, min = 0)
            } else {
                // A build's count lives in its own steppers, so the cart
                // offers removal rather than a second way to say "how many".
                RmButton(
                    text = stringResource(R.string.cart_remove),
                    onClick = onRemove,
                    style = RmButtonStyle.Quiet,
                    small = true,
                    fillWidth = false,
                )
            }
        },
    )
}

@Composable
private fun SlotPicker(state: CartUiState, onSelect: (Slot) -> Unit, onRetry: () -> Unit) {
    when {
        state.slotsLoading -> Text(
            stringResource(R.string.cart_slots_loading),
            style = RmTheme.type.secondary,
            color = RmTheme.colors.text2,
        )

        state.slotsError != null -> Column(verticalArrangement = Arrangement.spacedBy(Space.x12)) {
            Banner(text = stringResource(state.slotsError), tone = BannerTone.Error)
            RmButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
                style = RmButtonStyle.Secondary,
            )
        }

        state.noSlotsLeft -> Banner(text = stringResource(R.string.cart_no_slots), tone = BannerTone.Info)

        else -> Column(verticalArrangement = Arrangement.spacedBy(Space.x8)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.x8),
            ) {
                state.slots.forEach { slot ->
                    Chip(
                        text = slot.name,
                        selected = slot.id == state.selectedSlotId,
                        onClick = { onSelect(slot) },
                        enabled = !slot.isFull,
                    )
                }
            }
            state.selectedSlot?.let { slot ->
                Text(
                    stringResource(R.string.cart_slot_taken, slot.timeRange, slot.ordersTaken, slot.capacity),
                    style = RmTheme.type.caption,
                    color = RmTheme.colors.text2,
                )
            }
            state.slots.filter { it.isFull }.forEach { slot ->
                Text(
                    stringResource(R.string.cart_slot_full, slot.name),
                    style = RmTheme.type.caption,
                    color = RmTheme.colors.text3,
                )
            }
        }
    }
}
