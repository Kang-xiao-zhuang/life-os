package com.zk.lifeos.service

import com.zk.lifeos.data.repository.SettingsRepository
import com.zk.lifeos.model.ReminderKind
import com.zk.lifeos.model.ReminderSettings
import com.zk.lifeos.notify.ReminderNotifier
import com.zk.lifeos.notify.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * 提醒 — the business layer for the two daily nudges.
 *
 * It decides *when* (delegating the alarm plumbing to [ReminderScheduler]) and hands today's state
 * to [ReminderNotifier], which decides what words to use and whether the day is even worth
 * interrupting. This service never builds user-facing text: it has no resources.
 */
class ReminderService(
    private val settingsRepository: SettingsRepository,
    private val dashboardService: DashboardService,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
) {

    val settings: Flow<ReminderSettings> = settingsRepository.reminders

    suspend fun setMorning(enabled: Boolean, time: LocalTime) =
        settingsRepository.setMorningReminder(enabled, time)

    suspend fun setEvening(enabled: Boolean, time: LocalTime) =
        settingsRepository.setEveningReminder(enabled, time)

    /** Brings the alarms in line with what's stored. Safe to call as often as you like. */
    suspend fun sync() = scheduler.sync(settings.first())

    /**
     * An alarm went off. Post the reminder if today has anything outstanding, then arm tomorrow's.
     *
     * Re-arming happens **after** the notification and unconditionally, so a quiet day (nothing to
     * report, nothing posted) still schedules the next one instead of ending the chain.
     */
    suspend fun fire(kind: ReminderKind) {
        val stored = settings.first()
        // The alarm may outlive the setting — the user can switch a reminder off while its alarm is
        // already pending, and a cancel can lose the race.
        if (stored.enabled(kind)) {
            // LocalDate.now() read here, at firing time — the alarm may have been armed yesterday.
            notifier.post(
                kind = kind,
                snapshot = dashboardService.observe(LocalDate.now()).first(),
                language = settingsRepository.language.first(),
            )
        }
        scheduler.sync(stored)
    }
}
