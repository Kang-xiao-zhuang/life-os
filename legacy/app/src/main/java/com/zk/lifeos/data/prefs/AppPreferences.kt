package com.zk.lifeos.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zk.lifeos.model.AppLanguage
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

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE
    }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { prefs -> prefs[KEY_LANGUAGE] = value }
    }

    /** When the last export succeeded, as epoch millis. Null until one has. */
    val lastBackupAt: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[KEY_LAST_BACKUP_AT] }

    suspend fun setLastBackupAt(millis: Long) {
        context.dataStore.edit { prefs -> prefs[KEY_LAST_BACKUP_AT] = millis }
    }

    /**
     * Reminder switches and times. Times are stored as **minutes since midnight** — an Int sorts,
     * compares and survives a locale change, which a formatted "21:30" would not.
     */
    val reminders: Flow<StoredReminders> = context.dataStore.data.map { prefs ->
        StoredReminders(
            morningEnabled = prefs[KEY_REMIND_MORNING] ?: false,
            morningMinutes = prefs[KEY_REMIND_MORNING_AT],
            eveningEnabled = prefs[KEY_REMIND_EVENING] ?: false,
            eveningMinutes = prefs[KEY_REMIND_EVENING_AT],
        )
    }

    suspend fun setMorningReminder(enabled: Boolean, minuteOfDay: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMIND_MORNING] = enabled
            prefs[KEY_REMIND_MORNING_AT] = minuteOfDay
        }
    }

    suspend fun setEveningReminder(enabled: Boolean, minuteOfDay: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMIND_EVENING] = enabled
            prefs[KEY_REMIND_EVENING_AT] = minuteOfDay
        }
    }

    /** Raw storage shape; the repository turns it into a `ReminderSettings`. */
    data class StoredReminders(
        val morningEnabled: Boolean,
        val morningMinutes: Int?,
        val eveningEnabled: Boolean,
        val eveningMinutes: Int?,
    )

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        private val KEY_REMIND_MORNING = booleanPreferencesKey("remind_morning")
        private val KEY_REMIND_MORNING_AT = intPreferencesKey("remind_morning_at")
        private val KEY_REMIND_EVENING = booleanPreferencesKey("remind_evening")
        private val KEY_REMIND_EVENING_AT = intPreferencesKey("remind_evening_at")

        /** Nothing stored yet → the product default (see [ThemeMode.DEFAULT]). */
        val DEFAULT_THEME_MODE: String = ThemeMode.DEFAULT.name

        val DEFAULT_LANGUAGE: String = AppLanguage.DEFAULT.name
    }
}
