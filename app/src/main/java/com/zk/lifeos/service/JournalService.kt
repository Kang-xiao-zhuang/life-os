package com.zk.lifeos.service

import com.zk.lifeos.data.repository.JournalRepository
import com.zk.lifeos.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 每日复盘. Writing lands in Phase 3. */
class JournalService(private val journalRepository: JournalRepository) {

    fun observeToday(): Flow<JournalEntry> = journalRepository.observeByDate(LocalDate.now())

    fun observeRecent(): Flow<List<JournalEntry>> = journalRepository.observeRecent()
}
