package com.zk.lifeos.ui.screen.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.JournalEntry
import com.zk.lifeos.service.JournalService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class JournalViewModel(journalService: JournalService) : ViewModel() {

    val today: StateFlow<JournalEntry> = journalService.observeToday()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            JournalEntry(date = LocalDate.now()),
        )

    val recent: StateFlow<List<JournalEntry>> = journalService.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
