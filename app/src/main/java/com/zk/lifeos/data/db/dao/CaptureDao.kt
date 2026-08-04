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
}
