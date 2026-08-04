package com.zk.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zk.lifeos.data.db.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    /** One entry per day, so this is a single row (or null before anything is written). */
    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    fun observeByDate(date: Int): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entries ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT COUNT(*) FROM journal_entries")
    fun observeCount(): Flow<Int>

    /**
     * Deliberately NOT `@Upsert`. Room's upsert falls back to updating by PRIMARY KEY when the
     * insert conflicts — but the conflict here would be on the unique `date` index, so that
     * fallback would update nothing and silently drop what the user wrote. The repository looks
     * the row up by date first and then picks insert or update explicitly.
     */
    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: Int): JournalEntryEntity?

    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Query(
        """
        UPDATE journal_entries
        SET done = :done, win = :win, problems = :problems, tomorrowMit = :tomorrowMit, updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun update(
        id: Long,
        done: String,
        win: String,
        problems: String,
        tomorrowMit: String,
        now: Long,
    )

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun delete(id: Long)

    // ---- backup / restore ----

    @Query("SELECT * FROM journal_entries")
    suspend fun getAll(): List<JournalEntryEntity>

    @Insert
    suspend fun insertAll(entries: List<JournalEntryEntity>)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll()
}
