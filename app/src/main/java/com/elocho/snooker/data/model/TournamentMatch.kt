package com.elocho.snooker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournament_matches")
data class TournamentMatch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tournamentId: Long,
    val roundNumber: Int,
    val bracketPosition: Int,
    val player1Id: Long? = null,
    val player2Id: Long? = null,
    val winnerPlayerId: Long? = null,
    val linkedMatchId: Long? = null,  // links to the Match entity for scoreboard
    val state: String = "pending",     // "pending", "ready", "in_progress", "completed", "bye"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
