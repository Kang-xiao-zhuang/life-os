package com.zk.lifeos.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.BackupResult
import com.zk.lifeos.model.ThemeMode
import com.zk.lifeos.service.BackupService
import com.zk.lifeos.service.SettingsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the last export/import did, in words the user can read. */
data class BackupStatus(val message: String, val isError: Boolean)

class SettingsViewModel(
    private val settingsService: SettingsService,
    private val backupService: BackupService,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsService.themeMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ThemeMode.DEFAULT,
        )

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _status = MutableStateFlow<BackupStatus?>(null)
    val status: StateFlow<BackupStatus?> = _status.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsService.setThemeMode(mode) }
    }

    fun suggestedFileName(): String = backupService.suggestedFileName()

    fun export(target: Uri) = run(
        action = { backupService.export(target) },
        successPrefix = "已导出",
    )

    fun import(source: Uri) = run(
        action = { backupService.import(source) },
        successPrefix = "已恢复",
    )

    private fun run(action: suspend () -> BackupResult, successPrefix: String) {
        if (_busy.value) return // one at a time; a second tap must not race the first
        viewModelScope.launch {
            _busy.value = true
            _status.value = null
            _status.value = when (val result = action()) {
                is BackupResult.Success ->
                    BackupStatus("$successPrefix:${result.counts.describe()}", isError = false)

                is BackupResult.Failure -> BackupStatus(result.message, isError = true)
            }
            _busy.value = false
        }
    }
}
