package com.zk.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 任务. Kept deliberately thin — no tags, no sub-tasks.
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

    /**
     * `RepeatRule` name — DAILY / WEEKLY / MONTHLY — or null for a one-off task.
     *
     * A string rather than an Int so the stored value stays readable in a SQLite dump, and so an
     * unknown value from a newer build degrades to「不重复」instead of pointing at the wrong rule.
     */
    val repeatRule: String? = null,

    /** When it was ticked off; null while open. */
    val completedAt: Long? = null,

    val createdAt: Long,
    val updatedAt: Long,
)
