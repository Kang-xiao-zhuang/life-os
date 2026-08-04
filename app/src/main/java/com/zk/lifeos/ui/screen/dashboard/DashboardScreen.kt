package com.zk.lifeos.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.DashboardSnapshot
import com.zk.lifeos.model.Task
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.HabitRow
import com.zk.lifeos.ui.components.LifeOsFab
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.TaskEditSheet
import com.zk.lifeos.ui.components.TaskRow
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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

    LifeOsScreen(
        title = "LifeOS",
        modifier = modifier,
        actions = {
            // Settings deliberately lives here rather than in the bottom bar.
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "设置")
            }
        },
        floatingActionButton = { LifeOsFab("新任务") { editor = Editor.New } },
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
            onRescheduleOverdue = viewModel::rescheduleOverdue,
        )
        HabitsCard(
            state = state,
            onOpenHabits = onOpenHabits,
            onToggleHabit = viewModel::toggleHabit,
        )

        SectionCard(
            title = "快速记录",
            trailing = if (state.inboxCount > 0) "${state.inboxCount} 条待整理" else null,
            onClick = onOpenCapture,
        ) {
            EmptyHint("想到什么先记下来,以后再整理。")
        }

        SectionCard(
            title = "今日复盘",
            trailing = if (state.journal.isEmpty) "未写" else "已写",
            onClick = onOpenJournal,
        ) {
            EmptyHint(
                if (state.journal.isEmpty) {
                    "今天完成了什么、最大的收获、遇到的问题、明天最重要的一件事。"
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
    val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "今天",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "${today.monthValue} 月 ${today.dayOfMonth} 日 · $weekday",
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
        title = "今日最重要",
        // Counts what is still to do; finished ones stay listed but shouldn't inflate the number.
        trailing = when {
            state.mit.isEmpty() -> null
            open == 0 -> "都做完了"
            else -> "$open 项"
        },
    ) {
        if (state.mit.isEmpty()) {
            EmptyHint("还没有标记最重要的事。新建任务时勾上「今日最重要」,一天挑一到两件就够。")
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
        title = "今日任务",
        trailing = when {
            state.dueToday.isEmpty() -> null
            overdue > 0 -> "$open 项 · 逾期 $overdue"
            open == 0 -> "都做完了"
            else -> "$open 项"
        },
    ) {
        if (state.dueToday.isEmpty()) {
            // MIT tasks are filtered out of this list, so "nothing due" would be a lie when the
            // only thing due today is already featured above.
            EmptyHint(
                if (state.mit.isEmpty()) "今天没有到期的任务。" else "今天到期的都在上面了。"
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
                        Text("把 $overdue 项逾期挪到今天")
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
        title = "今日习惯",
        trailing = if (state.habits.isEmpty()) null else "${state.habitsCheckedToday} / ${state.habits.size}",
        onClick = onOpenHabits,
    ) {
        if (state.habits.isEmpty()) {
            EmptyHint("还没有习惯。阅读、健身、写作、英语…去习惯页加一个。")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.habits.take(4).forEach { habit ->
                    HabitRow(habit = habit, onToggle = { onToggleHabit(habit.id) })
                }
                if (state.habits.size > 4) {
                    EmptyHint("还有 ${state.habits.size - 4} 个,点开查看全部。")
                }
            }
        }
    }
}
