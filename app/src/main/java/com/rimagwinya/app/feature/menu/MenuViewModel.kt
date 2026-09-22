package com.rimagwinya.app.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.data.repository.MenuRepository
import com.rimagwinya.app.data.repository.friendly
import com.rimagwinya.app.domain.model.MenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One UiState for the screen, exposed as a StateFlow.
 *
 * Every screen in this app handles four cases: loading, empty, error and
 * success. Modelling them as one object rather than four booleans means an
 * impossible combination cannot be represented.
 */
data class MenuUiState(
    val isLoading: Boolean = true,
    val items: List<MenuItem> = emptyList(),
    val error: String? = null,
    /** True until Phase 2's keys are in local.properties. */
    val notConfigured: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && items.isEmpty()
}

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val repository: MenuRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (!AppConfig.isBackendConfigured) {
            // A clear message beats a network timeout nobody can interpret.
            _state.value = MenuUiState(isLoading = false, notConfigured = true)
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.menu()
                .onSuccess { items ->
                    _state.value = MenuUiState(isLoading = false, items = items)
                }
                .onFailure { throwable ->
                    val error = (throwable as? ApiError) ?: throwable.asApiError()
                    _state.value = MenuUiState(isLoading = false, error = error.friendly())
                }
        }
    }
}
