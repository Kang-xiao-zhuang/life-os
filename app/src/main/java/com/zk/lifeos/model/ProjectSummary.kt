package com.zk.lifeos.model

/** A project as the list shows it: identity plus how much is left to do. */
data class ProjectSummary(
    val id: Long,
    val name: String,
    val emoji: String,
    val openTasks: Int,
    val doneTasks: Int,
) {
    val totalTasks: Int get() = openTasks + doneTasks

    /** Null when the project has no tasks yet — there is no progress to speak of. */
    val progress: Float? get() = if (totalTasks == 0) null else doneTasks.toFloat() / totalTasks
}
