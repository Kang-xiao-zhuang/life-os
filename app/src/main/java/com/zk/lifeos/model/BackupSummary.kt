package com.zk.lifeos.model

/** How much a backup contains — shown after an export or import so the number is verifiable. */
data class BackupCounts(
    val projects: Int = 0,
    val tasks: Int = 0,
    val habits: Int = 0,
    val habitChecks: Int = 0,
    val captures: Int = 0,
    val journalEntries: Int = 0,
) {
    val total: Int get() = projects + tasks + habits + habitChecks + captures + journalEntries

    /** 「4 项目 · 11 任务 · 4 习惯 · 20 打卡 · 3 记录 · 2 复盘」 */
    fun describe(): String = listOf(
        "$projects 项目",
        "$tasks 任务",
        "$habits 习惯",
        "$habitChecks 打卡",
        "$captures 记录",
        "$journalEntries 复盘",
    ).joinToString(" · ")
}

/**
 * Outcome of an export or import. A failure carries a message meant for the user, not a stack
 * trace — this is a personal app and the only person who can act on it is the one holding it.
 */
sealed interface BackupResult {
    data class Success(val counts: BackupCounts) : BackupResult
    data class Failure(val message: String) : BackupResult
}
