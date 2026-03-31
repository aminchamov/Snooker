package com.elocho.snooker.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elocho.snooker.data.repository.SettingsRepository
import com.elocho.snooker.data.sync.SupabaseSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUiState(
    val isSyncing: Boolean = false,
    val lastSyncAt: Long? = null,
    val lastSyncStatus: String? = null,
    val lastSyncError: String? = null,
    val transientMessage: String? = null
)

class SyncViewModel(
    private val syncRepository: SupabaseSyncRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val transientState = MutableStateFlow(SyncUiState())
    private var autoRefreshAttempted = false

    val uiState: StateFlow<SyncUiState> = combine(
        settingsRepository.lastSyncAt,
        settingsRepository.lastSyncStatus,
        settingsRepository.lastSyncError,
        transientState
    ) { lastSyncAt, lastSyncStatus, lastSyncError, transient ->
        transient.copy(
            lastSyncAt = lastSyncAt,
            lastSyncStatus = lastSyncStatus,
            lastSyncError = lastSyncError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncUiState()
    )

    fun syncNow() {
        val current = uiState.value
        if (current.isSyncing) return

        transientState.update { it.copy(isSyncing = true, transientMessage = "Syncing data...") }
        viewModelScope.launch {
            val result = syncRepository.syncAll()
            transientState.update {
                it.copy(
                    isSyncing = false,
                    transientMessage = result.message
                )
            }
        }
    }

    fun autoRefreshIfNeeded() {
        if (autoRefreshAttempted) return
        autoRefreshAttempted = true

        val current = uiState.value
        if (current.isSyncing) return

        transientState.update { it.copy(isSyncing = true, transientMessage = "Refreshing cloud data...") }
        viewModelScope.launch {
            val result = syncRepository.autoRefreshIfStale()
            transientState.update {
                it.copy(
                    isSyncing = false,
                    transientMessage = result?.message
                )
            }
        }
    }

    class Factory(
        private val syncRepository: SupabaseSyncRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SyncViewModel(syncRepository, settingsRepository) as T
        }
    }
}
