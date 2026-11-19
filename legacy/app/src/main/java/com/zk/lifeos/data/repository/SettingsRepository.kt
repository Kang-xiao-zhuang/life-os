package com.zk.lifeos.data.repository

import com.zk.lifeos.data.prefs.AppPreferences
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.model.ReminderSettings
import com.zk.lifeos.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalTime

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

    /** The two daily reminders. Missing values fall back to the product defaults, never to 00:00. */
    val reminders: Flow<ReminderSettings> = prefs.reminders.map { stored ->
        ReminderSettings(
            morningEnabled = stored.morningEnabled,
            morningTime = stored.morningMinutes?.toLocalTime() ?: ReminderSettings.DEFAULT_MORNING,
            eveningEnabled = stored.eveningEnabled,
            eveningTime = stored.eveningMinutes?.toLocalTime() ?: ReminderSettings.DEFAULT_EVENING,
        )
    }

    suspend fun setMorningReminder(enabled: Boolean, time: LocalTime) =
        prefs.setMorningReminder(enabled, time.toMinuteOfDay())

    suspend fun setEveningReminder(enabled: Boolean, time: LocalTime) =
        prefs.setEveningReminder(enabled, time.toMinuteOfDay())

    private fun Int.toLocalTime(): LocalTime =
        LocalTime.of((this / 60).coerceIn(0, 23), (this % 60).coerceIn(0, 59))

    private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
}
