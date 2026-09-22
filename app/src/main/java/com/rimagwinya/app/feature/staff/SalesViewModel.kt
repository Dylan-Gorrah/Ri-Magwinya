package com.rimagwinya.app.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.core.util.UiText
import com.rimagwinya.app.data.repository.OrderRepository
import com.rimagwinya.app.data.repository.StaffRepository
import com.rimagwinya.app.domain.model.SalesSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SalesUiState(
    val day: LocalDate? = null,
    val summary: SalesSummary? = null,
    val loading: Boolean = true,
    val error: UiText? = null,
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val staff: StaffRepository,
    private val orders: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SalesUiState())
    val state: StateFlow<SalesUiState> = _state.asStateFlow()

    init {
        load()
        // Collected orders are what count, so refresh as the queue moves.
        viewModelScope.launch { orders.orderChanges().collect { load() } }
    }

    fun load() {
        val day = orders.today()
        viewModelScope.launch {
            staff.sales(day)
                .onSuccess { s -> _state.update { it.copy(day = day, summary = s, loading = false, error = null) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = UiText(e.asApiError().messageRes())) } }
        }
    }

    companion object {
        /**
         * The day as a CSV a spreadsheet opens cleanly: a totals block, the
         * hours, then the top sellers. Amounts are plain decimals with no
         * "R", so they sum as numbers.
         */
        fun csv(day: LocalDate, s: SalesSummary): String = buildString {
            fun rands(cents: Long) = "%d.%02d".format(cents / 100, cents % 100)
            fun cell(text: String) =
                if (text.any { it == ',' || it == '"' || it == '\n' }) "\"" + text.replace("\"", "\"\"") + "\"" else text

            appendLine("Ri-magwinya sales,$day")
            appendLine()
            appendLine("Revenue,${rands(s.revenue.cents)}")
            appendLine("Collected orders,${s.orderCount}")
            appendLine("Average order,${rands(s.average.cents)}")
            appendLine()
            appendLine("Hour,Orders")
            s.byHour.forEach { appendLine("${it.label},${it.orders}") }
            appendLine()
            appendLine("Item,Quantity,Revenue")
            s.topItems.forEach { appendLine("${cell(it.name)},${it.quantity},${rands(it.revenue.cents)}") }
        }
    }
}
