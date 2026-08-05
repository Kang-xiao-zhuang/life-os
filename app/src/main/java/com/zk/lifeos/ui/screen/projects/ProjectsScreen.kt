package com.zk.lifeos.ui.screen.projects

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.ConfirmDialog
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsFab
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.NameEmojiDialog
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.TaskRow
import com.zk.lifeos.ui.components.projectEmojis
import java.time.LocalDate

/**
 * 项目 — the long-running areas of life. A project is never "done", so the list shows how much
 * is left rather than a completion state.
 *
 * Tap a project to open its tasks; long-press to rename or archive it.
 */
@Composable
fun ProjectsScreen(
    onOpenProject: (Long) -> Unit,
    onOpenAllTasks: () -> Unit,
    onOpenArchived: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProjectsViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val unassigned by viewModel.unassigned.collectAsStateWithLifecycle()
    val openTaskCount by viewModel.openTaskCount.collectAsStateWithLifecycle()
    val archivedCount by viewModel.archivedCount.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProjectSummary?>(null) }
    var archiving by remember { mutableStateOf<ProjectSummary?>(null) }

    LifeOsScreen(
        title = "项目",
        modifier = modifier,
        floatingActionButton = { LifeOsFab("新项目") { creating = true } },
    ) {
        // First, because「我现在能做什么」is the question you bring to this tab. Without it a task
        // with no due date and no MIT flag is only findable by opening its project.
        if (openTaskCount > 0) {
            SectionCard(
                title = "所有待办",
                trailing = "$openTaskCount 项",
                onClick = onOpenAllTasks,
            ) {
                EmptyHint("不分项目,按时间先后排好的一整张清单。")
            }
        }

        if (projects.isEmpty()) {
            SectionCard(title = "还没有项目") {
                EmptyHint("项目是长期在做的事:工作、学习、阅读、健身、自媒体。每个项目下面挂任务。")
            }
        } else {
            projects.forEach { project ->
                ProjectCard(
                    project = project,
                    onClick = { onOpenProject(project.id) },
                    onLongClick = { editing = project },
                )
            }
        }

        // Last, because it's a rare visit — but present, so archiving isn't a dead end.
        if (archivedCount > 0) {
            SectionCard(
                title = "已归档",
                trailing = "$archivedCount 个",
                onClick = onOpenArchived,
            ) {
                EmptyHint("归档的项目在这里,可以恢复。")
            }
        }

        if (unassigned.isNotEmpty()) {
            SectionCard(title = "未归类", trailing = "${unassigned.size} 项") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    unassigned.take(8).forEach { task ->
                        TaskRow(
                            task = task,
                            today = today,
                            onToggle = { viewModel.toggleTask(task) },
                        )
                    }
                    if (unassigned.size > 8) {
                        EmptyHint("还有 ${unassigned.size - 8} 项。")
                    }
                }
            }
        }
    }

    if (creating) {
        NameEmojiDialog(
            title = "新建项目",
            label = "项目名称",
            emojiOptions = projectEmojis,
            confirmText = "创建",
            onDismiss = { creating = false },
            onConfirm = { name, emoji ->
                viewModel.createProject(name, emoji)
                creating = false
            },
        )
    }

    editing?.let { project ->
        NameEmojiDialog(
            title = "编辑项目",
            label = "项目名称",
            emojiOptions = projectEmojis,
            initialName = project.name,
            initialEmoji = project.emoji,
            destructiveText = "归档",
            onDestructive = {
                editing = null
                archiving = project
            },
            onDismiss = { editing = null },
            onConfirm = { name, emoji ->
                viewModel.renameProject(project.id, name, emoji)
                editing = null
            },
        )
    }

    archiving?.let { project ->
        ConfirmDialog(
            title = "归档「${project.name}」?",
            message = "它会从列表里移走,但任务和历史都还在 —— 不会删掉任何东西。",
            confirmText = "归档",
            onDismiss = { archiving = null },
            onConfirm = {
                viewModel.archiveProject(project.id)
                archiving = null
            },
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (project.emoji.isEmpty()) project.name else "${project.emoji}  ${project.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (project.totalTasks == 0) "暂无任务" else "${project.openTasks} 待做",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val progress = project.progress
            if (progress == null) {
                EmptyHint("还没有任务。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {},
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "已完成 ${project.doneTasks} / ${project.totalTasks}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
