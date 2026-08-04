package com.zk.lifeos.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * One month of habit check-ins, for the heatmap.
 *
 * [checksByDay] is keyed by day-of-month and holds how many habits were checked that day;
 * [habitCount] is the denominator, so a day can be shaded by how complete it was rather than
 * by a raw count that means nothing on its own.
 */
data class HabitMonth(
    val month: YearMonth,
    val checksByDay: Map<Int, Int> = emptyMap(),
    val habitCount: Int = 0,
) {
    /** 0f..1f — how much of that day was done. Null when there is nothing to measure against. */
    fun completion(day: Int): Float? {
        if (habitCount <= 0) return null
        val done = checksByDay[day] ?: return 0f
        return (done.toFloat() / habitCount).coerceIn(0f, 1f)
    }

    /** Days with at least one check-in. */
    val activeDays: Int get() = checksByDay.count { it.value > 0 }

    /** Days where every active habit was checked. */
    val perfectDays: Int
        get() = if (habitCount <= 0) 0 else checksByDay.count { it.value >= habitCount }

    val isCurrentMonth: Boolean get() = month == YearMonth.from(LocalDate.now())
}
