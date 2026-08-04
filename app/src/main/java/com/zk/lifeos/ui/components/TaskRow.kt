package com.zk.lifeos.ui.components

import androidx.compose.foundation.clickable
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
    onToggle: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val overdue = task.isOverdue(today)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (task.done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (task.done) "标记为未完成" else "标记为已完成",
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
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.done) scheme.onSurfaceVariant else scheme.onSurface,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 6.dp),
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
