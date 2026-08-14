package com.zk.lifeos.model

import java.time.LocalDate

/**
 * How often a task comes back.
 *
 * Four options, not an RRULE parser. A personal workbench is full of periodic obligations —
 * 每周一交周报, 每月付房租 — and until now they had nowhere to live: habits are streak-based rather
 * than due-date-based, and a plain task had to be retyped every time, so it eventually got missed.
 * 每天 / 每周 / 每月 / 每年 covers that without turning task creation into a scheduling dialog.
 *
 * **[YEARLY] is where most real obligations actually sit** — 车险续费, 体检, 年检, 域名续费, 生日.
 * 每月 can't express them, and a one-off task set twelve months out is exactly the kind of thing
 * that gets forgotten. It was missing from the first three purely by oversight.
 *
 * Stored as the enum *name* in a nullable column; null means「不重复」. Adding a constant therefore
 * needs no migration — but [fromStored] must keep degrading unknown values to「不重复」, because a
 * backup written by a newer build can be imported into an older one.
 */
enum class RepeatRule {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * The next occurrence after [from].
     *
     * Month and year steps use [LocalDate.plusMonths] / [LocalDate.plusYears], which clamp — the
     * 31st of January becomes the 28th of February rather than overflowing into March, and the 29th
     * of February becomes the 28th in a common year. Clamping is the behaviour people expect from
     * 「每月」/「每年」, and it means a repeating task never silently skips a period.
     */
    fun next(from: LocalDate): LocalDate = when (this) {
        DAILY -> from.plusDays(1)
        WEEKLY -> from.plusWeeks(1)
        MONTHLY -> from.plusMonths(1)
        YEARLY -> from.plusYears(1)
    }

    companion object {
        /** Null-safe parse for the stored column; an unknown value degrades to「不重复」. */
        fun fromStored(value: String?): RepeatRule? =
            value?.let { name -> entries.firstOrNull { it.name == name } }
    }
}
