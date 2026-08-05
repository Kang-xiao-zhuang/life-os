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

    /**
     * Check-ins per day over a range, counting only active habits — backs the month heatmap.
     * Grouped in SQL so a month costs one small query instead of loading every row.
     */
    @Query(
        """
        SELECT habit_checks.date AS date, COUNT(*) AS count
        FROM habit_checks
        JOIN habits ON habits.id = habit_checks.habitId
        WHERE habits.archived = 0 AND habit_checks.date BETWEEN :from AND :to
        GROUP BY habit_checks.date
        """
    )
    fun observeDailyCounts(from: Int, to: Int): Flow<List<DayCheckCount>>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Query("UPDATE habits SET name = :name, emoji = :emoji WHERE id = :id")
    suspend fun rename(id: Long, name: String, emoji: String)

    /**
     * Retire a habit without destroying it. The `archived` column existed from the start but
     * nothing ever wrote to it, so the only way to stop tracking something was to delete it —
     * taking every check-in with it. A season of 阅读 is worth more than the row it lives in.
     */
    @Query("UPDATE habits SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    /** Archived habits plus how much history each is holding, so 彻底删除 can say what it costs. */
    @Query(
        """
        SELECT h.*, (SELECT COUNT(*) FROM habit_checks c WHERE c.habitId = h.id) AS checkCount
        FROM habits h
        WHERE h.archived = 1
        ORDER BY h.sortOrder ASC, h.id ASC
        """
    )
    fun observeArchivedWithCounts(): Flow<List<HabitWithCheckCount>>

    @Query("SELECT COUNT(*) FROM habits WHERE archived = 1")
    fun observeArchivedCount(): Flow<Int>

    /** Deleting a habit cascades its check-ins (declared on [HabitCheckEntity]). */
    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM habits")
    suspend fun nextSortOrder(): Int

    /** The (habitId, date) unique index makes a repeated tap a no-op instead of a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: HabitCheckEntity): Long

    /** Un-checking: tapping a checked habit should take it back off, not add a second row. */
    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCheck(habitId: Long, date: Int)

    @Query("SELECT COUNT(*) FROM habit_checks WHERE habitId = :habitId AND date = :date")
    suspend fun isChecked(habitId: Long, date: Int): Int

    // ---- backup / restore ----

    @Query("SELECT * FROM habits")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habit_checks")
    suspend fun getAllChecks(): List<HabitCheckEntity>

    @Insert
    suspend fun insertAll(habits: List<HabitEntity>)

    @Insert
    suspend fun insertAllChecks(checks: List<HabitCheckEntity>)

    /** Checks first, then habits — the other order trips the foreign key. */
    @Query("DELETE FROM habit_checks")
    suspend fun deleteAllChecks()

    @Query("DELETE FROM habits")
    suspend fun deleteAll()
}
