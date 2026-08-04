package com.zk.lifeos.service

import android.net.Uri
import com.zk.lifeos.data.backup.BackupStore
import com.zk.lifeos.data.repository.SettingsRepository
import com.zk.lifeos.model.BackupResult
import com.zk.lifeos.model.ThemeMode
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Export / import as the Settings screen sees it: hand it a [Uri] from the file picker, get back a
 * success with counts or a message the user can act on.
 *
 * Exceptions are turned into [BackupResult.Failure] here rather than thrown at the UI — a failed
 * backup is a normal outcome (wrong file, no space, an archive from another version), not a crash.
 */
class BackupService(
    private val backupStore: BackupStore,
    private val settingsRepository: SettingsRepository,
) {

    fun suggestedFileName(): String = backupStore.suggestedFileName(LocalDate.now().toString())

    suspend fun export(target: Uri): BackupResult = runCatching {
        val theme = settingsRepository.themeMode.first()
        backupStore.export(target, theme.name)
    }.fold(
        onSuccess = { BackupResult.Success(it) },
        onFailure = { BackupResult.Failure(it.message ?: "导出失败") },
    )

    /** Replaces everything currently stored, and restores the saved appearance too. */
    suspend fun import(source: Uri): BackupResult = runCatching {
        val (counts, themeMode) = backupStore.import(source)
        themeMode?.let { settingsRepository.setThemeMode(ThemeMode.fromStored(it)) }
        counts
    }.fold(
        onSuccess = { BackupResult.Success(it) },
        onFailure = { BackupResult.Failure(it.message ?: "导入失败") },
    )
}
