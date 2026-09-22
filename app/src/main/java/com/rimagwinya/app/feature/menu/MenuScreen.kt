package com.rimagwinya.app.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.Chip
import com.rimagwinya.app.core.designsystem.component.EmptyState
import com.rimagwinya.app.core.designsystem.component.FoodIcon
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.MenuIcons
import com.rimagwinya.app.core.designsystem.component.RmBottomSheet
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmTextField
import com.rimagwinya.app.core.designsystem.component.SkeletonRow
import com.rimagwinya.app.core.designsystem.component.StockPill
import com.rimagwinya.app.core.designsystem.component.StockState
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.feature.orders.pill
import com.rimagwinya.app.domain.pricing.PriceCalculator

/**
 * The student's home screen.
 *
 * The weather strip and the active order card land in Phases 14 and 7. What
 * is here is the menu itself: the hero, search, categories, and one grouped
 * list with a stock pill on every row.
 */
@Composable
fun MenuScreen(
    fullName: String,
    balance: Money,
    onOpenOrder: (orderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.loadActiveOrder() }

    Column(modifier = modifier.fillMaxSize()) {
        MenuHero(fullName = fullName, balance = balance)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = Space.x32
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x16),
        ) {
            state.activeOrder?.let { order ->
                item {
                    Box(Modifier.padding(horizontal = Space.screen).padding(top = Space.x16)) {
                        ActiveOrderCard(order = order, onClick = { onOpenOrder(order.id) })
                    }
                }
            }

            item {
                Box(Modifier.padding(horizontal = Space.screen, vertical = Space.x16)) {
                    RmTextField(
                        value = state.query,
                        onValueChange = viewModel::onQuery,
                        label = stringResource(R.string.menu_search),
                        placeholder = stringResource(R.string.menu_search),
                        leadingIcon = R.drawable.ic_search,
                        keyboardType = KeyboardType.Text,
                    )
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.screen),
                    horizontalArrangement = Arrangement.spacedBy(Space.x8),
                ) {
                    CategoryFilter.entries.forEach { filter ->
                        Chip(
                            text = stringResource(filter.labelRes()),
                            selected = state.category == filter,
                            onClick = { viewModel.onCategory(filter) },
                        )
                    }
                }
            }

            when {
                state.notConfigured -> item {
                    Box(Modifier.padding(horizontal = Space.screen)) {
                        Banner(
                            tone = BannerTone.Warning,
                            text = stringResource(R.string.dev_backend_not_configured),
                        )
                    }
                }

                state.isLoading -> item {
                    Box(Modifier.padding(horizontal = Space.screen)) {
                        GroupedList {
                            repeat(6) { index ->
                                SkeletonRow()
                                if (index < 5) ListDivider()
                            }
                        }
                    }
                }

                state.error != null -> item {
                    Column(
                        Modifier.padding(horizontal = Space.screen),
                        verticalArrangement = Arrangement.spacedBy(Space.x12),
                    ) {
                        Banner(
                            tone = BannerTone.Error,
                            text = state.error?.let { stringResource(it) }.orEmpty(),
                        )
                        RmButton(
                            text = stringResource(R.string.action_retry),
                            onClick = viewModel::load,
                            style = RmButtonStyle.Secondary,
                        )
                    }
                }

                state.isEmpty -> item {
                    EmptyState(
                        title = stringResource(R.string.menu_empty_title),
                        body = stringResource(R.string.menu_empty_body),
                        icon = R.drawable.ic_search,
                    )
                }

                else -> {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.screen),
                            horizontalArrangement = Arrangement.spacedBy(Space.x8),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.menu_today).uppercase(),
                                style = RmTheme.type.label,
                                color = RmTheme.colors.text3,
                            )
                            LivePill()
                        }
                    }

                    item {
                        Box(Modifier.padding(horizontal = Space.screen)) {
                            GroupedList {
                                state.visible.forEachIndexed { index, item ->
                                    MenuItemRow(
                                        item = item,
                                        onClick = { viewModel.openItem(item) },
                                    )
                                    if (index < state.visible.lastIndex) ListDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.openItem?.let { item ->
        RmBottomSheet(onDismiss = viewModel::closeSheet) {
            ItemSheetContent(
                item = item,
                onAdd = { selection -> viewModel.addToCart(item, selection) },
            )
        }
    }
}

@Composable
private fun MenuHero(fullName: String, balance: Money) {
    val c = RmTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.navyDeep)
            .statusBarsPadding()
            .padding(horizontal = Space.screen)
            .padding(top = Space.x20, bottom = Space.x24),
        verticalArrangement = Arrangement.spacedBy(Space.x8),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.x4)) {
                Text(
                    stringResource(R.string.menu_greeting, fullName),
                    style = RmTheme.type.largeTitle,
                    color = Color.White,
                )
                Text(
                    stringResource(R.string.menu_balance, balance.format()),
                    style = RmTheme.type.secondary,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            Box(
                Modifier
                    .size(Space.touchTarget)
                    .clip(RoundedCornerShape(Radius.r12))
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = stringResource(R.string.menu_notifications),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ActiveOrderCard(order: Order, onClick: () -> Unit) {
    com.rimagwinya.app.core.designsystem.component.RmCard(highlighted = true, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.x4)) {
                Text(
                    stringResource(R.string.active_order_title, order.number.toInt(), order.code),
                    style = RmTheme.type.bodyStrong,
                    color = RmTheme.colors.text,
                )
                Text(
                    stringResource(R.string.order_slot_line, order.slotName.orEmpty(), order.slotTimeRange.orEmpty()),
                    style = RmTheme.type.caption,
                    color = RmTheme.colors.text2,
                )
            }
            com.rimagwinya.app.core.designsystem.component.StatusPill(order.status.pill())
        }
    }
}

@Composable
private fun LivePill() {
    val c = RmTheme.colors
    Text(
        text = stringResource(R.string.menu_live).uppercase(),
        style = RmTheme.type.label,
        color = c.success,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.r8))
            .background(c.success.copy(alpha = 0.12f))
            .padding(horizontal = Space.x8, vertical = Space.x4),
    )
}

