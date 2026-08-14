package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.HabitDao
import com.zk.lifeos.data.db.entity.HabitCheckEntity
import com.zk.lifeos.data.db.entity.HabitEntity
import com.zk.lifeos.model.ArchivedHabit
import com.zk.lifeos.model.HabitMonth
import com.zk.lifeos.model.HabitToday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
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
     * One month of check-in counts for the heatmap. The denominator is today's active habit
     * count — an approximation, but the alternative (reconstructing how many habits existed on
     * each past day) buys precision nobody looking at a heatmap needs.
     */
    fun observeMonth(month: YearMonth): Flow<HabitMonth> {
        val from = month.atDay(1)
        val to = month.atEndOfMonth()
        return combine(
            habitDao.observeDailyCounts(from.toEpochDayInt(), to.toEpochDayInt()),
            habitDao.observeActiveCount(),
        ) { counts, habitCount ->
            HabitMonth(
                month = month,
                checksByDay = counts.associate { row ->
                    row.date.toLocalDate().dayOfMonth to row.count
                },
                habitCount = habitCount,
            )
        }
    }

    suspend fun create(name: String, emoji: String): Long =
        habitDao.insert(
            HabitEntity(
                name = name,
                emoji = emoji,
                sortOrder = habitDao.nextSortOrder(),
                createdAt = System.currentTimeMillis(),
            )
        )

    suspend fun rename(id: Long, name: String, emoji: String) = habitDao.rename(id, name, emoji)

    /** Retire / bring back a habit. Its check-ins are untouched either way. */
    suspend fun setArchived(id: Long, archived: Boolean) = habitDao.setArchived(id, archived)

    fun observeArchived(): Flow<List<ArchivedHabit>> =
        habitDao.observeArchivedWithCounts().map { rows -> rows.map { it.toModel() } }

    fun observeArchivedCount(): Flow<Int> = habitDao.observeArchivedCount()

    /** Names of the habits checked on [date], for the review's「今天完成了什么」. */
    fun observeCheckedOn(date: LocalDate): Flow<List<String>> =
        habitDao.observeCheckedNames(date.toEpochDayInt())

    /** Habit check-ins between [from] and [to] inclusive, grouped by day. */
    suspend fun checkedByDay(from: LocalDate, to: LocalDate): Map<LocalDate, List<String>> =
        habitDao.findCheckedNamesBetween(from.toEpochDayInt(), to.toEpochDayInt())
            .groupBy { it.date.toLocalDate() }
            .mapValues { (_, rows) -> rows.map { it.name } }

    /** The days on which anything was ever checked. */
    suspend fun checkedDates(): List<LocalDate> =
        habitDao.findAllCheckDates().map { it.toLocalDate() }

    /** Deleting a habit also removes its check-ins (foreign key cascade). */
    suspend fun delete(id: Long) = habitDao.delete(id)

    /**
     * Tapping a habit toggles today: check it, or take the check back off if it was a mistake.
     * Returns the new state so the caller can react without re-querying.
     */
    suspend fun toggleCheck(habitId: Long, date: LocalDate): Boolean {
        val day = date.toEpochDayInt()
        return if (habitDao.isChecked(habitId, day) > 0) {
            habitDao.deleteCheck(habitId, day)
            false
        } else {
            habitDao.insertCheck(
                HabitCheckEntity(habitId = habitId, date = day, createdAt = System.currentTimeMillis())
            )
            true
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
