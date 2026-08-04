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
) {
    fun isOverdue(today: LocalDate): Boolean = !done && dueDate != null && dueDate.isBefore(today)

    fun isDueToday(today: LocalDate): Boolean = !done && dueDate == today
}
