package com.elocho.snooker.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elocho.snooker.data.model.Match
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.data.repository.MatchRepository
import com.elocho.snooker.data.repository.PlayerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlayersUiState(
    val players: List<Player> = emptyList(),
    val maxBreakByPlayerId: Map<Long, Int> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val editingPlayer: Player? = null,
    val showDeleteDialog: Boolean = false,
    val playerToDelete: Player? = null,
    // Add/Edit form
    val formName: String = "",
    val formImageUri: String? = null,
    val showForm: Boolean = false,
    val isEditing: Boolean = false,
    val formError: String? = null
)

class PlayersViewModel(
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayersUiState())
    val uiState: StateFlow<PlayersUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(
                _searchQuery.flatMapLatest { query ->
                    if (query.isBlank()) playerRepository.getAllPlayers()
                    else playerRepository.searchPlayers(query)
                },
                matchRepository.getAllMatches()
            ) { players, matches ->
                val completedMatches = matches.filter { it.endedAt != null }
                val maxBreakByPlayerId = players.associate { player ->
                    player.id to completedMatches.resolveMaxBreakForPlayer(player.id)
                }
                players to maxBreakByPlayerId
            }.collect { (players, maxBreakByPlayerId) ->
                _uiState.update {
                    it.copy(
                        players = players,
                        maxBreakByPlayerId = maxBreakByPlayerId,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun List<Match>.resolveMaxBreakForPlayer(playerId: Long): Int {
        return this
            .asSequence()
            .mapNotNull { match ->
                val slot = when (playerId) {
                    match.player1Id -> 1
                    match.player2Id -> 2
                    else -> return@mapNotNull null
                }
                val persisted = if (slot == 1) match.player1HighestBreak else match.player2HighestBreak
                if (persisted > 0) persisted else parseBreakHistoryMax(slot, match.breakHistorySummary)
            }
            .maxOrNull() ?: 0
    }

    private fun parseBreakHistoryMax(slot: Int, summary: String?): Int {
        if (summary.isNullOrBlank()) return 0
        return summary
            .split(';', ',', '|')
            .asSequence()
            .mapNotNull { token ->
                val cleaned = token.trim()
                if (cleaned.isEmpty()) return@mapNotNull null
                val parts = cleaned.split(':', '=', limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val prefix = parts[0].trim().uppercase()
                val points = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
                if (prefix == "P$slot" || prefix == "PLAYER$slot") points else null
            }
            .maxOrNull() ?: 0
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun showAddForm() {
        _uiState.update {
            it.copy(
                showForm = true, isEditing = false,
                formName = "", formImageUri = null, editingPlayer = null, formError = null
            )
        }
    }

    fun showEditForm(player: Player) {
        _uiState.update {
            it.copy(
                showForm = true, isEditing = true,
                formName = player.name, formImageUri = player.imageUri,
                editingPlayer = player,
                formError = null
            )
        }
    }

    fun hideForm() {
        _uiState.update { it.copy(showForm = false, editingPlayer = null, formError = null) }
    }

    fun updateFormName(name: String) {
        _uiState.update { it.copy(formName = name, formError = null) }
    }

    fun updateFormImageUri(uri: String?) {
        _uiState.update { it.copy(formImageUri = uri) }
    }

    fun savePlayer() {
        val state = _uiState.value
        val normalizedName = state.formName.trim()
        if (normalizedName.isBlank()) return

        viewModelScope.launch {
            val existing = playerRepository.getActivePlayerByExactName(normalizedName)
            val duplicateExists = existing != null && existing.id != state.editingPlayer?.id
            if (duplicateExists) {
                _uiState.update {
                    it.copy(formError = "A player with this name already exists. Please use a unique name.")
                }
                return@launch
            }

            if (state.isEditing && state.editingPlayer != null) {
                val updated = state.editingPlayer.copy(
                    name = normalizedName,
                    imageUri = state.formImageUri,
                    updatedAt = System.currentTimeMillis()
                )
                playerRepository.updatePlayer(updated)
            } else {
                playerRepository.insertPlayer(
                    Player(
                        name = normalizedName.uppercase(),
                        imageUri = state.formImageUri
                    )
                )
            }
            hideForm()
        }
    }

    fun confirmDelete(player: Player) {
        _uiState.update { it.copy(showDeleteDialog = true, playerToDelete = player) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteDialog = false, playerToDelete = null) }
    }

    fun deletePlayer() {
        val player = _uiState.value.playerToDelete ?: return
        viewModelScope.launch {
            // Soft delete (archive) to preserve history
            playerRepository.archivePlayer(player.id)
            cancelDelete()
        }
    }

    class Factory(
        private val playerRepository: PlayerRepository,
        private val matchRepository: MatchRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayersViewModel(playerRepository, matchRepository) as T
        }
    }
}
