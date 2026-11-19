package com.zk.lifeos.model

import java.time.LocalDate

/**
 * Everything Dashboard shows, in one value. Assembling it in the service layer keeps the
 * screen from juggling five separate flows and half-loaded states.
 */
data class DashboardSnapshot(
    val today: LocalDate,
    /** 今日最重要任务 — open ones first, then any finished today. */
    val mit: List<Task> = emptyList(),
    /** Due today or overdue, plus anything finished today. */
    val dueToday: List<Task> = emptyList(),
    val habits: List<HabitToday> = emptyList(),
    val journal: JournalEntry = JournalEntry(date = LocalDate.now()),
    val inboxCount: Int = 0,
    /**
     * What yesterday's review named as 明天最重要的一件事 — blank if it wasn't written.
     *
     * The loop it closes: you decide the next day's one thing every evening, and until now the app
     * forgot it overnight. You had to remember it yourself and retype it.
     */
    val carriedMit: String = "",
) {
    val habitsCheckedToday: Int get() = habits.count { it.checkedToday }

    val hasAnything: Boolean
        get() = mit.isNotEmpty() || dueToday.isNotEmpty() || habits.isNotEmpty() ||
            !journal.isEmpty || inboxCount > 0
}
