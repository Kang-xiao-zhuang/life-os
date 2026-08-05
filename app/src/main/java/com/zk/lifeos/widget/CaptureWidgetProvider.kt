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
        // A provider always runs in the app's own process, so the Application is available and
        // already knows the language — no blocking read of DataStore on the main thread.
        val language = (context.applicationContext as? LifeOsApplication)?.currentLanguage
            ?: AppLanguage.DEFAULT
        render(context, appWidgetManager, appWidgetIds, language)
    }

    companion object {
        private const val REQUEST_CAPTURE = 1

        /**
         * Re-renders every placed instance. Called when the language changes; a no-op when the user
         * has no widget on their home screen.
         */
        fun refreshAll(context: Context, language: AppLanguage) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, CaptureWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            render(context, manager, ids, language)
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
