package com.elocho.snooker.data.repository

import com.elocho.snooker.data.model.RemoteMappingAction
import com.elocho.snooker.data.local.SettingsDataStore
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dataStore: SettingsDataStore) {

    val isLoggedIn: Flow<Boolean> = dataStore.isLoggedIn
    val primaryColor: Flow<String> = dataStore.primaryColor
    val accentColor: Flow<String> = dataStore.accentColor
    val logoUri: Flow<String?> = dataStore.logoUri
    val backgroundUri: Flow<String?> = dataStore.backgroundUri
    val backgroundStyle: Flow<String> = dataStore.backgroundStyle
    val defaultAvatarUri: Flow<String?> = dataStore.defaultAvatarUri
    val themeVariant: Flow<String> = dataStore.themeVariant
    val remoteUndoKeyCode: Flow<Int?> = dataStore.remoteUndoKeyCode
    val remoteRedKeyCode: Flow<Int?> = dataStore.remoteRedKeyCode
    val remoteYellowKeyCode: Flow<Int?> = dataStore.remoteYellowKeyCode
    val remoteGreenKeyCode: Flow<Int?> = dataStore.remoteGreenKeyCode
    val remoteBrownKeyCode: Flow<Int?> = dataStore.remoteBrownKeyCode
    val remoteBlueKeyCode: Flow<Int?> = dataStore.remoteBlueKeyCode
    val remotePinkKeyCode: Flow<Int?> = dataStore.remotePinkKeyCode
    val remoteBlackKeyCode: Flow<Int?> = dataStore.remoteBlackKeyCode
    val remoteErrorKeyCode: Flow<Int?> = dataStore.remoteErrorKeyCode
    val lastSyncAt: Flow<Long?> = dataStore.lastSyncAt
    val lastSyncStatus: Flow<String?> = dataStore.lastSyncStatus
    val lastSyncError: Flow<String?> = dataStore.lastSyncError

    suspend fun setLoggedIn(loggedIn: Boolean) = dataStore.setLoggedIn(loggedIn)
    suspend fun setPrimaryColor(color: String) = dataStore.setPrimaryColor(color)
    suspend fun setAccentColor(color: String) = dataStore.setAccentColor(color)
    suspend fun setLogoUri(uri: String?) = dataStore.setLogoUri(uri)
    suspend fun setBackgroundUri(uri: String?) = dataStore.setBackgroundUri(uri)
    suspend fun setBackgroundStyle(style: String) = dataStore.setBackgroundStyle(style)
    suspend fun setDefaultAvatarUri(uri: String?) = dataStore.setDefaultAvatarUri(uri)
    suspend fun setThemeVariant(variant: String) = dataStore.setThemeVariant(variant)
    suspend fun setRemoteKeyCode(action: RemoteMappingAction, keyCode: Int?) =
        dataStore.setRemoteKeyCode(action, keyCode)
    suspend fun setLastSyncState(lastSyncAt: Long?, status: String, errorMessage: String?) =
        dataStore.setLastSyncState(lastSyncAt, status, errorMessage)
    suspend fun resetRemoteMappings() = dataStore.resetRemoteMappings()
    suspend fun resetToDefaults() = dataStore.resetToDefaults()
}
