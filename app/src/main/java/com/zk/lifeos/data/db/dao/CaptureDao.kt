package com.zk.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zk.lifeos.data.db.entity.CaptureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Query("SELECT * FROM captures WHERE processed = 0 ORDER BY createdAt DESC")
    fun observeInbox(): Flow<List<CaptureEntity>>

    @Query("SELECT COUNT(*) FROM captures WHERE processed = 0")
    fun observeInboxCount(): Flow<Int>

    @Insert
    suspend fun insert(capture: CaptureEntity): Long

    /**
     * Triaged into a task — kept rather than deleted, so the inbox stays a record of what was
     * captured instead of losing it on conversion.
     */
    @Query("UPDATE captures SET processed = 1 WHERE id = :id")
    suspend fun markProcessed(id: Long)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun delete(id: Long)

    // ---- backup / restore ----
    // Processed rows included: the inbox history is part of the record.

    @Query("SELECT * FROM captures")
    suspend fun getAll(): List<CaptureEntity>

    @Insert
    suspend fun insertAll(captures: List<CaptureEntity>)

    @Query("DELETE FROM captures")
    suspend fun deleteAll()
}
