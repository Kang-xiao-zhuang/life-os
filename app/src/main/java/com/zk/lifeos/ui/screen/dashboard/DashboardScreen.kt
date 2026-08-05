package com.zk.lifeos.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.R
import com.zk.lifeos.model.DashboardSnapshot
import com.zk.lifeos.model.RescheduledTask
import com.zk.lifeos.model.Task
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.HabitRow
import com.zk.lifeos.ui.components.LifeOsFab
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.TaskEditSheet
import com.zk.lifeos.ui.components.TaskRow
import com.zk.lifeos.ui.components.itemCount
import com.zk.lifeos.ui.currentLocale
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle

/** Which editor the screen currently has open. */
private sealed interface Editor {
    data object None : Editor
    data object New : Editor
    data class Edit(val task: Task) : Editor
}

/**
 * 首页 — the page opened most often, so it answers「今天要做什么」and nothing else.
 *
 * Everything here is now live: tick a task, check off a habit, tap a task to edit it.
 */
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenHabits: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DashboardViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val mitCount by viewModel.mitCount.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<Editor>(Editor.None) }

    // Coming back to a resident app after midnight must not leave 今天 pointing at yesterday.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose {}
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Snackbar text is built outside composition, so it goes through the (localized) context.
    val context = LocalContext.current
    val offerUndo: (List<RescheduledTask>) -> Unit = { moved ->
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.dash_moved_to_today, moved.size),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoReschedule(moved)
        }
    }

    LifeOsScreen(
        title = "LifeOS",
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        actions = {
            // Settings deliberately lives here rather than in the bottom bar.
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.nav_settings))
            }
        },
        floatingActionButton = { LifeOsFab(stringResource(R.string.dash_new_task)) { editor = Editor.New } },
    ) {
        DateHeader(state.today)

        MitCard(
            state = state,
            onToggle = viewModel::toggleTask,
            onEdit = { editor = Editor.Edit(it) },
        )
        TodayTasksCard(
            state = state,
            onToggle = viewModel::toggleTask,
            onEdit = { editor = Editor.Edit(it) },
            onRescheduleOverdue = { viewModel.rescheduleOverdue(offerUndo) },
        )
        HabitsCard(
            state = state,
            onOpenHabits = onOpenHabits,
            onToggleHabit = viewModel::toggleHabit,
        )

        SectionCard(
            title = stringResource(R.string.dash_capture_title),
            trailing = if (state.inboxCount > 0) {
                stringResource(R.string.dash_capture_trailing, state.inboxCount)
            } else {
                null
            },
            onClick = onOpenCapture,
        ) {
            EmptyHint(stringResource(R.string.dash_capture_hint))
        }

        SectionCard(
            title = stringResource(R.string.dash_journal_title),
            trailing = if (state.journal.isEmpty) stringResource(R.string.journal_not_written) else stringResource(R.string.journal_written),
            onClick = onOpenJournal,
        ) {
            EmptyHint(
                if (state.journal.isEmpty) {
                    stringResource(R.string.dash_journal_prompt_summary)
                } else {
                    state.journal.win.ifBlank { state.journal.done }.take(60)
                }
            )
        }
    }

    when (val current = editor) {
        Editor.None -> Unit
        Editor.New -> TaskEditSheet(
            existing = null,
            projects = projects,
            currentMitCount = mitCount,
            onDismiss = { editor = Editor.None },
            onSave = { draft ->
                viewModel.saveTask(null, draft)
                editor = Editor.None
            },
        )
        is Editor.Edit -> TaskEditSheet(
            existing = current.task,
            projects = projects,
            currentMitCount = mitCount,
            onDismiss = { editor = Editor.None },
            onSave = { draft ->
                viewModel.saveTask(current.task, draft)
                editor = Editor.None
            },
            onDelete = {
                viewModel.deleteTask(current.task.id)
                editor = Editor.None
            },
        )
    }
}

@Composable
private fun DateHeader(today: LocalDate) {
    // currentLocale(), not Locale.getDefault(): the latter is the *system* language and would
    // keep printing 星期一 after the user switched the app to English.
    val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, currentLocale())
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.label_today),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(
                R.string.date_month_day_weekday,
                today.monthValue,
                today.dayOfMonth,
                weekday,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 今日最重要任务 — first, because it is the one thing that must happen today. */
@Composable
private fun MitCard(
    state: DashboardSnapshot,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
) {
    val open = state.mit.count { !it.done }
    SectionCard(
        title = stringResource(R.string.task_mit),
        // Counts what is still to do; finished ones stay listed but shouldn't inflate the number.
        trailing = when {
            state.mit.isEmpty() -> null
            open == 0 -> stringResource(R.string.dash_all_done)
            else -> itemCount(open)
        },
    ) {
        if (state.mit.isEmpty()) {
            EmptyHint(stringResource(R.string.dash_mit_empty))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.mit.forEach { task ->
                    TaskRow(
                        task = task,
                        today = state.today,
                        onToggle = { onToggle(task) },
                        onClick = { onEdit(task) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayTasksCard(
    state: DashboardSnapshot,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onRescheduleOverdue: () -> Unit,
) {
    val overdue = state.dueToday.count { it.isOverdue(state.today) }
    val open = state.dueToday.count { !it.done }
    SectionCard(
        title = stringResource(R.string.dash_tasks_title),
        trailing = when {
            state.dueToday.isEmpty() -> null
            overdue > 0 -> stringResource(R.string.dash_tasks_trailing_overdue, open, overdue)
            open == 0 -> stringResource(R.string.dash_all_done)
            else -> itemCount(open)
        },
    ) {
        if (state.dueToday.isEmpty()) {
            // MIT tasks are filtered out of this list, so "nothing due" would be a lie when the
            // only thing due today is already featured above.
            EmptyHint(
                if (state.mit.isEmpty()) stringResource(R.string.dash_tasks_empty) else stringResource(R.string.dash_tasks_empty_all_mit)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.dueToday.forEach { task ->
                    TaskRow(
                        task = task,
                        today = state.today,
                        onToggle = { onToggle(task) },
                        onClick = { onEdit(task) },
                    )
                }
                // Overdue items otherwise just accumulate in red until the card becomes noise.
                if (overdue > 0) {
                    TextButton(onClick = onRescheduleOverdue) {
                        Text(stringResource(R.string.dash_reschedule_overdue, overdue))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitsCard(
    state: DashboardSnapshot,
    onOpenHabits: () -> Unit,
    onToggleHabit: (Long) -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.dash_habits_title),
        trailing = if (state.habits.isEmpty()) null else "${state.habitsCheckedToday} / ${state.habits.size}",
        onClick = onOpenHabits,
    ) {
        if (state.habits.isEmpty()) {
            EmptyHint(stringResource(R.string.dash_habits_empty))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.habits.take(4).forEach { habit ->
                    HabitRow(habit = habit, onToggle = { onToggleHabit(habit.id) })
                }
                if (state.habits.size > 4) {
                    EmptyHint(stringResource(R.string.dash_habits_more, state.habits.size - 4))
                }
            }
        }
    }
}
