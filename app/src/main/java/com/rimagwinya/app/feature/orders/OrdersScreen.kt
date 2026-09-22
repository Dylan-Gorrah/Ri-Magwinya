package com.rimagwinya.app.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.R
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.EmptyState
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.SectionLabel
import com.rimagwinya.app.core.designsystem.component.SkeletonRow
import com.rimagwinya.app.core.designsystem.component.StatusPill
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.util.resolve
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.data.repository.OrderRepository
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.clockTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val loading: Boolean = true,
    val error: Int? = null,
) {
    val inProgress: List<Order> get() = orders.filter { it.status.isActive }
    val past: List<Order> get() = orders.filterNot { it.status.isActive }
    val isEmpty: Boolean get() = !loading && error == null && orders.isEmpty()
}

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val repository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    init {
        // The cache first, so the list is there before the network answers.
        viewModelScope.launch {
            repository.observeOrders().collect { cached ->
                _state.update { it.copy(orders = cached, loading = false) }
            }
        }
        if (AppConfig.isBackendConfigured) {
            viewModelScope.launch { repository.orderChanges().collect { load() } }
        }
    }

    fun load() {
        viewModelScope.launch {
            repository.orders()
                .onSuccess { list -> _state.update { it.copy(orders = list, loading = false, error = null) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, error = if (it.orders.isEmpty()) e.asApiError().messageRes() else null)
                    }
                }
        }
    }
}

@Composable
fun OrdersScreen(
    onOpen: (orderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrdersViewModel = hiltViewModel(),
    syncViewModel: com.rimagwinya.app.feature.offline.SyncViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sync by syncViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier.fillMaxSize()) {
        RmAppBar(title = stringResource(R.string.title_orders))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            verticalArrangement = Arrangement.spacedBy(Space.x24),
        ) {
            // Orders placed with no signal, before anything the server knows about.
            sync.waiting.forEach { action ->
                Banner(
                    title = stringResource(R.string.offline_queued_title),
                    text = stringResource(R.string.offline_queued_body, action.summary),
                    tone = BannerTone.Info,
                )
            }
            sync.refused.forEach { action ->
                Column(verticalArrangement = Arrangement.spacedBy(Space.x8)) {
                    Banner(
                        title = stringResource(R.string.offline_failed_title),
                        text = com.rimagwinya.app.feature.cart.CheckoutViewModel.messageFor(
                            com.rimagwinya.app.core.network.ApiError.Conflict(
                                com.rimagwinya.app.core.network.ConflictCode.from(action.failureCode),
                                action.failureDetail,
                            )
                        ).resolve(),
                        tone = BannerTone.Error,
                    )
                    RmButton(
                        text = stringResource(R.string.offline_failed_dismiss),
                        onClick = { syncViewModel.dismiss(action.id) },
                        style = RmButtonStyle.Quiet,
                        fillWidth = false,
                    )
                }
            }

            when {
                state.loading && state.orders.isEmpty() -> GroupedList {
                    repeat(4) { i -> SkeletonRow(); if (i < 3) ListDivider() }
                }

                state.error != null -> Column(verticalArrangement = Arrangement.spacedBy(Space.x12)) {
                    Banner(text = stringResource(state.error!!), tone = BannerTone.Error)
                    RmButton(stringResource(R.string.action_retry), viewModel::load, style = RmButtonStyle.Secondary)
                }

                state.isEmpty -> EmptyState(
                    title = stringResource(R.string.orders_empty_title),
                    body = stringResource(R.string.orders_empty_body),
                    icon = R.drawable.ic_clock,
                )

                else -> {
                    if (state.inProgress.isNotEmpty()) {
                        OrderGroup(stringResource(R.string.orders_in_progress), state.inProgress, onOpen)
                    }
                    if (state.past.isNotEmpty()) {
                        OrderGroup(stringResource(R.string.orders_past), state.past, onOpen)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderGroup(title: String, orders: List<Order>, onOpen: (String) -> Unit) {
    Column {
        SectionLabel(title)
        GroupedList {
            orders.forEachIndexed { index, order ->
                ListRow(
                    title = stringResource(
                        R.string.orders_row_title,
                        order.number.toInt(),
                        pluralStringResource(R.plurals.orders_item_count, order.itemCount, order.itemCount),
                    ),
                    subtitle = stringResource(
                        R.string.orders_row_subtitle,
                        order.placedAt.clockTime(),
                        order.slotName.orEmpty(),
                    ),
                    onClick = { onOpen(order.id) },
                    showChevron = false,
                    trailing = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(order.total.format(), style = RmTheme.type.price, color = RmTheme.colors.text)
                            StatusPill(order.status.pill())
                        }
                    },
                )
                if (index < orders.lastIndex) ListDivider(inset = false)
            }
        }
    }
}
