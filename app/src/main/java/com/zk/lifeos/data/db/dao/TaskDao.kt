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

    /**
     * Every open task with the name of the project it belongs to.
     *
     * Backs the 「所有待办」 view: without it a task that has neither a due date nor the MIT flag is
     * only reachable by opening its project, so it effectively disappears.
     *
     * Undated tasks sort last — a deadline is the only ordering the data actually gives us.
     */
    @Query(
        """
        SELECT t.*, p.name AS projectName, p.emoji AS projectEmoji
        FROM tasks t LEFT JOIN projects p ON p.id = t.projectId
        WHERE t.done = 0
        ORDER BY t.dueDate IS NULL, t.dueDate ASC, t.isMit DESC, t.id ASC
        """
    )
    fun observeAllOpenWithProject(): Flow<List<TaskWithProject>>

    /** How many 今日最重要 are still open — used to warn when the flag is losing its meaning. */
    @Query("SELECT COUNT(*) FROM tasks WHERE done = 0 AND isMit = 1")
    fun observeOpenMitCount(): Flow<Int>

    /**
     * Drags every overdue task onto [today]. They are already late; letting them accumulate as a
     * red backlog is how the list turns into noise the user stops reading.
     *
     * @return how many were moved.
     */
    @Query(
        """
        UPDATE tasks SET dueDate = :today, updatedAt = :now
        WHERE done = 0 AND dueDate IS NOT NULL AND dueDate < :today
        """
    )
    suspend fun rescheduleOverdueTo(today: Int, now: Long): Int

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

    // ---- backup / restore ----

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
