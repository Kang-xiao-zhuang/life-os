package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.ProjectDao
import com.zk.lifeos.data.db.entity.ProjectEntity
import com.zk.lifeos.model.ProjectSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val projectDao: ProjectDao) {

    fun observeProjects(): Flow<List<ProjectSummary>> =
        projectDao.observeActiveWithCounts().map { rows -> rows.map { it.toModel() } }

    /** Name of a single project, for the detail screen's title. Null if it was archived away. */
    fun observeProjectName(projectId: Long): Flow<String?> =
        projectDao.observeById(projectId).map { it?.name }

    suspend fun create(name: String, emoji: String): Long {
        val now = System.currentTimeMillis()
        return projectDao.insert(
            ProjectEntity(
                name = name,
                emoji = emoji,
                sortOrder = projectDao.nextSortOrder(),
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun rename(id: Long, name: String, emoji: String) =
        projectDao.rename(id, name, emoji, System.currentTimeMillis())

    suspend fun setArchived(id: Long, archived: Boolean) =
        projectDao.setArchived(id, archived, System.currentTimeMillis())

    fun observeArchived(): Flow<List<ProjectSummary>> =
        projectDao.observeArchivedWithCounts().map { rows -> rows.map { it.toModel() } }

    fun observeArchivedCount(): Flow<Int> = projectDao.observeArchivedCount()

    /** Permanent. The project's tasks fall back to 未归类 rather than being deleted with it. */
    suspend fun deletePermanently(id: Long) = projectDao.deleteById(id)
}
