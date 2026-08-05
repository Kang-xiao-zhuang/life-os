package com.zk.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zk.lifeos.R
import com.zk.lifeos.model.HabitToday

/**
 * One habit line: tap anywhere to check today off (tap again to undo), long-press to edit.
 *
 * The whole row is the target rather than a small checkbox — this is a once-a-day action that
 * should take no aim.
 */
@Composable
fun HabitRow(
    habit: HabitToday,
    modifier: Modifier = Modifier,
    onToggle: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onToggle != null) {
                    Modifier.combinedClickable(onClick = onToggle, onLongClick = onLongClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (habit.checkedToday) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (habit.checkedToday) scheme.secondary else scheme.outline,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        if (habit.emoji.isNotEmpty()) {
            Text(text = habit.emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (habit.checkedToday) scheme.onSurface else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (habit.streak > 0) {
                    stringResource(R.string.habit_streak, habit.streak)
                } else {
                    stringResource(R.string.habit_week_progress, habit.weekDoneCount)
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.outline,
            )
        }
        WeekDots(week = habit.week)
    }
}

/** Seven dots, Monday first. Filled = checked; hollow = not. */
@Composable
fun WeekDots(week: List<Boolean>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        week.forEach { done ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (done) scheme.secondary else scheme.surfaceVariant,
                        shape = CircleShape,
                    )
            )
        }
    }
}
