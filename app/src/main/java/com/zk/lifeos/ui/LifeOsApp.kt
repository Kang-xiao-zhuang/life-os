package com.zk.lifeos.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zk.lifeos.ui.navigation.Routes
import com.zk.lifeos.ui.navigation.TopLevelDestination
import com.zk.lifeos.ui.screen.capture.CaptureScreen
import com.zk.lifeos.ui.screen.dashboard.DashboardScreen
import com.zk.lifeos.ui.screen.habits.HabitsScreen
import com.zk.lifeos.ui.screen.journal.JournalScreen
import com.zk.lifeos.ui.screen.projects.ProjectDetailScreen
import com.zk.lifeos.ui.screen.projects.ProjectsScreen
import com.zk.lifeos.ui.screen.settings.SettingsScreen

/**
 * App shell: bottom bar + nav host.
 *
 * The bar is hidden on detail screens (Settings, a project's tasks), which are pushed on top of
 * a tab rather than being tabs themselves.
 */
@Composable
fun LifeOsApp(captureRequest: Int = 0) {
    val navController = rememberNavController()
    // Set when the widget/shortcut asked for capture, cleared once the field has taken focus, so
    // coming back to the tab later doesn't pop the keyboard again.
    var autoFocusCapture by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentRoute !in setOf(Routes.SETTINGS, Routes.PROJECT_DETAIL)

    // Jump straight to the capture field when opened from the home screen. Keyed on the counter,
    // so tapping the widget again re-triggers it.
    LaunchedEffect(captureRequest) {
        if (captureRequest > 0) {
            autoFocusCapture = true
            navController.navigateToTab(TopLevelDestination.CAPTURE)
        }
    }

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
                DashboardScreen(
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenCapture = { navController.navigateToTab(TopLevelDestination.CAPTURE) },
                    onOpenJournal = { navController.navigateToTab(TopLevelDestination.JOURNAL) },
                    onOpenHabits = { navController.navigateToTab(TopLevelDestination.HABITS) },
                )
            }
            composable(TopLevelDestination.PROJECTS.route) {
                ProjectsScreen(
                    onOpenProject = { id -> navController.navigate(Routes.projectDetail(id)) },
                )
            }
            composable(TopLevelDestination.CAPTURE.route) {
                CaptureScreen(
                    autoFocus = autoFocusCapture,
                    onAutoFocusConsumed = { autoFocusCapture = false },
                )
            }
            composable(TopLevelDestination.HABITS.route) { HabitsScreen() }
            composable(TopLevelDestination.JOURNAL.route) { JournalScreen() }

            composable(
                route = Routes.PROJECT_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_PROJECT_ID) { type = NavType.LongType }),
            ) { entry ->
                val projectId = entry.arguments?.getLong(Routes.ARG_PROJECT_ID) ?: return@composable
                ProjectDetailScreen(
                    projectId = projectId,
                    onBack = { navController.popBackStack() },
                )
            }

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
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
