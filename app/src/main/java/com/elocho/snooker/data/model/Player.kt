package com.elocho.snooker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val tournamentsPlayed: Int = 0,
    val tournamentsWon: Int = 0
)
