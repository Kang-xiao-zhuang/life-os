package com.zk.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zk.lifeos.model.HabitToday

/**
 * One habit line: name, current streak, and this week as seven dots (Mon→Sun).
 *
 * Read-only in Phase 2 — 打卡 is Phase 3, so nothing here is tappable yet.
 */
@Composable
fun HabitRow(
    habit: HabitToday,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                text = if (habit.streak > 0) "连续 ${habit.streak} 天" else "本周 ${habit.weekDoneCount}/7",
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
