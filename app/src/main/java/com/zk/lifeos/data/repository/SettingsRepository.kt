package com.zk.lifeos.data.repository

import com.zk.lifeos.data.prefs.AppPreferences
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Settings persistence. The repository owns the storage format (strings and epoch millis in
 * DataStore) so nothing above it has to know how a [ThemeMode] or an [AppLanguage] is written down.
 */
class SettingsRepository(private val prefs: AppPreferences) {

    val themeMode: Flow<ThemeMode> = prefs.themeMode.map(ThemeMode::fromStored)

    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode.name)

    val language: Flow<AppLanguage> = prefs.language.map(AppLanguage::fromStored)

    suspend fun setLanguage(language: AppLanguage) = prefs.setLanguage(language.name)

    /** When the last successful export happened; null until there has been one. */
    val lastBackupAt: Flow<Instant?> = prefs.lastBackupAt.map { millis ->
        millis?.let(Instant::ofEpochMilli)
    }

    suspend fun markBackedUpNow() = prefs.setLastBackupAt(System.currentTimeMillis())
}
