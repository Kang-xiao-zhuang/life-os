package com.zk.lifeos.service

import com.zk.lifeos.data.repository.OverviewRepository
import com.zk.lifeos.model.OverviewCounts
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Supplies Dashboard with its numbers, and owns the notion of "today" so the UI and the
 * repository never each decide it separately.
 */
class OverviewService(private val repository: OverviewRepository) {

    fun observeCounts(): Flow<OverviewCounts> =
        repository.observeCounts(today = todayEpochDay())

    /** Local calendar day, as an epoch day — the form every date column uses. */
    fun todayEpochDay(): Int = LocalDate.now().toEpochDay().toInt()
}
