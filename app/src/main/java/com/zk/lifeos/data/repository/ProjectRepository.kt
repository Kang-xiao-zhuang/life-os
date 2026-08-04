package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.ProjectDao
import com.zk.lifeos.model.ProjectSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val projectDao: ProjectDao) {

    fun observeProjects(): Flow<List<ProjectSummary>> =
        projectDao.observeActiveWithCounts().map { rows -> rows.map { it.toModel() } }

    /** Name of a single project, for the detail screen's title. Null if it was deleted. */
    fun observeProjectName(projectId: Long): Flow<String?> =
        projectDao.observeById(projectId).map { it?.name }
}
