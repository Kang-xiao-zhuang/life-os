package com.zk.lifeos.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.random.Random

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

        // The hero: 今日最重要 is not a card. Everything below it is, and is quieter, so the eye
        // starts on the one thing that has to happen today instead of on five identical slabs.
        MitHero(
            state = state,
            onToggle = viewModel::toggleTask,
            onEdit = { editor = Editor.Edit(it) },
            onAdoptCarried = viewModel::adoptCarriedMit,
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
            quiet = true,
            onClick = onOpenCapture,
        ) {
            EmptyHint(stringResource(R.string.dash_capture_hint))
        }

        SectionCard(
            title = stringResource(R.string.dash_journal_title),
            trailing = if (state.journal.isEmpty) stringResource(R.string.journal_not_written) else stringResource(R.string.journal_written),
            quiet = true,
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
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.label_today),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.width(12.dp))
            // Beside 今天 rather than on a line of its own: 今天 is two characters wide and the rest
            // of that row is empty, so the motto costs no vertical space and doesn't push the day's
            // actual work further down. Baseline-aligned, because bottom- or centre-aligning text of
            // two very different sizes always looks slightly wrong.
            Text(
                text = dailyMotto(today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
            )
        }
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

/**
 * Yesterday's answer to 明天最重要的一件事, offered back as today's MIT.
 *
 * Quiet on purpose — it is a suggestion, not an instruction, and it sits where the empty-state text
 * used to be. One tap accepts it; ignoring it costs nothing and it disappears as soon as any MIT
 * exists.
 */
@Composable
private fun CarriedMitSuggestion(text: String, onAdopt: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.dash_carried_mit_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(
            onClick = onAdopt,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        ) {
            Text(stringResource(R.string.dash_carried_mit_adopt))
        }
    }
}

/**
 * Today's line, picked *from the date* rather than at random on every draw.
 *
 * Two reasons it is seeded by the day. A fresh pick per recomposition would make the text flicker as
 * you tick things off, and a fresh pick per launch would make it a slot machine you could reroll —
 * neither reads as「今天的一句」. Seeding by the epoch day also means nothing has to be stored, and it
 * survives the process being killed.
 */
@Composable
private fun dailyMotto(today: LocalDate): String {
    val mottos = stringArrayResource(R.array.daily_mottos)
    return remember(today, mottos) {
        if (mottos.isEmpty()) "" else mottos[Random(today.toEpochDay()).nextInt(mottos.size)]
    }
}

/**
 * 今日最重要 — the screen's one point, so it gets the screen's one piece of emphasis.
 *
 * No card, on purpose. A card would put it back on the same footing as 快速记录 below, which is what
 * made Dashboard read as an undifferentiated stack. Here it sits directly on the background under a
 * small label, at a larger text size, with room around it.
 */
@Composable
private fun MitHero(
    state: DashboardSnapshot,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onAdoptCarried: (String) -> Unit,
) {
    val open = state.mit.count { !it.done }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.task_mit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            // Counts what is still to do; finished ones stay listed but shouldn't inflate the number.
            val trailing = when {
                state.mit.isEmpty() -> null
                open == 0 -> stringResource(R.string.dash_all_done)
                else -> itemCount(open)
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.mit.isEmpty()) {
            // Yesterday evening you already decided what today's one thing is. Offering it back
            // beats an empty-state paragraph telling you to decide again.
            if (state.carriedMit.isNotBlank()) {
                CarriedMitSuggestion(text = state.carriedMit, onAdopt = { onAdoptCarried(state.carriedMit) })
            } else {
                EmptyHint(stringResource(R.string.dash_mit_empty))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.mit.forEach { task ->
                    TaskRow(
                        task = task,
                        today = state.today,
                        titleStyle = MaterialTheme.typography.bodyLarge,
                        onToggle = { onToggle(task) },
                        onClick = { onEdit(task) },
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
        quiet = true,
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
        trailing = if (state.habits.isEmpty()) {
            null
        } else {
            stringResource(R.string.habit_progress, state.habitsCheckedToday, state.habits.size)
        },
        quiet = true,
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
