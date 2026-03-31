package com.elocho.snooker.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elocho.snooker.data.model.Match
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.data.model.Tournament
import com.elocho.snooker.data.repository.MatchRepository
import com.elocho.snooker.data.repository.PlayerRepository
import com.elocho.snooker.data.repository.TournamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RankedPlayerStat(
    val rank: Int = 0,
    val playerId: Long,
    val playerName: String,
    val gamesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val maxBreak: Int,
    val averagePointsPerMatch: Double,
    val tournamentsWon: Int,
    val winRate: Double
)

data class StatisticsUiState(
    val rankedPlayers: List<RankedPlayerStat> = emptyList(),
    val totalPlayers: Int = 0,
    val totalCompletedMatches: Int = 0,
    val totalCompletedTournaments: Int = 0,
    val isLoading: Boolean = true,
    val errorMsg: String? = null
)

class StatisticsViewModel(
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _runtimeError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StatisticsUiState> = combine(
        playerRepository.getAllPlayers(),
        matchRepository.getAllMatches(),
        tournamentRepository.getAllTournaments(),
        _runtimeError.asStateFlow()
    ) { players, matches, tournaments, runtimeError ->
        val completedMatches = matches.filter { it.endedAt != null }
        val completedTournaments = tournaments.filter { it.championPlayerId != null || it.status == "completed" }
        val ranked = buildRankedStats(
            players = players,
            matches = completedMatches,
            tournaments = completedTournaments
        )

        StatisticsUiState(
            rankedPlayers = ranked,
            totalPlayers = players.size,
            totalCompletedMatches = completedMatches.size,
            totalCompletedTournaments = completedTournaments.size,
            isLoading = false,
            errorMsg = runtimeError
        )
    }.catch { throwable ->
        emit(
            StatisticsUiState(
                isLoading = false,
                errorMsg = throwable.message ?: "Failed to load statistics"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState(isLoading = true)
    )

    private fun buildRankedStats(
        players: List<Player>,
        matches: List<Match>,
        tournaments: List<Tournament>
    ): List<RankedPlayerStat> {
        val championCounts = tournaments
            .mapNotNull { it.championPlayerId }
            .groupingBy { it }
            .eachCount()

        val computed = players
            .filter { !it.archived }
            .map { player ->
                val playerMatches = matches.filter { it.player1Id == player.id || it.player2Id == player.id }
                val gamesPlayed = playerMatches.size
                val wins = playerMatches.count { !it.isDraw && it.winnerPlayerId == player.id }
                val draws = playerMatches.count { it.isDraw }
                val losses = (gamesPlayed - wins - draws).coerceAtLeast(0)
                val maxBreak = playerMatches.maxOfOrNull {
                    it.resolveMaxBreakForPlayer(player.id)
                } ?: 0
                val totalPoints = playerMatches.sumOf {
                    if (it.player1Id == player.id) it.player1Score else it.player2Score
                }
                val averagePoints = if (gamesPlayed == 0) 0.0 else totalPoints.toDouble() / gamesPlayed.toDouble()
                val derivedTournamentsWon = championCounts[player.id] ?: 0
                val tournamentsWon = maxOf(player.tournamentsWon, derivedTournamentsWon)
                val winRate = if (gamesPlayed == 0) 0.0 else wins.toDouble() / gamesPlayed.toDouble()

                RankedPlayerStat(
                    playerId = player.id,
                    playerName = player.name,
                    gamesPlayed = gamesPlayed,
                    wins = wins,
                    draws = draws,
                    losses = losses,
                    maxBreak = maxBreak,
                    averagePointsPerMatch = averagePoints,
                    tournamentsWon = tournamentsWon,
                    winRate = winRate
                )
            }
            .sortedWith(
                compareByDescending<RankedPlayerStat> { it.wins }
                    .thenByDescending { it.winRate }
                    .thenByDescending { it.maxBreak }
                    .thenByDescending { it.averagePointsPerMatch }
                    .thenBy { it.playerName.lowercase() }
            )

        return computed.mapIndexed { index, stat ->
            stat.copy(rank = index + 1)
        }
    }

    private fun Match.resolveMaxBreakForPlayer(playerId: Long): Int {
        val slot = when (playerId) {
            player1Id -> 1
            player2Id -> 2
            else -> return 0
        }

        val persisted = if (slot == 1) player1HighestBreak else player2HighestBreak
        if (persisted > 0) return persisted

        // Fallback for older matches where per-player highest break may remain 0.
        // breakHistorySummary format: "P1:12;P2:33;..."
        return parseBreakHistoryMax(slot, breakHistorySummary)
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

    class Factory(
        private val playerRepository: PlayerRepository,
        private val matchRepository: MatchRepository,
        private val tournamentRepository: TournamentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(playerRepository, matchRepository, tournamentRepository) as T
        }
    }
}
