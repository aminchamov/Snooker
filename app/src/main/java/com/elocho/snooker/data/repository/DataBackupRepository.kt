package com.elocho.snooker.data.repository

import androidx.room.withTransaction
import com.elocho.snooker.data.local.ElOchoDatabase
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.data.model.Tournament
import com.elocho.snooker.data.model.TournamentMatch
import org.json.JSONArray
import org.json.JSONObject

enum class BackupPayloadType(val raw: String) {
    PLAYERS("players"),
    TOURNAMENTS("tournaments"),
    ALL("all");

    companion object {
        fun fromRaw(raw: String?): BackupPayloadType = entries.firstOrNull { it.raw == raw } ?: ALL
    }
}

data class BackupExportData(
    val players: List<Player>,
    val tournaments: List<Tournament>,
    val tournamentMatches: List<TournamentMatch>
)

data class BackupImportData(
    val payloadType: BackupPayloadType,
    val schemaVersion: Int,
    val players: List<Player>,
    val tournaments: List<Tournament>,
    val tournamentMatches: List<TournamentMatch>
)

class DataBackupRepository(
    private val database: ElOchoDatabase
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }

    suspend fun exportData(type: BackupPayloadType): BackupExportData {
        val players = when (type) {
            BackupPayloadType.PLAYERS, BackupPayloadType.ALL -> database.playerDao().getAllPlayersOnce()
            BackupPayloadType.TOURNAMENTS -> emptyList()
        }
        val tournaments = when (type) {
            BackupPayloadType.TOURNAMENTS, BackupPayloadType.ALL -> database.tournamentDao().getAllTournamentsOnce()
            BackupPayloadType.PLAYERS -> emptyList()
        }
        val tournamentMatches = when (type) {
            BackupPayloadType.TOURNAMENTS, BackupPayloadType.ALL -> database.tournamentMatchDao().getAllTournamentMatchesOnce()
            BackupPayloadType.PLAYERS -> emptyList()
        }
        return BackupExportData(
            players = players,
            tournaments = tournaments,
            tournamentMatches = tournamentMatches
        )
    }

    fun encodeToJson(
        type: BackupPayloadType,
        exportData: BackupExportData,
        appVersion: String,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("payloadType", type.raw)
        root.put("appVersion", appVersion)
        root.put("exportedAt", exportedAt)
        root.put("players", JSONArray().apply { exportData.players.forEach { put(it.toJson()) } })
        root.put("tournaments", JSONArray().apply { exportData.tournaments.forEach { put(it.toJson()) } })
        root.put(
            "tournamentMatches",
            JSONArray().apply { exportData.tournamentMatches.forEach { put(it.toJson()) } }
        )
        return root.toString(2)
    }

    fun decodeFromJson(json: String): BackupImportData {
        val root = JSONObject(json)
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion <= 0) {
            throw IllegalArgumentException("Invalid backup schema version")
        }

        val payloadType = BackupPayloadType.fromRaw(root.optString("payloadType", BackupPayloadType.ALL.raw))

        val players = root.optJSONArray("players").toPlayers()
        val tournaments = root.optJSONArray("tournaments").toTournaments()
        val tournamentMatches = root.optJSONArray("tournamentMatches").toTournamentMatches()

        return BackupImportData(
            payloadType = payloadType,
            schemaVersion = schemaVersion,
            players = players,
            tournaments = tournaments,
            tournamentMatches = tournamentMatches
        )
    }

    suspend fun importPlayers(players: List<Player>): Int {
        if (players.isEmpty()) return 0
        database.withTransaction {
            database.playerDao().insertPlayers(players)
        }
        return players.size
    }

    suspend fun importTournaments(
        tournaments: List<Tournament>,
        tournamentMatches: List<TournamentMatch>
    ): Pair<Int, Int> {
        if (tournaments.isEmpty() && tournamentMatches.isEmpty()) return 0 to 0
        var importedMatches = 0
        database.withTransaction {
            if (tournaments.isNotEmpty()) {
                database.tournamentDao().insertTournaments(tournaments)
            }
            if (tournamentMatches.isNotEmpty()) {
                val validTournamentIds = database.tournamentDao().getAllTournamentsOnce().map { it.id }.toSet()
                val validMatches = tournamentMatches.filter { it.tournamentId in validTournamentIds }
                if (validMatches.isNotEmpty()) {
                    database.tournamentMatchDao().insertTournamentMatches(validMatches)
                }
                importedMatches = validMatches.size
            }
        }
        return tournaments.size to importedMatches
    }

    private fun JSONArray?.toPlayers(): List<Player> {
        if (this == null) return emptyList()
        val out = mutableListOf<Player>()
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isBlank()) continue
            out.add(
                Player(
                    id = obj.optLong("id", 0L),
                    name = name,
                    imageUri = obj.optStringOrNull("imageUri"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    archived = obj.optBoolean("archived", false),
                    totalMatches = obj.optInt("totalMatches", 0),
                    wins = obj.optInt("wins", 0),
                    losses = obj.optInt("losses", 0),
                    draws = obj.optInt("draws", 0),
                    tournamentsPlayed = obj.optInt("tournamentsPlayed", 0),
                    tournamentsWon = obj.optInt("tournamentsWon", 0)
                )
            )
        }
        return out
    }

    private fun JSONArray?.toTournaments(): List<Tournament> {
        if (this == null) return emptyList()
        val out = mutableListOf<Tournament>()
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isBlank()) continue
            out.add(
                Tournament(
                    id = obj.optLong("id", 0L),
                    name = name,
                    status = obj.optString("status", "created"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    championPlayerId = obj.optNullableLong("championPlayerId"),
                    totalRounds = obj.optInt("totalRounds", 0),
                    playerCount = obj.optInt("playerCount", 0)
                )
            )
        }
        return out
    }

    private fun JSONArray?.toTournamentMatches(): List<TournamentMatch> {
        if (this == null) return emptyList()
        val out = mutableListOf<TournamentMatch>()
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            val tournamentId = obj.optLong("tournamentId", 0L)
            if (tournamentId <= 0L) continue
            out.add(
                TournamentMatch(
                    id = obj.optLong("id", 0L),
                    tournamentId = tournamentId,
                    roundNumber = obj.optInt("roundNumber", 0),
                    bracketPosition = obj.optInt("bracketPosition", 0),
                    player1Id = obj.optNullableLong("player1Id"),
                    player2Id = obj.optNullableLong("player2Id"),
                    winnerPlayerId = obj.optNullableLong("winnerPlayerId"),
                    linkedMatchId = obj.optNullableLong("linkedMatchId"),
                    state = obj.optString("state", "pending"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
        return out
    }

    private fun Player.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("imageUri", imageUri)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("archived", archived)
        put("totalMatches", totalMatches)
        put("wins", wins)
        put("losses", losses)
        put("draws", draws)
        put("tournamentsPlayed", tournamentsPlayed)
        put("tournamentsWon", tournamentsWon)
    }

    private fun Tournament.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("status", status)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("championPlayerId", championPlayerId)
        put("totalRounds", totalRounds)
        put("playerCount", playerCount)
    }

    private fun TournamentMatch.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("tournamentId", tournamentId)
        put("roundNumber", roundNumber)
        put("bracketPosition", bracketPosition)
        put("player1Id", player1Id)
        put("player2Id", player2Id)
        put("winnerPlayerId", winnerPlayerId)
        put("linkedMatchId", linkedMatchId)
        put("state", state)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "")
        return if (value.isEmpty()) null else value
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key)
    }
}
