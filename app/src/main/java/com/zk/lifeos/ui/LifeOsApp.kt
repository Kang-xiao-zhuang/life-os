package com.zk.lifeos.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zk.lifeos.ui.navigation.Routes
import com.zk.lifeos.ui.navigation.TopLevelDestination
import com.zk.lifeos.ui.screen.capture.CaptureScreen
import com.zk.lifeos.ui.screen.dashboard.DashboardScreen
import com.zk.lifeos.ui.screen.habits.HabitsScreen
import com.zk.lifeos.ui.screen.journal.JournalScreen
import com.zk.lifeos.ui.screen.projects.ProjectsScreen
import com.zk.lifeos.ui.screen.settings.SettingsScreen

/**
 * App shell: bottom bar + nav host.
 *
 * The bar is hidden on Settings, which is a detail screen rather than a tab.
 */
@Composable
fun LifeOsApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route != Routes.SETTINGS

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(destination) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.DASHBOARD.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopLevelDestination.DASHBOARD.route) {
                DashboardScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
            }
            composable(TopLevelDestination.PROJECTS.route) { ProjectsScreen() }
            composable(TopLevelDestination.CAPTURE.route) { CaptureScreen() }
            composable(TopLevelDestination.HABITS.route) { HabitsScreen() }
            composable(TopLevelDestination.JOURNAL.route) { JournalScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/**
 * Tab switching keeps a single entry per tab: no back stack pile-up from tapping around,
 * and each tab remembers where it was.
 */
private fun androidx.navigation.NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
