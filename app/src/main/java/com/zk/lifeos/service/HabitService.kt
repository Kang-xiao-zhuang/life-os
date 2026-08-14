package com.zk.lifeos.service

import com.zk.lifeos.data.repository.HabitRepository
import com.zk.lifeos.model.ArchivedHabit
import com.zk.lifeos.model.HabitMonth
import com.zk.lifeos.model.HabitToday
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/** 习惯 + 每日打卡. */
class HabitService(private val habitRepository: HabitRepository) {

    /**
     * [today] is a parameter, not a `LocalDate.now()` read in here.
     *
     * It used to be read here, which fixed「今天」at the moment the flow was built. The screen then
     * disagreed with [toggleToday], which reads the clock when you tap: past midnight the list still
     * showed yesterday's check as「今天打过了」, and tapping it — which the UI offers as *un*-checking
     * — wrote a brand new check-in for the day you hadn't done anything on yet.
     * Same defect [DashboardService] had, and the same fix.
     */
    fun observeToday(today: LocalDate = LocalDate.now()): Flow<List<HabitToday>> =
        habitRepository.observeToday(today)

    /** One month of check-ins for the heatmap — 「这个月坚持得怎么样」. */
    fun observeMonth(month: YearMonth): Flow<HabitMonth> = habitRepository.observeMonth(month)

    fun currentMonth(): YearMonth = YearMonth.from(LocalDate.now())

    suspend fun create(name: String, emoji: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        habitRepository.create(clean, emoji.trim())
        return true
    }

    suspend fun rename(id: Long, name: String, emoji: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        habitRepository.rename(id, clean, emoji.trim())
        return true
    }

    /**
     * Retire a habit. This is the normal way to stop tracking something: the streak history stays,
     * and it can come back later. Deleting is reserved for the archive.
     */
    suspend fun archive(id: Long) = habitRepository.setArchived(id, archived = true)

    fun observeArchived(): Flow<List<ArchivedHabit>> = habitRepository.observeArchived()

    fun observeArchivedCount(): Flow<Int> = habitRepository.observeArchivedCount()

    suspend fun restore(id: Long) = habitRepository.setArchived(id, archived = false)

    /**
     * Permanent, and only reachable from the archive: this destroys every check-in the habit ever
     * recorded, so it takes two deliberate steps to get here.
     */
    suspend fun delete(id: Long) = habitRepository.delete(id)

    /** Tap toggles today's check-in, so a mis-tap is undone by tapping again. */
    suspend fun toggleToday(habitId: Long) =
        habitRepository.toggleCheck(habitId, LocalDate.now())
}