@Composable
private fun MenuItemRow(item: MenuItem, onClick: () -> Unit) {
    val price = if (PriceCalculator.hasVariablePrice(item)) {
        stringResource(R.string.menu_from_price, PriceCalculator.fromPrice(item).format())
    } else {
        item.price.format()
    }

    ListRow(
        title = item.name,
        subtitle = item.description,
        // Sold-out rows are dimmed and will not open.
        enabled = !item.isSoldOut,
        onClick = onClick,
        showChevron = false,
        leading = { FoodIcon(MenuIcons.drawableFor(item.iconKey)) },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(price, style = RmTheme.type.price, color = RmTheme.colors.text)
                StockPill(
                    StockState.of(item.stockQuantity, item.reorderLevel, item.isAvailable)
                )
            }
        },
    )
}

private fun CategoryFilter.labelRes(): Int = when (this) {
    CategoryFilter.All -> R.string.menu_category_all
    CategoryFilter.Meals -> R.string.menu_category_meals
    CategoryFilter.Snacks -> R.string.menu_category_snacks
    CategoryFilter.Drinks -> R.string.menu_category_drinks
}

@Preview(name = "Menu rows", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun MenuRowsPreview() = RimagwinyaTheme {
    Column {
        MenuHero(fullName = "Thabo", balance = Money.ofCents(4250))
        Box(Modifier.padding(Space.screen)) {
            GroupedList {
                PreviewMenu.all.forEachIndexed { index, item ->
                    MenuItemRow(item = item, onClick = {})
                    if (index < PreviewMenu.all.lastIndex) ListDivider()
                }
            }
        }
    }
}
