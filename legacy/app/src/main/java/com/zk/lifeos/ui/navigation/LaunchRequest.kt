package com.zk.lifeos.ui.navigation

/** Where an outside entry point wants the app to land. */
enum class LaunchTarget {
    CAPTURE,
    TODAY,
    REVIEW,
}

/**
 * A request from the widget, the launcher shortcut or a reminder to jump somewhere.
 *
 * [serial] is what makes a *repeat* tap work: two taps on the same notification produce two
 * requests that differ, so the `LaunchedEffect` keyed on this value runs again instead of deciding
 * nothing changed. [serial] 0 means "launched normally, go nowhere in particular".
 */
data class LaunchRequest(
    val target: LaunchTarget = LaunchTarget.TODAY,
    val serial: Int = 0,
) {
    val isPending: Boolean get() = serial > 0
}
