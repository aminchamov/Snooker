package com.elocho.snooker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournaments")
data class Tournament(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val status: String = "created", // "created", "in_progress", "completed"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val championPlayerId: Long? = null,
    val totalRounds: Int = 0,
    val playerCount: Int = 0
)
