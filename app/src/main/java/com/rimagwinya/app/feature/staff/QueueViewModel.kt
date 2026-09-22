package com.rimagwinya.app.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.R
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.core.util.UiText
import com.rimagwinya.app.data.repository.OrderRepository
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

enum class QueueFilter { New, Preparing, Ready, All }

data class QueueUiState(
    val orders: List<Order> = emptyList(),
    val filter: QueueFilter = QueueFilter.New,
    val loading: Boolean = true,
    val error: UiText? = null,
    /** Orders with a request in flight, so their button cannot be double-tapped. */
    val busy: Set<String> = emptySet(),
    /** Refreshed every minute, for the no-show button. */
    val now: LocalTime = LocalTime.MIDNIGHT,
) {
    val newCount get() = orders.count { it.status == OrderStatus.Placed }
    val preparingCount get() = orders.count { it.status == OrderStatus.Preparing }
    val readyCount get() = orders.count { it.status == OrderStatus.Ready }

    /** Oldest first: first in, first served. */
    val visible: List<Order>
        get() = when (filter) {
            QueueFilter.New -> orders.filter { it.status == OrderStatus.Placed }
            QueueFilter.Preparing -> orders.filter { it.status == OrderStatus.Preparing }
            QueueFilter.Ready -> orders.filter { it.status == OrderStatus.Ready }
            QueueFilter.All -> orders
        }.sortedBy { it.placedAt }

    /**
     * No-show only on a ready order whose break has ended — before then the
     * student may simply not have arrived yet.
     */
    fun canMarkNoShow(order: Order): Boolean =
        order.status == OrderStatus.Ready && order.slotEnd != null && !now.isBefore(order.slotEnd)
}

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val orders: OrderRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(QueueUiState(now = nowHere()))
    val state: StateFlow<QueueUiState> = _state.asStateFlow()

    init {
        load()
        if (AppConfig.isBackendConfigured) {
            // A student placing an order appears here without anyone refreshing.
            viewModelScope.launch { orders.orderChanges().collect { load() } }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                _state.update { it.copy(now = nowHere()) }
            }
        }
    }

    private fun nowHere(): LocalTime = LocalTime.now(clock.withZone(AppConfig.zone))

    fun load() {
        viewModelScope.launch {
            orders.openOrdersToday()
                .onSuccess { list -> _state.update { it.copy(orders = list, loading = false, error = null) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = UiText(e.asApiError().messageRes())) } }
        }
    }

    fun setFilter(filter: QueueFilter) = _state.update { it.copy(filter = filter) }

    /** The one next step for an order's current status. */
    fun advance(order: Order) {
        val next = nextStatus(order.status) ?: return
        move(order, next)
    }

    fun noShow(order: Order) {
        if (_state.value.canMarkNoShow(order)) move(order, OrderStatus.NoShow)
    }

    private fun move(order: Order, to: OrderStatus) {
        if (order.id in _state.value.busy) return
        _state.update { it.copy(busy = it.busy + order.id, error = null) }
        viewModelScope.launch {
            orders.setStatus(order.id, to)
                .onSuccess { updated ->
                    _state.update { s ->
                        s.copy(
                            busy = s.busy - order.id,
                            // Show the move at once; the realtime refetch confirms it.
                            orders = s.orders.mapNotNull { o ->
                                when {
                                    o.id != order.id -> o
                                    updated.status.isActive -> o.copy(status = updated.status)
                                    else -> null
                                }
                            },
                        )
                    }
                }
                .onFailure { e ->
                    val error = e.asApiError()
                    val message = if ((error as? ApiError.Conflict)?.code == ConflictCode.INVALID_TRANSITION) {
                        UiText(R.string.queue_moved_on)
                    } else {
                        UiText(error.messageRes())
                    }
                    _state.update { it.copy(busy = it.busy - order.id, error = message) }
                    load()
                }
        }
    }

    companion object {
        fun nextStatus(status: OrderStatus): OrderStatus? = when (status) {
            OrderStatus.Placed -> OrderStatus.Preparing
            OrderStatus.Preparing -> OrderStatus.Ready
            OrderStatus.Ready -> OrderStatus.Collected
            else -> null
        }
    }
}
