package com.zk.lifeos.ui.screen.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zk.lifeos.model.CaptureItem
import com.zk.lifeos.service.CaptureService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CaptureViewModel(captureService: CaptureService) : ViewModel() {

    val inbox: StateFlow<List<CaptureItem>> = captureService.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
