package com.rimagwinya.app.feature.cart

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.data.repository.CartRepository
import com.rimagwinya.app.data.repository.OrderRepository
import com.rimagwinya.app.domain.model.Cart
import com.rimagwinya.app.domain.model.Slot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

data class CartUiState(
    val cart: Cart = Cart(),
    /** Today's breaks that are still taking orders. Closed ones are hidden. */
    val slots: List<Slot> = emptyList(),
    val selectedSlotId: String? = null,
    val slotsLoading: Boolean = true,
    @StringRes val slotsError: Int? = null,
) {
    val selectedSlot: Slot? get() = slots.firstOrNull { it.id == selectedSlotId }

    /** Loaded, and nothing left to order for today. */
    val noSlotsLeft: Boolean get() = !slotsLoading && slotsError == null && slots.isEmpty()

    val canContinue: Boolean
        get() = !cart.isEmpty && selectedSlot?.isFull == false
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orders: OrderRepository,
    private val clock: Clock,
) : ViewModel() {

    private val slotState = MutableStateFlow(SlotState())

    val state: StateFlow<CartUiState> = combine(
        cartRepository.cart,
        cartRepository.slotId,
        slotState,
    ) { cart, slotId, slots ->
        CartUiState(
            cart = cart,
            slots = slots.slots,
            selectedSlotId = slotId,
            slotsLoading = slots.loading,
            slotsError = slots.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    init {
        if (AppConfig.isBackendConfigured) {
            // "12 of 40 taken" stays true while the student is looking.
            viewModelScope.launch { orders.slotChanges().collect { loadSlots() } }
        }
    }

    fun loadSlots() {
        viewModelScope.launch {
            slotState.update { it.copy(loading = it.slots.isEmpty(), error = null) }
            orders.todaysSlots()
                .onSuccess { all -> applySlots(all) }
                .onFailure { e ->
                    val error = (e as? ApiError)?.messageRes() ?: com.rimagwinya.app.R.string.error_generic
                    slotState.update { it.copy(loading = false, error = error) }
                }
        }
    }

    private fun applySlots(all: List<Slot>) {
        val now = LocalTime.now(clock.withZone(AppConfig.zone))
        val open = openSlots(all, now)
        slotState.value = SlotState(slots = open, loading = false)

        // Keep the student's choice if it is still valid; otherwise pick the
        // first break with room, so the common case needs no tap.
        val current = open.firstOrNull { it.id == cartRepository.slotId.value }
        if (current == null || current.isFull) {
            cartRepository.selectSlot(open.firstOrNull { !it.isFull }?.id)
        }
    }

    fun selectSlot(slot: Slot) {
        if (!slot.isFull) cartRepository.selectSlot(slot.id)
    }

    fun setQuantity(key: String, quantity: Int) = cartRepository.setQuantity(key, quantity)

    fun remove(key: String) = cartRepository.remove(key)

    private data class SlotState(
        val slots: List<Slot> = emptyList(),
        val loading: Boolean = true,
        @StringRes val error: Int? = null,
    )

    companion object {
        /** The breaks still taking orders at [now], in time order. */
        fun openSlots(all: List<Slot>, now: LocalTime): List<Slot> =
            all.filter { it.isOpenAt(now) }.sortedBy { it.startsAt }
    }
}
