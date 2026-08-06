package com.zk.lifeos.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** The widgets this app offers, so Settings doesn't have to know their provider classes. */
enum class LifeOsWidget {
    /** 记一笔 — one tap to the capture field. */
    CAPTURE,

    /** 今日最重要 — today's one or two things, glanceable. */
    MIT,
}

/**
 * Asks the launcher to place one of the widgets on the home screen.
 *
 * Without this they effectively don't exist: finding one means long-pressing the wallpaper and
 * scrolling a list of every widget on the phone. A button in Settings is the difference between a
 * feature and a feature nobody uses.
 *
 * Needs no permission — the launcher shows its own confirmation.
 */
object WidgetPinning {

    fun isSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context)?.isRequestPinAppWidgetSupported == true

    /** Returns false if the launcher refused or doesn't support pinning. */
    fun requestPin(context: Context, widget: LifeOsWidget): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = when (widget) {
            LifeOsWidget.CAPTURE -> ComponentName(context, CaptureWidgetProvider::class.java)
            LifeOsWidget.MIT -> ComponentName(context, MitWidgetProvider::class.java)
        }
        return runCatching { manager.requestPinAppWidget(provider, null, null) }
            .getOrDefault(false)
    }
}
