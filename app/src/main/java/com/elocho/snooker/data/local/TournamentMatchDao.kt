package com.elocho.snooker.data.local

import androidx.room.*
import com.elocho.snooker.data.model.TournamentMatch
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentMatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournamentMatch(match: TournamentMatch): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournamentMatches(matches: List<TournamentMatch>)

    @Update
    suspend fun updateTournamentMatch(match: TournamentMatch)

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId ORDER BY roundNumber ASC, bracketPosition ASC")
    fun getMatchesByTournament(tournamentId: Long): Flow<List<TournamentMatch>>

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId ORDER BY roundNumber ASC, bracketPosition ASC")
    suspend fun getMatchesByTournamentOnce(tournamentId: Long): List<TournamentMatch>

    @Query("SELECT * FROM tournament_matches ORDER BY tournamentId ASC, roundNumber ASC, bracketPosition ASC")
    suspend fun getAllTournamentMatchesOnce(): List<TournamentMatch>

    @Query("SELECT * FROM tournament_matches WHERE id = :id")
    suspend fun getTournamentMatchById(id: Long): TournamentMatch?

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId AND roundNumber = :round ORDER BY bracketPosition ASC")
    suspend fun getMatchesByRound(tournamentId: Long, round: Int): List<TournamentMatch>

    @Query("DELETE FROM tournament_matches WHERE tournamentId = :tournamentId")
    suspend fun deleteMatchesByTournament(tournamentId: Long)

    @Delete
    suspend fun deleteTournamentMatch(match: TournamentMatch)
}
