package com.zk.lifeos.data.db.dao

import androidx.room.Embedded
import com.zk.lifeos.data.db.entity.ProjectEntity

/** Query result: a project plus how many of its tasks are open / done. */
data class ProjectWithCounts(
    @Embedded val project: ProjectEntity,
    val openTasks: Int,
    val doneTasks: Int,
)
