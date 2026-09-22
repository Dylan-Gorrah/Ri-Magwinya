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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopUpUiState(
    val number: String = "",
    val finding: Boolean = false,
    val student: StudentAccount? = null,
    val preset: Money? = null,
    val custom: String = "",
    val confirming: Boolean = false,
    val adding: Boolean = false,
    /** Set once the top-up went through: the student with the new balance. */
    val done: StudentAccount? = null,
    val error: UiText? = null,
) {
    /** The custom amount wins when typed; otherwise the chosen preset. */
    val amount: Money?
        get() = if (custom.isNotBlank()) EditorForm.parseRands(custom)?.let(::Money) else preset

    val amountInRange: Boolean
        get() = amount?.let { it >= AppConfig.MIN_TOP_UP && it <= AppConfig.MAX_TOP_UP } == true

    val canFind: Boolean get() = number.isNotBlank() && !finding
    val canAdd: Boolean get() = student != null && amountInRange && !adding
}

@HiltViewModel
class TopUpViewModel @Inject constructor(
    private val staff: StaffRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TopUpUiState())
    val state: StateFlow<TopUpUiState> = _state.asStateFlow()

    fun onNumber(value: String) = _state.update { it.copy(number = value, student = null, error = null) }

    fun find() {
        val number = _state.value.number
        _state.update { it.copy(finding = true, error = null) }
        viewModelScope.launch {
            staff.findStudent(number)
                .onSuccess { s -> _state.update { it.copy(finding = false, student = s) } }
                .onFailure { e ->
                    val error = e.asApiError()
                    _state.update {
                        it.copy(
                            finding = false,
                            error = UiText(if (error is ApiError.NotFound) R.string.topup_not_found else error.messageRes()),
                        )
                    }
                }
        }
    }

    fun choosePreset(amount: Money) = _state.update { it.copy(preset = amount, custom = "", error = null) }

    fun onCustom(value: String) = _state.update { it.copy(custom = value, preset = null, error = null) }

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
            staff.topUp(student.studentNumber, amount)
                .onSuccess { updated -> _state.update { it.copy(adding = false, done = updated) } }
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

    fun reset() {
        _state.value = TopUpUiState()
    }
}
