package com.zk.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zk.lifeos.data.db.entity.HabitCheckEntity
import com.zk.lifeos.data.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT COUNT(*) FROM habits WHERE archived = 0")
    fun observeActiveCount(): Flow<Int>

    /** How many active habits are already checked for [date] (epoch day). */
    @Query(
        """
        SELECT COUNT(*) FROM habit_checks
        JOIN habits ON habits.id = habit_checks.habitId
        WHERE habit_checks.date = :date AND habits.archived = 0
        """
    )
    fun observeCheckedCount(date: Int): Flow<Int>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    /** The (habitId, date) unique index makes a repeated tap a no-op instead of a duplicate. */
    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: HabitCheckEntity): Long
}
