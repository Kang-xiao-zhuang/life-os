package com.zk.lifeos.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.model.BackupCounts
import com.zk.lifeos.model.BackupFailure
import com.zk.lifeos.model.BackupResult
import com.zk.lifeos.model.ExportLabels
import com.zk.lifeos.model.ExportRange
import com.zk.lifeos.model.ReminderSettings
import com.zk.lifeos.model.ThemeMode
import com.zk.lifeos.service.BackupService
import com.zk.lifeos.service.MarkdownExportResult
import com.zk.lifeos.service.MarkdownExportService
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
    data class MarkdownExported(val days: Int) : BackupStatus

    /** The chosen period turned out to hold nothing. Says so instead of writing an empty file. */
    data object NothingToExport : BackupStatus
    data class Failed(val failure: BackupFailure) : BackupStatus

    val isError: Boolean get() = this is Failed
}

class SettingsViewModel(
    private val settingsService: SettingsService,
    private val backupService: BackupService,
    private val markdownExportService: MarkdownExportService,
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

    /**
     * The periods worth exporting, newest first, with「全部」in front when there is more than
     * nothing. Reloaded whenever Settings is opened rather than observed: it changes when you write
     * a review, not while you sit on this screen.
     */
    private val _exportRanges = MutableStateFlow<List<ExportRange>>(emptyList())
    val exportRanges: StateFlow<List<ExportRange>> = _exportRanges.asStateFlow()

    fun refreshExportRanges() {
        viewModelScope.launch {
            val months = markdownExportService.exportableMonths()
            _exportRanges.value = if (months.isEmpty()) {
                emptyList()
            } else {
                // months arrive newest first, so the span runs from the last one to the first.
                listOf(
                    ExportRange.All(
                        from = months.last().atDay(1),
                        to = months.first().atEndOfMonth(),
                    )
                ) + months.map { ExportRange.Month(it) }
            }
        }
    }

    /** ASCII on purpose — a file name is not UI text, and it travels to other machines. */
    fun markdownFileName(range: ExportRange): String = when (range) {
        is ExportRange.All -> "LifeOS_all.md"
        is ExportRange.Month -> "LifeOS_%d-%02d.md".format(range.month.year, range.month.monthValue)
    }

    fun exportMarkdown(target: Uri, range: ExportRange, labels: ExportLabels) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _status.value = null
            _status.value = when (val result = markdownExportService.export(target, range, labels)) {
                is MarkdownExportResult.Success -> BackupStatus.MarkdownExported(result.days)
                is MarkdownExportResult.Nothing -> BackupStatus.NothingToExport
                is MarkdownExportResult.Failure -> BackupStatus.Failed(result.failure)
            }
            _busy.value = false
        }
    }

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
