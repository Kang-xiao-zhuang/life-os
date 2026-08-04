package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.TaskDao
import com.zk.lifeos.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TaskRepository(private val taskDao: TaskDao) {

    /** 今日最重要任务. */
    fun observeMit(): Flow<List<Task>> =
        taskDao.observeMit().map { list -> list.map { it.toModel() } }

    /** Due today or overdue. */
    fun observeDueBy(date: LocalDate): Flow<List<Task>> =
        taskDao.observeDueBy(date.toEpochDayInt()).map { list -> list.map { it.toModel() } }

    fun observeByProject(projectId: Long): Flow<List<Task>> =
        taskDao.observeByProject(projectId).map { list -> list.map { it.toModel() } }

    fun observeUnassigned(): Flow<List<Task>> =
        taskDao.observeUnassigned().map { list -> list.map { it.toModel() } }
}
