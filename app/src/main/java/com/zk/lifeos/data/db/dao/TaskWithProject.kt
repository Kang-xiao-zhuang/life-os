package com.zk.lifeos.data.db.dao

import androidx.room.Embedded
import com.zk.lifeos.data.db.entity.TaskEntity

/** Query result: a task plus which project it sits in (null for unassigned ones). */
data class TaskWithProject(
    @Embedded val task: TaskEntity,
    val projectName: String?,
    val projectEmoji: String?,
)
