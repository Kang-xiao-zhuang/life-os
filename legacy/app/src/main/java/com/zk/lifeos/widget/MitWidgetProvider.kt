package com.zk.lifeos.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
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
 * 今日最重要 on the home screen.
 *
 * Unlike [CaptureWidgetProvider] this one carries data, which is a real cost: it has to be kept
 * fresh. It is refreshed by being *pushed to*, never by polling —
 *
 * - [LifeOsApplication] watches the MIT list and calls [refreshAll] when the visible text changes,
 * - the daily reminder alarm refreshes it too, so it rolls over even if the app is never opened,
 * - and `onUpdate` covers being added to the home screen, a reboot, or a locale change.
 *
 * `updatePeriodMillis` stays 0: the system's floor is 30 minutes, which is simultaneously too slow
 * to be correct and too frequent to be free.
 */
/**
 * What the widget needs to say, reduced to the minimum.
 *
 * [anyFlagged] exists to keep two very different empty states apart: nothing chosen yet, versus
 * chosen and finished. Showing 「今天还没定」 to someone who just ticked their last task off would be
 * wrong, and mildly insulting.
 */
data class MitWidgetState(
    val openTitles: List<String> = emptyList(),
    val anyFlagged: Boolean = false,
)

class MitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = context.applicationContext as? LifeOsApplication ?: return
        // Reading the day means touching Room, which onReceive must not block on. The language is
        // read from storage rather than LifeOsApplication.currentLanguage for the same reason as in
        // CaptureWidgetProvider — this broadcast can arrive before the settings Flow has emitted.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                render(context, appWidgetManager, appWidgetIds, app.currentMitState(), app.storedLanguage())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val REQUEST_OPEN_TODAY = 2

        /**
         * Asks this provider to re-render, by broadcasting to itself. No-op when nothing is placed.
         *
         * Same reasoning as [CaptureWidgetProvider.requestUpdate]: [onUpdate] is the single render
         * path, and it fetches what it needs rather than being handed a snapshot that may already be
         * stale by the time the host is ready to accept it.
         */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, MitWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, MitWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            )
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            state: MitWidgetState,
            language: AppLanguage,
        ) {
            val strings = context.localized(language)
            // Two lines, matching TaskService.MIT_SOFT_LIMIT. A widget that tried to show everything
            // would just be a worse task list.
            val shown = state.openTitles.take(2)
            val views = RemoteViews(context.packageName, R.layout.widget_mit).apply {
                setTextViewText(R.id.mit_label, strings.getString(R.string.task_mit))

                setViewVisibility(R.id.mit_empty, if (shown.isEmpty()) View.VISIBLE else View.GONE)
                if (shown.isEmpty()) {
                    setTextViewText(
                        R.id.mit_empty,
                        strings.getString(
                            if (state.anyFlagged) R.string.dash_all_done else R.string.widget_mit_empty
                        ),
                    )
                }

                setViewVisibility(R.id.mit_line1, if (shown.isNotEmpty()) View.VISIBLE else View.GONE)
                shown.getOrNull(0)?.let { setTextViewText(R.id.mit_line1, it) }

                setViewVisibility(R.id.mit_line2, if (shown.size > 1) View.VISIBLE else View.GONE)
                shown.getOrNull(1)?.let { setTextViewText(R.id.mit_line2, it) }

                setContentDescription(R.id.mit_root, strings.getString(R.string.task_mit))
                setOnClickPendingIntent(R.id.mit_root, openTodayIntent(context))
            }
            ids.forEach { id -> manager.updateAppWidget(id, views) }
        }

        private fun openTodayIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = LifeOsIntents.ACTION_OPEN_TODAY
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            return PendingIntent.getActivity(
                context,
                REQUEST_OPEN_TODAY,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
