package com.zk.lifeos.data.repository

import com.zk.lifeos.data.prefs.AppPreferences
import com.zk.lifeos.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Settings persistence. The repository owns the storage format (a string in DataStore) so
 * nothing above it has to know how a [ThemeMode] is written down.
 */
class SettingsRepository(private val prefs: AppPreferences) {

    val themeMode: Flow<ThemeMode> = prefs.themeMode.map(ThemeMode::fromStored)

    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode.name)
}
