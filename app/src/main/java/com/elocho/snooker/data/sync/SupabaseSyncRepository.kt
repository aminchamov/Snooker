package com.elocho.snooker.data.sync

import androidx.room.withTransaction
import com.elocho.snooker.data.local.ElOchoDatabase
import com.elocho.snooker.data.model.LiveMatchSnapshot
import com.elocho.snooker.data.model.Match
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.data.model.Tournament
import com.elocho.snooker.data.model.TournamentMatch
import com.elocho.snooker.data.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject

data class SyncResult(
    val success: Boolean,
    val pushedRows: Int,
    val pulledRows: Int,
    val syncedAt: Long?,
    val message: String
)

class SupabaseSyncRepository(
    private val database: ElOchoDatabase,
    private val settingsRepository: SettingsRepository,
    private val supabaseClient: SupabaseRestClient
) {
    private var cachedToken: SupabaseAuthToken? = null

    suspend fun syncAll(): SyncResult {
        return runCatching {
            val token = ensureAdminToken()

            val remotePlayers = supabaseClient.fetchRows("players", token.accessToken)
            val remoteTournaments = supabaseClient.fetchRows("tournaments", token.accessToken)
            val remoteTournamentMatches = supabaseClient.fetchRows("tournament_matches", token.accessToken)
            val remoteMatches = supabaseClient.fetchRows("matches", token.accessToken)
            val remoteLiveRows = supabaseClient.fetchRows("live_match_state", token.accessToken)

            val localPlayers = database.playerDao().getAllPlayersOnce()
            val localTournaments = database.tournamentDao().getAllTournamentsOnce()
            val localTournamentMatches = database.tournamentMatchDao().getAllTournamentMatchesOnce()
            val localMatches = database.matchDao().getAllMatchesOnce()
            val localLive = database.liveMatchSnapshotDao().getSnapshotOnce()

            val pushPlayers = collectPlayersToPush(localPlayers, remotePlayers)
            val pullPlayers = collectPlayersToPull(localPlayers, remotePlayers)
            val pushTournaments = collectTournamentsToPush(localTournaments, remoteTournaments)
            val pullTournaments = collectTournamentsToPull(localTournaments, remoteTournaments)
            val pushTournamentMatches = collectTournamentMatchesToPush(localTournamentMatches, remoteTournamentMatches)
            val pullTournamentMatches = collectTournamentMatchesToPull(localTournamentMatches, remoteTournamentMatches)
            val pushMatches = collectMatchesToPush(localMatches, remoteMatches)
            val pullMatches = collectMatchesToPull(localMatches, remoteMatches)
            val remoteLive = remoteLiveRows.toRemoteLiveSnapshot()
            val pushLive = shouldPushLiveSnapshot(localLive, remoteLive)
            val pullLive = shouldPullLiveSnapshot(localLive, remoteLive)

            if (pushPlayers.isNotEmpty()) {
                supabaseClient.upsertRows("players", JSONArray().apply { pushPlayers.forEach { put(it) } }, token.accessToken)
            }
            if (pushTournaments.isNotEmpty()) {
                supabaseClient.upsertRows("tournaments", JSONArray().apply { pushTournaments.forEach { put(it) } }, token.accessToken)
            }
            if (pushTournamentMatches.isNotEmpty()) {
                supabaseClient.upsertRows(
                    "tournament_matches",
                    JSONArray().apply { pushTournamentMatches.forEach { put(it) } },
                    token.accessToken
                )
            }
            if (pushMatches.isNotEmpty()) {
                supabaseClient.upsertRows("matches", JSONArray().apply { pushMatches.forEach { put(it) } }, token.accessToken)
            }
            if (pushLive != null) {
                supabaseClient.upsertRows("live_match_state", JSONArray().put(pushLive), token.accessToken)
            }

            database.withTransaction {
                if (pullPlayers.isNotEmpty()) database.playerDao().insertPlayers(pullPlayers)
                if (pullTournaments.isNotEmpty()) database.tournamentDao().insertTournaments(pullTournaments)
                if (pullTournamentMatches.isNotEmpty()) database.tournamentMatchDao().insertTournamentMatches(pullTournamentMatches)
                if (pullMatches.isNotEmpty()) database.matchDao().insertMatches(pullMatches)
                if (pullLive) {
                    if (remoteLive == null || !remoteLive.isActive) {
                        database.liveMatchSnapshotDao().clearSnapshot()
                    } else {
                        database.liveMatchSnapshotDao().upsertSnapshot(remoteLive)
                    }
                }
            }

            val pushedCount = pushPlayers.size + pushTournaments.size + pushTournamentMatches.size + pushMatches.size + if (pushLive != null) 1 else 0
            val pulledCount = pullPlayers.size + pullTournaments.size + pullTournamentMatches.size + pullMatches.size + if (pullLive) 1 else 0
            val syncedAt = System.currentTimeMillis()
            settingsRepository.setLastSyncState(
                lastSyncAt = syncedAt,
                status = "success",
                errorMessage = null
            )

            SyncResult(
                success = true,
                pushedRows = pushedCount,
                pulledRows = pulledCount,
                syncedAt = syncedAt,
                message = "Sync completed. Pushed $pushedCount row(s), pulled $pulledCount row(s)."
            )
        }.getOrElse { throwable ->
            settingsRepository.setLastSyncState(
                lastSyncAt = null,
                status = "failed",
                errorMessage = throwable.message
            )
            SyncResult(
                success = false,
                pushedRows = 0,
                pulledRows = 0,
                syncedAt = null,
                message = throwable.message ?: "Sync failed"
            )
        }
    }

    suspend fun publishLiveSnapshot(snapshot: LiveMatchSnapshot) {
        runCatching {
            database.liveMatchSnapshotDao().upsertSnapshot(snapshot)
            val token = ensureAdminToken()
            supabaseClient.upsertRows("live_match_state", JSONArray().put(snapshot.toRemoteJson()), token.accessToken)
        }
    }

    suspend fun clearLiveSnapshot() {
        runCatching {
            database.liveMatchSnapshotDao().clearSnapshot()
            val token = ensureAdminToken()
            val cleared = LiveMatchSnapshot(
                id = 1,
                isActive = false,
                updatedAt = System.currentTimeMillis()
            )
            supabaseClient.upsertRows("live_match_state", JSONArray().put(cleared.toRemoteJson()), token.accessToken)
        }
    }

    suspend fun autoRefreshIfStale(minIntervalMs: Long = 5 * 60 * 1000L): SyncResult? {
        val now = System.currentTimeMillis()
        val lastSyncedAt = settingsRepository.lastSyncAt.firstOrNull()
        if (lastSyncedAt != null && now - lastSyncedAt < minIntervalMs) return null
        return syncAll()
    }

    private suspend fun ensureAdminToken(): SupabaseAuthToken {
        val now = System.currentTimeMillis()
        val cached = cachedToken
        if (cached != null && cached.expiresAtMs > now) return cached

        val fresh = supabaseClient.signInWithPassword(
            email = SupabaseConfig.adminEmail,
            password = SupabaseConfig.adminPassword
        )
        cachedToken = fresh
        return fresh
    }

    private fun collectPlayersToPush(local: List<Player>, remoteRows: JSONArray): List<JSONObject> {
        val remoteById = remoteRows.toRemoteUpdatedMap()
        return local
            .filter { it.id > 0L }
            .filter { player -> player.updatedAt > (remoteById[player.id] ?: Long.MIN_VALUE) }
            .map { it.toRemoteJson() }
    }

    private fun collectPlayersToPull(local: List<Player>, remoteRows: JSONArray): List<Player> {
        val localById = local.associateBy { it.id }
        return remoteRows
            .toList()
            .mapNotNull { it.toPlayerOrNull() }
            .filter { remote -> remote.id > 0L }
            .filter { remote ->
                val localRow = localById[remote.id]
                localRow == null || remote.updatedAt > localRow.updatedAt
            }
    }

    private fun collectTournamentsToPush(local: List<Tournament>, remoteRows: JSONArray): List<JSONObject> {
        val remoteById = remoteRows.toRemoteUpdatedMap()
        return local
            .filter { it.id > 0L }
            .filter { tournament -> tournament.updatedAt > (remoteById[tournament.id] ?: Long.MIN_VALUE) }
            .map { it.toRemoteJson() }
    }

    private fun collectTournamentsToPull(local: List<Tournament>, remoteRows: JSONArray): List<Tournament> {
        val localById = local.associateBy { it.id }
        return remoteRows
            .toList()
            .mapNotNull { it.toTournamentOrNull() }
            .filter { remote ->
                val localRow = localById[remote.id]
                localRow == null || remote.updatedAt > localRow.updatedAt
            }
    }

    private fun collectTournamentMatchesToPush(local: List<TournamentMatch>, remoteRows: JSONArray): List<JSONObject> {
        val remoteById = remoteRows.toRemoteUpdatedMap()
        return local
            .filter { it.id > 0L }
            .filter { match -> match.updatedAt > (remoteById[match.id] ?: Long.MIN_VALUE) }
            .map { it.toRemoteJson() }
    }

    private fun collectTournamentMatchesToPull(local: List<TournamentMatch>, remoteRows: JSONArray): List<TournamentMatch> {
        val localById = local.associateBy { it.id }
        return remoteRows
            .toList()
            .mapNotNull { it.toTournamentMatchOrNull() }
            .filter { remote ->
                val localRow = localById[remote.id]
                localRow == null || remote.updatedAt > localRow.updatedAt
            }
    }

    private fun collectMatchesToPush(local: List<Match>, remoteRows: JSONArray): List<JSONObject> {
        val remoteById = remoteRows.toRemoteUpdatedMap()
        return local
            .filter { it.id > 0L }
            .filter { match -> match.updatedAt > (remoteById[match.id] ?: Long.MIN_VALUE) }
            .map { it.toRemoteJson() }
    }

    private fun collectMatchesToPull(local: List<Match>, remoteRows: JSONArray): List<Match> {
        val localById = local.associateBy { it.id }
        return remoteRows
            .toList()
            .mapNotNull { it.toMatchOrNull() }
            .filter { remote ->
                val localRow = localById[remote.id]
                localRow == null || remote.updatedAt > localRow.updatedAt
            }
    }

    private fun shouldPushLiveSnapshot(local: LiveMatchSnapshot?, remote: LiveMatchSnapshot?): JSONObject? {
        if (local == null) return null
        val remoteUpdated = remote?.updatedAt ?: Long.MIN_VALUE
        return if (local.updatedAt > remoteUpdated) local.toRemoteJson() else null
    }

    private fun shouldPullLiveSnapshot(local: LiveMatchSnapshot?, remote: LiveMatchSnapshot?): Boolean {
        if (remote == null) return false
        val localUpdated = local?.updatedAt ?: Long.MIN_VALUE
        return remote.updatedAt > localUpdated
    }
}

