package com.zk.lifeos.service

import com.zk.lifeos.data.repository.SettingsRepository
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Business layer for settings: appearance, language, and how stale the last backup is.
 * The UI never reaches into a repository directly.
 */
class SettingsService(private val repository: SettingsRepository) {

    val themeMode: Flow<ThemeMode> = repository.themeMode

    suspend fun setThemeMode(mode: ThemeMode) = repository.setThemeMode(mode)

    val language: Flow<AppLanguage> = repository.language

    suspend fun setLanguage(language: AppLanguage) = repository.setLanguage(language)

    /**
     * When the last export succeeded. Surfaced in Settings because the app keeps everything on one
     * device — a backup nobody remembers to take is the same as no backup.
     */
    val lastBackupAt: Flow<Instant?> = repository.lastBackupAt
}
