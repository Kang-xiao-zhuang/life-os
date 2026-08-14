package com.zk.lifeos.model

/**
 * What the app already knows you finished on one day: the tasks you ticked and the habits you
 * checked.
 *
 * This exists for the evening review. 今天完成了什么 opens as a blank box even though every one of
 * these lines is already in the database, and a blank page is the reason people stop writing
 * reviews. The other three prompts stay hand-written — 收获 / 问题 / 明天最重要 are where a review is
 * actually worth something, and no app can fill those in.
 *
 * Tasks and habits are kept apart rather than pre-merged into one list: the two came from different
 * places and only the UI should decide how they read on screen.
 */
data class DayCompletions(
    val taskTitles: List<String> = emptyList(),
    val habitNames: List<String> = emptyList(),
) {
    /** Tasks first, then habits — the order things are shown in on the Dashboard. */
    val lines: List<String> get() = taskTitles + habitNames

    val count: Int get() = taskTitles.size + habitNames.size

    val isEmpty: Boolean get() = count == 0
}
