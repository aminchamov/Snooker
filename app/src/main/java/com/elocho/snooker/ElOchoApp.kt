package com.elocho.snooker

import android.app.Application
import com.elocho.snooker.data.local.ElOchoDatabase

/**
 * Application class - initializes the Room database singleton.
 */
class ElOchoApp : Application() {

    val database: ElOchoDatabase by lazy {
        ElOchoDatabase.getInstance(this)
    }

    val settingsDataStore: com.elocho.snooker.data.local.SettingsDataStore by lazy {
        com.elocho.snooker.data.local.SettingsDataStore(this)
    }

    val settingsRepository: com.elocho.snooker.data.repository.SettingsRepository by lazy {
        com.elocho.snooker.data.repository.SettingsRepository(settingsDataStore)
    }

    val playerRepository: com.elocho.snooker.data.repository.PlayerRepository by lazy {
        com.elocho.snooker.data.repository.PlayerRepository(database.playerDao())
    }

    val matchRepository: com.elocho.snooker.data.repository.MatchRepository by lazy {
        com.elocho.snooker.data.repository.MatchRepository(database.matchDao())
    }

    val liveMatchSnapshotRepository: com.elocho.snooker.data.repository.LiveMatchSnapshotRepository by lazy {
        com.elocho.snooker.data.repository.LiveMatchSnapshotRepository(database.liveMatchSnapshotDao())
    }

    val tournamentRepository: com.elocho.snooker.data.repository.TournamentRepository by lazy {
        com.elocho.snooker.data.repository.TournamentRepository(database.tournamentDao(), database.tournamentMatchDao())
    }

    val dataBackupRepository: com.elocho.snooker.data.repository.DataBackupRepository by lazy {
        com.elocho.snooker.data.repository.DataBackupRepository(database)
    }

    val supabaseRestClient: com.elocho.snooker.data.sync.SupabaseRestClient by lazy {
        com.elocho.snooker.data.sync.SupabaseRestClient()
    }

    val supabaseSyncRepository: com.elocho.snooker.data.sync.SupabaseSyncRepository by lazy {
        com.elocho.snooker.data.sync.SupabaseSyncRepository(
            database = database,
            settingsRepository = settingsRepository,
            supabaseClient = supabaseRestClient
        )
    }

    companion object {
        lateinit var instance: ElOchoApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
