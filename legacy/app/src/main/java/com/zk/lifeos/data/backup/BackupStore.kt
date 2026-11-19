package com.zk.lifeos.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.zk.lifeos.data.db.LifeOsDatabase
import com.zk.lifeos.model.BackupCounts
import com.zk.lifeos.model.BackupException
import com.zk.lifeos.model.BackupFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.OffsetDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Reads and writes `LifeOS_Backup.zip`, the archive described in the spec:
 *
 * ```
 * database.db      the SQLite file
 * attachments/     app-private files (V1 has no attachment feature yet, so usually absent)
 * config.json      settings + metadata, including the schema version
 * ```
 *
 * The [Uri]s come from the system file picker (SAF), which is why the app still declares **zero
 * permissions**: the user chooses the file and grants access to that one file only.
 *
 * This is repository-level code — it owns the file format and the database plumbing, and nothing
 * above it needs to know either.
 */
class BackupStore(
    private val context: Context,
    private val database: LifeOsDatabase,
) {

    /** Writes the archive to [target]. Returns what went into it. */
    suspend fun export(target: Uri, themeMode: String): BackupCounts = withContext(Dispatchers.IO) {
        // Push the write-ahead log into the main file first, or the copy misses recent writes.
        database.checkpoint()

        val counts = liveCounts()
        val dbFile = context.getDatabasePath(LifeOsDatabase.NAME)

        val output = context.contentResolver.openOutputStream(target)
            ?: throw BackupException(BackupFailure.CannotWrite)

        output.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(ENTRY_DB))
                dbFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()

                attachmentsDir().takeIf { it.isDirectory }?.let { dir ->
                    dir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relative = file.relativeTo(dir).path.replace(File.separatorChar, '/')
                        zip.putNextEntry(ZipEntry("$ENTRY_ATTACHMENTS$relative"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }

                zip.putNextEntry(ZipEntry(ENTRY_CONFIG))
                zip.write(configJson(themeMode, counts).toByteArray())
                zip.closeEntry()
            }
        }
        counts
    }

    /**
     * Restores from [source], replacing everything currently stored.
     *
     * The rows are copied out of the backup and written into the live database inside one
     * transaction, rather than swapping the file underneath an open Room instance — a half-applied
     * restore would be worse than a failed one. Primary keys are preserved so the links between
     * tasks, projects and habit check-ins survive.
     *
     * @return the restored counts and the theme mode from the archive (null if it had none).
     */
    suspend fun import(source: Uri): Pair<BackupCounts, String?> = withContext(Dispatchers.IO) {
        val work = File(context.cacheDir, "restore").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            val extractedDb = File(work, "backup.db")
            var config: JSONObject? = null

            val input = context.contentResolver.openInputStream(source)
                ?: throw BackupException(BackupFailure.CannotRead)
            input.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == ENTRY_DB ->
                                extractedDb.outputStream().use { zip.copyTo(it) }

                            entry.name == ENTRY_CONFIG ->
                                config = JSONObject(zip.readBytes().decodeToString())

                            entry.name.startsWith(ENTRY_ATTACHMENTS) && !entry.isDirectory -> {
                                val relative = entry.name.removePrefix(ENTRY_ATTACHMENTS)
                                // Reject path traversal: a crafted zip must not write outside
                                // the attachments directory.
                                val destination = File(attachmentsDir(), relative)
                                val root = attachmentsDir().canonicalFile
                                if (destination.canonicalFile.startsWith(root)) {
                                    destination.parentFile?.mkdirs()
                                    destination.outputStream().use { zip.copyTo(it) }
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            if (!extractedDb.isFile) throw BackupException(BackupFailure.NotABackup)

            config?.let { json ->
                val version = json.optInt(KEY_SCHEMA_VERSION, -1)
                if (version != -1 && version != LifeOsDatabase.SCHEMA_VERSION) {
                    throw BackupException(
                        BackupFailure.SchemaMismatch(
                            backupVersion = version,
                            appVersion = LifeOsDatabase.SCHEMA_VERSION,
                        )
                    )
                }
            }

            val backup = LifeOsDatabase.openBackup(context, extractedDb)
            val counts: BackupCounts
            try {
                val projects = backup.projectDao().getAll()
                val tasks = backup.taskDao().getAll()
                val habits = backup.habitDao().getAll()
                val checks = backup.habitDao().getAllChecks()
                val captures = backup.captureDao().getAll()
                val journals = backup.journalDao().getAll()

                database.withTransaction {
                    // Children before parents, or the foreign keys refuse the delete.
                    database.habitDao().deleteAllChecks()
                    database.habitDao().deleteAll()
                    database.taskDao().deleteAll()
                    database.projectDao().deleteAll()
                    database.captureDao().deleteAll()
                    database.journalDao().deleteAll()

                    // Parents before children on the way back in.
                    database.projectDao().insertAll(projects)
                    database.taskDao().insertAll(tasks)
                    database.habitDao().insertAll(habits)
                    database.habitDao().insertAllChecks(checks)
                    database.captureDao().insertAll(captures)
                    database.journalDao().insertAll(journals)
                }

                counts = BackupCounts(
                    projects = projects.size,
                    tasks = tasks.size,
                    habits = habits.size,
                    habitChecks = checks.size,
                    captures = captures.size,
                    journalEntries = journals.size,
                )
            } finally {
                backup.close()
            }

            counts to config?.optString(KEY_THEME_MODE)?.takeIf { it.isNotEmpty() }
        } finally {
            work.deleteRecursively()
        }
    }

    /** Suggested filename for the picker: `LifeOS_Backup_2026-08-04.zip`. */
    fun suggestedFileName(today: String): String = "LifeOS_Backup_$today.zip"

    private suspend fun liveCounts() = BackupCounts(
        projects = database.projectDao().getAll().size,
        tasks = database.taskDao().getAll().size,
        habits = database.habitDao().getAll().size,
        habitChecks = database.habitDao().getAllChecks().size,
        captures = database.captureDao().getAll().size,
        journalEntries = database.journalDao().getAll().size,
    )

    private fun attachmentsDir() = File(context.filesDir, "attachments")

    private fun configJson(themeMode: String, counts: BackupCounts) = JSONObject().apply {
        put("app", "LifeOS")
        put("versionName", appVersionName())
        put(KEY_SCHEMA_VERSION, LifeOsDatabase.SCHEMA_VERSION)
        put("exportedAt", OffsetDateTime.now().toString())
        put(KEY_THEME_MODE, themeMode)
        put(
            "counts",
            JSONObject().apply {
                put("projects", counts.projects)
                put("tasks", counts.tasks)
                put("habits", counts.habits)
                put("habitChecks", counts.habitChecks)
                put("captures", counts.captures)
                put("journalEntries", counts.journalEntries)
            },
        )
    }.toString(2)

    private fun appVersionName(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")

    private companion object {
        const val ENTRY_DB = "database.db"
        const val ENTRY_CONFIG = "config.json"
        const val ENTRY_ATTACHMENTS = "attachments/"
        const val KEY_SCHEMA_VERSION = "schemaVersion"
        const val KEY_THEME_MODE = "themeMode"
    }
}
