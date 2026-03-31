package com.elocho.snooker.data.repository

import com.elocho.snooker.data.local.LiveMatchSnapshotDao
import com.elocho.snooker.data.model.LiveMatchSnapshot
import kotlinx.coroutines.flow.Flow

class LiveMatchSnapshotRepository(
    private val dao: LiveMatchSnapshotDao
) {
    fun observeSnapshot(): Flow<LiveMatchSnapshot?> = dao.observeSnapshot()

    suspend fun getSnapshotOnce(): LiveMatchSnapshot? = dao.getSnapshotOnce()

    suspend fun upsertSnapshot(snapshot: LiveMatchSnapshot) = dao.upsertSnapshot(snapshot)

    suspend fun clearSnapshot() = dao.clearSnapshot()
}
