package com.elocho.snooker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elocho.snooker.data.model.Match
import com.elocho.snooker.data.model.LiveMatchSnapshot
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.data.model.Tournament
import com.elocho.snooker.data.model.TournamentMatch

@Database(
    entities = [Player::class, Match::class, Tournament::class, TournamentMatch::class, LiveMatchSnapshot::class],
    version = 3,
    exportSchema = true
)
abstract class ElOchoDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun tournamentMatchDao(): TournamentMatchDao
    abstract fun liveMatchSnapshotDao(): LiveMatchSnapshotDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN player1HighestBreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE matches ADD COLUMN player2HighestBreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE matches ADD COLUMN matchHighestBreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE matches ADD COLUMN breakHistorySummary TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `live_match_snapshot` (
                        `id` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `player1Id` INTEGER,
                        `player2Id` INTEGER,
                        `player1Name` TEXT,
                        `player2Name` TEXT,
                        `player1Score` INTEGER NOT NULL,
                        `player2Score` INTEGER NOT NULL,
                        `activePlayerNumber` INTEGER,
                        `currentBreakPlayer1` INTEGER NOT NULL,
                        `currentBreakPlayer2` INTEGER NOT NULL,
                        `highestBreakPlayer1` INTEGER NOT NULL,
                        `highestBreakPlayer2` INTEGER NOT NULL,
                        `highestBreakInMatch` INTEGER NOT NULL,
                        `redsRemaining` INTEGER NOT NULL,
                        `yellowVisible` INTEGER NOT NULL,
                        `greenVisible` INTEGER NOT NULL,
                        `brownVisible` INTEGER NOT NULL,
                        `blueVisible` INTEGER NOT NULL,
                        `pinkVisible` INTEGER NOT NULL,
                        `blackVisible` INTEGER NOT NULL,
                        `tournamentId` INTEGER,
                        `tournamentRound` INTEGER,
                        `tournamentMatchId` INTEGER,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE matches ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE matches SET updatedAt = COALESCE(endedAt, startedAt)")
                db.execSQL("ALTER TABLE tournament_matches ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tournament_matches ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE tournament_matches SET createdAt = strftime('%s','now') * 1000 WHERE createdAt = 0")
                db.execSQL("UPDATE tournament_matches SET updatedAt = createdAt WHERE updatedAt = 0")
            }
        }

        @Volatile
        private var INSTANCE: ElOchoDatabase? = null

        fun getInstance(context: Context): ElOchoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ElOchoDatabase::class.java,
                    "elocho_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
