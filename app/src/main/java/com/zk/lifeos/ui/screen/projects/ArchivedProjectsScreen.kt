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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.ConfirmDialog
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard

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
        title = "已归档",
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
    ) {
        if (projects.isEmpty()) {
            SectionCard(title = "没有归档的项目") {
                EmptyHint("在项目列表里长按一个项目可以归档它。归档后它会来到这里,任务和历史都还在。")
            }
            return@LifeOsScreen
        }

        projects.forEach { project ->
            SectionCard(
                title = if (project.emoji.isEmpty()) project.name else "${project.emoji}  ${project.name}",
                trailing = if (project.totalTasks == 0) "暂无任务" else "${project.totalTasks} 个任务",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmptyHint(
                        if (project.totalTasks == 0) {
                            "没有任务。"
                        } else {
                            "待做 ${project.openTasks} · 已完成 ${project.doneTasks}"
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.restore(project.id) }) { Text("恢复") }
                        TextButton(onClick = { deleting = project }) {
                            Text("彻底删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        EmptyHint("彻底删除只会删掉项目本身,它的任务会变成「未归类」,不会跟着消失。")
    }

    deleting?.let { project ->
        ConfirmDialog(
            title = "彻底删除「${project.name}」?",
            message = if (project.totalTasks == 0) {
                "这个项目会被永久删除,无法恢复。"
            } else {
                "这个项目会被永久删除,无法恢复。它的 ${project.totalTasks} 个任务会保留下来,变成「未归类」。"
            },
            confirmText = "彻底删除",
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.deletePermanently(project.id)
                deleting = null
            },
        )
    }
}
