package com.zk.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 任务. Kept deliberately thin — no tags, no sub-tasks, no recurrence (see 开发原则「保持界面简单」).
 *
 * Dates are stored as epoch-day [Int] rather than text, so "due today" is an integer compare.
 * A task may exist without a project (captured straight into the inbox).
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            // Deleting a project keeps its tasks; they fall back to the inbox.
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index("projectId"), Index("dueDate"), Index("done")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val notes: String = "",

    val projectId: Long? = null,

    val done: Boolean = false,

    /** Epoch day; null means no deadline. */
    val dueDate: Int? = null,

    /** 今日最重要任务 (MIT) — at most a couple per day, surfaced at the top of Dashboard. */
    val isMit: Boolean = false,

    /** When it was ticked off; null while open. */
    val completedAt: Long? = null,

    val createdAt: Long,
    val updatedAt: Long,
)
