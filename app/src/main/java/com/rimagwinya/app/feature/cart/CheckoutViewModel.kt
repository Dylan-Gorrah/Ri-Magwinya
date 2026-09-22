package com.rimagwinya.app.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.R
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.core.util.UiText
import com.rimagwinya.app.data.repository.AuthRepository
import com.rimagwinya.app.data.repository.CartRepository
import com.rimagwinya.app.data.repository.OrderRepository
import com.rimagwinya.app.data.repository.PlaceResult
import com.rimagwinya.app.domain.model.Cart
import com.rimagwinya.app.domain.model.PaymentMethod
import com.rimagwinya.app.domain.model.Profile
import com.rimagwinya.app.domain.model.Slot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Why pay-at-counter is not offered, if it is not. */
enum class CounterBlock { NoTopUp, NoShows }

data class CheckoutUiState(
    val cart: Cart = Cart(),
    val slot: Slot? = null,
    val profile: Profile? = null,
    /** Null while unknown; the counter option stays disabled until it is. */
    val hasToppedUp: Boolean? = null,
    val method: PaymentMethod = PaymentMethod.Wallet,
    val placing: Boolean = false,
    val error: UiText? = null,
) {
    val total get() = cart.total

    /** Display only — the server checks the real balance when placing. */
    val walletShortBy get() = profile?.let { total - it.walletBalance }?.takeIf { it.isPositive }

    val walletAvailable: Boolean get() = profile != null && walletShortBy == null

    val counterBlock: CounterBlock?
        get() = when {
            profile?.noShowBlocked == true -> CounterBlock.NoShows
            hasToppedUp == false -> CounterBlock.NoTopUp
            else -> null
        }

    val counterAvailable: Boolean get() = hasToppedUp == true && counterBlock == null

    val canPlace: Boolean
        get() = !placing && !cart.isEmpty && slot != null && when (method) {
            PaymentMethod.Wallet -> walletAvailable
            PaymentMethod.Counter -> counterAvailable
        }
}

sealed interface CheckoutEvent {
    data class Placed(val orderId: String) : CheckoutEvent

    /** No signal: it is in the queue and goes when the phone is back. */
    data object Queued : CheckoutEvent
    /** The slot filled or closed: back to the cart to pick another. */
    data object PickAnotherSlot : CheckoutEvent
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orders: OrderRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val local = MutableStateFlow(Local())
    private val events = Channel<CheckoutEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    /**
     * One reference per attempt to place *this* cart. A retry after a
     * timeout reuses it, so the server returns the first order rather than
     * charging twice. Replaced only when the order goes through.
     */
    private var clientRef: String = UUID.randomUUID().toString()

    val state: StateFlow<CheckoutUiState> = combine(
        cartRepository.cart,
        auth.currentProfile,
        local,
    ) { cart, profile, l ->
        CheckoutUiState(
            cart = cart,
            slot = l.slot,
            profile = profile,
            hasToppedUp = l.hasToppedUp,
            method = l.method,
            placing = l.placing,
            error = l.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { auth.profile() }
        viewModelScope.launch {
            orders.hasToppedUp().onSuccess { topped -> local.update { it.copy(hasToppedUp = topped) } }
        }
        viewModelScope.launch {
            val id = cartRepository.slotId.value
            orders.todaysSlots().onSuccess { slots ->
                local.update { it.copy(slot = slots.firstOrNull { s -> s.id == id }) }
            }
        }
    }

    fun choose(method: PaymentMethod) = local.update { it.copy(method = method, error = null) }

    fun place() {
        val current = state.value
        val slot = current.slot ?: return
        if (!current.canPlace) return

        local.update { it.copy(placing = true, error = null) }
        viewModelScope.launch {
            orders.place(current.cart, slot.id, current.method, clientRef)
                .onSuccess { result ->
                    clientRef = UUID.randomUUID().toString()
                    cartRepository.clear()
                    local.update { it.copy(placing = false) }
                    when (result) {
                        is PlaceResult.Placed -> {
                            // The wallet just changed; every balance follows.
                            auth.profile()
                            events.send(CheckoutEvent.Placed(result.order.id))
                        }
                        PlaceResult.Queued -> events.send(CheckoutEvent.Queued)
                    }
                }
                .onFailure { e -> onFailure(e.asApiError()) }
        }
    }

    private suspend fun onFailure(error: ApiError) {
        local.update { it.copy(placing = false, error = messageFor(error)) }
        when ((error as? ApiError.Conflict)?.code) {
            ConflictCode.SLOT_FULL, ConflictCode.SLOT_NOT_TODAY -> {
                // A new attempt: the next one is a different order.
                clientRef = UUID.randomUUID().toString()
                events.send(CheckoutEvent.PickAnotherSlot)
            }
            ConflictCode.INSUFFICIENT_FUNDS -> {
                clientRef = UUID.randomUUID().toString()
                auth.profile()
            }
            null -> Unit // Network or server trouble: keep the ref, retry is safe.
            else -> clientRef = UUID.randomUUID().toString()
        }
    }

    private data class Local(
        val slot: Slot? = null,
        val hasToppedUp: Boolean? = null,
        val method: PaymentMethod = PaymentMethod.Wallet,
        val placing: Boolean = false,
        val error: UiText? = null,
    )

    companion object {
        /** One specific sentence per reason the server can refuse. */
        fun messageFor(error: ApiError): UiText {
            if (error !is ApiError.Conflict) return UiText(error.messageRes())
            return when (error.code) {
                ConflictCode.OUT_OF_STOCK -> error.detail
                    ?.let { UiText(R.string.order_error_out_of_stock, it) }
                    ?: UiText(R.string.order_error_out_of_stock_generic)
                ConflictCode.SLOT_FULL -> UiText(R.string.order_error_slot_full)
                ConflictCode.SLOT_NOT_TODAY -> UiText(R.string.order_error_slot_closed)
                ConflictCode.INSUFFICIENT_FUNDS -> UiText(R.string.order_error_funds)
                ConflictCode.COUNTER_BLOCKED ->
                    if (error.detail == "TOO_MANY_NO_SHOWS") {
                        UiText(R.string.order_error_counter_no_shows)
                    } else {
                        UiText(R.string.order_error_counter_no_topup)
                    }
                ConflictCode.EMPTY_SELECTION, ConflictCode.OPTION_REQUIRED -> UiText(R.string.order_error_empty)
                ConflictCode.STUDENT_NUMBER_REQUIRED -> UiText(R.string.order_error_student_number)
                else -> UiText(error.messageRes())
            }
        }
    }
}
