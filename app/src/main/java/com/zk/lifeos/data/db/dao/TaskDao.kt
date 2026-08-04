package com.zk.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zk.lifeos.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY isMit DESC, dueDate IS NULL, dueDate ASC, id ASC")
    fun observeOpen(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE done = 0")
    fun observeOpenCount(): Flow<Int>

    /** 今日最重要任务 — flagged by hand, not inferred from a due date. */
    @Query("SELECT * FROM tasks WHERE done = 0 AND isMit = 1 ORDER BY id ASC")
    fun observeMit(): Flow<List<TaskEntity>>

    /**
     * What Dashboard calls 今日任务: due today or already overdue. Deliberately excludes
     * undated tasks — those live in their project, not on today's list.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE done = 0 AND dueDate IS NOT NULL AND dueDate <= :today
        ORDER BY dueDate ASC, id ASC
        """
    )
    fun observeDueBy(today: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY done ASC, isMit DESC, dueDate IS NULL, dueDate ASC, id ASC")
    fun observeByProject(projectId: Long): Flow<List<TaskEntity>>

    /** Tasks with no project — the leftovers from quick capture. */
    @Query("SELECT * FROM tasks WHERE projectId IS NULL ORDER BY done ASC, isMit DESC, dueDate IS NULL, dueDate ASC, id ASC")
    fun observeUnassigned(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity): Long
}
