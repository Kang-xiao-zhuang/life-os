package com.zk.lifeos.service

import com.zk.lifeos.data.repository.HabitRepository
import com.zk.lifeos.data.repository.JournalRepository
import com.zk.lifeos.data.repository.TaskRepository
import com.zk.lifeos.model.DayCompletions
import com.zk.lifeos.model.ExportDay
import com.zk.lifeos.model.ExportRange
import com.zk.lifeos.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

/** 每日复盘 —— one entry per day, four prompts, Markdown text stored as written. */
class JournalService(
    private val journalRepository: JournalRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
) {

    /** The entry for one particular day — empty rather than absent when nothing was written. */
    fun observeDate(date: LocalDate): Flow<JournalEntry> = journalRepository.observeByDate(date)

    fun observeToday(): Flow<JournalEntry> = observeDate(LocalDate.now())

    fun observeRecent(): Flow<List<JournalEntry>> = journalRepository.observeRecent()

    /**
     * What the app can already answer about [date] — see [DayCompletions].
     *
     * Kept as a flow rather than a one-shot read so the offer stays honest: tick one more thing off
     * and the count on the button follows, instead of quoting a number from when the screen opened.
     */
    fun observeCompletions(date: LocalDate): Flow<DayCompletions> = combine(
        taskRepository.observeCompletedOn(date),
        habitRepository.observeCheckedOn(date),
    ) { tasks, habits -> DayCompletions(taskTitles = tasks, habitNames = habits) }

    suspend fun save(entry: JournalEntry) = journalRepository.save(entry)

    /**
     * The months that actually hold something, newest first — what the export offers to write out.
     *
     * Built from three cheap "which dates exist" queries rather than SQL date arithmetic on epoch
     * days, which SQLite makes unpleasant and which would have to agree with `java.time` about
     * month boundaries. Offering only non-empty months means the picker can't produce a file with
     * one heading and nothing under it.
     */
    suspend fun exportableMonths(): List<YearMonth> {
        val dates = journalRepository.allDates() +
            taskRepository.completedDates() +
            habitRepository.checkedDates()
        return dates.map { YearMonth.from(it) }.distinct().sortedDescending()
    }

    /**
     * Everything worth writing down inside [range], oldest day first.
     *
     * Days with neither a review nor a completion are dropped here rather than in the renderer:
     * "which days had anything" is a question about the data, not about formatting.
     */
    suspend fun collectForExport(range: ExportRange): List<ExportDay> {
        val entries = journalRepository.findBetween(range.from, range.to).associateBy { it.date }
        val tasks = taskRepository.completedByDay(range.from, range.to)
        val habits = habitRepository.checkedByDay(range.from, range.to)

        return (entries.keys + tasks.keys + habits.keys)
            .sorted()
            .map { date ->
                ExportDay(
                    date = date,
                    entry = entries[date],
                    taskTitles = tasks[date].orEmpty(),
                    habitNames = habits[date].orEmpty(),
                )
            }
    }
}
