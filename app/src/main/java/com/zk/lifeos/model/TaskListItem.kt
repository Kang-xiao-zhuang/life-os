package com.zk.lifeos.model

/**
 * A task shown outside its own project, so it has to say where it belongs.
 * [projectName] is null for tasks captured without a project.
 */
data class TaskListItem(
    val task: Task,
    val projectName: String?,
    val projectEmoji: String?,
) {
    /** 「💼 工作」 / 「未归类」 — the label under the title. */
    val projectLabel: String
        get() = when {
            projectName == null -> "未归类"
            projectEmoji.isNullOrEmpty() -> projectName
            else -> "$projectEmoji $projectName"
        }
}
