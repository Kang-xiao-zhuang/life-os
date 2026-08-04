package com.zk.lifeos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zk.lifeos.data.db.dao.CaptureDao
import com.zk.lifeos.data.db.dao.HabitDao
import com.zk.lifeos.data.db.dao.JournalDao
import com.zk.lifeos.data.db.dao.ProjectDao
import com.zk.lifeos.data.db.dao.TaskDao
import com.zk.lifeos.data.db.entity.CaptureEntity
import com.zk.lifeos.data.db.entity.HabitCheckEntity
import com.zk.lifeos.data.db.entity.HabitEntity
import com.zk.lifeos.data.db.entity.JournalEntryEntity
import com.zk.lifeos.data.db.entity.ProjectEntity
import com.zk.lifeos.data.db.entity.TaskEntity
import java.io.File

/**
 * The one and only database. Lives in the app's private storage; nothing syncs anywhere.
 *
 * The full V1 schema was declared up front so building out the features needed no migrations.
 *
 * ## Changing the schema from here on
 *
 * There is **no** `fallbackToDestructiveMigration` any more — it used to be here as a development
 * convenience and it would have wiped every project, task and journal entry the first time a
 * column changed. Now a schema change without a migration fails loudly at startup instead, which
 * is what you want when the alternative is silent data loss.
 *
 * So: bump [SCHEMA_VERSION], write a `Migration(old, new)`, add it to [MIGRATIONS], and check the
 * generated JSON under `app/schemas/` into git so the change is reviewable.
 */
@Database(
    entities = [
        ProjectEntity::class,
        TaskEntity::class,
        HabitEntity::class,
        HabitCheckEntity::class,
        JournalEntryEntity::class,
        CaptureEntity::class,
    ],
    version = LifeOsDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
abstract class LifeOsDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun journalDao(): JournalDao
    abstract fun captureDao(): CaptureDao

    /**
     * Flush the write-ahead log into the main database file.
     *
     * Required before copying `lifeos.db` for a backup: with WAL enabled the most recent writes
     * live in `lifeos.db-wal`, so copying the main file alone would silently produce a backup
     * that is missing whatever the user just did.
     */
    fun checkpoint() {
        openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    companion object {
        const val NAME = "lifeos.db"
        const val SCHEMA_VERSION = 1

        /** Empty while the schema is still at its first version. See the class docs. */
        private val MIGRATIONS = emptyArray<androidx.room.migration.Migration>()

        fun build(context: Context): LifeOsDatabase =
            Room.databaseBuilder(context, LifeOsDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()

        /**
         * Opens a database file from a backup, read-only in intent. Room still validates the
         * schema, so a backup from a different schema version is rejected here rather than
         * quietly importing the wrong shape of data.
         */
        fun openBackup(context: Context, file: File): LifeOsDatabase =
            Room.databaseBuilder(context, LifeOsDatabase::class.java, file.absolutePath)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
