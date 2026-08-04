package com.zk.lifeos.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.DashboardSnapshot
import com.zk.lifeos.service.DashboardService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** The UI observes a single [StateFlow]; it never touches a repository or DAO. */
class DashboardViewModel(dashboardService: DashboardService) : ViewModel() {

    val state: StateFlow<DashboardSnapshot> = dashboardService.observe()
        .stateIn(
            scope = viewModelScope,
            // Keep collecting briefly across config changes instead of restarting the queries.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardSnapshot(today = LocalDate.now()),
        )
}
