package com.zk.lifeos.service

import com.zk.lifeos.data.repository.TaskRepository
import com.zk.lifeos.model.RepeatRule
import com.zk.lifeos.model.RescheduledTask
import com.zk.lifeos.model.Task
import com.zk.lifeos.model.TaskListItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 任务管理. Kept deliberately plain: no filters, no sub-tasks. */
class TaskService(private val taskRepository: TaskRepository) {

    fun observeByProject(projectId: Long): Flow<List<Task>> = taskRepository.observeByProject(projectId)

    fun observeUnassigned(): Flow<List<Task>> = taskRepository.observeUnassigned()

    /** Every open task, wherever it lives — answers「我现在能做什么」in one screen. */
    fun observeAllOpen(): Flow<List<TaskListItem>> = taskRepository.observeAllOpen()

    fun observeOpenMitCount(): Flow<Int> = taskRepository.observeOpenMitCount()

    /** 今日最重要 for [today], open ones plus whatever was finished today. Used by the home-screen widget. */
    fun observeMit(today: LocalDate = LocalDate.now()): Flow<List<Task>> =
        taskRepository.observeMit(today)

    /**
     * 「把逾期的挪到今天」. They are already late, and a growing red backlog is how the list stops
     * being read at all.
     *
     * Returns what it moved so the caller can offer an undo — one tap must not silently rewrite a
     * dozen due dates with no way back.
     */
    suspend fun rescheduleOverdueToToday(): List<RescheduledTask> =
        taskRepository.rescheduleOverdueTo(LocalDate.now())

    suspend fun undoReschedule(moved: List<RescheduledTask>) =
        taskRepository.restoreDueDates(moved)

    companion object {
        /** 一天挑一到两件就够 — beyond this the flag stops meaning anything. Advisory, not enforced. */
        const val MIT_SOFT_LIMIT = 2
    }

    /** Returns false when the title is blank. */
    suspend fun create(
        title: String,
        notes: String = "",
        projectId: Long? = null,
        dueDate: LocalDate? = null,
        isMit: Boolean = false,
        repeatRule: RepeatRule? = null,
    ): Boolean {
        val clean = title.trim()
        if (clean.isEmpty()) return false
        taskRepository.create(
            title = clean,
            notes = notes.trim(),
            projectId = projectId,
            dueDate = dueDate,
            isMit = isMit,
            repeatRule = repeatRule,
        )
        return true
    }

    suspend fun update(
        id: Long,
        title: String,
        notes: String,
        projectId: Long?,
        dueDate: LocalDate?,
        isMit: Boolean,
        repeatRule: RepeatRule? = null,
    ): Boolean {
        val clean = title.trim()
        if (clean.isEmpty()) return false
        taskRepository.update(
            id = id,
            title = clean,
            notes = notes.trim(),
            projectId = projectId,
            dueDate = dueDate,
            isMit = isMit,
            repeatRule = repeatRule,
        )
        return true
    }

    /**
     * Tick / untick. [Task.done] is what the caller currently sees, so this flips it.
     *
     * Completing a repeating task also creates the next occurrence, and un-completing takes that
     * occurrence back. The pairing matters: without the second half, tick-then-untick — an ordinary
     * mis-tap — would leave a duplicate sitting in the future, and this app's rule is that no single
     * step leaves you worse off than before you took it. The removal is deliberately narrow (see
     * `TaskDao.findGeneratedOccurrence`), so anything you have since edited or ticked off is safe.
     */
    suspend fun toggleDone(task: Task, today: LocalDate = LocalDate.now()) {
        val completing = !task.done
        taskRepository.setDone(task.id, completing)

        val next = task.nextOccurrence(today) ?: return
        if (completing) {
            taskRepository.createNextOccurrence(task, next)
        } else {
            taskRepository.removeGeneratedOccurrence(task, next)
        }
    }

    suspend fun delete(id: Long) = taskRepository.delete(id)
}
