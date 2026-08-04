package com.zk.lifeos.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.ThemeMode
import com.zk.lifeos.service.SettingsService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsService: SettingsService) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsService.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.DEFAULT,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsService.setThemeMode(mode) }
    }
}
