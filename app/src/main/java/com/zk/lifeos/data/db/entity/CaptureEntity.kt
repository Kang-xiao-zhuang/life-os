package com.zk.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 快速记录 —— the inbox. Anything typed in one tap lands here with no structure at all;
 * sorting it into a task or a project happens later, on purpose.
 */
@Entity(
    tableName = "captures",
    indices = [Index("processed"), Index("createdAt")],
)
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val text: String,

    /** Triaged into a task/project already — kept for the record instead of being deleted. */
    val processed: Boolean = false,

    val createdAt: Long,
)
