package com.rimagwinya.app.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.R
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.core.util.UiText
import com.rimagwinya.app.data.repository.StaffRepository
import com.rimagwinya.app.domain.model.StudentAccount
import com.rimagwinya.app.domain.model.WalletEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The staff wallet screen: find a student by number or name, see who they
 * are and what has moved through their wallet, then add credit.
 */
data class TopUpUiState(
    val query: String = "",
    val searching: Boolean = false,
    /** Null until a search has run; empty when it found nobody. */
    val results: List<StudentAccount>? = null,
    val student: StudentAccount? = null,
    val history: List<WalletEntry> = emptyList(),
    val historyLoading: Boolean = false,
    val preset: Money? = null,
    val custom: String = "",
    val confirming: Boolean = false,
    val adding: Boolean = false,
    /** The amount just added, shown until the next top-up starts. */
    val added: Money? = null,
    val error: UiText? = null,
) {
    /** The custom amount wins when typed; otherwise the chosen preset. */
    val amount: Money?
        get() = if (custom.isNotBlank()) EditorForm.parseRands(custom)?.let(::Money) else preset

    val amountInRange: Boolean
        get() = amount?.let { it >= AppConfig.MIN_TOP_UP && it <= AppConfig.MAX_TOP_UP } == true

    val canSearch: Boolean get() = query.isNotBlank() && !searching
    val canAdd: Boolean get() = student != null && amountInRange && !adding
}

@HiltViewModel
class TopUpViewModel @Inject constructor(
    private val staff: StaffRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TopUpUiState())
    val state: StateFlow<TopUpUiState> = _state.asStateFlow()

    private var historyJob: Job? = null

    fun onQuery(value: String) = _state.update { it.copy(query = value, error = null) }

    fun search() {
        val query = _state.value.query
        if (query.isBlank()) return
        _state.update { it.copy(searching = true, error = null) }
        viewModelScope.launch {
            staff.searchStudents(query)
                .onSuccess { found ->
                    _state.update { it.copy(searching = false, results = found) }
                    // One match is the usual case, a typed or scanned number:
                    // skip the list and go straight to the student.
                    if (found.size == 1) select(found.single())
                }
                .onFailure { e ->
                    _state.update { it.copy(searching = false, error = UiText(e.asApiError().messageRes())) }
                }
        }
    }

    fun select(student: StudentAccount) {
        _state.update {
            it.copy(
                student = student,
                history = emptyList(),
                preset = null,
                custom = "",
                added = null,
                error = null,
            )
        }
        loadHistory(student.id)
    }

    /** Back to the search results, keeping what was typed. */
    fun changeStudent() {
        historyJob?.cancel()
        _state.update {
            it.copy(student = null, history = emptyList(), preset = null, custom = "", added = null, error = null)
        }
    }

    fun choosePreset(amount: Money) = _state.update { it.copy(preset = amount, custom = "", added = null, error = null) }

    fun onCustom(value: String) = _state.update { it.copy(custom = value, preset = null, added = null, error = null) }

    fun askConfirm() {
        if (_state.value.canAdd) _state.update { it.copy(confirming = true) }
    }

    fun dismissConfirm() = _state.update { it.copy(confirming = false) }

    fun confirm() {
        val s = _state.value
        val student = s.student ?: return
        val amount = s.amount ?: return
        _state.update { it.copy(confirming = false, adding = true, error = null) }
        viewModelScope.launch {
            staff.topUp(student, amount)
                .onSuccess { updated ->
                    _state.update { st ->
                        st.copy(
                            adding = false,
                            student = updated,
                            // Keep the search list in step with the new balance.
                            results = st.results?.map { if (it.id == updated.id) updated else it },
                            added = amount,
                            preset = null,
                            custom = "",
                        )
                    }
                    loadHistory(updated.id)
                }
                .onFailure { e ->
                    val error = e.asApiError()
                    val message = when ((error as? ApiError.Conflict)?.code) {
                        ConflictCode.STUDENT_NOT_FOUND -> UiText(R.string.topup_not_found)
                        ConflictCode.AMOUNT_OUT_OF_RANGE -> UiText(
                            R.string.topup_range, AppConfig.MIN_TOP_UP.format(), AppConfig.MAX_TOP_UP.format(),
                        )
                        else -> UiText(error.messageRes())
                    }
                    _state.update { it.copy(adding = false, error = message) }
                }
        }
    }

    /** Start over with an empty search. */
    fun reset() {
        historyJob?.cancel()
        _state.value = TopUpUiState()
    }

    private fun loadHistory(studentId: String) {
        historyJob?.cancel()
        _state.update { it.copy(historyLoading = true) }
        historyJob = viewModelScope.launch {
            // History is extra detail: if it fails, the top-up still works,
            // so the list simply stays empty rather than showing an error.
            val history = staff.walletHistory(studentId).getOrDefault(emptyList())
            _state.update {
                if (it.student?.id == studentId) it.copy(history = history, historyLoading = false) else it
            }
        }
    }
}
