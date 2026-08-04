package com.zk.lifeos.ui.screen.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.HabitToday
import com.zk.lifeos.service.HabitService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitsViewModel(private val habitService: HabitService) : ViewModel() {

    val habits: StateFlow<List<HabitToday>> = habitService.observeToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, emoji: String) = viewModelScope.launch { habitService.create(name, emoji) }

    fun rename(id: Long, name: String, emoji: String) =
        viewModelScope.launch { habitService.rename(id, name, emoji) }

    /** Also removes the habit's check-in history — the UI confirms before calling this. */
    fun delete(id: Long) = viewModelScope.launch { habitService.delete(id) }

    fun toggleToday(habitId: Long) = viewModelScope.launch { habitService.toggleToday(habitId) }
}
