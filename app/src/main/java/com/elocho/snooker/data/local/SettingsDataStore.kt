package com.elocho.snooker.data.local

import android.content.Context
import com.elocho.snooker.data.model.RemoteMappingAction
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "elocho_settings")

/**
 * DataStore wrapper for app settings/preferences.
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val LOGO_URI = stringPreferencesKey("logo_uri")
        val PRIMARY_COLOR = stringPreferencesKey("primary_color")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val BACKGROUND_URI = stringPreferencesKey("background_uri")
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        val DEFAULT_AVATAR_URI = stringPreferencesKey("default_avatar_uri")
        val THEME_VARIANT = stringPreferencesKey("theme_variant")
        val REMOTE_UNDO_KEY_CODE = intPreferencesKey("remote_undo_key_code")
        val REMOTE_RED_KEY_CODE = intPreferencesKey("remote_red_key_code")
        val REMOTE_YELLOW_KEY_CODE = intPreferencesKey("remote_yellow_key_code")
        val REMOTE_GREEN_KEY_CODE = intPreferencesKey("remote_green_key_code")
        val REMOTE_BROWN_KEY_CODE = intPreferencesKey("remote_brown_key_code")
        val REMOTE_BLUE_KEY_CODE = intPreferencesKey("remote_blue_key_code")
        val REMOTE_PINK_KEY_CODE = intPreferencesKey("remote_pink_key_code")
        val REMOTE_BLACK_KEY_CODE = intPreferencesKey("remote_black_key_code")
        val REMOTE_ERROR_KEY_CODE = intPreferencesKey("remote_error_key_code")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_SYNC_STATUS = stringPreferencesKey("last_sync_status")
        val LAST_SYNC_ERROR = stringPreferencesKey("last_sync_error")

        const val DEFAULT_PRIMARY_COLOR = "#791b2f"
        const val DEFAULT_ACCENT_COLOR = "#c4a35a"
        const val DEFAULT_BACKGROUND_STYLE = "diagonal_stripes"
        const val DEFAULT_THEME_VARIANT = "dark"
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN] ?: false
    }

    val primaryColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PRIMARY_COLOR] ?: DEFAULT_PRIMARY_COLOR
    }

    val accentColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR] ?: DEFAULT_ACCENT_COLOR
    }

    val logoUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[LOGO_URI]
    }

    val backgroundUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[BACKGROUND_URI]
    }

    val backgroundStyle: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[BACKGROUND_STYLE] ?: DEFAULT_BACKGROUND_STYLE
    }

    val defaultAvatarUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_AVATAR_URI]
    }

    val themeVariant: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_VARIANT] ?: DEFAULT_THEME_VARIANT
    }

    val remoteUndoKeyCode: Flow<Int?> = context.dataStore.data.map { prefs -> prefs[REMOTE_UNDO_KEY_CODE] }
    val remoteRedKeyCode: Flow<Int?> = context.dataStore.data.map { prefs -> prefs[REMOTE_RED_KEY_CODE] }
    val remoteYellowKeyCode: Flow<Int?> = context.dataStore.data.map { prefs -> prefs[REMOTE_YELLOW_KEY_CODE] }
    val remoteGreenKeyCode: Flow<Int?> = context.dataStore.data.map { prefs -> prefs[REMOTE_GREEN_KEY_CODE] }
    val remoteBrownKeyCode: Flow<Int?> = context.dataStore.data.map { prefs -> prefs[REMOTE_BROWN_KEY_CODE] }
    val remoteBlueKeyCode: Flow<Int?> = context.dataStore.data.map { prefs -> prefs[REMOTE_BLUE_KEY_CODE] }
    val remotePinkKeyCode: Flow<Int?> = context.dataStore.data.map { prefs -> prefs[REMOTE_PINK_KEY_CODE] }
    val remoteBlackKeyCode: Flow<Int?> = context.dataStore.data.map { prefs -> prefs[REMOTE_BLACK_KEY_CODE] }
    val remoteErrorKeyCode: Flow<Int?> = context.dataStore.data.map { prefs -> prefs[REMOTE_ERROR_KEY_CODE] }
    val lastSyncAt: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[LAST_SYNC_AT] }
    val lastSyncStatus: Flow<String?> = context.dataStore.data.map { prefs -> prefs[LAST_SYNC_STATUS] }
    val lastSyncError: Flow<String?> = context.dataStore.data.map { prefs -> prefs[LAST_SYNC_ERROR] }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = loggedIn
        }
    }

    suspend fun setPrimaryColor(color: String) {
        context.dataStore.edit { prefs ->
            prefs[PRIMARY_COLOR] = color
        }
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCENT_COLOR] = color
        }
    }

    suspend fun setLogoUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[LOGO_URI] = uri
            else prefs.remove(LOGO_URI)
        }
    }

    suspend fun setBackgroundUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[BACKGROUND_URI] = uri
            else prefs.remove(BACKGROUND_URI)
        }
    }

    suspend fun setBackgroundStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[BACKGROUND_STYLE] = style
        }
    }

    suspend fun setDefaultAvatarUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[DEFAULT_AVATAR_URI] = uri
            else prefs.remove(DEFAULT_AVATAR_URI)
        }
    }

    suspend fun setThemeVariant(variant: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_VARIANT] = variant
        }
    }

    suspend fun setRemoteKeyCode(action: RemoteMappingAction, keyCode: Int?) {
        context.dataStore.edit { prefs ->
            val allRemoteKeys = listOf(
                REMOTE_UNDO_KEY_CODE,
                REMOTE_RED_KEY_CODE,
                REMOTE_YELLOW_KEY_CODE,
                REMOTE_GREEN_KEY_CODE,
                REMOTE_BROWN_KEY_CODE,
                REMOTE_BLUE_KEY_CODE,
                REMOTE_PINK_KEY_CODE,
                REMOTE_BLACK_KEY_CODE,
                REMOTE_ERROR_KEY_CODE
            )

            if (keyCode != null) {
                // Preferred conflict behavior: newest assignment wins.
                allRemoteKeys.forEach { prefKey ->
                    if (prefs[prefKey] == keyCode) {
                        prefs.remove(prefKey)
                    }
                }
            }

            val targetKey = when (action) {
                RemoteMappingAction.UNDO -> REMOTE_UNDO_KEY_CODE
                RemoteMappingAction.RED -> REMOTE_RED_KEY_CODE
                RemoteMappingAction.YELLOW -> REMOTE_YELLOW_KEY_CODE
                RemoteMappingAction.GREEN -> REMOTE_GREEN_KEY_CODE
                RemoteMappingAction.BROWN -> REMOTE_BROWN_KEY_CODE
                RemoteMappingAction.BLUE -> REMOTE_BLUE_KEY_CODE
                RemoteMappingAction.PINK -> REMOTE_PINK_KEY_CODE
                RemoteMappingAction.BLACK -> REMOTE_BLACK_KEY_CODE
                RemoteMappingAction.ERROR -> REMOTE_ERROR_KEY_CODE
            }

            if (keyCode == null) prefs.remove(targetKey) else prefs[targetKey] = keyCode
        }
    }

    suspend fun resetRemoteMappings() {
        context.dataStore.edit { prefs ->
            prefs.remove(REMOTE_UNDO_KEY_CODE)
            prefs.remove(REMOTE_RED_KEY_CODE)
            prefs.remove(REMOTE_YELLOW_KEY_CODE)
            prefs.remove(REMOTE_GREEN_KEY_CODE)
            prefs.remove(REMOTE_BROWN_KEY_CODE)
            prefs.remove(REMOTE_BLUE_KEY_CODE)
            prefs.remove(REMOTE_PINK_KEY_CODE)
            prefs.remove(REMOTE_BLACK_KEY_CODE)
            prefs.remove(REMOTE_ERROR_KEY_CODE)
        }
    }

    suspend fun setLastSyncState(lastSyncAt: Long?, status: String, errorMessage: String?) {
        context.dataStore.edit { prefs ->
            if (lastSyncAt == null) prefs.remove(LAST_SYNC_AT) else prefs[LAST_SYNC_AT] = lastSyncAt
            prefs[LAST_SYNC_STATUS] = status
            if (errorMessage.isNullOrBlank()) prefs.remove(LAST_SYNC_ERROR) else prefs[LAST_SYNC_ERROR] = errorMessage
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs.remove(PRIMARY_COLOR)
            prefs.remove(ACCENT_COLOR)
            prefs.remove(LOGO_URI)
            prefs.remove(BACKGROUND_URI)
            prefs.remove(BACKGROUND_STYLE)
            prefs.remove(DEFAULT_AVATAR_URI)
            prefs.remove(THEME_VARIANT)
            prefs.remove(REMOTE_UNDO_KEY_CODE)
            prefs.remove(REMOTE_RED_KEY_CODE)
            prefs.remove(REMOTE_YELLOW_KEY_CODE)
            prefs.remove(REMOTE_GREEN_KEY_CODE)
            prefs.remove(REMOTE_BROWN_KEY_CODE)
            prefs.remove(REMOTE_BLUE_KEY_CODE)
            prefs.remove(REMOTE_PINK_KEY_CODE)
            prefs.remove(REMOTE_BLACK_KEY_CODE)
            prefs.remove(REMOTE_ERROR_KEY_CODE)
            prefs.remove(LAST_SYNC_AT)
            prefs.remove(LAST_SYNC_STATUS)
            prefs.remove(LAST_SYNC_ERROR)
        }
    }
}
