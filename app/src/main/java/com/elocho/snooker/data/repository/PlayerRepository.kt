package com.elocho.snooker.data.repository

import com.elocho.snooker.data.local.PlayerDao
import com.elocho.snooker.data.model.Player
import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {

    fun getAllPlayers(): Flow<List<Player>> = playerDao.getAllPlayers()

    fun searchPlayers(query: String): Flow<List<Player>> = playerDao.searchPlayers(query)

    suspend fun getPlayerById(id: Long): Player? = playerDao.getPlayerById(id)

    suspend fun getPlayersByIds(ids: List<Long>): List<Player> = playerDao.getPlayersByIds(ids)

    suspend fun getActivePlayerByExactName(name: String): Player? =
        playerDao.getActivePlayerByExactName(name)

    suspend fun insertPlayer(player: Player): Long = playerDao.insertPlayer(player)

    suspend fun updatePlayer(player: Player) = playerDao.updatePlayer(player)

    suspend fun archivePlayer(playerId: Long) = playerDao.archivePlayer(playerId)

    fun getPlayerCount(): Flow<Int> = playerDao.getPlayerCount()

    fun getTopPlayersByWins(limit: Int = 10): Flow<List<Player>> = playerDao.getTopPlayersByWins(limit)

    suspend fun updatePlayerMatchStats(playerId: Long, won: Boolean, draw: Boolean) {
        when {
            draw -> playerDao.updatePlayerStats(playerId, drawInc = 1)
            won -> playerDao.updatePlayerStats(playerId, winInc = 1)
            else -> playerDao.updatePlayerStats(playerId, lossInc = 1)
        }
    }

    suspend fun incrementTournamentsPlayed(playerId: Long) = playerDao.incrementTournamentsPlayed(playerId)

    suspend fun incrementTournamentsWon(playerId: Long) = playerDao.incrementTournamentsWon(playerId)
}
