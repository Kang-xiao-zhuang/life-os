package com.zk.lifeos.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.ui.graphics.vector.ImageVector
import com.zk.lifeos.R

/**
 * The five bottom-bar destinations, in order. Capture sits in the middle so the thumb reaches
 * it without moving — quick capture is the highest-frequency action after Dashboard.
 *
 * Settings is intentionally NOT here; it opens from the Dashboard top bar (per spec).
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    DASHBOARD(
        route = "dashboard",
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Home,
        icon = Icons.Outlined.Home,
    ),
    PROJECTS(
        route = "projects",
        labelRes = R.string.nav_projects,
        selectedIcon = Icons.Filled.Folder,
        icon = Icons.Outlined.FolderOpen,
    ),
    CAPTURE(
        route = "capture",
        labelRes = R.string.nav_capture,
        selectedIcon = Icons.Filled.AddCircle,
        icon = Icons.Outlined.AddCircleOutline,
    ),
    HABITS(
        route = "habits",
        labelRes = R.string.nav_habits,
        selectedIcon = Icons.Filled.LocalFireDepartment,
        icon = Icons.Outlined.LocalFireDepartment,
    ),
    JOURNAL(
        route = "journal",
        labelRes = R.string.nav_journal,
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        icon = Icons.AutoMirrored.Outlined.MenuBook,
    ),
}

/** Routes that are not bottom-bar tabs. */
object Routes {
    const val SETTINGS = "settings"

    /** A project's task list. Tasks only make sense inside their project (or on Dashboard). */
    const val PROJECT_DETAIL = "projects/{projectId}"
    const val ARG_PROJECT_ID = "projectId"

    fun projectDetail(projectId: Long) = "projects/$projectId"

    /** Every open task, flat — for「我现在能做什么」without walking each project. */
    const val ALL_TASKS = "tasks/all"
}
