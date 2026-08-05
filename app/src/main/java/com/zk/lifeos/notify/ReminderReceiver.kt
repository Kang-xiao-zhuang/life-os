package com.zk.lifeos.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zk.lifeos.LifeOsApplication
import com.zk.lifeos.model.ReminderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Where a reminder actually happens: an alarm fires, this reads today's state and decides whether
 * there is anything worth interrupting for.
 *
 * Also listens for `BOOT_COMPLETED`, because alarms do not survive a reboot. Without it a phone
 * restarted overnight would quietly stop reminding until the app was next opened by hand — and the
 * whole point of a reminder is that you didn't open the app.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? LifeOsApplication ?: return
        val reminders = app.container.reminderService

        // A receiver's onReceive must not block, and reading the day means touching Room. goAsync()
        // keeps the process alive for the few milliseconds that takes.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> reminders.sync()
                    ReminderScheduler.ACTION_FIRE -> {
                        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND)
                            ?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } }
                        if (kind != null) reminders.fire(kind)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
