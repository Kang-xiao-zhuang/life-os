package com.zk.lifeos.service

import com.zk.lifeos.data.repository.CaptureRepository
import com.zk.lifeos.model.CaptureItem
import kotlinx.coroutines.flow.Flow

/** 快速记录. Capturing itself lands in Phase 3; this shows what is already in the inbox. */
class CaptureService(private val captureRepository: CaptureRepository) {

    fun observeInbox(): Flow<List<CaptureItem>> = captureRepository.observeInbox()
}
