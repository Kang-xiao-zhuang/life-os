package com.zk.lifeos.ui.screen.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.CaptureItem
import com.zk.lifeos.service.CaptureService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CaptureViewModel(private val captureService: CaptureService) : ViewModel() {

    val inbox: StateFlow<List<CaptureItem>> = captureService.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun capture(text: String) = viewModelScope.launch { captureService.capture(text) }

    /**
     * Becomes an unassigned task on purpose: deciding the project here would turn a two-second
     * capture into a filing exercise. Sorting happens later, from the task itself.
     */
    fun convertToTask(item: CaptureItem) = viewModelScope.launch { captureService.convertToTask(item) }

    fun delete(id: Long) = viewModelScope.launch { captureService.delete(id) }
}
