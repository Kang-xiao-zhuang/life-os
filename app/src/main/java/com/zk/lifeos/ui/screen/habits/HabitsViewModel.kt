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

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel(private val habitService: HabitService) : ViewModel() {

    val habits: StateFlow<List<HabitToday>> = habitService.observeToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Which month the heatmap is showing. Starts on this one. */
    private val selectedMonth = MutableStateFlow(habitService.currentMonth())

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

    /** Also removes the habit's check-in history — the UI confirms before calling this. */
    fun delete(id: Long) = viewModelScope.launch { habitService.delete(id) }

    fun toggleToday(habitId: Long) = viewModelScope.launch { habitService.toggleToday(habitId) }
}
