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

    /**
     * 今日最重要任务 — flagged by hand, not inferred from a due date.
     *
     * Keeps tasks finished today in the list (struck through, sorted last) rather than making
     * them vanish the instant they're ticked: a mis-tap must be undoable where it happened, and
     * seeing what you already did today is the point of the screen. [dayStart] is today's
     * midnight in epoch millis.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isMit = 1 AND (done = 0 OR completedAt >= :dayStart)
        ORDER BY done ASC, id ASC
        """
    )
    fun observeMit(dayStart: Long): Flow<List<TaskEntity>>

    /**
     * What Dashboard calls 今日任务: due today or already overdue, plus anything finished today.
     * Deliberately excludes undated tasks — those live in their project, not on today's list.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate IS NOT NULL AND dueDate <= :today
          AND (done = 0 OR completedAt >= :dayStart)
        ORDER BY done ASC, dueDate ASC, id ASC
        """
    )
    fun observeDueBy(today: Int, dayStart: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY done ASC, isMit DESC, dueDate IS NULL, dueDate ASC, id ASC")
    fun observeByProject(projectId: Long): Flow<List<TaskEntity>>

    /** Tasks with no project — the leftovers from quick capture. */
    @Query("SELECT * FROM tasks WHERE projectId IS NULL ORDER BY done ASC, isMit DESC, dueDate IS NULL, dueDate ASC, id ASC")
    fun observeUnassigned(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query(
        """
        UPDATE tasks SET title = :title, notes = :notes, projectId = :projectId,
          dueDate = :dueDate, isMit = :isMit, updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun update(
        id: Long,
        title: String,
        notes: String,
        projectId: Long?,
        dueDate: Int?,
        isMit: Boolean,
        now: Long,
    )

    /** [completedAt] records when it was ticked; null clears it when a task is reopened. */
    @Query("UPDATE tasks SET done = :done, completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, completedAt: Long?, now: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}
