package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.JournalDao
import com.zk.lifeos.data.db.entity.JournalEntryEntity
import com.zk.lifeos.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class JournalRepository(private val journalDao: JournalDao) {

    /** Today's entry, or an empty one for [date] when nothing has been written yet. */
    fun observeByDate(date: LocalDate): Flow<JournalEntry> =
        journalDao.observeByDate(date.toEpochDayInt())
            .map { it?.toModel() ?: JournalEntry(date = date) }

    fun observeRecent(limit: Int = 14): Flow<List<JournalEntry>> =
        journalDao.observeRecent(limit).map { list -> list.map { it.toModel() } }

    /**
     * One entry per day. Looked up by date first, then insert or update explicitly — see the
     * note on [JournalDao.findByDate] for why `@Upsert` is not used here.
     *
     * Saving an entry that has been emptied out deletes the row instead of leaving a blank one,
     * so「未写」and「写了但清空了」don't end up looking different.
     */
    suspend fun save(entry: JournalEntry) {
        val day = entry.date.toEpochDayInt()
        val now = System.currentTimeMillis()
        val existing = journalDao.findByDate(day)

        if (entry.isEmpty) {
            if (existing != null) journalDao.delete(existing.id)
            return
        }

        if (existing == null) {
            journalDao.insert(
                JournalEntryEntity(
                    date = day,
                    done = entry.done,
                    win = entry.win,
                    problems = entry.problems,
                    tomorrowMit = entry.tomorrowMit,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } else {
            journalDao.update(
                id = existing.id,
                done = entry.done,
                win = entry.win,
                problems = entry.problems,
                tomorrowMit = entry.tomorrowMit,
                now = now,
            )
        }
    }
}
