package com.zk.lifeos.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zk.lifeos.model.Task
import java.time.LocalDate

/**
 * One task line.
 *
 * The leading circle is a **status indicator, not a checkbox** — completing a task is Phase 3,
 * and a control that silently does nothing is worse than no control at all.
 */
@Composable
fun TaskRow(
    task: Task,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val overdue = task.isOverdue(today)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (task.done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (task.done) "已完成" else "未完成",
            tint = if (task.done) scheme.secondary else scheme.outline,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.done) scheme.onSurfaceVariant else scheme.onSurface,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (task.dueDate != null && !task.done) {
            Text(
                text = dueLabel(task.dueDate, today),
                style = MaterialTheme.typography.labelSmall,
                color = if (overdue) scheme.error else scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Relative where it helps ("今天"/"昨天"), absolute where it doesn't. */
private fun dueLabel(due: LocalDate, today: LocalDate): String = when (due) {
    today -> "今天"
    today.minusDays(1) -> "昨天"
    today.plusDays(1) -> "明天"
    else -> "${due.monthValue}/${due.dayOfMonth}"
}
