package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.CaptureDao
import com.zk.lifeos.data.db.dao.HabitDao
import com.zk.lifeos.data.db.dao.JournalDao
import com.zk.lifeos.data.db.dao.ProjectDao
import com.zk.lifeos.data.db.dao.TaskDao
import com.zk.lifeos.model.OverviewCounts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Reads the Dashboard numbers straight out of SQLite. This is the only layer allowed to touch
 * DAOs — per 开发原则「业务层不得直接操作数据库」.
 */
class OverviewRepository(
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val captureDao: CaptureDao,
    private val journalDao: JournalDao,
) {

    /** [today] is an epoch day, passed in rather than read here so the caller controls "now". */
    fun observeCounts(today: Int): Flow<OverviewCounts> = combine(
        projectDao.observeActiveCount(),
        taskDao.observeOpenCount(),
        habitDao.observeActiveCount(),
        habitDao.observeCheckedCount(today),
        captureDao.observeInboxCount(),
        journalDao.observeCount(),
    ) { values ->
        OverviewCounts(
            activeProjects = values[0],
            openTasks = values[1],
            activeHabits = values[2],
            habitsCheckedToday = values[3],
            inboxItems = values[4],
            journalEntries = values[5],
        )
    }
}
