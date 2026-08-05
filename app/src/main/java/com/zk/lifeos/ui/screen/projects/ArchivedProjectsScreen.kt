package com.zk.lifeos.ui.screen.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.R
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.ConfirmDialog
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.taskCount

/**
 * 已归档的项目 — the way back.
 *
 * Archiving used to be a one-way trip: every list filters `archived = 0`, so an archived project
 * simply stopped existing as far as the UI was concerned. "归档而不是删除" only means something if
 * you can still get to what you archived.
 */
@Composable
fun ArchivedProjectsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ArchivedProjectsViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<ProjectSummary?>(null) }

    LifeOsScreen(
        title = stringResource(R.string.archived_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
    ) {
        if (projects.isEmpty()) {
            SectionCard(title = stringResource(R.string.archived_projects_empty_title)) {
                EmptyHint(stringResource(R.string.archived_projects_empty_hint))
            }
            return@LifeOsScreen
        }

        projects.forEach { project ->
            SectionCard(
                title = if (project.emoji.isEmpty()) project.name else "${project.emoji}  ${project.name}",
                trailing = if (project.totalTasks == 0) {
                    stringResource(R.string.project_no_tasks)
                } else {
                    taskCount(project.totalTasks)
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmptyHint(
                        if (project.totalTasks == 0) {
                            stringResource(R.string.project_no_tasks_yet)
                        } else {
                            stringResource(
                                R.string.archived_projects_counts,
                                project.openTasks,
                                project.doneTasks,
                            )
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.restore(project.id) }) { Text(stringResource(R.string.action_restore)) }
                        TextButton(onClick = { deleting = project }) {
                            Text(stringResource(R.string.action_delete_forever), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        EmptyHint(stringResource(R.string.archived_projects_delete_note))
    }

    deleting?.let { project ->
        ConfirmDialog(
            title = stringResource(R.string.archived_project_delete_title, project.name),
            message = if (project.totalTasks == 0) {
                stringResource(R.string.archived_project_delete_message_empty)
            } else {
                stringResource(R.string.archived_project_delete_message, project.totalTasks)
            },
            confirmText = stringResource(R.string.action_delete_forever),
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.deletePermanently(project.id)
                deleting = null
            },
        )
    }
}
