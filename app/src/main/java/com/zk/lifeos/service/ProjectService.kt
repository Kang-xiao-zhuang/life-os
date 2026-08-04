package com.zk.lifeos.service

import com.zk.lifeos.data.repository.ProjectRepository
import com.zk.lifeos.data.repository.TaskRepository
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.Task
import kotlinx.coroutines.flow.Flow

/** 项目 + 任务. Read-only for now; 项目管理 / 任务管理 arrive in Phase 3. */
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
) {

    fun observeProjects(): Flow<List<ProjectSummary>> = projectRepository.observeProjects()

    fun observeProjectName(projectId: Long): Flow<String?> =
        projectRepository.observeProjectName(projectId)

    fun observeTasks(projectId: Long): Flow<List<Task>> = taskRepository.observeByProject(projectId)

    /** Tasks that belong to no project — the inbox leftovers. */
    fun observeUnassignedTasks(): Flow<List<Task>> = taskRepository.observeUnassigned()
}
