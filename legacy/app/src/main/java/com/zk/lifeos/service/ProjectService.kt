package com.zk.lifeos.service

import com.zk.lifeos.data.repository.ProjectRepository
import com.zk.lifeos.model.ProjectSummary
import kotlinx.coroutines.flow.Flow

/** 项目管理. Projects are archived, never deleted — their tasks and history stay. */
class ProjectService(private val projectRepository: ProjectRepository) {

    fun observeProjects(): Flow<List<ProjectSummary>> = projectRepository.observeProjects()

    fun observeProjectName(projectId: Long): Flow<String?> =
        projectRepository.observeProjectName(projectId)

    /** Returns false when the name is blank — nothing is created from an empty field. */
    suspend fun create(name: String, emoji: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        projectRepository.create(clean, emoji.trim())
        return true
    }

    suspend fun rename(id: Long, name: String, emoji: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        projectRepository.rename(id, clean, emoji.trim())
        return true
    }

    suspend fun archive(id: Long) = projectRepository.setArchived(id, archived = true)

    // ---- 已归档 ----

    fun observeArchived(): Flow<List<ProjectSummary>> = projectRepository.observeArchived()

    fun observeArchivedCount(): Flow<Int> = projectRepository.observeArchivedCount()

    suspend fun restore(id: Long) = projectRepository.setArchived(id, archived = false)

    /**
     * The only permanent delete for projects, and only reachable from the archive — so losing a
     * project always takes two deliberate steps. Its tasks survive as 未归类.
     */
    suspend fun deletePermanently(id: Long) = projectRepository.deletePermanently(id)
}
