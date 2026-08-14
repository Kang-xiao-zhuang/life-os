package com.zk.lifeos.service

import android.net.Uri
import com.zk.lifeos.data.backup.TextDocumentStore
import com.zk.lifeos.model.BackupException
import com.zk.lifeos.model.BackupFailure
import com.zk.lifeos.model.ExportLabels
import com.zk.lifeos.model.ExportRange
import com.zk.lifeos.model.MarkdownReport
import java.time.YearMonth

/** How a Markdown export ended, in the same shape [BackupService] uses. */
sealed interface MarkdownExportResult {
    /** [days] is what the file actually covers, so the UI can say more than「导出成功」. */
    data class Success(val days: Int) : MarkdownExportResult
    data class Failure(val failure: BackupFailure) : MarkdownExportResult
    /** Nothing in the chosen period — not an error, and not worth writing an empty file for. */
    data object Nothing : MarkdownExportResult
}

/**
 * 复盘导出 —— a period of reviews and completions written out as readable Markdown.
 *
 * Separate from [BackupService] because they answer different questions. Backup answers「把它装回
 * 去」and its format is a SQLite file this app version can open. This answers「十年后还读得到吗」and
 * its format is text. Neither replaces the other, and using the backup zip as an archive is exactly
 * the mistake this exists to prevent.
 *
 * Uses the same SAF [Uri] plumbing as backup, so it still needs **no permission**.
 */
class MarkdownExportService(
    private val journalService: JournalService,
    private val textDocumentStore: TextDocumentStore,
) {

    suspend fun exportableMonths(): List<YearMonth> = journalService.exportableMonths()

    suspend fun export(
        target: Uri,
        range: ExportRange,
        labels: ExportLabels,
    ): MarkdownExportResult = runCatching {
        val days = journalService.collectForExport(range)
        if (days.isEmpty()) return MarkdownExportResult.Nothing
        textDocumentStore.write(target, MarkdownReport.render(days, labels))
        days.size
    }.fold(
        onSuccess = { MarkdownExportResult.Success(it) },
        onFailure = { throwable ->
            MarkdownExportResult.Failure(
                (throwable as? BackupException)?.failure
                    ?: BackupFailure.Unexpected(throwable.message)
            )
        },
    )
}
