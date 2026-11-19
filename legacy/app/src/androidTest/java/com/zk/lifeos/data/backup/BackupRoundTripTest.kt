package com.zk.lifeos.data.backup

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zk.lifeos.data.db.LifeOsDatabase
import com.zk.lifeos.data.db.entity.CaptureEntity
import com.zk.lifeos.data.db.entity.HabitCheckEntity
import com.zk.lifeos.data.db.entity.HabitEntity
import com.zk.lifeos.data.db.entity.JournalEntryEntity
import com.zk.lifeos.data.db.entity.ProjectEntity
import com.zk.lifeos.data.db.entity.TaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Backup is the one feature where a silent bug costs the user everything they've recorded, so it
 * gets a real round trip against the real database file rather than a hand check.
 *
 * Runs against the app's actual database (`lifeos.db`) because export copies that exact file —
 * an in-memory Room instance would not exercise the WAL checkpoint, which is the part most likely
 * to lose recent writes.
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: LifeOsDatabase
    private lateinit var store: BackupStore
    private lateinit var archive: File

    @Before
    fun setUp() {
        database = LifeOsDatabase.build(context)
        store = BackupStore(context, database)
        archive = File(context.cacheDir, "roundtrip.zip").apply { delete() }
        runBlocking { clearAll() }
    }

    @After
    fun tearDown() {
        runBlocking { clearAll() }
        archive.delete()
        database.close()
    }

    private suspend fun clearAll() {
        database.habitDao().deleteAllChecks()
        database.habitDao().deleteAll()
        database.taskDao().deleteAll()
        database.projectDao().deleteAll()
        database.captureDao().deleteAll()
        database.journalDao().deleteAll()
    }

    /** Data that exercises every table plus the links between them. */
    private suspend fun seed(): Long {
        val now = 1_770_000_000_000L
        val projectId = database.projectDao().insert(
            ProjectEntity(name = "工作", emoji = "💼", createdAt = now, updatedAt = now)
        )
        database.taskDao().insertAll(
            listOf(
                TaskEntity(
                    title = "带截止日期的任务", notes = "备注", projectId = projectId,
                    dueDate = 20669, isMit = true, createdAt = now, updatedAt = now,
                ),
                TaskEntity(
                    title = "已完成的任务", projectId = projectId, done = true,
                    completedAt = now, createdAt = now, updatedAt = now,
                ),
                TaskEntity(title = "未归类任务", createdAt = now, updatedAt = now),
            )
        )
        val habitId = database.habitDao().insert(
            HabitEntity(name = "阅读", emoji = "📚", createdAt = now)
        )
        database.habitDao().insertAllChecks(
            listOf(
                HabitCheckEntity(habitId = habitId, date = 20668, createdAt = now),
                HabitCheckEntity(habitId = habitId, date = 20669, createdAt = now),
            )
        )
        database.captureDao().insertAll(
            listOf(
                CaptureEntity(text = "一个想法", createdAt = now),
                CaptureEntity(text = "已整理过的", processed = true, createdAt = now),
            )
        )
        database.journalDao().insertAll(
            listOf(
                JournalEntryEntity(
                    date = 20669, done = "写了备份", win = "往返验证通过",
                    problems = "无", tomorrowMit = "收尾", createdAt = now, updatedAt = now,
                )
            )
        )
        return projectId
    }

    @Test
    fun exportThenImport_restoresEverythingIncludingRelations() = runBlocking {
        val projectId = seed()

        val exported = store.export(Uri.fromFile(archive), themeMode = "DARK")
        assertEquals(1, exported.projects)
        assertEquals(3, exported.tasks)
        assertEquals(1, exported.habits)
        assertEquals(2, exported.habitChecks)
        assertEquals(2, exported.captures)
        assertEquals(1, exported.journalEntries)
        assertTrue("archive should exist and be non-empty", archive.length() > 0)

        // Wipe as if this were a fresh install, then restore.
        clearAll()
        assertEquals(0, database.taskDao().getAll().size)

        val (restored, themeMode) = store.import(Uri.fromFile(archive))
        assertEquals("DARK", themeMode)
        assertEquals(exported, restored)

        // Contents, not just counts.
        val project = database.projectDao().getAll().single()
        assertEquals("工作", project.name)
        assertEquals("💼", project.emoji)

        val tasks = database.taskDao().getAll().sortedBy { it.id }
        assertEquals(listOf("带截止日期的任务", "已完成的任务", "未归类任务"), tasks.map { it.title })
        // Primary keys are preserved, so a task still points at its project.
        assertEquals(project.id, tasks[0].projectId)
        assertEquals(projectId, project.id)
        assertEquals(20669, tasks[0].dueDate)
        assertTrue(tasks[0].isMit)
        assertEquals("备注", tasks[0].notes)
        assertTrue(tasks[1].done)
        assertEquals(null, tasks[2].projectId)

        val habit = database.habitDao().getAll().single()
        val checks = database.habitDao().getAllChecks()
        assertEquals(2, checks.size)
        assertTrue("check-ins must still belong to the habit", checks.all { it.habitId == habit.id })

        val journal = database.journalDao().getAll().single()
        assertEquals("往返验证通过", journal.win)

        // Processed captures are part of the record and must survive too.
        assertEquals(1, database.captureDao().getAll().count { it.processed })
    }

    @Test
    fun archive_containsTheStructureTheSpecDescribes() = runBlocking {
        seed()
        store.export(Uri.fromFile(archive), themeMode = "SYSTEM")

        val entries = mutableListOf<String>()
        ZipInputStream(archive.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries += entry.name
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertTrue("database.db missing: $entries", "database.db" in entries)
        assertTrue("config.json missing: $entries", "config.json" in entries)
    }

    @Test
    fun writesMadeJustBeforeExport_areInTheArchive() = runBlocking {
        seed()
        // With write-ahead logging the newest rows sit in lifeos.db-wal; without the checkpoint
        // in export() this task would be missing from the copied database file.
        database.taskDao().insert(
            TaskEntity(title = "刚刚才加的", createdAt = 1L, updatedAt = 1L)
        )

        store.export(Uri.fromFile(archive), themeMode = "DARK")
        clearAll()
        val (restored, _) = store.import(Uri.fromFile(archive))

        assertEquals(4, restored.tasks)
        assertTrue(
            "the last write before export must be in the backup",
            database.taskDao().getAll().any { it.title == "刚刚才加的" },
        )
    }

    @Test
    fun importingSomethingThatIsNotABackup_failsWithoutTouchingData() = runBlocking {
        seed()
        val before = database.taskDao().getAll().size

        val junk = File(context.cacheDir, "not-a-backup.zip").apply {
            writeText("this is not a zip archive at all")
        }
        val failure = runCatching { store.import(Uri.fromFile(junk)) }.exceptionOrNull()
        junk.delete()

        assertTrue("import should reject a non-backup file", failure != null)
        assertEquals("existing data must be untouched", before, database.taskDao().getAll().size)
        assertFalse(
            "the restore workspace must be cleaned up",
            File(context.cacheDir, "restore").exists(),
        )
    }
}
