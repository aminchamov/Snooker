package com.elocho.snooker.data.local

import androidx.room.*
import com.elocho.snooker.data.model.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players WHERE archived = 0 ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE archived = 0 AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPlayers(query: String): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): Player?

    @Query("SELECT * FROM players WHERE id IN (:ids)")
    suspend fun getPlayersByIds(ids: List<Long>): List<Player>

    @Query("SELECT * FROM players ORDER BY id ASC")
    suspend fun getAllPlayersOnce(): List<Player>

    @Query("SELECT * FROM players WHERE archived = 0 AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getActivePlayerByExactName(name: String): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<Player>)

    @Update
    suspend fun updatePlayer(player: Player)

    @Query("UPDATE players SET archived = 1, updatedAt = :time WHERE id = :playerId")
    suspend fun archivePlayer(playerId: Long, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun deletePlayer(player: Player)

    @Query("SELECT COUNT(*) FROM players WHERE archived = 0")
    fun getPlayerCount(): Flow<Int>

    @Query("SELECT * FROM players WHERE archived = 0 ORDER BY wins DESC LIMIT :limit")
    fun getTopPlayersByWins(limit: Int = 10): Flow<List<Player>>

    @Query("UPDATE players SET totalMatches = totalMatches + 1, wins = wins + :winInc, losses = losses + :lossInc, draws = draws + :drawInc, updatedAt = :time WHERE id = :playerId")
    suspend fun updatePlayerStats(
        playerId: Long,
        winInc: Int = 0,
        lossInc: Int = 0,
        drawInc: Int = 0,
        time: Long = System.currentTimeMillis()
    )

    @Query("UPDATE players SET tournamentsPlayed = tournamentsPlayed + 1, updatedAt = :time WHERE id = :playerId")
    suspend fun incrementTournamentsPlayed(playerId: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE players SET tournamentsWon = tournamentsWon + 1, updatedAt = :time WHERE id = :playerId")
    suspend fun incrementTournamentsWon(playerId: Long, time: Long = System.currentTimeMillis())
}
