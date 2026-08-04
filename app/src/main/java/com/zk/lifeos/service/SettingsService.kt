package com.zk.lifeos.service

import com.zk.lifeos.data.repository.SettingsRepository
import com.zk.lifeos.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Business layer for settings. Thin today — it exists so the UI never reaches into a
 * repository directly, which keeps the UI → Service → Repository → SQLite direction intact
 * as Settings grows (export / import / about land here in Phase 4).
 */
class SettingsService(private val repository: SettingsRepository) {

    val themeMode: Flow<ThemeMode> = repository.themeMode

    suspend fun setThemeMode(mode: ThemeMode) = repository.setThemeMode(mode)
}
