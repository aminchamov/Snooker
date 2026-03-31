package com.elocho.snooker.data.repository

import com.elocho.snooker.data.local.TournamentDao
import com.elocho.snooker.data.local.TournamentMatchDao
import com.elocho.snooker.data.model.Tournament
import com.elocho.snooker.data.model.TournamentMatch
import kotlinx.coroutines.flow.Flow

class TournamentRepository(
    private val tournamentDao: TournamentDao,
    private val tournamentMatchDao: TournamentMatchDao
) {
    suspend fun insertTournament(tournament: Tournament): Long =
        tournamentDao.insertTournament(tournament)

    suspend fun updateTournament(tournament: Tournament) =
        tournamentDao.updateTournament(tournament)

    suspend fun getTournamentById(id: Long): Tournament? =
        tournamentDao.getTournamentById(id)

    fun getAllTournaments(): Flow<List<Tournament>> = tournamentDao.getAllTournaments()

    fun getRecentTournaments(limit: Int = 10): Flow<List<Tournament>> =
        tournamentDao.getRecentTournaments(limit)

    fun getTournamentCount(): Flow<Int> = tournamentDao.getTournamentCount()

    fun getCompletedTournaments(): Flow<List<Tournament>> =
        tournamentDao.getCompletedTournaments()

    suspend fun deleteTournament(id: Long) {
        tournamentMatchDao.deleteMatchesByTournament(id)
        tournamentDao.deleteTournamentById(id)
    }

    // Tournament Match operations
    suspend fun insertTournamentMatch(match: TournamentMatch): Long =
        tournamentMatchDao.insertTournamentMatch(match)

    suspend fun insertTournamentMatches(matches: List<TournamentMatch>) =
        tournamentMatchDao.insertTournamentMatches(matches)

    suspend fun updateTournamentMatch(match: TournamentMatch) =
        tournamentMatchDao.updateTournamentMatch(match)

    fun getTournamentMatches(tournamentId: Long): Flow<List<TournamentMatch>> =
        tournamentMatchDao.getMatchesByTournament(tournamentId)

    suspend fun getTournamentMatchesOnce(tournamentId: Long): List<TournamentMatch> =
        tournamentMatchDao.getMatchesByTournamentOnce(tournamentId)

    suspend fun getTournamentMatchById(id: Long): TournamentMatch? =
        tournamentMatchDao.getTournamentMatchById(id)

    suspend fun getMatchesByRound(tournamentId: Long, round: Int): List<TournamentMatch> =
        tournamentMatchDao.getMatchesByRound(tournamentId, round)
}
