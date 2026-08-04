package com.zk.lifeos.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Asks the launcher to place the capture widget on the home screen.
 *
 * Without this the widget effectively doesn't exist: finding it means long-pressing the wallpaper
 * and scrolling a list of every widget on the phone. A button in Settings is the difference
 * between a feature and a feature nobody uses.
 *
 * Needs no permission — the launcher shows its own confirmation.
 */
object WidgetPinning {

    fun isSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context)?.isRequestPinAppWidgetSupported == true

    /** Returns false if the launcher refused or doesn't support pinning. */
    fun requestPin(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, CaptureWidgetProvider::class.java)
        return runCatching { manager.requestPinAppWidget(provider, null, null) }
            .getOrDefault(false)
    }
}
