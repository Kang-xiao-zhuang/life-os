package com.zk.lifeos.model

import java.time.LocalDate

/**
 * A task in domain terms — dates are [LocalDate] here, epoch-day ints only in the database.
 */
data class Task(
    val id: Long,
    val title: String,
    val notes: String,
    val projectId: Long?,
    val done: Boolean,
    val dueDate: LocalDate?,
    val isMit: Boolean,
    /** 每天 / 每周 / 每月, or null for a one-off. */
    val repeatRule: RepeatRule? = null,
) {
    fun isOverdue(today: LocalDate): Boolean = !done && dueDate != null && dueDate.isBefore(today)

    fun isDueToday(today: LocalDate): Boolean = !done && dueDate == today

    /**
     * The date the next occurrence should carry, or null when this task doesn't repeat.
     *
     * Anchored on [dueDate] so a weekly task stays on its weekday even when ticked off a day late;
     * falls back to [today] for a repeating task that never had a date.
     */
    fun nextOccurrence(today: LocalDate): LocalDate? = repeatRule?.next(dueDate ?: today)
}
