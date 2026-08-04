package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.JournalDao
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
}
