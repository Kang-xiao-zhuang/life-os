package com.zk.lifeos.ui.screen.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zk.lifeos.model.Task
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsFab
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.TaskEditSheet
import com.zk.lifeos.ui.components.TaskRow
import com.zk.lifeos.ui.rememberContainer
import java.time.LocalDate

private sealed interface Editor {
    data object None : Editor
    data object New : Editor
    data class Edit(val task: Task) : Editor
}

/**
 * 任务 — a project's task list. Tasks are not a bottom-bar tab: they only make sense inside the
 * project they belong to, or on today's Dashboard.
 */
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = rememberContainer()
    // Scoped to this project id, so it gets its own ViewModel instance and factory.
    val factory = remember(projectId) {
        viewModelFactory {
            initializer {
                ProjectDetailViewModel(container.projectService, container.taskService, projectId)
            }
        }
    }
    val viewModel: ProjectDetailViewModel = viewModel(key = "project-$projectId", factory = factory)

    val name by viewModel.projectName.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    var editor by remember { mutableStateOf<Editor>(Editor.None) }

    val open = tasks.filterNot { it.done }
    val done = tasks.filter { it.done }

    LifeOsScreen(
        title = name ?: "项目",
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        floatingActionButton = { LifeOsFab("新任务") { editor = Editor.New } },
    ) {
        SectionCard(title = "待做", trailing = if (open.isEmpty()) null else "${open.size} 项") {
            if (open.isEmpty()) {
                EmptyHint("没有待做的任务了。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    open.forEach { task ->
                        TaskRow(
                            task = task,
                            today = today,
                            onToggle = { viewModel.toggleTask(task) },
                            onClick = { editor = Editor.Edit(task) },
                        )
                    }
                }
            }
        }

        if (done.isNotEmpty()) {
            SectionCard(title = "已完成", trailing = "${done.size} 项") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    done.take(20).forEach { task ->
                        TaskRow(
                            task = task,
                            today = today,
                            onToggle = { viewModel.toggleTask(task) },
                            onClick = { editor = Editor.Edit(task) },
                        )
                    }
                    if (done.size > 20) {
                        EmptyHint("还有 ${done.size - 20} 项已完成。")
                    }
                }
            }
        }
    }

    when (val current = editor) {
        Editor.None -> Unit
        Editor.New -> TaskEditSheet(
            existing = null,
            projects = projects,
            defaultProjectId = projectId,
            onDismiss = { editor = Editor.None },
            onSave = { draft ->
                viewModel.saveTask(null, draft)
                editor = Editor.None
            },
        )
        is Editor.Edit -> TaskEditSheet(
            existing = current.task,
            projects = projects,
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
