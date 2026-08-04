package com.zk.lifeos.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zk.lifeos.LifeOsApplication
import com.zk.lifeos.ui.screen.dashboard.DashboardViewModel
import com.zk.lifeos.ui.screen.settings.SettingsViewModel

/**
 * ViewModel construction without a DI framework: each initializer pulls what it needs out of
 * the [com.zk.lifeos.AppContainer] that the Application already built.
 */
private val CreationExtras.container
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LifeOsApplication).container

object LifeOsViewModelFactory {

    val Factory = viewModelFactory {
        initializer { DashboardViewModel(container.overviewService) }
        initializer { SettingsViewModel(container.settingsService) }
    }
}
