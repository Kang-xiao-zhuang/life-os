package com.zk.lifeos.ui.screen.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.PhaseNote
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.TaskRow
import com.zk.lifeos.ui.rememberContainer
import java.time.LocalDate

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
            initializer { ProjectDetailViewModel(container.projectService, projectId) }
        }
    }
    val viewModel: ProjectDetailViewModel = viewModel(key = "project-$projectId", factory = factory)

    val name by viewModel.projectName.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val today = LocalDate.now()

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
    ) {
        SectionCard(title = "待做", trailing = if (open.isEmpty()) null else "${open.size} 项") {
            if (open.isEmpty()) {
                EmptyHint("没有待做的任务了。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    open.forEach { TaskRow(task = it, today = today) }
                }
            }
        }

        if (done.isNotEmpty()) {
            SectionCard(title = "已完成", trailing = "${done.size} 项") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    done.take(20).forEach { TaskRow(task = it, today = today) }
                    if (done.size > 20) {
                        EmptyHint("还有 ${done.size - 20} 项已完成。")
                    }
                }
            }
        }

        PhaseNote("Phase 3 会接上:新建任务、编辑、设置截止日期、标记完成。")
    }
}
