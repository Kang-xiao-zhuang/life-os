package com.zk.lifeos.ui.screen.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.Task
import com.zk.lifeos.service.ProjectService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProjectsViewModel(projectService: ProjectService) : ViewModel() {

    val projects: StateFlow<List<ProjectSummary>> = projectService.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tasks captured without a project — shown at the bottom so they don't get lost. */
    val unassigned: StateFlow<List<Task>> = projectService.observeUnassignedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
