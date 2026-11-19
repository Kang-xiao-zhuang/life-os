package com.zk.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One day's check-in for one habit. The (habitId, date) pair is unique, so double-tapping
 * can never produce two rows for the same day — the DB enforces it rather than the UI.
 */
@Entity(
    tableName = "habit_checks",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            // Deleting a habit removes its history; keeping orphaned check-ins would be meaningless.
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["habitId", "date"], unique = true), Index("date")],
)
data class HabitCheckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val habitId: Long,

    /** Epoch day. */
    val date: Int,

    val createdAt: Long,
)