private fun JSONArray.toList(): List<JSONObject> {
    val out = mutableListOf<JSONObject>()
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        out.add(obj)
    }
    return out
}

private fun JSONArray.toRemoteUpdatedMap(): Map<Long, Long> {
    val map = mutableMapOf<Long, Long>()
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        val id = obj.optLong("id", 0L)
        if (id <= 0L) continue
        val updatedAt = obj.optLong("source_updated_at_ms", obj.optLong("updatedAt", 0L))
        map[id] = updatedAt
    }
    return map
}

private fun JSONObject.toPlayerOrNull(): Player? {
    val id = optLong("id", 0L)
    if (id <= 0L) return null
    return Player(
        id = id,
        name = optString("name", "").ifBlank { return null },
        imageUri = optStringOrNull("image_uri"),
        createdAt = optLong("source_created_at_ms", System.currentTimeMillis()),
        updatedAt = optLong("source_updated_at_ms", System.currentTimeMillis()),
        archived = optBoolean("archived", false),
        totalMatches = optInt("total_matches", 0),
        wins = optInt("wins", 0),
        losses = optInt("losses", 0),
        draws = optInt("draws", 0),
        tournamentsPlayed = optInt("tournaments_played", 0),
        tournamentsWon = optInt("tournaments_won", 0)
    )
}

