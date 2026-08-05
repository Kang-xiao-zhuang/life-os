package com.zk.lifeos.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.model.BackupCounts
import com.zk.lifeos.model.BackupFailure
import com.zk.lifeos.model.BackupResult
import com.zk.lifeos.model.ReminderSettings
import com.zk.lifeos.model.ThemeMode
import com.zk.lifeos.service.BackupService
import com.zk.lifeos.service.ReminderService
import com.zk.lifeos.service.SettingsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime

/**
 * What the last export/import did — as data, not as a sentence.
 *
 * A ViewModel has no access to string resources, so a pre-formatted message here would be frozen
 * in whatever language was hard-coded. The screen turns these into words.
 */
sealed interface BackupStatus {
    data class Exported(val counts: BackupCounts) : BackupStatus
    data class Restored(val counts: BackupCounts) : BackupStatus
    data class Failed(val failure: BackupFailure) : BackupStatus

    val isError: Boolean get() = this is Failed
}

class SettingsViewModel(
    private val settingsService: SettingsService,
    private val backupService: BackupService,
    private val reminderService: ReminderService,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsService.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.DEFAULT)

    val language: StateFlow<AppLanguage> = settingsService.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.DEFAULT)

    /** null = never exported. */
    val lastBackupAt: StateFlow<Instant?> = settingsService.lastBackupAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reminders: StateFlow<ReminderSettings> = reminderService.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _status = MutableStateFlow<BackupStatus?>(null)
    val status: StateFlow<BackupStatus?> = _status.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsService.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsService.setLanguage(language) }
    }

    fun setMorningReminder(enabled: Boolean, time: LocalTime) {
        viewModelScope.launch { reminderService.setMorning(enabled, time) }
    }

    fun setEveningReminder(enabled: Boolean, time: LocalTime) {
        viewModelScope.launch { reminderService.setEvening(enabled, time) }
    }

    fun suggestedFileName(): String = backupService.suggestedFileName()

    fun export(target: Uri) = run(
        action = { backupService.export(target) },
        onSuccess = BackupStatus::Exported,
    )

    fun import(source: Uri) = run(
        action = { backupService.import(source) },
        onSuccess = BackupStatus::Restored,
    )

    private fun run(
        action: suspend () -> BackupResult,
        onSuccess: (BackupCounts) -> BackupStatus,
    ) {
        if (_busy.value) return // one at a time; a second tap must not race the first
        viewModelScope.launch {
            _busy.value = true
            _status.value = null
            _status.value = when (val result = action()) {
                is BackupResult.Success -> onSuccess(result.counts)
                is BackupResult.Failure -> BackupStatus.Failed(result.failure)
            }
            _busy.value = false
        }
    }
}
