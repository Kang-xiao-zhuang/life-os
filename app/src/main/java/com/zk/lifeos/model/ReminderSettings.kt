package com.zk.lifeos.model

import java.time.LocalTime

/** Which of the two daily reminders an alarm or notification belongs to. */
enum class ReminderKind {
    /** 早上看一眼今天 — what today holds. */
    MORNING,

    /** 晚上收个尾 — habits still unchecked, review not written. */
    EVENING,
}

/**
 * The two daily reminders.
 *
 * Two, not one per habit and one per task: 「不增加无意义配置」. A morning glance and an evening
 * wrap-up cover the whole day, and each is a single switch plus a time.
 *
 * Both default to **off**. Reminders are the only thing in this app that interrupts you, so they
 * start as something you turn on, never something you have to discover and turn off.
 */
data class ReminderSettings(
    val morningEnabled: Boolean = false,
    val morningTime: LocalTime = DEFAULT_MORNING,
    val eveningEnabled: Boolean = false,
    val eveningTime: LocalTime = DEFAULT_EVENING,
) {
    fun enabled(kind: ReminderKind): Boolean = when (kind) {
        ReminderKind.MORNING -> morningEnabled
        ReminderKind.EVENING -> eveningEnabled
    }

    fun time(kind: ReminderKind): LocalTime = when (kind) {
        ReminderKind.MORNING -> morningTime
        ReminderKind.EVENING -> eveningTime
    }

    val anyEnabled: Boolean get() = morningEnabled || eveningEnabled

    companion object {
        /** After you're up, before the day has taken its own shape. */
        val DEFAULT_MORNING: LocalTime = LocalTime.of(8, 30)

        /** Late enough that the day is done, early enough that you'll still write something. */
        val DEFAULT_EVENING: LocalTime = LocalTime.of(21, 30)
    }
}
