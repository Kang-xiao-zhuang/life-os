package com.zk.lifeos.ui.screen.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.HabitMonth
import com.zk.lifeos.model.HabitToday
import com.zk.lifeos.service.HabitService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel(private val habitService: HabitService) : ViewModel() {

    /**
     * Which day the list is about. Re-read when the screen resumes, never fixed at construction —
     * see [HabitService.observeToday] for what a stale value did to the check-in it wrote.
     */
    private val today = MutableStateFlow(LocalDate.now())

    val habits: StateFlow<List<HabitToday>> = today
        .flatMapLatest { habitService.observeToday(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Which month the heatmap is showing. Starts on this one. */
    private val selectedMonth = MutableStateFlow(habitService.currentMonth())

    /**
     * Called when the screen comes back to the foreground; a no-op unless the date really changed.
     *
     * Also drags the heatmap forward when the month rolls over, but only while it is showing the
     * month that *was* current — someone paging back through June must not be yanked to July.
     */
    fun refreshToday() {
        val now = LocalDate.now()
        val previous = today.value
        if (now == previous) return
        today.value = now
        if (selectedMonth.value == YearMonth.from(previous)) {
            selectedMonth.value = YearMonth.from(now)
        }
    }

    /** Re-queries whenever the month changes, so only one month is ever loaded. */
    val month: StateFlow<HabitMonth> = selectedMonth
        .flatMapLatest { habitService.observeMonth(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HabitMonth(month = habitService.currentMonth()),
        )

    fun showPreviousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    /** Never past the current month — there is nothing recorded in the future. */
    fun showNextMonth() {
        val next = selectedMonth.value.plusMonths(1)
        if (!next.isAfter(habitService.currentMonth())) selectedMonth.value = next
    }

    fun create(name: String, emoji: String) = viewModelScope.launch { habitService.create(name, emoji) }

    fun rename(id: Long, name: String, emoji: String) =
        viewModelScope.launch { habitService.rename(id, name, emoji) }

    /**
     * The normal way to stop tracking something: history stays, and it can come back.
     * Permanent deletion lives in the archive screen, two steps away.
     */
    fun archive(id: Long) = viewModelScope.launch { habitService.archive(id) }

    /** Drives the 「已归档」 entry; hidden while nothing has been archived. */
    val archivedCount: StateFlow<Int> = habitService.observeArchivedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleToday(habitId: Long) = viewModelScope.launch { habitService.toggleToday(habitId) }
}
