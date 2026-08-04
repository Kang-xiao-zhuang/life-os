package com.zk.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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

    /** Upsert keyed on the unique date index — editing today's entry never creates a second row. */
    @Upsert
    suspend fun upsert(entry: JournalEntryEntity)
}
