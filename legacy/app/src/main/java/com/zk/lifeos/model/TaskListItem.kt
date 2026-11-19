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
    /**
     * 「💼 工作」— the label under the title, or null when the task belongs to no project.
     *
     * Null rather than a ready-made "未归类": a model has no access to string resources, so the
     * wording for the empty case belongs to the UI, in whichever language is switched on.
     */
    val projectLabel: String?
        get() = when {
            projectName == null -> null
            projectEmoji.isNullOrEmpty() -> projectName
            else -> "$projectEmoji $projectName"
        }
}
