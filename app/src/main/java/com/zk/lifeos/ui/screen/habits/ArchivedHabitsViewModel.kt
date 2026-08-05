package com.zk.lifeos.ui.screen.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.ArchivedHabit
import com.zk.lifeos.service.HabitService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchivedHabitsViewModel(private val habitService: HabitService) : ViewModel() {

    val habits: StateFlow<List<ArchivedHabit>> = habitService.observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(id: Long) = viewModelScope.launch { habitService.restore(id) }

    /** Destroys the habit and every check-in it has — the screen confirms with the count first. */
    fun deletePermanently(id: Long) = viewModelScope.launch { habitService.delete(id) }
}
