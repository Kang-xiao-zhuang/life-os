package com.zk.lifeos.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.zk.lifeos.LifeOsIntents
import com.zk.lifeos.MainActivity
import com.zk.lifeos.R

/**
 * Home-screen widget: one tap goes straight to the capture field.
 *
 * Deliberately a plain [RemoteViews] button rather than a data-bound widget — it shows no counts,
 * so it never needs updating, never wakes the app, and can't go stale. Written with the platform
 * API instead of Glance to avoid another dependency (开发原则「不引入复杂依赖」).
 */
class CaptureWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_capture).apply {
            setOnClickPendingIntent(R.id.widget_root, capturePendingIntent(context))
        }
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }

    private fun capturePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = LifeOsIntents.ACTION_QUICK_CAPTURE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CAPTURE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val REQUEST_CAPTURE = 1
    }
}
