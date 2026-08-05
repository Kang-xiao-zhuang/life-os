package com.zk.lifeos.service

import com.zk.lifeos.data.repository.JournalRepository
import com.zk.lifeos.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 每日复盘 —— one entry per day, four prompts, Markdown text stored as written. */
class JournalService(private val journalRepository: JournalRepository) {

    /** The entry for one particular day — empty rather than absent when nothing was written. */
    fun observeDate(date: LocalDate): Flow<JournalEntry> = journalRepository.observeByDate(date)

    fun observeToday(): Flow<JournalEntry> = observeDate(LocalDate.now())

    fun observeRecent(): Flow<List<JournalEntry>> = journalRepository.observeRecent()

    suspend fun save(entry: JournalEntry) = journalRepository.save(entry)
}
