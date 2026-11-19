package com.zk.lifeos.service

import com.zk.lifeos.data.repository.CaptureRepository
import com.zk.lifeos.data.repository.TaskRepository
import com.zk.lifeos.model.CaptureItem
import kotlinx.coroutines.flow.Flow

/**
 * 快速记录 —— the inbox. Capturing must stay one field and one tap: the moment it asks which
 * project or when it's due, it stops being quick and the thought is gone.
 */
class CaptureService(
    private val captureRepository: CaptureRepository,
    private val taskRepository: TaskRepository,
) {

    fun observeInbox(): Flow<List<CaptureItem>> = captureRepository.observeInbox()

    /** Returns false for blank input, so an accidental tap records nothing. */
    suspend fun capture(text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty()) return false
        captureRepository.capture(clean)
        return true
    }

    /**
     * Triage: turn a captured line into a task. The capture is marked processed rather than
     * deleted, so the inbox stays an honest record of what was thought and when.
     */
    suspend fun convertToTask(item: CaptureItem, projectId: Long? = null) {
        taskRepository.create(title = item.text, projectId = projectId)
        captureRepository.markProcessed(item.id)
    }

    suspend fun delete(id: Long) = captureRepository.delete(id)
}
