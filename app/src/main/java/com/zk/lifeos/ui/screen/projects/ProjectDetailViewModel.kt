package com.zk.lifeos.ui.screen.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.Task
import com.zk.lifeos.service.ProjectService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Scoped to one project, so the id is a constructor argument rather than screen state. */
class ProjectDetailViewModel(
    projectService: ProjectService,
    val projectId: Long,
) : ViewModel() {

    val projectName: StateFlow<String?> = projectService.observeProjectName(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tasks: StateFlow<List<Task>> = projectService.observeTasks(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
