package com.zk.lifeos.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.DashboardSnapshot
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.RescheduledTask
import com.zk.lifeos.model.Task
import com.zk.lifeos.service.DashboardService
import com.zk.lifeos.service.HabitService
import com.zk.lifeos.service.ProjectService
import com.zk.lifeos.service.TaskService
import com.zk.lifeos.ui.components.TaskDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The UI observes state flows and calls these actions; it never touches a repository or DAO.
 * Every write goes through a service, and the DB flows push the result back — no local mirroring.
 */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
class DashboardViewModel(
    dashboardService: DashboardService,
    projectService: ProjectService,
    private val taskService: TaskService,
    private val habitService: HabitService,
) : ViewModel() {

    /**
     * Which day 首页 is showing. Re-read when the screen resumes rather than fixed at construction:
     * a phone left on the Dashboard overnight was still showing yesterday, which also made a morning
     * reminder open onto the wrong day.
     */
    private val today = MutableStateFlow(LocalDate.now())

    val state: StateFlow<DashboardSnapshot> = today
        .flatMapLatest { dashboardService.observe(it) }
        .stateIn(
            scope = viewModelScope,
            // Keep collecting briefly across config changes instead of restarting the queries.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardSnapshot(today = LocalDate.now()),
        )

    /** Called when the screen comes back to the foreground; a no-op unless the date really changed. */
    fun refreshToday() {
        today.value = LocalDate.now()
    }

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
                repeatRule = draft.repeatRule,
            )
        } else {
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
    }

    fun deleteTask(id: Long) = viewModelScope.launch { taskService.delete(id) }

    /**
     * Turns yesterday's 明天最重要的一件事 into today's MIT, in one tap.
     *
     * No extra state is kept for it: the suggestion only shows while MIT is empty, so accepting it
     * makes the suggestion disappear on its own.
     */
    fun adoptCarriedMit(title: String) = viewModelScope.launch {
        taskService.create(title = title, dueDate = today.value, isMit = true)
    }

    fun toggleHabit(habitId: Long) = viewModelScope.launch { habitService.toggleToday(habitId) }

    /**
     * 把逾期的挪到今天 — clears the red backlog in one tap instead of task by task.
     *
     * Reports back how many moved so the screen can offer an undo: one tap rewriting a dozen due
     * dates with no way back is not an improvement.
     */
    fun rescheduleOverdue(onMoved: (List<RescheduledTask>) -> Unit) = viewModelScope.launch {
        val moved = taskService.rescheduleOverdueToToday()
        if (moved.isNotEmpty()) onMoved(moved)
    }

    fun undoReschedule(moved: List<RescheduledTask>) = viewModelScope.launch {
        taskService.undoReschedule(moved)
    }
}
