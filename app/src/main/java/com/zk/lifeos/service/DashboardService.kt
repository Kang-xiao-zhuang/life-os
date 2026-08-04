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

    fun observe(): Flow<DashboardSnapshot> {
        val today = LocalDate.now()
        return combine(
            taskRepository.observeMit(),
            taskRepository.observeDueBy(today),
            habitRepository.observeToday(today),
            journalRepository.observeByDate(today),
            captureRepository.observeInbox().map { it.size },
        ) { mit, dueToday, habits, journal, inboxCount ->
            DashboardSnapshot(
                today = today,
                mit = mit,
                dueToday = dueToday,
                habits = habits,
                journal = journal,
                inboxCount = inboxCount,
            )
        }
    }
}
