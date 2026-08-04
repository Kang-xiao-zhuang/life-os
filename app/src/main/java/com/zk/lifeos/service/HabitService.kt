package com.zk.lifeos.service

import com.zk.lifeos.data.repository.HabitRepository
import com.zk.lifeos.model.HabitToday
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 习惯 + 每日打卡. */
class HabitService(private val habitRepository: HabitRepository) {

    fun observeToday(): Flow<List<HabitToday>> = habitRepository.observeToday(LocalDate.now())

    suspend fun create(name: String, emoji: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        habitRepository.create(clean, emoji.trim())
        return true
    }

    suspend fun rename(id: Long, name: String, emoji: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        habitRepository.rename(id, clean, emoji.trim())
        return true
    }

    /**
     * Deleting a habit removes its whole check-in history — that is why the UI confirms first.
     * There is no archive for habits: an abandoned habit is noise, unlike a project.
     */
    suspend fun delete(id: Long) = habitRepository.delete(id)

    /** Tap toggles today's check-in, so a mis-tap is undone by tapping again. */
    suspend fun toggleToday(habitId: Long) =
        habitRepository.toggleCheck(habitId, LocalDate.now())
}
