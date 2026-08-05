package com.zk.lifeos.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.zk.lifeos.model.ReminderKind
import com.zk.lifeos.model.ReminderSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Arms the daily alarms with `AlarmManager`.
 *
 * `setAndAllowWhileIdle`, deliberately, not `setExactAndAllowWhileIdle`: an exact alarm needs
 * `SCHEDULE_EXACT_ALARM` (and Play treats `USE_EXACT_ALARM` as reserved for alarm-clock apps),
 * which is a lot of permission for a nudge. Inexact still fires in Doze, just possibly a few
 * minutes late — nobody's day depends on 21:30 versus 21:36.
 *
 * There is no repeating alarm here either. A repeating one would keep firing at the old time after
 * the user moved it, and inexact repeats drift; instead every firing schedules the next one, and the
 * app re-arms both on launch and on boot, so a missed schedule always heals.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /** Brings the alarms in line with [settings] — arming what's on, cancelling what's off. */
    fun sync(settings: ReminderSettings) {
        ReminderKind.entries.forEach { kind ->
            if (settings.enabled(kind)) arm(kind, settings.time(kind)) else cancel(kind)
        }
    }

    private fun arm(kind: ReminderKind, time: LocalTime) {
        val manager = alarmManager ?: return
        // Scheduling an alarm must never be the thing that crashes the app: some OEM builds throw
        // when an app has too many alarms, and a missed reminder is not worth a crash.
        runCatching {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextOccurrence(time),
                pendingIntent(kind, mutable = false),
            )
        }
    }

    private fun cancel(kind: ReminderKind) {
        alarmManager?.cancel(pendingIntent(kind, mutable = false))
    }

    /** Today at [time] if that is still ahead of us, otherwise tomorrow. */
    private fun nextOccurrence(time: LocalTime): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now().atTime(time).atZone(zone)
        val target = if (today.toInstant().toEpochMilli() > System.currentTimeMillis()) {
            today
        } else {
            today.plusDays(1)
        }
        return target.toInstant().toEpochMilli()
    }

    private fun pendingIntent(kind: ReminderKind, mutable: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            // In the extras *and* in the data URI: PendingIntent equality ignores extras, so two
            // reminders sharing an action would otherwise be the same alarm and overwrite each other.
            putExtra(EXTRA_KIND, kind.name)
            data = android.net.Uri.parse("lifeos://reminder/${kind.name}")
        }
        return PendingIntent.getBroadcast(
            context,
            kind.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_FIRE = "com.zk.lifeos.action.REMINDER"
        const val EXTRA_KIND = "kind"
    }
}