private fun JSONObject.toTournamentOrNull(): Tournament? {
    val id = optLong("id", 0L)
    if (id <= 0L) return null
    return Tournament(
        id = id,
        name = optString("name", "").ifBlank { return null },
        status = optString("status", "created"),
        createdAt = optLong("source_created_at_ms", System.currentTimeMillis()),
        updatedAt = optLong("source_updated_at_ms", System.currentTimeMillis()),
        championPlayerId = optLongOrNull("champion_player_id"),
        totalRounds = optInt("total_rounds", 0),
        playerCount = optInt("player_count", 0)
    )
}

private fun JSONObject.toTournamentMatchOrNull(): TournamentMatch? {
    val id = optLong("id", 0L)
    if (id <= 0L) return null
    return TournamentMatch(
        id = id,
        tournamentId = optLong("tournament_id", 0L),
        roundNumber = optInt("round_number", 0),
        bracketPosition = optInt("bracket_position", 0),
        player1Id = optLongOrNull("player1_id"),
        player2Id = optLongOrNull("player2_id"),
        winnerPlayerId = optLongOrNull("winner_player_id"),
        linkedMatchId = optLongOrNull("linked_match_id"),
        state = optString("state", "pending"),
        createdAt = optLong("source_created_at_ms", System.currentTimeMillis()),
        updatedAt = optLong("source_updated_at_ms", System.currentTimeMillis())
    )
}

