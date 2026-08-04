package com.zk.lifeos.ui.screen.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.JournalEntry
import com.zk.lifeos.service.JournalService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Editing state lives here rather than in the composable.
 *
 * The reason is a real trap: the entry arrives from the database a moment after the screen opens,
 * so a `remember`-ed local copy would be seeded from the empty placeholder and then never
 * updated — the user would see blank fields over saved text. Here the draft is filled from the
 * first DB emission and afterwards only the user changes it.
 */
class JournalViewModel(private val journalService: JournalService) : ViewModel() {

    private val _draft = MutableStateFlow(JournalEntry(date = LocalDate.now()))
    val draft: StateFlow<JournalEntry> = _draft.asStateFlow()

    private val _dirty = MutableStateFlow(false)
    val dirty: StateFlow<Boolean> = _dirty.asStateFlow()

    val recent: StateFlow<List<JournalEntry>> = journalService.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            journalService.observeToday().collect { stored ->
                // Never overwrite what the user is in the middle of typing.
                if (!_dirty.value) _draft.value = stored
            }
        }
    }

    fun setDone(value: String) = edit { it.copy(done = value) }
    fun setWin(value: String) = edit { it.copy(win = value) }
    fun setProblems(value: String) = edit { it.copy(problems = value) }
    fun setTomorrowMit(value: String) = edit { it.copy(tomorrowMit = value) }

    private inline fun edit(transform: (JournalEntry) -> JournalEntry) {
        _draft.update(transform)
        _dirty.value = true
    }

    fun save() {
        viewModelScope.launch {
            journalService.save(_draft.value)
            // Hand control back to the DB flow now that it matches what's on screen.
            _dirty.value = false
        }
    }
}
