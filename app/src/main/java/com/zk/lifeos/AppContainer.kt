package com.zk.lifeos

import android.content.Context
import com.zk.lifeos.data.db.LifeOsDatabase
import com.zk.lifeos.data.prefs.AppPreferences
import com.zk.lifeos.data.repository.CaptureRepository
import com.zk.lifeos.data.repository.HabitRepository
import com.zk.lifeos.data.repository.JournalRepository
import com.zk.lifeos.data.repository.ProjectRepository
import com.zk.lifeos.data.repository.SettingsRepository
import com.zk.lifeos.data.repository.TaskRepository
import com.zk.lifeos.service.CaptureService
import com.zk.lifeos.service.DashboardService
import com.zk.lifeos.service.HabitService
import com.zk.lifeos.service.JournalService
import com.zk.lifeos.service.ProjectService
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

    private val projectRepository: ProjectRepository by lazy { ProjectRepository(database.projectDao()) }

    private val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao()) }

    private val habitRepository: HabitRepository by lazy { HabitRepository(database.habitDao()) }

    private val journalRepository: JournalRepository by lazy { JournalRepository(database.journalDao()) }

    private val captureRepository: CaptureRepository by lazy { CaptureRepository(database.captureDao()) }

    // ---- services (what the UI is allowed to talk to) ----

    val settingsService: SettingsService by lazy { SettingsService(settingsRepository) }

    val dashboardService: DashboardService by lazy {
        DashboardService(
            taskRepository = taskRepository,
            habitRepository = habitRepository,
            journalRepository = journalRepository,
            captureRepository = captureRepository,
        )
    }

    val projectService: ProjectService by lazy { ProjectService(projectRepository, taskRepository) }

    val habitService: HabitService by lazy { HabitService(habitRepository) }

    val journalService: JournalService by lazy { JournalService(journalRepository) }

    val captureService: CaptureService by lazy { CaptureService(captureRepository) }
}
