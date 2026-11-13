package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "studio_settings")

data class StudioSettings(
    val activeProjectId: Long = -1L,
    val compilerVersion: String = "AMX Mod X 1.9.0",
    val compilerServerUrl: String = "",
    val targetServerOs: String = "Linux (.so)",
    val editorFontSizeSp: Int = 13,
    val savedCstrikeUri: String = "",
    val autoSaveEnabled: Boolean = true
)

class StudioPreferences(private val context: Context) {

    private object Keys {
        val ACTIVE_PROJECT_ID = longPreferencesKey("active_project_id")
        val COMPILER_VERSION = stringPreferencesKey("compiler_version")
        val COMPILER_SERVER_URL = stringPreferencesKey("compiler_server_url")
        val TARGET_SERVER_OS = stringPreferencesKey("target_server_os")
        val EDITOR_FONT_SIZE_SP = intPreferencesKey("editor_font_size_sp")
        val SAVED_CSTRIKE_URI = stringPreferencesKey("saved_cstrike_uri")
        val AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
    }

    val settingsFlow: Flow<StudioSettings> = context.studioDataStore.data.map { prefs ->
        StudioSettings(
            activeProjectId = prefs[Keys.ACTIVE_PROJECT_ID] ?: -1L,
            compilerVersion = prefs[Keys.COMPILER_VERSION] ?: "AMX Mod X 1.9.0",
            compilerServerUrl = prefs[Keys.COMPILER_SERVER_URL] ?: "",
            targetServerOs = prefs[Keys.TARGET_SERVER_OS] ?: "Linux (.so)",
            editorFontSizeSp = prefs[Keys.EDITOR_FONT_SIZE_SP] ?: 13,
            savedCstrikeUri = prefs[Keys.SAVED_CSTRIKE_URI] ?: "",
            autoSaveEnabled = prefs[Keys.AUTO_SAVE_ENABLED] ?: true
        )
    }

    suspend fun setActiveProjectId(id: Long) {
        context.studioDataStore.edit { it[Keys.ACTIVE_PROJECT_ID] = id }
    }

    suspend fun setCompilerVersion(version: String) {
        context.studioDataStore.edit { it[Keys.COMPILER_VERSION] = version }
    }

    suspend fun setCompilerServerUrl(url: String) {
        context.studioDataStore.edit { it[Keys.COMPILER_SERVER_URL] = url.trim() }
    }

    suspend fun setTargetServerOs(os: String) {
        context.studioDataStore.edit { it[Keys.TARGET_SERVER_OS] = os }
    }

    suspend fun setEditorFontSizeSp(sizeSp: Int) {
        context.studioDataStore.edit { it[Keys.EDITOR_FONT_SIZE_SP] = sizeSp.coerceIn(10, 22) }
    }

    suspend fun setSavedCstrikeUri(uri: String) {
        context.studioDataStore.edit { it[Keys.SAVED_CSTRIKE_URI] = uri }
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.AUTO_SAVE_ENABLED] = enabled }
    }
}
