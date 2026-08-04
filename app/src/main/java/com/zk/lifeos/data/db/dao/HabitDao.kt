package com.zk.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    /**
     * All check-ins from [from] onwards, in one query. Streaks and the week grid are computed
     * from this in the repository — never stored, so they can't drift out of date.
     */
    @Query("SELECT * FROM habit_checks WHERE date >= :from ORDER BY date ASC")
    fun observeChecksSince(from: Int): Flow<List<HabitCheckEntity>>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    /** The (habitId, date) unique index makes a repeated tap a no-op instead of a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: HabitCheckEntity): Long
}
