package com.zk.lifeos.service

import com.zk.lifeos.data.repository.CaptureRepository
import com.zk.lifeos.data.repository.HabitRepository
import com.zk.lifeos.data.repository.JournalRepository
import com.zk.lifeos.data.repository.TaskRepository
import com.zk.lifeos.model.DashboardSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Assembles the Dashboard from the pieces each feature owns. This service decides what
 * 「今天」 means so the screen never computes a date itself.
 */
class DashboardService(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val journalRepository: JournalRepository,
    private val captureRepository: CaptureRepository,
) {

    /**
     * [today] is a parameter, not something this function reads from the clock.
     *
     * It used to call `LocalDate.now()` itself, which looked harmless but froze the day at whatever
     * it was when the flow was first built: leave the app resident overnight and 首页 still shows
     * yesterday's date and yesterday's queries. The caller decides which day it wants, and can ask
     * again — see `DashboardViewModel.refreshToday`.
     */
    fun observe(today: LocalDate = LocalDate.now()): Flow<DashboardSnapshot> {
        return combine(
            taskRepository.observeMit(today),
            taskRepository.observeDueBy(today),
            habitRepository.observeToday(today),
            journalRepository.observeByDate(today),
            captureRepository.observeInbox().map { it.size },
        ) { mit, dueToday, habits, journal, inboxCount ->
            val mitIds = mit.mapTo(mutableSetOf()) { it.id }
            DashboardSnapshot(
                today = today,
                mit = mit,
                // A task flagged MIT is already called out above; listing it again in 今日任务
                // would make the same line appear twice on a screen meant to be minimal.
                dueToday = dueToday.filterNot { it.id in mitIds },
                habits = habits,
                journal = journal,
                inboxCount = inboxCount,
            )
        }
    }
}
