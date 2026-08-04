package com.zk.lifeos

/**
 * Entry points into the app from outside it — the home-screen widget and the launcher shortcut.
 *
 * Both aim at the same thing: getting from the home screen to a cursor in the capture field
 * without going through the app's navigation. A thought you have to navigate to record is a
 * thought you lose.
 */
object LifeOsIntents {
    const val ACTION_QUICK_CAPTURE = "com.zk.lifeos.action.QUICK_CAPTURE"
}
