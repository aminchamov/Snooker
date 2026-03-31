package com.elocho.snooker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_match_snapshot")
data class LiveMatchSnapshot(
    @PrimaryKey
    val id: Int = 1,
    val isActive: Boolean = false,
    val player1Id: Long? = null,
    val player2Id: Long? = null,
    val player1Name: String? = null,
    val player2Name: String? = null,
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val activePlayerNumber: Int? = null,
    val currentBreakPlayer1: Int = 0,
    val currentBreakPlayer2: Int = 0,
    val highestBreakPlayer1: Int = 0,
    val highestBreakPlayer2: Int = 0,
    val highestBreakInMatch: Int = 0,
    val redsRemaining: Int = 15,
    val yellowVisible: Boolean = true,
    val greenVisible: Boolean = true,
    val brownVisible: Boolean = true,
    val blueVisible: Boolean = true,
    val pinkVisible: Boolean = true,
    val blackVisible: Boolean = true,
    val tournamentId: Long? = null,
    val tournamentRound: Int? = null,
    val tournamentMatchId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
