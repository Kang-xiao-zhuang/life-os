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

/**
 * The one and only database. Lives in the app's private storage; nothing syncs anywhere.
 *
 * The full V1 schema is declared up front on purpose: defining all six tables once avoids a
 * string of Room migrations while the app is still being built out.
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
    version = 1,
    exportSchema = true,
)
abstract class LifeOsDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun journalDao(): JournalDao
    abstract fun captureDao(): CaptureDao

    companion object {
        const val NAME = "lifeos.db"

        fun build(context: Context): LifeOsDatabase =
            Room.databaseBuilder(context, LifeOsDatabase::class.java, NAME)
                // ⚠️ DEVELOPMENT ONLY. While the schema is still moving, a mismatch wipes the DB
                // instead of crashing. This MUST be replaced with real migrations before the app
                // holds data worth keeping — otherwise a schema tweak silently deletes everything.
                // Tracked as the last task of Phase 4 (数据管理).
                .fallbackToDestructiveMigration(dropAllTables = true)
                // Foreign keys are declared on the entities; SQLite only enforces them if asked.
                .apply { setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) }
                .build()
    }
}
