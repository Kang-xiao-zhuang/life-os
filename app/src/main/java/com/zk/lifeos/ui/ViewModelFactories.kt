package com.zk.lifeos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zk.lifeos.AppContainer
import com.zk.lifeos.LifeOsApplication
import com.zk.lifeos.ui.screen.capture.CaptureViewModel
import com.zk.lifeos.ui.screen.dashboard.DashboardViewModel
import com.zk.lifeos.ui.screen.habits.HabitsViewModel
import com.zk.lifeos.ui.screen.journal.JournalViewModel
import com.zk.lifeos.ui.screen.projects.ProjectsViewModel
import com.zk.lifeos.ui.screen.settings.SettingsViewModel
import com.zk.lifeos.ui.screen.tasks.AllTasksViewModel

/**
 * ViewModel construction without a DI framework: each initializer pulls what it needs out of
 * the [AppContainer] that the Application already built.
 */
private val CreationExtras.container: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LifeOsApplication).container

object LifeOsViewModelFactory {

    val Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                dashboardService = container.dashboardService,
                projectService = container.projectService,
                taskService = container.taskService,
                habitService = container.habitService,
            )
        }
        initializer { ProjectsViewModel(container.projectService, container.taskService) }
        initializer { AllTasksViewModel(container.taskService, container.projectService) }
        initializer { HabitsViewModel(container.habitService) }
        initializer { JournalViewModel(container.journalService) }
        initializer { CaptureViewModel(container.captureService) }
        initializer { SettingsViewModel(container.settingsService, container.backupService) }
    }
}

/**
 * The app's object graph, for the few places that need a runtime argument the shared factory
 * above cannot express (see project detail, which is scoped to one project id).
 */
@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) { (context.applicationContext as LifeOsApplication).container }
}
