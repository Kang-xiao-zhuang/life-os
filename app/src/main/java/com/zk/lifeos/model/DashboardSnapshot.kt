package com.zk.lifeos.model

import java.time.LocalDate

/**
 * Everything Dashboard shows, in one value. Assembling it in the service layer keeps the
 * screen from juggling five separate flows and half-loaded states.
 */
data class DashboardSnapshot(
    val today: LocalDate,
    /** 今日最重要任务 */
    val mit: List<Task> = emptyList(),
    /** Due today or overdue. */
    val dueToday: List<Task> = emptyList(),
    val habits: List<HabitToday> = emptyList(),
    val journal: JournalEntry = JournalEntry(date = LocalDate.now()),
    val inboxCount: Int = 0,
) {
    val habitsCheckedToday: Int get() = habits.count { it.checkedToday }

    val hasAnything: Boolean
        get() = mit.isNotEmpty() || dueToday.isNotEmpty() || habits.isNotEmpty() ||
            !journal.isEmpty || inboxCount > 0
}