private fun JSONObject.toMatchOrNull(): Match? {
    val id = optLong("id", 0L)
    if (id <= 0L) return null
    return Match(
        id = id,
        player1Id = optLong("player1_id", 0L),
        player2Id = optLong("player2_id", 0L),
        player1Score = optInt("player1_score", 0),
        player2Score = optInt("player2_score", 0),
        startedAt = optLong("started_at_ms", System.currentTimeMillis()),
        endedAt = optLongOrNull("ended_at_ms"),
        durationSeconds = optLong("duration_seconds", 0L),
        winnerPlayerId = optLongOrNull("winner_player_id"),
        isDraw = optBoolean("is_draw", false),
        player1HighestBreak = optInt("player1_highest_break", 0),
        player2HighestBreak = optInt("player2_highest_break", 0),
        matchHighestBreak = optInt("match_highest_break", 0),
        breakHistorySummary = optStringOrNull("break_history_summary"),
        matchType = optString("match_type", "quick_match"),
        tournamentId = optLongOrNull("tournament_id"),
        tournamentRound = optIntOrNull("tournament_round"),
        updatedAt = optLong("source_updated_at_ms", System.currentTimeMillis())
    )
}

private fun JSONArray.toRemoteLiveSnapshot(): LiveMatchSnapshot? {
    if (length() == 0) return null
    return optJSONObject(0)?.toLiveMatchSnapshotOrNull()
}

private fun JSONObject.toLiveMatchSnapshotOrNull(): LiveMatchSnapshot? {
    val idText = optString("id", "active")
    if (idText != "active") return null
    return LiveMatchSnapshot(
        id = 1,
        isActive = optBoolean("is_active", false),
        player1Id = optLongOrNull("player1_id"),
        player2Id = optLongOrNull("player2_id"),
        player1Name = optStringOrNull("player1_name"),
        player2Name = optStringOrNull("player2_name"),
        player1Score = optInt("player1_score", 0),
        player2Score = optInt("player2_score", 0),
        activePlayerNumber = optIntOrNull("active_player_number"),
        currentBreakPlayer1 = optInt("current_break_player1", 0),
        currentBreakPlayer2 = optInt("current_break_player2", 0),
        highestBreakPlayer1 = optInt("highest_break_player1", 0),
        highestBreakPlayer2 = optInt("highest_break_player2", 0),
        highestBreakInMatch = optInt("highest_break_in_match", 0),
        redsRemaining = optInt("reds_remaining", 15),
        yellowVisible = optBoolean("yellow_visible", true),
        greenVisible = optBoolean("green_visible", true),
        brownVisible = optBoolean("brown_visible", true),
        blueVisible = optBoolean("blue_visible", true),
        pinkVisible = optBoolean("pink_visible", true),
        blackVisible = optBoolean("black_visible", true),
        tournamentId = optLongOrNull("tournament_id"),
        tournamentRound = optIntOrNull("tournament_round"),
        tournamentMatchId = optLongOrNull("tournament_match_id"),
        updatedAt = optLong("source_updated_at_ms", System.currentTimeMillis())
    )
}

