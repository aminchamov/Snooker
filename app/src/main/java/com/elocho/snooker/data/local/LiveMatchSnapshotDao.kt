package com.elocho.snooker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elocho.snooker.data.model.LiveMatchSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveMatchSnapshotDao {

    @Query("SELECT * FROM live_match_snapshot WHERE id = 1 LIMIT 1")
    fun observeSnapshot(): Flow<LiveMatchSnapshot?>

    @Query("SELECT * FROM live_match_snapshot WHERE id = 1 LIMIT 1")
    suspend fun getSnapshotOnce(): LiveMatchSnapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: LiveMatchSnapshot)

    @Query("DELETE FROM live_match_snapshot WHERE id = 1")
    suspend fun clearSnapshot()
}
