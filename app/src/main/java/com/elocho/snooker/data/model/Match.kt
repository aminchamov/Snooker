package com.elocho.snooker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class Match(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val player1Id: Long,
    val player2Id: Long,
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val durationSeconds: Long = 0,
    val winnerPlayerId: Long? = null,
    val isDraw: Boolean = false,
    val player1HighestBreak: Int = 0,
    val player2HighestBreak: Int = 0,
    val matchHighestBreak: Int = 0,
    val breakHistorySummary: String? = null,
    val matchType: String = "quick_match", // "quick_match" or "tournament"
    val tournamentId: Long? = null,
    val tournamentRound: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
