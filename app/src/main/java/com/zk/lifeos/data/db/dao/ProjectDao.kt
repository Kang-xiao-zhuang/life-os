package com.zk.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zk.lifeos.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Phase 1 keeps every DAO to the minimum needed to prove the wiring works end to end.
 * Phase 3 grows these as the features land — no speculative queries up front.
 */
@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects WHERE archived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects WHERE archived = 0")
    fun observeActiveCount(): Flow<Int>

    @Insert
    suspend fun insert(project: ProjectEntity): Long
}
