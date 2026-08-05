package com.zk.lifeos.data.db.dao

/** Query result: just enough of a task to put its due date back. */
data class TaskDueDate(
    val id: Long,
    val dueDate: Int?,
)
