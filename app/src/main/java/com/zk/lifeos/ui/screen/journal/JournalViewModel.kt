package com.zk.lifeos.ui.screen.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.JournalEntry
import com.zk.lifeos.service.JournalService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
 *
 * The editor is pointed at [selectedDate], not hard-wired to today: a review you can only write
 * before midnight is a review you skip on the evenings that were actually worth writing about.
 */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
class JournalViewModel(private val journalService: JournalService) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _draft = MutableStateFlow(JournalEntry(date = LocalDate.now()))
    val draft: StateFlow<JournalEntry> = _draft.asStateFlow()

    private val _dirty = MutableStateFlow(false)
    val dirty: StateFlow<Boolean> = _dirty.asStateFlow()

    val recent: StateFlow<List<JournalEntry>> = journalService.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _selectedDate
                .flatMapLatest { date -> journalService.observeDate(date) }
                .collect { stored ->
                    // Two guards, both load-bearing: never overwrite what the user is typing, and
                    // never accept a late emission from the day we just navigated away from.
                    if (!_dirty.value && stored.date == _selectedDate.value) _draft.value = stored
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

    /**
     * Point the editor at another day.
     *
     * Unsaved text on the day being left is written out first rather than dropped — switching days
     * is navigation, and no navigation in this app is allowed to destroy something the user typed.
     * Future days are refused: there is nothing to review yet.
     */
    fun selectDate(date: LocalDate) {
        if (date == _selectedDate.value || date.isAfter(LocalDate.now())) return
        viewModelScope.launch {
            if (_dirty.value) journalService.save(_draft.value)
            _dirty.value = false
            // Blank placeholder first, so the day being left can't linger in the fields while the
            // new day's row is still on its way out of the database.
            _draft.value = JournalEntry(date = date)
            _selectedDate.value = date
        }
    }

    fun selectToday() = selectDate(LocalDate.now())

    fun previousDay() = selectDate(_selectedDate.value.minusDays(1))

    fun nextDay() = selectDate(_selectedDate.value.plusDays(1))
}
