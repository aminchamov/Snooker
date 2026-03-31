package com.elocho.snooker.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elocho.snooker.data.model.LiveMatchSnapshot
import com.elocho.snooker.data.model.Match
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.data.repository.LiveMatchSnapshotRepository
import com.elocho.snooker.data.repository.MatchRepository
import com.elocho.snooker.data.repository.PlayerRepository
import com.elocho.snooker.data.sync.SupabaseSyncRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BreakRecord(
    val playerNumber: Int,
    val points: Int,
    val sequence: Int
)

data class SnookerTableState(
    val redsRemaining: Int = 15,
    val yellowVisible: Boolean = true,
    val greenVisible: Boolean = true,
    val brownVisible: Boolean = true,
    val blueVisible: Boolean = true,
    val pinkVisible: Boolean = true,
    val blackVisible: Boolean = true,
    val cueVisible: Boolean = true
)

data class ScoreboardUiState(
    val player1: Player? = null,
    val player2: Player? = null,
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val activePlayerNumber: Int? = null,
    val currentBreakPlayer1: Int = 0,
    val currentBreakPlayer2: Int = 0,
    val highestBreakPlayer1: Int = 0,
    val highestBreakPlayer2: Int = 0,
    val highestBreakInMatch: Int = 0,
    val breakHistory: List<BreakRecord> = emptyList(),
    val tableState: SnookerTableState = SnookerTableState(),
    val elapsedSeconds: Long = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val winnerPlayer: Player? = null,
    val isDraw: Boolean = false,
    val savedMatchId: Long? = null,
    val activeTournamentId: Long? = null,
    val activeTournamentRound: Int? = null,
    val activeTournamentMatchId: Long? = null,
    // TV remote directional player selection: 1 = left/player1, 2 = right/player2
    val tvSelectedPlayer: Int = 1,
    // Show "End Match?" confirmation when the final black ball is potted
    val showEndMatchConfirmation: Boolean = false
)

