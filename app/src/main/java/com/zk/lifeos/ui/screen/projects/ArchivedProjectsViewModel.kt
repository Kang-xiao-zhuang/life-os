package com.zk.lifeos.ui.screen.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.service.ProjectService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchivedProjectsViewModel(private val projectService: ProjectService) : ViewModel() {

    val projects: StateFlow<List<ProjectSummary>> = projectService.observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(id: Long) = viewModelScope.launch { projectService.restore(id) }

    fun deletePermanently(id: Long) = viewModelScope.launch { projectService.deletePermanently(id) }
}
