package com.zk.lifeos.model

/**
 * The handful of numbers Dashboard needs. In Phase 1 it exists to prove the
 * UI → Service → Repository → SQLite chain actually runs; Phase 2/3 build the real cards on it.
 */
data class OverviewCounts(
    val activeProjects: Int = 0,
    val openTasks: Int = 0,
    val activeHabits: Int = 0,
    val habitsCheckedToday: Int = 0,
    val inboxItems: Int = 0,
    val journalEntries: Int = 0,
)
