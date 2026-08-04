package com.zk.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 习惯 —— something checked off daily. Streaks are derived from [HabitCheckEntity], never stored. */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val emoji: String = "",

    val archived: Boolean = false,

    val sortOrder: Int = 0,

    val createdAt: Long,
)
