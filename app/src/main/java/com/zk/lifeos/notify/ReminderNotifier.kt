package com.zk.lifeos.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zk.lifeos.LifeOsIntents
import com.zk.lifeos.MainActivity
import com.zk.lifeos.R
import com.zk.lifeos.localized
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.model.DashboardSnapshot
import com.zk.lifeos.model.ReminderKind

/**
 * Turns today's state into the one line a reminder is allowed to be.
 *
 * The rule that makes reminders survivable: **if there is nothing to say, say nothing.** A
 * notification that arrives every evening to report that everything is already done is exactly why
 * people turn notifications off, and it would take the useful ones with it. [post] returns without
 * notifying whenever the day has nothing outstanding.
 *
 * This is the notification's "view" layer — it owns the wording, which is why it lives here and not
 * in a service. Services have no resources and would be stuck in one language.
 */
class ReminderNotifier(private val context: Context) {

    /**
     * Posts the reminder for [kind], or nothing at all if [snapshot] holds nothing worth saying.
     *
     * [language] is passed in rather than read from `LifeOsApplication.currentLanguage`: an alarm can
     * fire in a process that has only just started, where that field is still the default.
     */
    fun post(kind: ReminderKind, snapshot: DashboardSnapshot, language: AppLanguage) {
        val manager = NotificationManagerCompat.from(context)
        // Notifications may be off at the system level (denied on 33+, or switched off later). No
        // point building text nobody will see, and notify() would silently drop it anyway.
        if (!manager.areNotificationsEnabled()) return

        val strings = context.localized(language)
        val parts = when (kind) {
            ReminderKind.MORNING -> morningParts(strings, snapshot)
            ReminderKind.EVENING -> eveningParts(strings, snapshot)
        }
        if (parts.isEmpty()) return

        ensureChannel(strings)

        val title = strings.getString(
            when (kind) {
                ReminderKind.MORNING -> R.string.notif_morning_title
                ReminderKind.EVENING -> R.string.notif_evening_title
            }
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(parts.joinToString(SEPARATOR))
            .setStyle(NotificationCompat.BigTextStyle().bigText(parts.joinToString(SEPARATOR)))
            .setContentIntent(openIntent(kind))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        // Still guarded: on 33+ notify() without the runtime permission is a no-op, not a crash,
        // but a SecurityException from an OEM build must not take the alarm receiver down with it.
        runCatching { manager.notify(kind.notificationId, notification) }
    }

    /** 「最重要 1 件 · 今天到期 3 项 · 逾期 2 项」— only the parts that are actually non-zero. */
    private fun morningParts(strings: Context, snapshot: DashboardSnapshot): List<String> {
        val today = snapshot.today
        val mit = snapshot.mit.count { !it.done }
        val due = snapshot.dueToday.count { !it.done && it.dueDate == today }
        val overdue = snapshot.dueToday.count { it.isOverdue(today) }
        return buildList {
            if (mit > 0) add(strings.getString(R.string.notif_morning_mit, mit))
            if (due > 0) add(strings.getString(R.string.notif_morning_due, due))
            if (overdue > 0) add(strings.getString(R.string.notif_morning_overdue, overdue))
        }
    }

    /** 「2 个习惯还没打卡 · 复盘还没写」— nothing when the day is already closed out. */
    private fun eveningParts(strings: Context, snapshot: DashboardSnapshot): List<String> {
        val unchecked = snapshot.habits.count { !it.checkedToday }
        return buildList {
            if (unchecked > 0) add(strings.getString(R.string.notif_evening_habits, unchecked))
            if (snapshot.journal.isEmpty) add(strings.getString(R.string.notif_evening_journal))
        }
    }

    /**
     * The channel is (re)created on every post so its name follows the in-app language — creating an
     * existing channel only updates the name, and the user's own importance choice is left alone.
     */
    private fun ensureChannel(strings: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                strings.getString(R.string.notif_channel_reminders),
                // DEFAULT, not HIGH: a reminder should show up, not push itself in front of you.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = strings.getString(R.string.notif_channel_reminders_description)
            }
        )
    }

    /** Morning opens 首页, evening opens 复盘 — the screen the reminder is actually about. */
    private fun openIntent(kind: ReminderKind): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = when (kind) {
                ReminderKind.MORNING -> LifeOsIntents.ACTION_OPEN_TODAY
                ReminderKind.EVENING -> LifeOsIntents.ACTION_OPEN_REVIEW
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_BASE + kind.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private val ReminderKind.notificationId: Int get() = 100 + ordinal

    private companion object {
        const val CHANNEL_ID = "reminders"
        const val SEPARATOR = " · "
        const val REQUEST_BASE = 10
    }
}
