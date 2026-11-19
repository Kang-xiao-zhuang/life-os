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
import com.zk.lifeos.model.RepeatRule
import com.zk.lifeos.model.Task
import com.zk.lifeos.service.TaskService
import com.zk.lifeos.ui.LifeOsOverlayLocalization
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
    val repeatRule: RepeatRule? = null,
)

/**
 * Create / edit a task. One sheet for both, because the fields are identical and the spec asks
 * to「保持简单」.
 *
 * [existing] null = creating. [onDelete] is only offered when editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var repeatRule by remember { mutableStateOf(existing?.repeatRule) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // A sheet is its own subcomposition and otherwise reverts to the phone's language — the
        // whole sheet came up in English in a Chinese app. See LifeOsOverlayLocalization.
        LifeOsOverlayLocalization {
            SheetBody(
                existing = existing,
                projects = projects,
                currentMitCount = currentMitCount,
                title = title,
                onTitleChange = { title = it },
                notes = notes,
                onNotesChange = { notes = it },
                projectId = projectId,
                onProjectChange = { projectId = it },
                dueDate = dueDate,
                onDueDateChange = { dueDate = it },
                onPickDate = { showDatePicker = true },
                isMit = isMit,
                onMitToggle = { isMit = !isMit },
                repeatRule = repeatRule,
                onRepeatChange = { repeatRule = it },
                onSave = { onSave(TaskDraft(title, notes, projectId, dueDate, isMit, repeatRule)) },
                onDismiss = onDismiss,
                onRequestDelete = if (onDelete == null) null else ({ confirmDelete = true }),
            )
        }
    }

    if (showDatePicker) {
        DueDatePicker(
            initial = dueDate,
            onDismiss = { showDatePicker = false },
            onPicked = {
                dueDate = it
                showDatePicker = false
            },
        )
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

/**
 * The sheet's contents, split out only so the localization wrapper above doesn't cost the whole
 * body an extra indent level.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetBody(
    existing: Task?,
    projects: List<ProjectSummary>,
    currentMitCount: Int,
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    projectId: Long?,
    onProjectChange: (Long?) -> Unit,
    dueDate: LocalDate?,
    onDueDateChange: (LocalDate?) -> Unit,
    onPickDate: () -> Unit,
    isMit: Boolean,
    onMitToggle: () -> Unit,
    repeatRule: RepeatRule?,
    onRepeatChange: (RepeatRule?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRequestDelete: (() -> Unit)?,
) {
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
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.task_title_label)) },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
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
                    onClick = onPickDate,
                    label = {
                        Text(
                            dueDate?.let { stringResource(R.string.date_short, it.monthValue, it.dayOfMonth) }
                                ?: stringResource(R.string.task_pick_date)
                        )
                    },
                )
                AssistChip(
                    onClick = { onDueDateChange(LocalDate.now()) },
                    label = { Text(stringResource(R.string.label_today)) },
                )
                AssistChip(
                    onClick = { onDueDateChange(LocalDate.now().plusDays(1)) },
                    label = { Text(stringResource(R.string.label_tomorrow)) },
                )
                if (dueDate != null) {
                    TextButton(onClick = { onDueDateChange(null) }) {
                        Text(stringResource(R.string.action_clear))
                    }
                }
            }
        }

        // ---- repeat ----
        // A row of chips rather than a switch plus a picker: there are only three intervals, so
        // showing all of them costs one line and removes a step. 「不重复」is a chip too, so turning
        // repetition off is the same gesture as turning it on.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.task_repeat),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = repeatRule == null,
                    onClick = { onRepeatChange(null) },
                    label = { Text(stringResource(R.string.repeat_none)) },
                )
                RepeatRule.entries.forEach { rule ->
                    FilterChip(
                        selected = repeatRule == rule,
                        onClick = { onRepeatChange(rule) },
                        label = { Text(stringResource(rule.labelRes)) },
                    )
                }
            }
            if (repeatRule != null) {
                Text(
                    text = stringResource(R.string.task_repeat_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        // ---- MIT ----
        // Advisory, never blocking: it's the user's day. But 「一天挑一到两件就够」 is the whole
        // point of the flag, and nothing else in the app would ever mention it.
        val othersFlagged = currentMitCount - if (existing?.isMit == true) 1 else 0
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = isMit,
                onClick = onMitToggle,
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
                        onClick = { onProjectChange(null) },
                        label = { Text(stringResource(R.string.label_unassigned)) },
                    )
                    projects.forEach { project ->
                        FilterChip(
                            selected = projectId == project.id,
                            onClick = { onProjectChange(project.id) },
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
            Button(onClick = onSave, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }

            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }

            if (onRequestDelete != null) {
                TextButton(onClick = onRequestDelete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDatePicker(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            LifeOsOverlayLocalization {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        // The picker works in UTC; convert on that basis or the date can slip a day.
                        onPicked(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                    } ?: onDismiss()
                }) { Text(stringResource(R.string.action_ok)) }
            }
        },
        dismissButton = {
            LifeOsOverlayLocalization {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    ) {
        // Wrapped so Material's own strings — the picker's title, the month and weekday names —
        // follow the app language rather than the phone's.
        LifeOsOverlayLocalization {
            DatePicker(state = state)
        }
    }
}
