package com.rimagwinya.app.feature.offline

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimagwinya.app.core.connectivity.ConnectivityObserver
import com.rimagwinya.app.core.database.PendingActionEntity
import com.rimagwinya.app.data.sync.SyncRepository
import com.rimagwinya.app.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncUiState(
    val online: Boolean = true,
    val pending: List<PendingActionEntity> = emptyList(),
) {
    val waiting: List<PendingActionEntity> get() = pending.filter { it.failureCode == null }
    val refused: List<PendingActionEntity> get() = pending.filter { it.failureCode != null }
}

/**
 * The offline banner, the queue, and the nudge that sends it.
 *
 * Shared by the whole app rather than owned by one screen: being offline is
 * not a property of the menu or the cart.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    connectivity: ConnectivityObserver,
    private val sync: SyncRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val state: StateFlow<SyncUiState> = combine(connectivity.isOnline, sync.pending) { online, pending ->
        SyncUiState(online = online, pending = pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    init {
        viewModelScope.launch {
            // Coming back online is the moment to try the queue, rather than
            // waiting for whatever WorkManager decides.
            connectivity.isOnline.collect { online ->
                if (online) SyncWorker.schedule(context)
            }
        }
    }

    fun dismiss(id: Long) = viewModelScope.launch { sync.dismiss(id) }
}
