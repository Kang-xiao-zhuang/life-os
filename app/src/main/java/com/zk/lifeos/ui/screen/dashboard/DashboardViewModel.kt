package com.zk.lifeos.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.OverviewCounts
import com.zk.lifeos.service.OverviewService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Dashboard state. The UI observes a single [StateFlow]; it never touches a repository or DAO.
 */
class DashboardViewModel(overviewService: OverviewService) : ViewModel() {

    val counts: StateFlow<OverviewCounts> = overviewService.observeCounts()
        .stateIn(
            scope = viewModelScope,
            // Keep collecting briefly across config changes instead of restarting the query.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverviewCounts(),
        )
}
