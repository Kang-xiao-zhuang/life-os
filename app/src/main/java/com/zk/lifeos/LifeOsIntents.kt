package com.zk.lifeos

/**
 * Entry points into the app from outside it — the home-screen widget, the launcher shortcut, and
 * the two daily reminders.
 *
 * They share one idea: land on the thing the user came for, not on wherever the app happened to be.
 * A thought you have to navigate to record is a thought you lose, and a reminder that drops you on
 * the wrong screen is a reminder you dismiss.
 */
object LifeOsIntents {
    const val ACTION_QUICK_CAPTURE = "com.zk.lifeos.action.QUICK_CAPTURE"

    /** Morning reminder → 首页. */
    const val ACTION_OPEN_TODAY = "com.zk.lifeos.action.OPEN_TODAY"

    /** Evening reminder → 复盘, where both of the things it nags about get closed out. */
    const val ACTION_OPEN_REVIEW = "com.zk.lifeos.action.OPEN_REVIEW"
}
