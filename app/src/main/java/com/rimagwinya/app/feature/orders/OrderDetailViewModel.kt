package com.rimagwinya.app.feature.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rimagwinya.app.R
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.core.util.UiText
import com.rimagwinya.app.data.repository.AuthRepository
import com.rimagwinya.app.data.repository.OrderRepository
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.OrderStatus
import com.rimagwinya.app.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailUiState(
    val order: Order? = null,
    val loading: Boolean = true,
    val error: UiText? = null,
    val confirmingCancel: Boolean = false,
    val cancelling: Boolean = false,
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orders: OrderRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val orderId: String = savedStateHandle.toRoute<Route.OrderDetail>().orderId

    private val _state = MutableStateFlow(OrderDetailUiState())
    val state: StateFlow<OrderDetailUiState> = _state.asStateFlow()

    init {
        load()
        if (AppConfig.isBackendConfigured) {
            // Every staff tap shows up here without the student refreshing.
            viewModelScope.launch { orders.orderChanges(orderId).collect { load() } }
        }
    }

    fun load() {
        viewModelScope.launch {
            orders.order(orderId)
                .onSuccess { order -> _state.update { it.copy(order = order, loading = false, error = null) } }
                .onFailure { e ->
                    // Keep showing what we have; only an empty screen needs the error.
                    _state.update {
                        it.copy(loading = false, error = if (it.order == null) UiText(e.asApiError().messageRes()) else it.error)
                    }
                }
        }
    }

    fun askCancel() = _state.update { it.copy(confirmingCancel = true) }

    fun dismissCancel() = _state.update { it.copy(confirmingCancel = false) }

    fun cancel() {
        _state.update { it.copy(confirmingCancel = false, cancelling = true, error = null) }
        viewModelScope.launch {
            orders.setStatus(orderId, OrderStatus.Cancelled)
                .onSuccess { order ->
                    _state.update { it.copy(cancelling = false) }
                    // Merge rather than replace: the function returns the
                    // order without its lines and slot.
                    _state.update { s -> s.copy(order = s.order?.copy(status = order.status, cancelledAt = order.cancelledAt)) }
                    auth.profile()
                    load()
                }
                .onFailure { e ->
                    val error = e.asApiError()
                    val message = if ((error as? ApiError.Conflict)?.code == ConflictCode.INVALID_TRANSITION) {
                        // The kitchen started while the student was deciding.
                        UiText(R.string.order_error_transition)
                    } else {
                        UiText(error.messageRes())
                    }
                    _state.update { it.copy(cancelling = false, error = message) }
                    load()
                }
        }
    }
}
