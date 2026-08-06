package com.zk.lifeos.ui.screen.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.RescheduledTask
import com.zk.lifeos.model.Task
import com.zk.lifeos.model.TaskListItem
import com.zk.lifeos.service.ProjectService
import com.zk.lifeos.service.TaskService
import com.zk.lifeos.ui.components.TaskDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AllTasksViewModel(
    private val taskService: TaskService,
    projectService: ProjectService,
) : ViewModel() {

    val tasks: StateFlow<List<TaskListItem>> = taskService.observeAllOpen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mitCount: StateFlow<Int> = taskService.observeOpenMitCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** For the editor's project picker. */
    val projects: StateFlow<List<ProjectSummary>> = projectService.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleTask(task: Task) = viewModelScope.launch { taskService.toggleDone(task) }

    fun saveTask(existing: Task, draft: TaskDraft) = viewModelScope.launch {
        taskService.update(
            id = existing.id,
            title = draft.title,
            notes = draft.notes,
            projectId = draft.projectId,
            dueDate = draft.dueDate,
            isMit = draft.isMit,
            repeatRule = draft.repeatRule,
        )
    }

    fun deleteTask(id: Long) = viewModelScope.launch { taskService.delete(id) }

    /** Reports what moved so the screen can offer an undo — see DashboardViewModel for why. */
    fun rescheduleOverdue(onMoved: (List<RescheduledTask>) -> Unit) = viewModelScope.launch {
        val moved = taskService.rescheduleOverdueToToday()
        if (moved.isNotEmpty()) onMoved(moved)
    }

    fun undoReschedule(moved: List<RescheduledTask>) = viewModelScope.launch {
        taskService.undoReschedule(moved)
    }
}
