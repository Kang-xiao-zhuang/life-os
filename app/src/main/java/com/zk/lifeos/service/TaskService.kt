package com.zk.lifeos.service

import com.zk.lifeos.data.repository.TaskRepository
import com.zk.lifeos.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 任务管理. Kept deliberately plain: no filters, no sub-tasks, no recurrence. */
class TaskService(private val taskRepository: TaskRepository) {

    fun observeByProject(projectId: Long): Flow<List<Task>> = taskRepository.observeByProject(projectId)

    fun observeUnassigned(): Flow<List<Task>> = taskRepository.observeUnassigned()

    /** Returns false when the title is blank. */
    suspend fun create(
        title: String,
        notes: String = "",
        projectId: Long? = null,
        dueDate: LocalDate? = null,
        isMit: Boolean = false,
    ): Boolean {
        val clean = title.trim()
        if (clean.isEmpty()) return false
        taskRepository.create(
            title = clean,
            notes = notes.trim(),
            projectId = projectId,
            dueDate = dueDate,
            isMit = isMit,
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
        )
        return true
    }

    /** Tick / untick. [Task.done] is what the caller currently sees, so this flips it. */
    suspend fun toggleDone(task: Task) = taskRepository.setDone(task.id, !task.done)

    suspend fun delete(id: Long) = taskRepository.delete(id)
}
