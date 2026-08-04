package com.zk.lifeos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the sheet hands back when 保存 is tapped. */
data class TaskDraft(
    val title: String,
    val notes: String,
    val projectId: Long?,
    val dueDate: LocalDate?,
    val isMit: Boolean,
)

/**
 * Create / edit a task. One sheet for both, because the fields are identical and the spec asks
 * to「保持简单」.
 *
 * [existing] null = creating. [onDelete] is only offered when editing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditSheet(
    existing: Task?,
    projects: List<ProjectSummary>,
    /** Pre-selected project when creating from inside a project. */
    defaultProjectId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var projectId by remember { mutableStateOf(existing?.projectId ?: defaultProjectId) }
    var dueDate by remember { mutableStateOf(existing?.dueDate) }
    var isMit by remember { mutableStateOf(existing?.isMit ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (existing == null) "新建任务" else "编辑任务",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("要做什么") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注(可选)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            // ---- due date ----
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "截止日期",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(dueDate?.let { "${it.monthValue}/${it.dayOfMonth}" } ?: "选择日期") },
                    )
                    AssistChip(onClick = { dueDate = LocalDate.now() }, label = { Text("今天") })
                    AssistChip(
                        onClick = { dueDate = LocalDate.now().plusDays(1) },
                        label = { Text("明天") },
                    )
                    if (dueDate != null) {
                        TextButton(onClick = { dueDate = null }) { Text("清除") }
                    }
                }
            }

            // ---- MIT ----
            FilterChip(
                selected = isMit,
                onClick = { isMit = !isMit },
                label = { Text("今日最重要") },
            )

            // ---- project ----
            if (projects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "所属项目",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = projectId == null,
                            onClick = { projectId = null },
                            label = { Text("未归类") },
                        )
                        projects.forEach { project ->
                            FilterChip(
                                selected = projectId == project.id,
                                onClick = { projectId = project.id },
                                label = {
                                    Text(
                                        if (project.emoji.isEmpty()) project.name
                                        else "${project.emoji} ${project.name}"
                                    )
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        onSave(TaskDraft(title, notes, projectId, dueDate, isMit))
                    },
                    enabled = title.isNotBlank(),
                ) { Text("保存") }

                TextButton(onClick = onDismiss) { Text("取消") }

                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dueDate
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        // The picker works in UTC; convert on that basis or the date can slip a day.
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = "删除这个任务?",
            message = "「${existing?.title.orEmpty()}」会被永久删除。",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
        )
    }
}
