package com.zk.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 项目 —— a long-running area of life (工作 / 学习 / 阅读 / 健身 / 自媒体).
 * Tasks hang off a project; a project is never "done", it is archived.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    /** Single emoji shown in lists. Empty means "no icon". */
    val emoji: String = "",

    /** Hidden from pickers and lists, but its tasks and history are kept. */
    val archived: Boolean = false,

    /** Manual ordering; lower comes first. */
    val sortOrder: Int = 0,

    val createdAt: Long,
    val updatedAt: Long,
)