private fun Player.toRemoteJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("image_uri", imageUri)
    .put("archived", archived)
    .put("total_matches", totalMatches)
    .put("wins", wins)
    .put("losses", losses)
    .put("draws", draws)
    .put("tournaments_played", tournamentsPlayed)
    .put("tournaments_won", tournamentsWon)
    .put("source_created_at_ms", createdAt)
    .put("source_updated_at_ms", updatedAt)
    .put("deleted_at", JSONObject.NULL)

private fun Tournament.toRemoteJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("status", status)
    .put("champion_player_id", championPlayerId)
    .put("total_rounds", totalRounds)
    .put("player_count", playerCount)
    .put("source_created_at_ms", createdAt)
    .put("source_updated_at_ms", updatedAt)
    .put("deleted_at", JSONObject.NULL)

private fun TournamentMatch.toRemoteJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("tournament_id", tournamentId)
    .put("round_number", roundNumber)
    .put("bracket_position", bracketPosition)
    .put("player1_id", player1Id)
    .put("player2_id", player2Id)
    .put("winner_player_id", winnerPlayerId)
    .put("linked_match_id", linkedMatchId)
    .put("state", state)
    .put("source_created_at_ms", createdAt)
    .put("source_updated_at_ms", updatedAt)
    .put("deleted_at", JSONObject.NULL)

private fun Match.toRemoteJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("player1_id", player1Id)
    .put("player2_id", player2Id)
    .put("player1_score", player1Score)
    .put("player2_score", player2Score)
    .put("started_at_ms", startedAt)
    .put("ended_at_ms", endedAt)
    .put("duration_seconds", durationSeconds)
    .put("winner_player_id", winnerPlayerId)
    .put("is_draw", isDraw)
    .put("player1_highest_break", player1HighestBreak)
    .put("player2_highest_break", player2HighestBreak)
    .put("match_highest_break", matchHighestBreak)
    .put("break_history_summary", breakHistorySummary)
    .put("match_type", matchType)
    .put("tournament_id", tournamentId)
    .put("tournament_round", tournamentRound)
    .put("source_created_at_ms", startedAt)
    .put("source_updated_at_ms", updatedAt)
    .put("deleted_at", JSONObject.NULL)

private fun LiveMatchSnapshot.toRemoteJson(): JSONObject = JSONObject()
    .put("id", "active")
    .put("is_active", isActive)
    .put("player1_id", player1Id)
    .put("player2_id", player2Id)
    .put("player1_name", player1Name)
    .put("player2_name", player2Name)
    .put("player1_score", player1Score)
    .put("player2_score", player2Score)
    .put("active_player_number", activePlayerNumber)
    .put("current_break_player1", currentBreakPlayer1)
    .put("current_break_player2", currentBreakPlayer2)
    .put("highest_break_player1", highestBreakPlayer1)
    .put("highest_break_player2", highestBreakPlayer2)
    .put("highest_break_in_match", highestBreakInMatch)
    .put("reds_remaining", redsRemaining)
    .put("yellow_visible", yellowVisible)
    .put("green_visible", greenVisible)
    .put("brown_visible", brownVisible)
    .put("blue_visible", blueVisible)
    .put("pink_visible", pinkVisible)
    .put("black_visible", blackVisible)
    .put("tournament_id", tournamentId)
    .put("tournament_round", tournamentRound)
    .put("tournament_match_id", tournamentMatchId)
    .put("source_updated_at_ms", updatedAt)
    .put("deleted_at", JSONObject.NULL)

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = optString(key, "").trim()
    return value.ifBlank { null }
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    val value = optLong(key, Long.MIN_VALUE)
    return if (value == Long.MIN_VALUE) null else value
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    val value = optInt(key, Int.MIN_VALUE)
    return if (value == Int.MIN_VALUE) null else value
}
