package com.elocho.snooker.data.local

import androidx.room.*
import com.elocho.snooker.data.model.Tournament
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: Tournament): Long

    @Update
    suspend fun updateTournament(tournament: Tournament)

    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC")
    fun getAllTournaments(): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: Long): Tournament?

    @Query("SELECT * FROM tournaments ORDER BY id ASC")
    suspend fun getAllTournamentsOnce(): List<Tournament>

    @Query("SELECT * FROM tournaments WHERE status = :status ORDER BY createdAt DESC")
    fun getTournamentsByStatus(status: String): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentTournaments(limit: Int = 10): Flow<List<Tournament>>

    @Query("SELECT COUNT(*) FROM tournaments")
    fun getTournamentCount(): Flow<Int>

    @Query("SELECT * FROM tournaments WHERE championPlayerId IS NOT NULL ORDER BY updatedAt DESC")
    fun getCompletedTournaments(): Flow<List<Tournament>>

    @Delete
    suspend fun deleteTournament(tournament: Tournament)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteTournamentById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournaments(tournaments: List<Tournament>)
}
