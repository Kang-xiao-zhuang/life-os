package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.HabitDao
import com.zk.lifeos.model.HabitToday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class HabitRepository(private val habitDao: HabitDao) {

    /**
     * Habits with today's state, the current streak and this week's pattern.
     *
     * Streaks and the week grid are computed here from the raw check-ins rather than stored,
     * so they can never go stale. One query covers both: everything since the earliest date
     * either calculation could need.
     */
    fun observeToday(today: LocalDate): Flow<List<HabitToday>> {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        // A streak can only be as long as the window we load, so cap it at a year — far beyond
        // any streak worth displaying, and still a single small query.
        val since = minOf(weekStart, today.minusDays(365))

        return combine(
            habitDao.observeActive(),
            habitDao.observeChecksSince(since.toEpochDayInt()),
        ) { habits, checks ->
            val byHabit: Map<Long, Set<Int>> = checks
                .groupBy { it.habitId }
                .mapValues { (_, rows) -> rows.map { it.date }.toSet() }

            habits.map { habit ->
                val dates = byHabit[habit.id].orEmpty()
                HabitToday(
                    id = habit.id,
                    name = habit.name,
                    emoji = habit.emoji,
                    checkedToday = today.toEpochDayInt() in dates,
                    streak = streakOf(dates, today),
                    week = (0..6).map { offset -> weekStart.plusDays(offset.toLong()).toEpochDayInt() in dates },
                )
            }
        }
    }

    /**
     * Consecutive days ending today. If today is not checked yet the streak is measured up to
     * yesterday — the day isn't over, so a pending check-in shouldn't read as a broken streak.
     */
    private fun streakOf(dates: Set<Int>, today: LocalDate): Int {
        var cursor = if (today.toEpochDayInt() in dates) today else today.minusDays(1)
        var count = 0
        while (cursor.toEpochDayInt() in dates) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }
}
