package com.zk.lifeos.model

import java.time.LocalDate

/** 每日复盘 — the four prompts from the spec, as Markdown text. */
data class JournalEntry(
    val date: LocalDate,
    val done: String = "",
    val win: String = "",
    val problems: String = "",
    val tomorrowMit: String = "",
) {
    val isEmpty: Boolean
        get() = done.isBlank() && win.isBlank() && problems.isBlank() && tomorrowMit.isBlank()
}
