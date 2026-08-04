package com.zk.lifeos.ui.screen.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.PhaseNote
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.TaskRow
import java.time.LocalDate

/**
 * 项目 — the long-running areas of life. A project is never "done", so the list shows how much
 * is left rather than a completion state.
 */
@Composable
fun ProjectsScreen(
    onOpenProject: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProjectsViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val unassigned by viewModel.unassigned.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    LifeOsScreen(title = "项目", modifier = modifier) {
        if (projects.isEmpty()) {
            SectionCard(title = "还没有项目") {
                EmptyHint("项目是长期在做的事:工作、学习、阅读、健身、自媒体。每个项目下面挂任务。")
            }
        } else {
            projects.forEach { project ->
                ProjectCard(project = project, onClick = { onOpenProject(project.id) })
            }
        }

        if (unassigned.isNotEmpty()) {
            SectionCard(title = "未归类", trailing = "${unassigned.size} 项") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    unassigned.take(8).forEach { TaskRow(task = it, today = today) }
                    if (unassigned.size > 8) {
                        EmptyHint("还有 ${unassigned.size - 8} 项。")
                    }
                }
            }
        }

        PhaseNote("Phase 3 会接上:新建 / 重命名 / 归档项目。")
    }
}

@Composable
private fun ProjectCard(project: ProjectSummary, onClick: () -> Unit) {
    SectionCard(
        title = if (project.emoji.isEmpty()) project.name else "${project.emoji}  ${project.name}",
        trailing = if (project.totalTasks == 0) "暂无任务" else "${project.openTasks} 待做",
        onClick = onClick,
    ) {
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
