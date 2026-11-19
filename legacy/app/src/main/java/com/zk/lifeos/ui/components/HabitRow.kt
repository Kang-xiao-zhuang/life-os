package com.zk.lifeos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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

    // A once-a-day action deserves to feel like it happened.
    val checkScale by animateFloatAsState(
        targetValue = if (habit.checkedToday) 1f else 0.88f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkScale",
    )
    val checkColor by animateColorAsState(
        targetValue = if (habit.checkedToday) scheme.secondary else scheme.outline,
        animationSpec = tween(220),
        label = "checkColor",
    )

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
            tint = checkColor,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { scaleX = checkScale; scaleY = checkScale },
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

/**
 * One column of the week strip.
 *
 * The dot itself is 8dp, but the column has to be wide enough for the 一/二/三 label that sits above
 * it on the Habits screen. Sizing the label to the *dot* (8dp) is what was clipping those characters:
 * a 汉字 is wider than 8dp at any readable size, so each one overflowed its box into its neighbour.
 * Both rows lay out on this same width, which is also what keeps a label centred over its own dot.
 */
internal val WeekColumnWidth = 15.dp

/** Seven dots, Monday first. Filled = checked; hollow = not. */
@Composable
fun WeekDots(week: List<Boolean>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier) {
        week.forEach { done ->
            // Today's dot fills in as you check in, so the change is visible in two places at once
            // and the week strip stops looking like a static decoration.
            val dotColor by animateColorAsState(
                targetValue = if (done) scheme.secondary else scheme.surfaceVariant,
                animationSpec = tween(260),
                label = "weekDot",
            )
            Box(
                modifier = Modifier.width(WeekColumnWidth),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color = dotColor, shape = CircleShape)
                )
            }
        }
    }
}
