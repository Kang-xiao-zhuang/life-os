package com.zk.lifeos.ui.screen.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.Task
import com.zk.lifeos.service.ProjectService
import com.zk.lifeos.service.TaskService
import com.zk.lifeos.ui.components.TaskDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Scoped to one project, so the id is a constructor argument rather than screen state. */
class ProjectDetailViewModel(
    projectService: ProjectService,
    private val taskService: TaskService,
    val projectId: Long,
) : ViewModel() {

    val projectName: StateFlow<String?> = projectService.observeProjectName(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tasks: StateFlow<List<Task>> = taskService.observeByProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** For the editor's project picker — a task can be moved out of this project. */
    val projects: StateFlow<List<ProjectSummary>> = projectService.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** So the editor can warn when 今日最重要 is being spread across too many tasks. */
    val mitCount: StateFlow<Int> = taskService.observeOpenMitCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleTask(task: Task) = viewModelScope.launch { taskService.toggleDone(task) }

    fun saveTask(existing: Task?, draft: TaskDraft) = viewModelScope.launch {
        if (existing == null) {
            taskService.create(
                title = draft.title,
                notes = draft.notes,
                // New tasks created here default to this project.
                projectId = draft.projectId ?: projectId,
                dueDate = draft.dueDate,
                isMit = draft.isMit,
            )
        } else {
            taskService.update(
                id = existing.id,
                title = draft.title,
                notes = draft.notes,
                projectId = draft.projectId,
                dueDate = draft.dueDate,
                isMit = draft.isMit,
            )
        }
    }

    fun deleteTask(id: Long) = viewModelScope.launch { taskService.delete(id) }
}
