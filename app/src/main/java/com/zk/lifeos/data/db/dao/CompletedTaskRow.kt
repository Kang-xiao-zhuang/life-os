package com.zk.lifeos.data.db.dao

/** Query result: a finished task and the moment it was ticked, for grouping by day on export. */
data class CompletedTaskRow(
    val title: String,
    val completedAt: Long,
)
