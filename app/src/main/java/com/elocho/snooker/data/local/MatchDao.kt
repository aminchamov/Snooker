package com.elocho.snooker.data.local

import androidx.room.*
import com.elocho.snooker.data.model.Match
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match): Long

    @Update
    suspend fun updateMatch(match: Match)

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Long): Match?

    @Query("SELECT * FROM matches ORDER BY id ASC")
    suspend fun getAllMatchesOnce(): List<Match>

    @Query("SELECT * FROM matches ORDER BY startedAt DESC")
    fun getAllMatches(): Flow<List<Match>>

    @Query("SELECT * FROM matches WHERE matchType = 'quick_match' ORDER BY startedAt DESC")
    fun getQuickMatches(): Flow<List<Match>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY tournamentRound ASC")
    fun getMatchesByTournament(tournamentId: Long): Flow<List<Match>>

    @Query("SELECT * FROM matches WHERE player1Id = :playerId OR player2Id = :playerId ORDER BY startedAt DESC")
    fun getMatchesByPlayer(playerId: Long): Flow<List<Match>>

    @Query("SELECT * FROM matches ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentMatches(limit: Int = 10): Flow<List<Match>>

    @Query("SELECT COUNT(*) FROM matches")
    fun getMatchCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM matches WHERE isDraw = 1")
    fun getDrawCount(): Flow<Int>

    @Delete
    suspend fun deleteMatch(match: Match)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<Match>)
}
