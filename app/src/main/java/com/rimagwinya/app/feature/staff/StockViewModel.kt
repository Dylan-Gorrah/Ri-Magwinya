package com.rimagwinya.app.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.R
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.network.messageRes
import com.rimagwinya.app.core.util.UiText
import com.rimagwinya.app.data.repository.MenuRepository
import com.rimagwinya.app.data.repository.StaffRepository
import com.rimagwinya.app.domain.model.ItemDraft
import com.rimagwinya.app.domain.model.MenuCategory
import com.rimagwinya.app.domain.model.MenuChange
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.Temperature
import com.rimagwinya.app.domain.model.applying
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The editor's fields as typed, before they are validated into an [ItemDraft]. */
data class EditorForm(
    val editingId: String? = null,
    val name: String = "",
    val description: String = "",
    val category: MenuCategory = MenuCategory.Drinks,
    val price: String = "",
    val iconKey: String = "can",
    val openingStock: String = "0",
    val reorderLevel: String = "5",
    val temperature: Temperature? = null,
    val isAvailable: Boolean = true,
    val saving: Boolean = false,
    val confirmingDelete: Boolean = false,
    val error: UiText? = null,
) {
    val isNew: Boolean get() = editingId == null

    /** The draft, or the first thing wrong with the form. */
    fun validate(): Result<ItemDraft> {
        if (name.isBlank()) return Result.failure(FormProblem(R.string.editor_error_name))
        val cents = parseRands(price) ?: return Result.failure(FormProblem(R.string.editor_error_price))
        val stock = openingStock.trim().toIntOrNull()?.takeIf { it >= 0 }
            ?: return Result.failure(FormProblem(R.string.editor_error_number))
        val reorder = reorderLevel.trim().toIntOrNull()?.takeIf { it >= 0 }
            ?: return Result.failure(FormProblem(R.string.editor_error_number))
        return Result.success(
            ItemDraft(
                name = name.trim(),
                description = description.trim().ifEmpty { null },
                category = category,
                price = Money(cents),
                iconKey = iconKey,
                openingStock = stock,
                reorderLevel = reorder,
                temperature = temperature,
                isAvailable = isAvailable,
            )
        )
    }

    companion object {
        fun from(item: MenuItem) = EditorForm(
            editingId = item.id,
            name = item.name,
            description = item.description.orEmpty(),
            category = item.category,
            price = item.price.format().removePrefix("R"),
            iconKey = item.iconKey,
            openingStock = item.stockQuantity.toString(),
            reorderLevel = item.reorderLevel.toString(),
            temperature = item.temperature,
            isAvailable = item.isAvailable,
        )

        /** "16", "16.5", "16.50", "R16,50" to cents; null if not a price. */
        fun parseRands(raw: String): Long? {
            val cleaned = raw.trim().removePrefix("R").replace(',', '.')
            if (!Regex("""^\d{1,5}(\.\d{1,2})?$""").matches(cleaned)) return null
            val parts = cleaned.split('.')
            val rands = parts[0].toLong()
            val cents = parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0L
            return rands * 100 + cents
        }
    }
}

class FormProblem(val res: Int) : Exception()

data class StockUiState(
    val items: List<MenuItem> = emptyList(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val editor: EditorForm? = null,
    /** Stock taps made with no signal, waiting for the sync worker. */
    val queuedChanges: Int = 0,
) {
    /** At or under the reorder level, including sold out. */
    val needsAttention: List<MenuItem>
        get() = items.filter { it.stockQuantity <= it.reorderLevel }
}

@HiltViewModel
class StockViewModel @Inject constructor(
    private val menu: MenuRepository,
    private val staff: StaffRepository,
    private val sync: com.rimagwinya.app.data.sync.SyncRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StockUiState())
    val state: StateFlow<StockUiState> = _state.asStateFlow()

    init {
        load()
        if (AppConfig.isBackendConfigured) {
            // Sales change stock too; the counts here stay true.
            viewModelScope.launch {
                menu.changes().collect { change ->
                    when (change) {
                        is MenuChange.Updated -> _state.update { it.copy(items = it.items.applying(change)) }
                        MenuChange.Reload -> load()
                    }
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            menu.menu()
                .onSuccess { list -> _state.update { it.copy(items = list, loading = false, error = null) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = UiText(e.asApiError().messageRes())) } }
        }
    }

    /**
     * Shown at once, sent as a change. If it fails the number goes back and
     * the reason shows; realtime then brings the true figure.
     */
    fun adjust(item: MenuItem, delta: Int) {
        if (item.stockQuantity + delta < 0) return
        patchLocal(item.id) { it.copy(stockQuantity = it.stockQuantity + delta) }
        viewModelScope.launch {
            staff.adjustStock(item.id, delta)
                .onSuccess { updated -> patchLocal(item.id) { it.copy(stockQuantity = updated.stockQuantity) } }
                .onFailure { e ->
                    if (e.asApiError() is com.rimagwinya.app.core.network.ApiError.Offline) {
                        // A change, not a total, so replaying it later still
                        // lands on the right number.
                        sync.queueStockChange(
                            com.rimagwinya.app.data.remote.AdjustStockBody(item.id, delta),
                            "${item.name} ${if (delta > 0) "+" else ""}$delta",
                        )
                        _state.update { it.copy(queuedChanges = it.queuedChanges + 1) }
                    } else {
                        patchLocal(item.id) { it.copy(stockQuantity = it.stockQuantity - delta) }
                        _state.update { it.copy(error = UiText(e.asApiError().messageRes())) }
                    }
                }
        }
    }

    private fun patchLocal(id: String, change: (MenuItem) -> MenuItem) =
        _state.update { s -> s.copy(items = s.items.map { if (it.id == id) change(it) else it }) }

    // --- Editor ---------------------------------------------------------------

    fun openNew() = _state.update { it.copy(editor = EditorForm()) }

    fun openEdit(item: MenuItem) = _state.update { it.copy(editor = EditorForm.from(item)) }

    fun closeEditor() = _state.update { it.copy(editor = null) }

    fun editForm(change: (EditorForm) -> EditorForm) =
        _state.update { s -> s.copy(editor = s.editor?.let { change(it).copy(error = null) }) }

    fun save() {
        val form = _state.value.editor ?: return
        val draft = form.validate().getOrElse { e ->
            _state.update { it.copy(editor = form.copy(error = UiText((e as FormProblem).res))) }
            return
        }
        _state.update { it.copy(editor = form.copy(saving = true)) }
        viewModelScope.launch {
            val result = if (form.isNew) staff.createItem(draft) else staff.updateItem(form.editingId!!, draft)
            result
                .onSuccess { _state.update { it.copy(editor = null) }; load() }
                .onFailure { e ->
                    _state.update { it.copy(editor = form.copy(saving = false, error = UiText(e.asApiError().messageRes()))) }
                }
        }
    }

    fun askDelete() = editForm { it.copy(confirmingDelete = true) }

    fun delete() {
        val form = _state.value.editor ?: return
        val id = form.editingId ?: return
        _state.update { it.copy(editor = form.copy(saving = true, confirmingDelete = false)) }
        viewModelScope.launch {
            staff.deleteItem(id)
                .onSuccess { _state.update { it.copy(editor = null) }; load() }
                .onFailure { e ->
                    _state.update { it.copy(editor = form.copy(saving = false, error = UiText(e.asApiError().messageRes()))) }
                }
        }
    }
}
