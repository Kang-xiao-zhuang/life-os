package com.zk.lifeos.service

import com.zk.lifeos.data.repository.HabitRepository
import com.zk.lifeos.model.HabitToday
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 习惯. Check-in lands in Phase 3; this only reads. */
class HabitService(private val habitRepository: HabitRepository) {

    fun observeToday(): Flow<List<HabitToday>> = habitRepository.observeToday(LocalDate.now())
}
