package com.zk.lifeos

import android.content.Context
import com.zk.lifeos.data.db.LifeOsDatabase
import com.zk.lifeos.data.prefs.AppPreferences
import com.zk.lifeos.data.repository.OverviewRepository
import com.zk.lifeos.data.repository.SettingsRepository
import com.zk.lifeos.service.OverviewService
import com.zk.lifeos.service.SettingsService

/**
 * Hand-rolled dependency container — a plain object graph built once at startup.
 *
 * Deliberately not Hilt/Koin: this is a single-module personal app, and 开发原则 says
 * 「不引入复杂依赖」. If the graph ever outgrows one screen of code, revisit that.
 *
 * Everything is lazy so nothing (including opening the database) happens until first use,
 * which keeps cold start fast.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: LifeOsDatabase by lazy { LifeOsDatabase.build(appContext) }

    private val preferences: AppPreferences by lazy { AppPreferences(appContext) }

    // ---- repositories (the only layer that touches DAOs / DataStore) ----

    private val settingsRepository: SettingsRepository by lazy { SettingsRepository(preferences) }

    private val overviewRepository: OverviewRepository by lazy {
        OverviewRepository(
            projectDao = database.projectDao(),
            taskDao = database.taskDao(),
            habitDao = database.habitDao(),
            captureDao = database.captureDao(),
            journalDao = database.journalDao(),
        )
    }

    // ---- services (what the UI is allowed to talk to) ----

    val settingsService: SettingsService by lazy { SettingsService(settingsRepository) }

    val overviewService: OverviewService by lazy { OverviewService(overviewRepository) }
}
