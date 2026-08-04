package com.zk.lifeos.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.DashboardSnapshot
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.HabitRow
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.PhaseNote
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.TaskRow
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 首页 — the page opened most often, so it answers「今天要做什么」and nothing else.
 *
 * Phase 2 lays out the five sections from the spec against real data. Creating / completing /
 * checking in arrive in Phase 3, so nothing here is tappable except navigation.
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

    LifeOsScreen(
        title = "LifeOS",
        modifier = modifier,
        actions = {
            // Settings deliberately lives here rather than in the bottom bar.
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "设置")
            }
        },
    ) {
        DateHeader(state.today)

        MitCard(state)
        TodayTasksCard(state)
        HabitsCard(state, onOpenHabits)

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

        PhaseNote("Phase 3 会接上:新建任务、完成任务、习惯打卡、写复盘。")
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
private fun MitCard(state: DashboardSnapshot) {
    SectionCard(title = "今日最重要", trailing = if (state.mit.isEmpty()) null else "${state.mit.size} 项") {
        if (state.mit.isEmpty()) {
            EmptyHint("还没有标记最重要的事。一天挑一到两件就够。")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.mit.forEach { TaskRow(task = it, today = state.today) }
            }
        }
    }
}

@Composable
private fun TodayTasksCard(state: DashboardSnapshot) {
    val overdue = state.dueToday.count { it.isOverdue(state.today) }
    SectionCard(
        title = "今日任务",
        trailing = when {
            state.dueToday.isEmpty() -> null
            overdue > 0 -> "${state.dueToday.size} 项 · 逾期 $overdue"
            else -> "${state.dueToday.size} 项"
        },
    ) {
        if (state.dueToday.isEmpty()) {
            EmptyHint("今天没有到期的任务。")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.dueToday.forEach { TaskRow(task = it, today = state.today) }
            }
        }
    }
}

@Composable
private fun HabitsCard(state: DashboardSnapshot, onOpenHabits: () -> Unit) {
    SectionCard(
        title = "今日习惯",
        trailing = if (state.habits.isEmpty()) null else "${state.habitsCheckedToday} / ${state.habits.size}",
        onClick = onOpenHabits,
    ) {
        if (state.habits.isEmpty()) {
            EmptyHint("还没有习惯。阅读、健身、写作、英语…先加一个。")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.habits.take(4).forEach { HabitRow(habit = it) }
                if (state.habits.size > 4) {
                    EmptyHint("还有 ${state.habits.size - 4} 个,点开查看全部。")
                }
            }
        }
    }
}
