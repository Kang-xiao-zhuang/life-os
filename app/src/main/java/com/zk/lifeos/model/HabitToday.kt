package com.zk.lifeos.model

/**
 * A habit with everything the UI needs for one row: today's state, the current streak and
 * this week's pattern. All three are derived from the check-in table, never stored.
 */
data class HabitToday(
    val id: Long,
    val name: String,
    val emoji: String,
    val checkedToday: Boolean,
    /** Consecutive days up to today. Today being unchecked does not break it — the day isn't over. */
    val streak: Int,
    /** Monday → Sunday of the current week; true where a check-in exists. */
    val week: List<Boolean>,
) {
    val weekDoneCount: Int get() = week.count { it }
}
