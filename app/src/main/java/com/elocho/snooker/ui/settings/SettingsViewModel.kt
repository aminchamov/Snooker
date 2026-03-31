package com.elocho.snooker.ui.settings

import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elocho.snooker.data.model.RemoteMappingAction
import com.elocho.snooker.data.repository.BackupPayloadType
import com.elocho.snooker.data.repository.DataBackupRepository
import com.elocho.snooker.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val primaryColorHex: String = "",
    val accentColorHex: String = "",
    val remoteUndoKeyCode: Int? = null,
    val remoteRedKeyCode: Int? = null,
    val remoteYellowKeyCode: Int? = null,
    val remoteGreenKeyCode: Int? = null,
    val remoteBrownKeyCode: Int? = null,
    val remoteBlueKeyCode: Int? = null,
    val remotePinkKeyCode: Int? = null,
    val remoteBlackKeyCode: Int? = null,
    val remoteErrorKeyCode: Int? = null,
    val listeningAction: RemoteMappingAction? = null,
    val assignmentMessage: String? = null,
    val isAssignmentError: Boolean = false,
    val backupMessage: String? = null,
    val isBackupError: Boolean = false,
    val lastDetectedKeyCode: Int? = null,
    val lastDetectedKeyName: String? = null,
    val isLoading: Boolean = true
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val backupRepository: DataBackupRepository,
    private val appContext: Context
) : ViewModel() {
    private data class PersistentSettingsState(
        val primaryColorHex: String = "",
        val accentColorHex: String = "",
        val remoteUndoKeyCode: Int? = null,
        val remoteRedKeyCode: Int? = null,
        val remoteYellowKeyCode: Int? = null,
        val remoteGreenKeyCode: Int? = null,
        val remoteBrownKeyCode: Int? = null,
        val remoteBlueKeyCode: Int? = null,
        val remotePinkKeyCode: Int? = null,
        val remoteBlackKeyCode: Int? = null,
        val remoteErrorKeyCode: Int? = null
    )

    private val transientState = MutableStateFlow(
        SettingsUiState(
            listeningAction = null,
            assignmentMessage = null,
            isAssignmentError = false,
            lastDetectedKeyCode = null,
            lastDetectedKeyName = null
        )
    )

    private val persistentState: StateFlow<PersistentSettingsState> = combine(
        repository.primaryColor,
        repository.accentColor,
        repository.remoteRedKeyCode,
        repository.remoteYellowKeyCode,
        repository.remoteGreenKeyCode
    ) { primary, accent, red, yellow, green ->
        PersistentSettingsState(
            primaryColorHex = primary,
            accentColorHex = accent,
            remoteRedKeyCode = red,
            remoteYellowKeyCode = yellow,
            remoteGreenKeyCode = green
        )
    }.combine(repository.remoteUndoKeyCode) { state, undo ->
        state.copy(remoteUndoKeyCode = undo)
    }.combine(repository.remoteBrownKeyCode) { state, brown ->
        state.copy(remoteBrownKeyCode = brown)
    }.combine(repository.remoteBlueKeyCode) { state, blue ->
        state.copy(remoteBlueKeyCode = blue)
    }.combine(repository.remotePinkKeyCode) { state, pink ->
        state.copy(remotePinkKeyCode = pink)
    }.combine(repository.remoteBlackKeyCode) { state, black ->
        state.copy(remoteBlackKeyCode = black)
    }.combine(repository.remoteErrorKeyCode) { state, error ->
        state.copy(remoteErrorKeyCode = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersistentSettingsState())

    val uiState: StateFlow<SettingsUiState> = combine(
        persistentState,
        transientState.asStateFlow()
    ) { persistent, transient ->
        SettingsUiState(
            primaryColorHex = persistent.primaryColorHex,
            accentColorHex = persistent.accentColorHex,
            remoteUndoKeyCode = persistent.remoteUndoKeyCode,
            remoteRedKeyCode = persistent.remoteRedKeyCode,
            remoteYellowKeyCode = persistent.remoteYellowKeyCode,
            remoteGreenKeyCode = persistent.remoteGreenKeyCode,
            remoteBrownKeyCode = persistent.remoteBrownKeyCode,
            remoteBlueKeyCode = persistent.remoteBlueKeyCode,
            remotePinkKeyCode = persistent.remotePinkKeyCode,
            remoteBlackKeyCode = persistent.remoteBlackKeyCode,
            remoteErrorKeyCode = persistent.remoteErrorKeyCode,
            listeningAction = transient.listeningAction,
            assignmentMessage = transient.assignmentMessage,
            isAssignmentError = transient.isAssignmentError,
            backupMessage = transient.backupMessage,
            isBackupError = transient.isBackupError,
            lastDetectedKeyCode = transient.lastDetectedKeyCode,
            lastDetectedKeyName = transient.lastDetectedKeyName,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun beginAssignment(action: RemoteMappingAction) {
        transientState.update {
            it.copy(
                listeningAction = action,
                assignmentMessage = "Press a remote button now...",
                isAssignmentError = false,
                backupMessage = null,
                isBackupError = false
            )
        }
    }

    fun clearAssignment(action: RemoteMappingAction) {
        viewModelScope.launch {
            repository.setRemoteKeyCode(action, null)
            transientState.update {
                it.copy(
                    assignmentMessage = "Mapping cleared for ${action.displayName()}",
                    isAssignmentError = false,
                    backupMessage = null,
                    isBackupError = false
                )
            }
        }
    }

    fun resetRemoteMappings() {
        viewModelScope.launch {
            repository.resetRemoteMappings()
            transientState.update {
                it.copy(
                    listeningAction = null,
                    assignmentMessage = "Remote mappings reset to defaults",
                    isAssignmentError = false,
                    backupMessage = null,
                    isBackupError = false
                )
            }
        }
    }

    fun exportPlayers(uri: Uri) = exportToUri(uri, BackupPayloadType.PLAYERS, "Players")

    fun exportTournaments(uri: Uri) = exportToUri(uri, BackupPayloadType.TOURNAMENTS, "Tournaments")

    fun exportAll(uri: Uri) = exportToUri(uri, BackupPayloadType.ALL, "All data")

    fun importPlayers(uri: Uri) = importFromUri(uri, BackupPayloadType.PLAYERS, "Players")

    fun importTournaments(uri: Uri) = importFromUri(uri, BackupPayloadType.TOURNAMENTS, "Tournaments")

    fun importAll(uri: Uri) = importFromUri(uri, BackupPayloadType.ALL, "All data")

    private fun exportToUri(uri: Uri, type: BackupPayloadType, label: String) {
        viewModelScope.launch {
            runCatching {
                val exportData = backupRepository.exportData(type)
                val json = backupRepository.encodeToJson(
                    type = type,
                    exportData = exportData,
                    appVersion = currentAppVersion()
                )
                appContext.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Unable to open destination file")
            }.onSuccess {
                transientState.update {
                    it.copy(
                        backupMessage = "$label exported successfully",
                        isBackupError = false
                    )
                }
            }.onFailure { throwable ->
                transientState.update {
                    it.copy(
                        backupMessage = throwable.message ?: "Export failed",
                        isBackupError = true
                    )
                }
            }
        }
    }

    private fun importFromUri(uri: Uri, type: BackupPayloadType, label: String) {
        viewModelScope.launch {
            runCatching {
                val json = appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Unable to open selected file")
                if (json.isBlank()) error("Backup file is empty")

                val parsed = backupRepository.decodeFromJson(json)
                when (type) {
                    BackupPayloadType.PLAYERS -> {
                        val imported = backupRepository.importPlayers(parsed.players)
                        "Imported $imported players"
                    }

                    BackupPayloadType.TOURNAMENTS -> {
                        val (tournaments, matches) = backupRepository.importTournaments(
                            tournaments = parsed.tournaments,
                            tournamentMatches = parsed.tournamentMatches
                        )
                        "Imported $tournaments tournaments and $matches bracket matches"
                    }

                    BackupPayloadType.ALL -> {
                        val players = backupRepository.importPlayers(parsed.players)
                        val (tournaments, matches) = backupRepository.importTournaments(
                            tournaments = parsed.tournaments,
                            tournamentMatches = parsed.tournamentMatches
                        )
                        "Imported $players players, $tournaments tournaments, $matches bracket matches"
                    }
                }
            }.onSuccess { summary ->
                transientState.update {
                    it.copy(
                        backupMessage = "$label import successful. $summary",
                        isBackupError = false
                    )
                }
            }.onFailure { throwable ->
                transientState.update {
                    it.copy(
                        backupMessage = throwable.message ?: "Import failed",
                        isBackupError = true
                    )
                }
            }
        }
    }

    /**
     * Returns true when consumed.
     */
    fun handleRemoteKeyEvent(keyCode: Int, isKeyUp: Boolean): Boolean {
        val keyName = readableKeyName(keyCode)
        transientState.update {
            it.copy(lastDetectedKeyCode = keyCode, lastDetectedKeyName = keyName)
        }

        val listening = transientState.value.listeningAction
        if (listening == null) return false

        // While listening, swallow key events to avoid accidental navigation.
        if (!isKeyUp) return true

        if (keyCode in blockedAssignmentKeys) {
            transientState.update {
                it.copy(
                    assignmentMessage = "This button cannot be assigned",
                    isAssignmentError = true
                )
            }
            return true
        }

        viewModelScope.launch {
            repository.setRemoteKeyCode(listening, keyCode)
            transientState.update {
                it.copy(
                    listeningAction = null,
                    assignmentMessage = "${listening.displayName()} assigned to $keyName",
                    isAssignmentError = false
                )
            }
        }
        return true
    }

    private fun readableKeyName(keyCode: Int): String {
        return KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
    }

    private fun RemoteMappingAction.displayName(): String = when (this) {
        RemoteMappingAction.UNDO -> "Undo"
        RemoteMappingAction.RED -> "Red"
        RemoteMappingAction.YELLOW -> "Yellow"
        RemoteMappingAction.GREEN -> "Green"
        RemoteMappingAction.BROWN -> "Brown"
        RemoteMappingAction.BLUE -> "Blue"
        RemoteMappingAction.PINK -> "Pink"
        RemoteMappingAction.BLACK -> "Black"
        RemoteMappingAction.ERROR -> "Error/Foul"
    }

    private val blockedAssignmentKeys = setOf(
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_MENU
    )

    private fun currentAppVersion(): String {
        return runCatching {
            val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            info.versionName ?: "unknown"
        }.getOrDefault("unknown")
    }

    class Factory(
        private val repository: SettingsRepository,
        private val backupRepository: DataBackupRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, backupRepository, appContext.applicationContext) as T
        }
    }
}
