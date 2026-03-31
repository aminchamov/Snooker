package com.elocho.snooker.ui.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elocho.snooker.data.model.Match
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.data.model.Tournament
import com.elocho.snooker.data.model.TournamentMatch
import com.elocho.snooker.data.repository.MatchRepository
import com.elocho.snooker.data.repository.PlayerRepository
import com.elocho.snooker.data.repository.TournamentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

data class TournamentListUiState(
    val tournaments: List<Tournament> = emptyList(),
    val isLoading: Boolean = true
)

data class CreateTournamentUiState(
    val name: String = "",
    val availablePlayers: List<Player> = emptyList(),
    val selectedPlayerCount: Int = 2,
    val firstRoundSlots: List<Long?> = List(2) { null },
    val selectedPlayerIds: List<Long> = emptyList(),
    val error: String? = null
)

data class BracketUiState(
    val tournament: Tournament? = null,
    val matches: List<TournamentMatch> = emptyList(),
    val players: Map<Long, Player> = emptyMap(),
    val totalRounds: Int = 0,
    val isFinished: Boolean = false,
    val showDrawDialog: Boolean = false,
    val drawMatchId: Long? = null,
    val drawPlayer1: Player? = null,
    val drawPlayer2: Player? = null
)

class TournamentViewModel(
    private val tournamentRepository: TournamentRepository,
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(TournamentListUiState())
    val listState: StateFlow<TournamentListUiState> = _listState.asStateFlow()

    private val _createState = MutableStateFlow(CreateTournamentUiState())
    val createState: StateFlow<CreateTournamentUiState> = _createState.asStateFlow()

    private val _bracketState = MutableStateFlow(BracketUiState())
    val bracketState: StateFlow<BracketUiState> = _bracketState.asStateFlow()

    init {
        viewModelScope.launch {
            tournamentRepository.getAllTournaments().collect { tournaments ->
                _listState.update { it.copy(tournaments = tournaments, isLoading = false) }
            }
        }
    }

    fun loadAvailablePlayers() {
        viewModelScope.launch {
            playerRepository.getAllPlayers().collect { players ->
                _createState.update { state ->
                    state.copy(
                        availablePlayers = players,
                        selectedPlayerIds = state.firstRoundSlots.filterNotNull()
                    )
                }
            }
        }
    }

    fun updateTournamentName(name: String) {
        _createState.update { it.copy(name = name, error = null) }
    }

    fun setSelectedPlayerCount(count: Int) {
        if (count !in listOf(2, 4, 8, 16, 32)) return
        _createState.update { state ->
            val resizedSlots = List(count) { index -> state.firstRoundSlots.getOrNull(index) }
            state.copy(
                selectedPlayerCount = count,
                firstRoundSlots = resizedSlots,
                selectedPlayerIds = resizedSlots.filterNotNull(),
                error = null
            )
        }
    }

    fun assignPlayerToSlot(slotIndex: Int, playerId: Long?) {
        _createState.update { state ->
            if (slotIndex !in state.firstRoundSlots.indices) return@update state

            val updatedSlots = state.firstRoundSlots.toMutableList()
            if (playerId != null) {
                // Keep each player unique across first-round bracket slots.
                for (i in updatedSlots.indices) {
                    if (i != slotIndex && updatedSlots[i] == playerId) {
                        updatedSlots[i] = null
                    }
                }
            }
            updatedSlots[slotIndex] = playerId

            state.copy(
                firstRoundSlots = updatedSlots,
                selectedPlayerIds = updatedSlots.filterNotNull(),
                error = null
            )
        }
    }

    fun createTournament(onCreated: (Long) -> Unit) {
        val state = _createState.value
        if (state.name.isBlank()) {
            _createState.update { it.copy(error = "Please enter a tournament name") }
            return
        }
        if (state.selectedPlayerCount !in listOf(2, 4, 8, 16, 32)) {
            _createState.update { it.copy(error = "Please select a valid player count") }
            return
        }

        val selected = state.firstRoundSlots.filterNotNull()
        if (selected.size != state.selectedPlayerCount) {
            _createState.update { it.copy(error = "Please fill all first-round bracket slots") }
            return
        }
        if (selected.distinct().size != selected.size) {
            _createState.update { it.copy(error = "Each player can only be used once in round 1") }
            return
        }

        viewModelScope.launch {
            val playerCount = state.selectedPlayerCount
            val totalRounds = ceil(log2(playerCount.toDouble())).toInt()

            val tournament = Tournament(
                name = state.name.trim(),
                status = "in_progress",
                totalRounds = totalRounds,
                playerCount = playerCount
            )
            val tournamentId = tournamentRepository.insertTournament(tournament)

            // Create bracket matches
            val matches = mutableListOf<TournamentMatch>()

            // First round matchups
            val playerIds = selected
            val firstRoundMatchCount = playerCount / 2

            for (i in 0 until firstRoundMatchCount) {
                val p1Id = playerIds.getOrNull(i * 2)
                val p2Id = playerIds.getOrNull(i * 2 + 1)

                matches.add(
                    TournamentMatch(
                        tournamentId = tournamentId,
                        roundNumber = 1,
                        bracketPosition = i,
                        player1Id = p1Id,
                        player2Id = p2Id,
                        winnerPlayerId = null,
                        state = if (p1Id != null && p2Id != null) "ready" else "pending"
                    )
                )
            }

            // Create empty slots for subsequent rounds
            for (round in 2..totalRounds) {
                val matchCount = playerCount / (2.0.pow(round).toInt())
                for (pos in 0 until matchCount) {
                    matches.add(
                        TournamentMatch(
                            tournamentId = tournamentId,
                            roundNumber = round,
                            bracketPosition = pos,
                            state = "pending"
                        )
                    )
                }
            }

            tournamentRepository.insertTournamentMatches(matches)

            // Update players' tournament count
            selected.forEach { pid ->
                playerRepository.incrementTournamentsPlayed(pid)
            }

            // Reset create form
            _createState.value = CreateTournamentUiState()
            onCreated(tournamentId)
        }
    }

    fun loadBracket(tournamentId: Long) {
        viewModelScope.launch {
            val tournament = tournamentRepository.getTournamentById(tournamentId)
            _bracketState.update { it.copy(tournament = tournament) }

            tournamentRepository.getTournamentMatches(tournamentId).collect { matches ->
                // Collect all player IDs
                val playerIds = matches.flatMap { m ->
                    listOfNotNull(m.player1Id, m.player2Id, m.winnerPlayerId)
                }.distinct()

                val players = if (playerIds.isNotEmpty()) {
                    playerRepository.getPlayersByIds(playerIds).associateBy { it.id }
                } else emptyMap()

                val totalRounds = tournament?.totalRounds ?: 0
                val isFinished = tournament?.status == "completed"

                _bracketState.update {
                    it.copy(
                        matches = matches,
                        players = players,
                        totalRounds = totalRounds,
                        isFinished = isFinished
                    )
                }
            }
        }
    }

    fun completeMatch(
        tournamentMatchId: Long,
        matchResult: Match,
        onNeedDrawResolution: (Long) -> Unit
    ) {
        val state = _bracketState.value
        val tm = state.matches.find { it.id == tournamentMatchId } ?: return

        // Check for draw
        if (matchResult.isDraw || matchResult.winnerPlayerId == null) {
            _bracketState.update {
                it.copy(
                    showDrawDialog = true,
                    drawMatchId = tournamentMatchId,
                    drawPlayer1 = state.players[tm.player1Id],
                    drawPlayer2 = state.players[tm.player2Id]
                )
            }
            // Save the match result anyway
            viewModelScope.launch {
                val savedMatchId = matchRepository.insertMatch(matchResult)
                val updatedTm = tm.copy(
                    linkedMatchId = savedMatchId,
                    updatedAt = System.currentTimeMillis()
                )
                tournamentRepository.updateTournamentMatch(updatedTm)
            }
            return
        }

        viewModelScope.launch {
            // Save match
            val savedMatchId = matchRepository.insertMatch(matchResult)

            // Update tournament match
            val updatedTm = tm.copy(
                winnerPlayerId = matchResult.winnerPlayerId,
                linkedMatchId = savedMatchId,
                state = "completed",
                updatedAt = System.currentTimeMillis()
            )
            tournamentRepository.updateTournamentMatch(updatedTm)

            // Update player stats
            val loserId = if (matchResult.winnerPlayerId == tm.player1Id) tm.player2Id else tm.player1Id
            playerRepository.updatePlayerMatchStats(matchResult.winnerPlayerId, won = true, draw = false)
            if (loserId != null) {
                playerRepository.updatePlayerMatchStats(loserId, won = false, draw = false)
            }

            // Advance winner
            val allMatches = tournamentRepository.getTournamentMatchesOnce(state.tournament!!.id)
            advanceWinner(state.tournament.id, updatedTm, allMatches)
        }
    }

    fun resolveDrawWinner(winnerId: Long) {
        val state = _bracketState.value
        val tmId = state.drawMatchId ?: return

        viewModelScope.launch {
            val tm = tournamentRepository.getTournamentMatchById(tmId) ?: return@launch
            val updatedTm = tm.copy(
                winnerPlayerId = winnerId,
                state = "completed",
                updatedAt = System.currentTimeMillis()
            )
            tournamentRepository.updateTournamentMatch(updatedTm)

            // Update stats
            val loserId = if (winnerId == tm.player1Id) tm.player2Id else tm.player1Id
            playerRepository.updatePlayerMatchStats(winnerId, won = true, draw = false)
            if (loserId != null) {
                playerRepository.updatePlayerMatchStats(loserId, won = false, draw = false)
            }

            // Advance winner
            val allMatches = tournamentRepository.getTournamentMatchesOnce(state.tournament!!.id)
            advanceWinner(state.tournament.id, updatedTm, allMatches)

            _bracketState.update {
                it.copy(showDrawDialog = false, drawMatchId = null, drawPlayer1 = null, drawPlayer2 = null)
            }
        }
    }

    fun dismissDrawDialog() {
        _bracketState.update {
            it.copy(showDrawDialog = false, drawMatchId = null, drawPlayer1 = null, drawPlayer2 = null)
        }
    }

    private suspend fun advanceWinner(
        tournamentId: Long,
        completedMatch: TournamentMatch,
        allMatches: List<TournamentMatch>
    ) {
        val winnerId = completedMatch.winnerPlayerId ?: return
        val nextRound = completedMatch.roundNumber + 1
        val nextPos = completedMatch.bracketPosition / 2

        val tournament = tournamentRepository.getTournamentById(tournamentId) ?: return

        // Check if this is the final match
        if (nextRound > tournament.totalRounds) {
            // Tournament completed!
            tournamentRepository.updateTournament(
                tournament.copy(
                    status = "completed",
                    championPlayerId = winnerId,
                    updatedAt = System.currentTimeMillis()
                )
            )
            playerRepository.incrementTournamentsWon(winnerId)
            return
        }

        // Find the next round match
        val nextMatch = allMatches.find {
            it.tournamentId == tournamentId &&
                    it.roundNumber == nextRound &&
                    it.bracketPosition == nextPos
        } ?: return

        // Determine if winner goes to player1 or player2 slot
        val isTopHalf = completedMatch.bracketPosition % 2 == 0
        val updatedNext = if (isTopHalf) {
            nextMatch.copy(player1Id = winnerId)
        } else {
            nextMatch.copy(player2Id = winnerId)
        }

        // Check if both players are set → mark as ready
        val finalState = if (updatedNext.player1Id != null && updatedNext.player2Id != null) {
            updatedNext.copy(
                state = "ready",
                updatedAt = System.currentTimeMillis()
            )
        } else {
            updatedNext.copy(updatedAt = System.currentTimeMillis())
        }

        tournamentRepository.updateTournamentMatch(finalState)
    }

    fun deleteTournament(id: Long) {
        viewModelScope.launch {
            tournamentRepository.deleteTournament(id)
        }
    }

    class Factory(
        private val tournamentRepository: TournamentRepository,
        private val playerRepository: PlayerRepository,
        private val matchRepository: MatchRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TournamentViewModel(tournamentRepository, playerRepository, matchRepository) as T
        }
    }
}
