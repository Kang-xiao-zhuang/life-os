package com.zk.lifeos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zk.lifeos.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zk.lifeos.model.Task
import java.time.LocalDate

/**
 * One task line: tap the circle to tick it, tap the text to edit it.
 *
 * The tap targets are separate on purpose — ticking something off is the most frequent action
 * and must not risk opening an editor by accident.
 */
@Composable
fun TaskRow(
    task: Task,
    today: LocalDate,
    modifier: Modifier = Modifier,
    /** Where the task lives. Shown only outside its own project, where it's ambiguous. */
    projectLabel: String? = null,
    onToggle: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val overdue = task.isOverdue(today)

    Row(
        modifier = modifier.fillMaxWidth(),
        // Top, not centre: a row can now be three lines tall (title + 备注 + project), and a tick
        // floating beside the second line reads as belonging to it rather than to the task.
        // Identical to centring for the common single-line row.
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (task.done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (task.done) stringResource(R.string.task_mark_undone) else stringResource(R.string.task_mark_done),
            tint = if (task.done) scheme.secondary else scheme.outline,
            modifier = Modifier
                .clip(CircleShape)
                .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier)
                // Padding inside the clickable so the touch target is comfortable while the
                // glyph stays small and quiet.
                .padding(6.dp)
                .size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (task.done) scheme.onSurfaceVariant else scheme.onSurface,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // 备注 was write-only: you could add it in the sheet and then never see it again without
            // reopening the task. One ellipsized line is enough for「记得带充电器」to do its job,
            // and it stays out of the way on finished tasks, where it is no longer a reminder.
            if (task.notes.isNotBlank() && !task.done) {
                Text(
                    // Flattened: with maxLines = 1, a note that starts with a blank line would
                    // otherwise render as an empty row.
                    text = task.notes.replace('\n', ' ').trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (projectLabel != null) {
                Text(
                    text = projectLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (task.dueDate != null && !task.done) {
            Text(
                text = dueLabel(task.dueDate, today),
                style = MaterialTheme.typography.labelSmall,
                color = if (overdue) scheme.error else scheme.onSurfaceVariant,
                // Same 6dp the title column carries, so the date sits on the title's line rather
                // than at the very top of a three-line row.
                modifier = Modifier.padding(start = 8.dp, top = 6.dp),
            )
        }
    }
}

/** Relative where it helps (today / yesterday / tomorrow), absolute where it doesn't. */
@Composable
private fun dueLabel(due: LocalDate, today: LocalDate): String = when (due) {
    today -> stringResource(R.string.label_today)
    today.minusDays(1) -> stringResource(R.string.label_yesterday)
    today.plusDays(1) -> stringResource(R.string.label_tomorrow)
    else -> stringResource(R.string.date_short, due.monthValue, due.dayOfMonth)
}
