package com.zk.lifeos.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.DashboardSnapshot
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.Task
import com.zk.lifeos.service.DashboardService
import com.zk.lifeos.service.HabitService
import com.zk.lifeos.service.ProjectService
import com.zk.lifeos.service.TaskService
import com.zk.lifeos.ui.components.TaskDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The UI observes state flows and calls these actions; it never touches a repository or DAO.
 * Every write goes through a service, and the DB flows push the result back — no local mirroring.
 */
class DashboardViewModel(
    dashboardService: DashboardService,
    projectService: ProjectService,
    private val taskService: TaskService,
    private val habitService: HabitService,
) : ViewModel() {

    val state: StateFlow<DashboardSnapshot> = dashboardService.observe()
        .stateIn(
            scope = viewModelScope,
            // Keep collecting briefly across config changes instead of restarting the queries.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardSnapshot(today = LocalDate.now()),
        )

    /** Needed by the task editor so a task can be moved between projects from here. */
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
                projectId = draft.projectId,
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

    fun toggleHabit(habitId: Long) = viewModelScope.launch { habitService.toggleToday(habitId) }

    /** 把逾期的挪到今天 — clears the red backlog in one tap instead of task by task. */
    fun rescheduleOverdue() = viewModelScope.launch { taskService.rescheduleOverdueToToday() }
}
