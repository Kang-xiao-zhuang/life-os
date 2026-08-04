package com.zk.lifeos.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zk.lifeos.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore instance, scoped to the application context (one per process, as required). */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifeos_settings")

/**
 * Small key/value settings — the equivalent of the MMKV layer in the original spec.
 * DataStore ships with AndroidX, so this needs no extra dependency.
 *
 * Only *preferences* go here. Real data lives in SQLite.
 */
class AppPreferences(private val context: Context) {

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: DEFAULT_THEME_MODE
    }

    suspend fun setThemeMode(value: String) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = value }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

        /** Nothing stored yet → the product default (see [ThemeMode.DEFAULT]). */
        val DEFAULT_THEME_MODE: String = ThemeMode.DEFAULT.name
    }
}
