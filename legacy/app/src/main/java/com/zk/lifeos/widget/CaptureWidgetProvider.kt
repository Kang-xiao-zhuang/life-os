package com.zk.lifeos.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.zk.lifeos.LifeOsApplication
import com.zk.lifeos.LifeOsIntents
import com.zk.lifeos.MainActivity
import com.zk.lifeos.R
import com.zk.lifeos.localized
import com.zk.lifeos.model.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Home-screen widget: one tap goes straight to the capture field.
 *
 * Deliberately a plain [RemoteViews] button rather than a data-bound widget — it shows no counts,
 * so it never wakes the app and can't go stale. Written with the platform API instead of Glance to
 * avoid another dependency (开发原则「不引入复杂依赖」).
 *
 * The one thing that *does* change is its label, when the in-app language changes: the launcher
 * inflates the layout with the system locale, so the text has to be set explicitly.
 */
class CaptureWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = context.applicationContext as? LifeOsApplication ?: return
        // The persisted language, awaited — not LifeOsApplication.currentLanguage. This broadcast
        // arrives during `install -r` in a process where the settings Flow has not emitted yet, so
        // the volatile field would still say「跟随系统」and the widget would render in the phone's
        // language, overwriting the correct push from the app's own collector.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                render(context, appWidgetManager, appWidgetIds, app.storedLanguage())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CAPTURE = 1

        /**
         * Asks this provider to re-render, by broadcasting to itself.
         *
         * It deliberately does **not** render here. Pushing `RemoteViews` straight from the app was
         * the original design and it was quietly fragile: around a package update the host unbinds
         * and rebinds the widget, so `getAppWidgetIds` could return empty for a moment, the push
         * would no-op, and nothing ever retried — leaving a widget stuck in the previous language
         * until it was removed and added again. Routing everything through [onUpdate] means one
         * render path, and the system delivers the broadcast whether or not the app is alive.
         */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, CaptureWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            // Explicit component, so a non-exported receiver in our own uid still receives it.
            context.sendBroadcast(
                Intent(context, CaptureWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            )
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            language: AppLanguage,
        ) {
            val strings = context.localized(language)
            val views = RemoteViews(context.packageName, R.layout.widget_capture).apply {
                setTextViewText(R.id.widget_label, strings.getString(R.string.widget_capture_label))
                setContentDescription(R.id.widget_root, strings.getString(R.string.widget_capture_label))
                setOnClickPendingIntent(R.id.widget_root, capturePendingIntent(context))
            }
            ids.forEach { id -> manager.updateAppWidget(id, views) }
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
    }
}
