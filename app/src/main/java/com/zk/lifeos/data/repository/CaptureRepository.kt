package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.CaptureDao
import com.zk.lifeos.model.CaptureItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CaptureRepository(private val captureDao: CaptureDao) {

    fun observeInbox(): Flow<List<CaptureItem>> =
        captureDao.observeInbox().map { list -> list.map { it.toModel() } }
}
