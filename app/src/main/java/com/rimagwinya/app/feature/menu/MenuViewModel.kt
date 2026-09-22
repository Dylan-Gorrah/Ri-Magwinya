package com.rimagwinya.app.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.data.repository.CartRepository
import com.rimagwinya.app.data.repository.MenuRepository
import com.rimagwinya.app.data.repository.friendly
import com.rimagwinya.app.domain.model.Cart
import com.rimagwinya.app.domain.model.MenuCategory
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.pricing.Selection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** All, or one of the three categories. */
enum class CategoryFilter { All, Meals, Snacks, Drinks }

data class MenuUiState(
    val isLoading: Boolean = true,
    val items: List<MenuItem> = emptyList(),
    val query: String = "",
    val category: CategoryFilter = CategoryFilter.All,
    val cart: Cart = Cart(),
    val error: String? = null,
    /** True until the Supabase keys are in local.properties. */
    val notConfigured: Boolean = false,
    /** The item whose sheet is open, if any. */
    val openItem: MenuItem? = null,
) {
    /** Search and category applied. Sold-out items stay, dimmed. */
    val visible: List<MenuItem>
        get() = items
            .filter { item ->
                category == CategoryFilter.All ||
                    item.category.name.equals(category.name, ignoreCase = true)
            }
            .filter { item ->
                query.isBlank() ||
                    item.name.contains(query.trim(), ignoreCase = true) ||
                    item.description.orEmpty().contains(query.trim(), ignoreCase = true)
            }

    val isEmpty: Boolean
        get() = !isLoading && error == null && visible.isEmpty()

    /** Nothing matched a search, as opposed to the menu being empty. */
    val isFilteredEmpty: Boolean
        get() = isEmpty && items.isNotEmpty()
}

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val repository: MenuRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val local = MutableStateFlow(MenuUiState())

    val state: StateFlow<MenuUiState> = combine(local, cartRepository.cart) { state, cart ->
        state.copy(cart = cart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState())

    init {
        load()
    }

    fun load() {
        if (!AppConfig.isBackendConfigured) {
            // A clear message beats a network timeout nobody can interpret.
            local.update { it.copy(isLoading = false, notConfigured = true) }
            return
        }

        local.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.menu()
                .onSuccess { items ->
                    local.update {
                        it.copy(isLoading = false, items = items, error = null)
                    }
                }
                .onFailure { throwable ->
                    val error = (throwable as? ApiError) ?: throwable.asApiError()
                    local.update { it.copy(isLoading = false, error = error.friendly()) }
                }
        }
    }

    fun onQuery(value: String) = local.update { it.copy(query = value) }

    fun onCategory(value: CategoryFilter) = local.update { it.copy(category = value) }

    /** Sold-out rows do not open. */
    fun openItem(item: MenuItem) {
        if (item.isSoldOut) return
        local.update { it.copy(openItem = item) }
    }

    fun closeSheet() = local.update { it.copy(openItem = null) }

    fun addToCart(item: MenuItem, selection: Selection) {
        cartRepository.add(item, selection)
        closeSheet()
    }
}

/** The server's category names, for the chip row. */
fun CategoryFilter.toCategory(): MenuCategory? = when (this) {
    CategoryFilter.All -> null
    CategoryFilter.Meals -> MenuCategory.Meals
    CategoryFilter.Snacks -> MenuCategory.Snacks
    CategoryFilter.Drinks -> MenuCategory.Drinks
}
