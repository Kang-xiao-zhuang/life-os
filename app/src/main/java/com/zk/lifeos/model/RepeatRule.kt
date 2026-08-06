package com.zk.lifeos.model

import java.time.LocalDate

/**
 * How often a task comes back.
 *
 * Three options, not an RRULE parser. A personal workbench is full of periodic obligations —
 * 每周一交周报, 每月付房租 — and until now they had nowhere to live: habits are streak-based rather
 * than due-date-based, and a plain task had to be retyped every time, so it eventually got missed.
 * 每天 / 每周 / 每月 covers that without turning task creation into a scheduling dialog.
 *
 * Stored as the enum *name* in a nullable column; null means「不重复」.
 */
enum class RepeatRule {
    DAILY,
    WEEKLY,
    MONTHLY;

    /**
     * The next occurrence after [from].
     *
     * Month steps use [LocalDate.plusMonths], which clamps — the 31st of January becomes the 28th of
     * February rather than overflowing into March. Clamping is the behaviour people expect from
     * 「每月」, and it means a monthly task never silently skips a month.
     */
    fun next(from: LocalDate): LocalDate = when (this) {
        DAILY -> from.plusDays(1)
        WEEKLY -> from.plusWeeks(1)
        MONTHLY -> from.plusMonths(1)
    }

    companion object {
        /** Null-safe parse for the stored column; an unknown value degrades to「不重复」. */
        fun fromStored(value: String?): RepeatRule? =
            value?.let { name -> entries.firstOrNull { it.name == name } }
    }
}