class QuickMatchViewModel(
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    private val liveMatchSnapshotRepository: LiveMatchSnapshotRepository,
    private val syncRepository: SupabaseSyncRepository
) : ViewModel() {
    private sealed interface MatchEvent {
        data class ScoreAction(
            val playerNumber: Int,
            val action: SnookerScoreAction
        ) : MatchEvent

        data class ScoreCorrection(
            val playerNumber: Int,
            val points: Int
        ) : MatchEvent
    }

    private data class RecomputedMatchState(
        val player1Score: Int,
        val player2Score: Int,
        val activePlayerNumber: Int?,
        val currentBreakPlayer1: Int,
        val currentBreakPlayer2: Int,
        val highestBreakPlayer1: Int,
        val highestBreakPlayer2: Int,
        val highestBreakInMatch: Int,
        val breakHistory: List<BreakRecord>,
        val tableState: SnookerTableState
    )

    private data class FinalizedBreakState(
        val currentBreakPlayer1: Int,
        val currentBreakPlayer2: Int,
        val breakHistory: List<BreakRecord>
    )

    private data class FinalizedHighestBreaks(
        val player1: Int,
        val player2: Int,
        val match: Int
    )

    private val _uiState = MutableStateFlow(ScoreboardUiState())
    val uiState: StateFlow<ScoreboardUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var matchStartTime: Long = 0
    private val matchEvents = mutableListOf<MatchEvent>()

    fun setupMatch(
        player1Id: Long,
        player2Id: Long,
        tournamentId: Long? = null,
        tournamentRound: Int? = null,
        tournamentMatchId: Long? = null
    ) {
        viewModelScope.launch {
            val p1 = playerRepository.getPlayerById(player1Id)
            val p2 = playerRepository.getPlayerById(player2Id)
            _uiState.update {
                it.copy(
                    player1 = p1,
                    player2 = p2,
                    player1Score = 0,
                    player2Score = 0,
                    activePlayerNumber = null,
                    currentBreakPlayer1 = 0,
                    currentBreakPlayer2 = 0,
                    highestBreakPlayer1 = 0,
                    highestBreakPlayer2 = 0,
                    highestBreakInMatch = 0,
                    breakHistory = emptyList(),
                    activeTournamentId = tournamentId,
                    activeTournamentRound = tournamentRound,
                    activeTournamentMatchId = tournamentMatchId,
                    tvSelectedPlayer = 1,
                    showEndMatchConfirmation = false
                )
            }
            matchEvents.clear()
            startTimer()
            publishLiveSnapshot(_uiState.value)
        }
    }

    private fun startTimer() {
        matchStartTime = System.currentTimeMillis()
        _uiState.update { it.copy(isRunning = true) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update {
                    it.copy(elapsedSeconds = it.elapsedSeconds + 1)
                }
            }
        }
    }

    fun addScore(playerNumber: Int, action: SnookerScoreAction) {
        matchEvents.add(MatchEvent.ScoreAction(playerNumber = playerNumber, action = action))
        recomputeMatchStatsFromEvents()
    }

    /** Called by TV remote directional keys to pick which player receives remote scoring. */
    fun selectTvPlayer(playerNumber: Int) {
        _uiState.update { it.copy(tvSelectedPlayer = playerNumber) }
    }

    /**
     * Routes a remote-mapped scoring action to the TV-selected player.
     * On TV, the user explicitly selects left (player 1) or right (player 2)
     * via DPAD_LEFT / DPAD_RIGHT before pressing a score button.
     */
    fun addScoreForActivePlayer(action: SnookerScoreAction) {
        val playerNumber = _uiState.value.tvSelectedPlayer
        addScore(playerNumber, action)
    }

    fun applyScoreCorrection(playerNumber: Int, points: Int = 1) {
        matchEvents.add(MatchEvent.ScoreCorrection(playerNumber = playerNumber, points = points))
        recomputeMatchStatsFromEvents()
    }

    fun undoLastScoreAction() {
        val lastScoreActionIndex = matchEvents.indexOfLast { it is MatchEvent.ScoreAction }
        if (lastScoreActionIndex == -1) return
        matchEvents.removeAt(lastScoreActionIndex)
        recomputeMatchStatsFromEvents()
    }

    private fun recomputeMatchStatsFromEvents() {
        val recomputed = computeStateFromEvents(matchEvents)
        _uiState.update { prev ->
            val t = recomputed.tableState
            val allBallsCleared = t.redsRemaining == 0 &&
                !t.yellowVisible && !t.greenVisible && !t.brownVisible &&
                !t.blueVisible && !t.pinkVisible && !t.blackVisible
            // Show confirmation when black is JUST potted (prev had black visible),
            // keep showing if already shown, hide if undo restored the black.
            val showConfirmation = allBallsCleared &&
                (prev.tableState.blackVisible || prev.showEndMatchConfirmation)
            prev.copy(
                player1Score = recomputed.player1Score,
                player2Score = recomputed.player2Score,
                activePlayerNumber = recomputed.activePlayerNumber,
                currentBreakPlayer1 = recomputed.currentBreakPlayer1,
                currentBreakPlayer2 = recomputed.currentBreakPlayer2,
                highestBreakPlayer1 = recomputed.highestBreakPlayer1,
                highestBreakPlayer2 = recomputed.highestBreakPlayer2,
                highestBreakInMatch = recomputed.highestBreakInMatch,
                breakHistory = recomputed.breakHistory,
                tableState = recomputed.tableState,
                showEndMatchConfirmation = showConfirmation
            )
        }
        publishLiveSnapshot(_uiState.value)
    }

    fun dismissEndMatchConfirmation() {
        _uiState.update { it.copy(showEndMatchConfirmation = false) }
    }

    private fun computeStateFromEvents(events: List<MatchEvent>): RecomputedMatchState {
        var player1Score = 0
        var player2Score = 0
        var activePlayerNumber: Int? = null
        var currentBreakPlayer1 = 0
        var currentBreakPlayer2 = 0
        val breakHistory = mutableListOf<BreakRecord>()

        fun finalizeActiveBreak() {
            when (activePlayerNumber) {
                1 -> {
                    if (currentBreakPlayer1 > 0) {
                        breakHistory.add(BreakRecord(playerNumber = 1, points = currentBreakPlayer1, sequence = breakHistory.size + 1))
                    }
                    currentBreakPlayer1 = 0
                }

                2 -> {
                    if (currentBreakPlayer2 > 0) {
                        breakHistory.add(BreakRecord(playerNumber = 2, points = currentBreakPlayer2, sequence = breakHistory.size + 1))
                    }
                    currentBreakPlayer2 = 0
                }
            }
            activePlayerNumber = null
        }

        events.forEach { event ->
            when (event) {
                is MatchEvent.ScoreAction -> {
                    val points = event.action.points
                    if (event.playerNumber == 1) {
                        player1Score += points
                    } else {
                        player2Score += points
                    }

                    if (event.action == SnookerScoreAction.ERROR) {
                        // Foul/error ends the active visit and current break.
                        finalizeActiveBreak()
                    } else {
                        if (activePlayerNumber == null) {
                            activePlayerNumber = event.playerNumber
                        } else if (activePlayerNumber != event.playerNumber) {
                            finalizeActiveBreak()
                            activePlayerNumber = event.playerNumber
                        }

                        if (event.playerNumber == 1) {
                            currentBreakPlayer1 += points
                        } else {
                            currentBreakPlayer2 += points
                        }
                    }
                }

                is MatchEvent.ScoreCorrection -> {
                    if (event.playerNumber == 1) {
                        player1Score = maxOf(0, player1Score - event.points)
                        if (activePlayerNumber == 1) {
                            currentBreakPlayer1 = maxOf(0, currentBreakPlayer1 - event.points)
                        }
                    } else {
                        player2Score = maxOf(0, player2Score - event.points)
                        if (activePlayerNumber == 2) {
                            currentBreakPlayer2 = maxOf(0, currentBreakPlayer2 - event.points)
                        }
                    }
                }
            }
        }

        val highestBreakPlayer1 = maxOf(
            currentBreakPlayer1,
            breakHistory.filter { it.playerNumber == 1 }.maxOfOrNull { it.points } ?: 0
        )
        val highestBreakPlayer2 = maxOf(
            currentBreakPlayer2,
            breakHistory.filter { it.playerNumber == 2 }.maxOfOrNull { it.points } ?: 0
        )
        val highestBreakInMatch = maxOf(highestBreakPlayer1, highestBreakPlayer2)
        val tableState = deriveSnookerTableState(events)

        return RecomputedMatchState(
            player1Score = player1Score,
            player2Score = player2Score,
            activePlayerNumber = activePlayerNumber,
            currentBreakPlayer1 = currentBreakPlayer1,
            currentBreakPlayer2 = currentBreakPlayer2,
            highestBreakPlayer1 = highestBreakPlayer1,
            highestBreakPlayer2 = highestBreakPlayer2,
            highestBreakInMatch = highestBreakInMatch,
            breakHistory = breakHistory,
            tableState = tableState
        )
    }

    private fun deriveSnookerTableState(events: List<MatchEvent>): SnookerTableState {
        val scoreActions = events.filterIsInstance<MatchEvent.ScoreAction>()
        val redsScored = scoreActions.count { it.action == SnookerScoreAction.RED }.coerceIn(0, 15)
        val redsRemaining = (15 - redsScored).coerceIn(0, 15)

        var yellowVisible = true
        var greenVisible = true
        var brownVisible = true
        var blueVisible = true
        var pinkVisible = true
        var blackVisible = true

        if (redsRemaining == 0) {
            var redCounter = 0
            var redsExhaustedIndex: Int? = null
            scoreActions.forEachIndexed { index, event ->
                if (event.action == SnookerScoreAction.RED && redCounter < 15) {
                    redCounter++
                    if (redCounter == 15) {
                        redsExhaustedIndex = index
                    }
                }
            }

            val clearanceOrder = listOf(
                SnookerScoreAction.YELLOW,
                SnookerScoreAction.GREEN,
                SnookerScoreAction.BROWN,
                SnookerScoreAction.BLUE,
                SnookerScoreAction.PINK,
                SnookerScoreAction.BLACK
            )
            var clearanceIndex = 0
            val startIndex = ((redsExhaustedIndex ?: -1) + 1).coerceAtLeast(0)
            for (i in startIndex until scoreActions.size) {
                if (clearanceIndex >= clearanceOrder.size) break
                if (scoreActions[i].action == clearanceOrder[clearanceIndex]) {
                    when (clearanceOrder[clearanceIndex]) {
                        SnookerScoreAction.YELLOW -> yellowVisible = false
                        SnookerScoreAction.GREEN -> greenVisible = false
                        SnookerScoreAction.BROWN -> brownVisible = false
                        SnookerScoreAction.BLUE -> blueVisible = false
                        SnookerScoreAction.PINK -> pinkVisible = false
                        SnookerScoreAction.BLACK -> blackVisible = false
                        else -> Unit
                    }
                    clearanceIndex++
                }
            }
        }

        return SnookerTableState(
            redsRemaining = redsRemaining,
            yellowVisible = yellowVisible,
            greenVisible = greenVisible,
            brownVisible = brownVisible,
            blueVisible = blueVisible,
            pinkVisible = pinkVisible,
            blackVisible = blackVisible,
            cueVisible = true
        )
    }

    private fun finalizeCurrentBreak(state: ScoreboardUiState): FinalizedBreakState {
        var currentBreakPlayer1 = state.currentBreakPlayer1
        var currentBreakPlayer2 = state.currentBreakPlayer2
        val breakHistory = state.breakHistory.toMutableList()
        val activePlayerNumber = state.activePlayerNumber

        when (activePlayerNumber) {
            1 -> {
                if (currentBreakPlayer1 > 0) {
                    breakHistory.add(BreakRecord(playerNumber = 1, points = currentBreakPlayer1, sequence = breakHistory.size + 1))
                }
                currentBreakPlayer1 = 0
            }

            2 -> {
                if (currentBreakPlayer2 > 0) {
                    breakHistory.add(BreakRecord(playerNumber = 2, points = currentBreakPlayer2, sequence = breakHistory.size + 1))
                }
                currentBreakPlayer2 = 0
            }
        }

        return FinalizedBreakState(
            currentBreakPlayer1 = currentBreakPlayer1,
            currentBreakPlayer2 = currentBreakPlayer2,
            breakHistory = breakHistory
        )
    }

    private fun encodeBreakHistory(history: List<BreakRecord>): String? {
        if (history.isEmpty()) return null
        return history.joinToString(separator = ";") { "P${it.playerNumber}:${it.points}" }
    }

    private fun resolveFinalHighestBreaks(state: ScoreboardUiState): FinalizedHighestBreaks {
        val historyP1 = state.breakHistory
            .asSequence()
            .filter { it.playerNumber == 1 }
            .maxOfOrNull { it.points } ?: 0
        val historyP2 = state.breakHistory
            .asSequence()
            .filter { it.playerNumber == 2 }
            .maxOfOrNull { it.points } ?: 0

        val player1 = maxOf(state.highestBreakPlayer1, state.currentBreakPlayer1, historyP1)
        val player2 = maxOf(state.highestBreakPlayer2, state.currentBreakPlayer2, historyP2)
        return FinalizedHighestBreaks(
            player1 = player1,
            player2 = player2,
            match = maxOf(player1, player2)
        )
    }

    fun endGame() {
        timerJob?.cancel()
        val state = _uiState.value
        val finalizedBreaks = finalizeCurrentBreak(state)
        val finalizedState = state.copy(
            activePlayerNumber = null,
            currentBreakPlayer1 = finalizedBreaks.currentBreakPlayer1,
            currentBreakPlayer2 = finalizedBreaks.currentBreakPlayer2,
            breakHistory = finalizedBreaks.breakHistory
        )
        val finalHighestBreaks = resolveFinalHighestBreaks(finalizedState)
        val finalState = finalizedState.copy(
            highestBreakPlayer1 = finalHighestBreaks.player1,
            highestBreakPlayer2 = finalHighestBreaks.player2,
            highestBreakInMatch = finalHighestBreaks.match
        )

        val isDraw = finalState.player1Score == finalState.player2Score
        val winnerId = when {
            isDraw -> null
            finalState.player1Score > finalState.player2Score -> finalState.player1?.id
            else -> finalState.player2?.id
        }
        val winnerPlayer = when (winnerId) {
            finalState.player1?.id -> finalState.player1
            finalState.player2?.id -> finalState.player2
            else -> null
        }

        _uiState.value = finalState.copy(
            isRunning = false,
            isFinished = true,
            winnerPlayer = winnerPlayer,
            isDraw = isDraw
        )

        // Save match and update player stats
        viewModelScope.launch {
            val match = Match(
                player1Id = finalState.player1!!.id,
                player2Id = finalState.player2!!.id,
                player1Score = finalState.player1Score,
                player2Score = finalState.player2Score,
                startedAt = matchStartTime,
                endedAt = System.currentTimeMillis(),
                durationSeconds = finalState.elapsedSeconds,
                winnerPlayerId = winnerId,
                isDraw = isDraw,
                player1HighestBreak = finalState.highestBreakPlayer1,
                player2HighestBreak = finalState.highestBreakPlayer2,
                matchHighestBreak = finalState.highestBreakInMatch,
                breakHistorySummary = encodeBreakHistory(finalState.breakHistory),
                matchType = "quick_match",
                updatedAt = System.currentTimeMillis()
            )
            val matchId = matchRepository.insertMatch(match)
            _uiState.update { it.copy(savedMatchId = matchId) }

            // Update player stats
            if (isDraw) {
                playerRepository.updatePlayerMatchStats(finalState.player1!!.id, won = false, draw = true)
                playerRepository.updatePlayerMatchStats(finalState.player2!!.id, won = false, draw = true)
            } else {
                playerRepository.updatePlayerMatchStats(winnerId!!, won = true, draw = false)
                val loserId = if (winnerId == finalState.player1!!.id) finalState.player2!!.id else finalState.player1.id
                playerRepository.updatePlayerMatchStats(loserId, won = false, draw = false)
            }
            syncRepository.clearLiveSnapshot()
        }
    }

    fun endTournamentGame(forcedWinnerId: Long? = null): Match? {
        timerJob?.cancel()
        val state = _uiState.value
        val finalizedBreaks = finalizeCurrentBreak(state)
        val finalizedState = state.copy(
            activePlayerNumber = null,
            currentBreakPlayer1 = finalizedBreaks.currentBreakPlayer1,
            currentBreakPlayer2 = finalizedBreaks.currentBreakPlayer2,
            breakHistory = finalizedBreaks.breakHistory
        )
        val finalHighestBreaks = resolveFinalHighestBreaks(finalizedState)
        val finalState = finalizedState.copy(
            highestBreakPlayer1 = finalHighestBreaks.player1,
            highestBreakPlayer2 = finalHighestBreaks.player2,
            highestBreakInMatch = finalHighestBreaks.match
        )

        val isDraw = finalState.player1Score == finalState.player2Score
        val winnerId = forcedWinnerId ?: when {
            finalState.player1Score > finalState.player2Score -> finalState.player1?.id
            finalState.player2Score > finalState.player1Score -> finalState.player2?.id
            else -> null
        }

        _uiState.value = finalState.copy(isRunning = false, isFinished = true)
        viewModelScope.launch { syncRepository.clearLiveSnapshot() }

        return Match(
            player1Id = finalState.player1!!.id,
            player2Id = finalState.player2!!.id,
            player1Score = finalState.player1Score,
            player2Score = finalState.player2Score,
            startedAt = matchStartTime,
            endedAt = System.currentTimeMillis(),
            durationSeconds = finalState.elapsedSeconds,
            winnerPlayerId = winnerId,
            isDraw = isDraw,
            player1HighestBreak = finalState.highestBreakPlayer1,
            player2HighestBreak = finalState.highestBreakPlayer2,
            matchHighestBreak = finalState.highestBreakInMatch,
            breakHistorySummary = encodeBreakHistory(finalState.breakHistory),
            matchType = "tournament",
            tournamentId = finalState.activeTournamentId,
            tournamentRound = finalState.activeTournamentRound,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun reset() {
        timerJob?.cancel()
        matchEvents.clear()
        _uiState.value = ScoreboardUiState()
        viewModelScope.launch {
            syncRepository.clearLiveSnapshot()
        }
    }

    private fun publishLiveSnapshot(state: ScoreboardUiState) {
        if (state.player1 == null || state.player2 == null) return
        val snapshot = LiveMatchSnapshot(
            id = 1,
            isActive = !state.isFinished,
            player1Id = state.player1.id,
            player2Id = state.player2.id,
            player1Name = state.player1.name,
            player2Name = state.player2.name,
            player1Score = state.player1Score,
            player2Score = state.player2Score,
            activePlayerNumber = state.activePlayerNumber,
            currentBreakPlayer1 = state.currentBreakPlayer1,
            currentBreakPlayer2 = state.currentBreakPlayer2,
            highestBreakPlayer1 = state.highestBreakPlayer1,
            highestBreakPlayer2 = state.highestBreakPlayer2,
            highestBreakInMatch = state.highestBreakInMatch,
            redsRemaining = state.tableState.redsRemaining,
            yellowVisible = state.tableState.yellowVisible,
            greenVisible = state.tableState.greenVisible,
            brownVisible = state.tableState.brownVisible,
            blueVisible = state.tableState.blueVisible,
            pinkVisible = state.tableState.pinkVisible,
            blackVisible = state.tableState.blackVisible,
            tournamentId = state.activeTournamentId,
            tournamentRound = state.activeTournamentRound,
            tournamentMatchId = state.activeTournamentMatchId,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            liveMatchSnapshotRepository.upsertSnapshot(snapshot)
            syncRepository.publishLiveSnapshot(snapshot)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(
        private val matchRepository: MatchRepository,
        private val playerRepository: PlayerRepository,
        private val liveMatchSnapshotRepository: LiveMatchSnapshotRepository,
        private val syncRepository: SupabaseSyncRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuickMatchViewModel(
                matchRepository = matchRepository,
                playerRepository = playerRepository,
                liveMatchSnapshotRepository = liveMatchSnapshotRepository,
                syncRepository = syncRepository
            ) as T
        }
    }
}
