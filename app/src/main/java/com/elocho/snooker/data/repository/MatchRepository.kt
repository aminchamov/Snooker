package com.elocho.snooker.data.repository

import com.elocho.snooker.data.local.MatchDao
import com.elocho.snooker.data.model.Match
import kotlinx.coroutines.flow.Flow

class MatchRepository(private val matchDao: MatchDao) {

    suspend fun insertMatch(match: Match): Long = matchDao.insertMatch(match)

    suspend fun updateMatch(match: Match) = matchDao.updateMatch(match)

    suspend fun getMatchById(id: Long): Match? = matchDao.getMatchById(id)

    suspend fun getAllMatchesOnce(): List<Match> = matchDao.getAllMatchesOnce()

    fun getAllMatches(): Flow<List<Match>> = matchDao.getAllMatches()

    fun getQuickMatches(): Flow<List<Match>> = matchDao.getQuickMatches()

    fun getMatchesByTournament(tournamentId: Long): Flow<List<Match>> =
        matchDao.getMatchesByTournament(tournamentId)

    fun getMatchesByPlayer(playerId: Long): Flow<List<Match>> =
        matchDao.getMatchesByPlayer(playerId)

    fun getRecentMatches(limit: Int = 10): Flow<List<Match>> = matchDao.getRecentMatches(limit)

    fun getMatchCount(): Flow<Int> = matchDao.getMatchCount()

    fun getDrawCount(): Flow<Int> = matchDao.getDrawCount()

    suspend fun insertMatches(matches: List<Match>) = matchDao.insertMatches(matches)
}
