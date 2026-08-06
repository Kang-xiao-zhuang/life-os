package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.TaskDao
import com.zk.lifeos.data.db.entity.TaskEntity
import com.zk.lifeos.model.RepeatRule
import com.zk.lifeos.model.RescheduledTask
import com.zk.lifeos.model.Task
import com.zk.lifeos.model.TaskListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TaskRepository(private val taskDao: TaskDao) {

    /** 今日最重要任务 (open, plus whatever was finished today). */
    fun observeMit(today: LocalDate): Flow<List<Task>> =
        taskDao.observeMit(today.startOfDayMillis()).map { list -> list.map { it.toModel() } }

    /** Due today or overdue, plus whatever was finished today. */
    fun observeDueBy(date: LocalDate): Flow<List<Task>> =
        taskDao.observeDueBy(date.toEpochDayInt(), date.startOfDayMillis())
            .map { list -> list.map { it.toModel() } }

    fun observeByProject(projectId: Long): Flow<List<Task>> =
        taskDao.observeByProject(projectId).map { list -> list.map { it.toModel() } }

    fun observeUnassigned(): Flow<List<Task>> =
        taskDao.observeUnassigned().map { list -> list.map { it.toModel() } }

    /** Every open task, each carrying its project name — for the 「所有待办」 view. */
    fun observeAllOpen(): Flow<List<TaskListItem>> =
        taskDao.observeAllOpenWithProject().map { rows -> rows.map { it.toModel() } }

    fun observeOpenMitCount(): Flow<Int> = taskDao.observeOpenMitCount()

    /**
     * Moves every overdue task to [today] and returns what it moved, so the caller can offer an
     * undo. Reads the rows first and then updates exactly those ids — the undo set can't drift
     * away from what actually changed.
     */
    suspend fun rescheduleOverdueTo(today: LocalDate): List<RescheduledTask> {
        val overdue = taskDao.findOverdue(today.toEpochDayInt())
        if (overdue.isEmpty()) return emptyList()
        taskDao.setDueDateFor(
            ids = overdue.map { it.id },
            today = today.toEpochDayInt(),
            now = System.currentTimeMillis(),
        )
        return overdue.map { RescheduledTask(it.id, it.dueDate?.toLocalDate()) }
    }

    /** Puts the dates back, one by one — the batch is small and each row had its own date. */
    suspend fun restoreDueDates(items: List<RescheduledTask>) {
        val now = System.currentTimeMillis()
        items.forEach { item ->
            taskDao.setDueDate(item.id, item.previousDueDate?.toEpochDayInt(), now)
        }
    }

    suspend fun create(
        title: String,
        notes: String = "",
        projectId: Long? = null,
        dueDate: LocalDate? = null,
        isMit: Boolean = false,
        repeatRule: RepeatRule? = null,
    ): Long {
        val now = System.currentTimeMillis()
        return taskDao.insert(
            TaskEntity(
                title = title,
                notes = notes,
                projectId = projectId,
                dueDate = dueDate?.toEpochDayInt(),
                isMit = isMit,
                repeatRule = repeatRule?.name,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun update(
        id: Long,
        title: String,
        notes: String,
        projectId: Long?,
        dueDate: LocalDate?,
        isMit: Boolean,
        repeatRule: RepeatRule?,
    ) = taskDao.update(
        id = id,
        title = title,
        notes = notes,
        projectId = projectId,
        dueDate = dueDate?.toEpochDayInt(),
        isMit = isMit,
        repeatRule = repeatRule?.name,
        now = System.currentTimeMillis(),
    )

    /** Reopening clears the completion timestamp so history stays truthful. */
    suspend fun setDone(id: Long, done: Boolean) {
        val now = System.currentTimeMillis()
        taskDao.setDone(id = id, done = done, completedAt = if (done) now else null, now = now)
    }

    /**
     * Creates the next occurrence of a repeating task, carrying everything except the MIT flag and
     * the completion state.
     *
     * MIT is deliberately dropped: 「今日最重要」is a decision you make for a particular day, not a
     * property of the task, so re-flagging next week's copy would put a choice in your list that you
     * never made.
     */
    suspend fun createNextOccurrence(task: Task, on: LocalDate): Long = create(
        title = task.title,
        notes = task.notes,
        projectId = task.projectId,
        dueDate = on,
        isMit = false,
        repeatRule = task.repeatRule,
    )

    /** Takes back an untouched generated occurrence; see [TaskDao.findGeneratedOccurrence]. */
    suspend fun removeGeneratedOccurrence(task: Task, on: LocalDate): Boolean {
        val rule = task.repeatRule ?: return false
        val existing = taskDao.findGeneratedOccurrence(
            title = task.title,
            repeatRule = rule.name,
            dueDate = on.toEpochDayInt(),
        ) ?: return false
        taskDao.delete(existing.id)
        return true
    }

    suspend fun delete(id: Long) = taskDao.delete(id)
}
