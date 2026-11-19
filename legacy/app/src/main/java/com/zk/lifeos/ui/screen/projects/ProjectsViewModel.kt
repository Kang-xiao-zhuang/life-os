package com.zk.lifeos.ui.screen.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.Task
import com.zk.lifeos.service.ProjectService
import com.zk.lifeos.service.TaskService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val projectService: ProjectService,
    private val taskService: TaskService,
) : ViewModel() {

    val projects: StateFlow<List<ProjectSummary>> = projectService.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tasks captured without a project — shown at the bottom so they don't get lost. */
    val unassigned: StateFlow<List<Task>> = taskService.observeUnassigned()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Drives the 「所有待办」 entry; hidden when there is nothing open. */
    val openTaskCount: StateFlow<Int> = taskService.observeAllOpen()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Drives the 「已归档」 entry; hidden while nothing has been archived. */
    val archivedCount: StateFlow<Int> = projectService.observeArchivedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun createProject(name: String, emoji: String) =
        viewModelScope.launch { projectService.create(name, emoji) }

    fun renameProject(id: Long, name: String, emoji: String) =
        viewModelScope.launch { projectService.rename(id, name, emoji) }

    /** Archive, not delete: the project's tasks and history survive. */
    fun archiveProject(id: Long) = viewModelScope.launch { projectService.archive(id) }

    fun toggleTask(task: Task) = viewModelScope.launch { taskService.toggleDone(task) }
}
