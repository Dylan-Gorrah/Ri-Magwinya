package com.rimagwinya.app.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.EmptyState
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.data.remote.NotificationDto
import com.rimagwinya.app.data.remote.SupabaseApi
import com.rimagwinya.app.data.remote.parseInstant
import com.rimagwinya.app.domain.model.clockTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class NotificationsUiState(
    val items: List<NotificationDto> = emptyList(),
    val loading: Boolean = true,
) {
    val unread: Int get() = items.count { !it.isRead }
}

/**
 * The bell. These rows are written by the database when an order is placed,
 * becomes ready, or a wallet is topped up — so they are there whether or not
 * a push ever arrived.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val api: SupabaseApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val items = withContext(io) { runCatching { api.notifications() }.getOrDefault(emptyList()) }
            _state.update { it.copy(items = items, loading = false) }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            withContext(io) { runCatching { api.markNotificationsRead() } }
            _state.update { s -> s.copy(items = s.items.map { it.copy(isRead = true) }) }
        }
    }
}

@Composable
fun NotificationsSheetContent(
    onOpenOrder: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen)
            .padding(bottom = Space.x24),
        verticalArrangement = Arrangement.spacedBy(Space.x16),
    ) {
        Text(stringResource(R.string.notif_sheet_title), style = RmTheme.type.screenTitle, color = RmTheme.colors.text)

        if (state.items.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.notif_empty_title),
                body = stringResource(R.string.notif_empty_body),
                icon = R.drawable.ic_bell,
            )
            return@Column
        }

        GroupedList {
            state.items.forEachIndexed { index, item ->
                ListRow(
                    title = item.title,
                    subtitle = "${item.message}\n${parseInstant(item.sentAt).clockTime()}",
                    onClick = item.orderId?.let { id -> { onOpenOrder(id) } },
                    showChevron = item.orderId != null,
                    // Unread stands out by weight, not by colour alone.
                    enabled = true,
                )
                if (index < state.items.lastIndex) ListDivider(inset = false)
            }
        }

        if (state.unread > 0) {
            RmButton(stringResource(R.string.notif_mark_read), viewModel::markAllRead, style = RmButtonStyle.Secondary)
        }
    }
}
