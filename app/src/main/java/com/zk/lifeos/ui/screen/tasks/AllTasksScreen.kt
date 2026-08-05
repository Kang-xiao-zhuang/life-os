package com.zk.lifeos.ui.screen.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.RescheduledTask
import com.zk.lifeos.model.Task
import com.zk.lifeos.model.TaskListItem
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.TaskEditSheet
import com.zk.lifeos.ui.components.TaskRow
import java.time.LocalDate

/**
 * 所有待办 — every open task in one flat list, whichever project it belongs to.
 *
 * Exists because Dashboard only shows what is due today or flagged MIT: a task with neither was
 * reachable only by opening its project, so with five projects「我现在能做什么」cost five taps.
 *
 * Grouped by *when*, not by project — the project is already on each row, and what you want from
 * this screen is the order to work in.
 */
@Composable
fun AllTasksScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AllTasksViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val mitCount by viewModel.mitCount.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var editing by remember { mutableStateOf<Task?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val offerUndo: (List<RescheduledTask>) -> Unit = { moved ->
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "已把 ${moved.size} 项挪到今天",
                actionLabel = "撤销",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoReschedule(moved)
        }
    }

    val overdue = tasks.filter { it.task.isOverdue(today) }
    val dueToday = tasks.filter { it.task.isDueToday(today) }
    val later = tasks.filter { it.task.dueDate != null && it.task.dueDate.isAfter(today) }
    val undated = tasks.filter { it.task.dueDate == null }

    LifeOsScreen(
        title = "所有待办",
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
    ) {
        if (tasks.isEmpty()) {
            SectionCard(title = "没有待办了") {
                EmptyHint("所有任务都完成了。")
            }
            return@LifeOsScreen
        }

        if (overdue.isNotEmpty()) {
            SectionCard(title = "已经逾期", trailing = "${overdue.size} 项") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    overdue.forEach { Row(it, today, viewModel) { editing = it.task } }
                    // Bulk escape hatch: a red backlog that only grows is a list you stop reading.
                    TextButton(onClick = { viewModel.rescheduleOverdue(offerUndo) }) {
                        Text("把这 ${overdue.size} 项挪到今天")
                    }
                }
            }
        }

        if (dueToday.isNotEmpty()) {
            SectionCard(title = "今天到期", trailing = "${dueToday.size} 项") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    dueToday.forEach { Row(it, today, viewModel) { editing = it.task } }
                }
            }
        }

        if (later.isNotEmpty()) {
            SectionCard(title = "以后", trailing = "${later.size} 项") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    later.forEach { Row(it, today, viewModel) { editing = it.task } }
                }
            }
        }

        if (undated.isNotEmpty()) {
            SectionCard(title = "没有日期", trailing = "${undated.size} 项") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    undated.forEach { Row(it, today, viewModel) { editing = it.task } }
                }
            }
        }
    }

    editing?.let { task ->
        TaskEditSheet(
            existing = task,
            projects = projects,
            currentMitCount = mitCount,
            onDismiss = { editing = null },
            onSave = { draft ->
                viewModel.saveTask(task, draft)
                editing = null
            },
            onDelete = {
                viewModel.deleteTask(task.id)
                editing = null
            },
        )
    }
}

/** Named [Row] to keep the four call sites above readable; shows which project each task is in. */
@Composable
private fun Row(
    item: TaskListItem,
    today: LocalDate,
    viewModel: AllTasksViewModel,
    onEdit: () -> Unit,
) {
    TaskRow(
        task = item.task,
        today = today,
        projectLabel = item.projectLabel,
        onToggle = { viewModel.toggleTask(item.task) },
        onClick = onEdit,
    )
}
