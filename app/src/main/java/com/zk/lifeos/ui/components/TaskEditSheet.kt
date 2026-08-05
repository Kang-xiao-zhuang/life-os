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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zk.lifeos.R
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.Task
import com.zk.lifeos.service.TaskService
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
    /** How many 今日最重要 are already open, so the sheet can say when it's becoming meaningless. */
    currentMitCount: Int = 0,
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
                text = if (existing == null) stringResource(R.string.task_new) else stringResource(R.string.task_edit),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_title_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.task_notes_label)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            // ---- due date ----
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.task_due_date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = {
                            Text(
                                dueDate?.let { stringResource(R.string.date_short, it.monthValue, it.dayOfMonth) }
                                    ?: stringResource(R.string.task_pick_date)
                            )
                        },
                    )
                    AssistChip(onClick = { dueDate = LocalDate.now() }, label = { Text(stringResource(R.string.label_today)) })
                    AssistChip(
                        onClick = { dueDate = LocalDate.now().plusDays(1) },
                        label = { Text(stringResource(R.string.label_tomorrow)) },
                    )
                    if (dueDate != null) {
                        TextButton(onClick = { dueDate = null }) { Text(stringResource(R.string.action_clear)) }
                    }
                }
            }

            // ---- MIT ----
            // Advisory, never blocking: it's the user's day. But 「一天挑一到两件就够」 is the whole
            // point of the flag, and nothing else in the app would ever mention it.
            val othersFlagged = currentMitCount - if (existing?.isMit == true) 1 else 0
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = isMit,
                    onClick = { isMit = !isMit },
                    label = { Text(stringResource(R.string.task_mit)) },
                )
                if (isMit && othersFlagged >= TaskService.MIT_SOFT_LIMIT) {
                    Text(
                        text = stringResource(R.string.task_mit_warning, othersFlagged),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // ---- project ----
            if (projects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.task_project),
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
                            label = { Text(stringResource(R.string.label_unassigned)) },
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
                ) { Text(stringResource(R.string.action_save)) }

                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }

                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.task_delete_title),
            message = stringResource(R.string.task_delete_message, existing?.title.orEmpty()),
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
        )
    }
}
