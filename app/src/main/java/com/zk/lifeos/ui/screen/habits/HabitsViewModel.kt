package com.zk.lifeos.ui.screen.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.HabitToday
import com.zk.lifeos.service.HabitService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HabitsViewModel(habitService: HabitService) : ViewModel() {

    val habits: StateFlow<List<HabitToday>> = habitService.observeToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
