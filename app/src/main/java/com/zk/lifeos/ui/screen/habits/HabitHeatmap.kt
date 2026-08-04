package com.zk.lifeos.ui.screen.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zk.lifeos.model.HabitMonth
import java.time.DayOfWeek
import java.time.LocalDate

private val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 这个月坚持得怎么样 — a month of check-ins as a shaded grid, Monday first.
 *
 * A week of dots (which is all the habit rows show) can't answer whether a habit is actually
 * holding. Shading is by *completion* (checked ÷ active habits), not a raw count, so a day means
 * the same thing whether you track two habits or eight.
 *
 * Built from plain Rows rather than a lazy grid: it lives inside a scrolling column, and nesting
 * a scrollable grid there fights the parent for gestures.
 */
@Composable
fun HabitHeatmap(
    month: HabitMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val firstDay = month.month.atDay(1)
    val daysInMonth = month.month.lengthOfMonth()
    // Monday = 0 … Sunday = 6, so the first row starts in the right column.
    val leadingBlanks = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
            }
            Text(
                text = "${month.month.year} 年 ${month.month.monthValue} 月",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            // Nothing to show past this month, so don't offer to go there.
            IconButton(onClick = onNextMonth, enabled = !month.isCurrentMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下个月",
                    tint = if (month.isCurrentMonth) scheme.surfaceVariant else scheme.onSurfaceVariant,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val cells = leadingBlanks + daysInMonth
        val weeks = (cells + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(weeks) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(7) { column ->
                        val index = week * 7 + column
                        val day = index - leadingBlanks + 1
                        if (day in 1..daysInMonth) {
                            DayCell(
                                day = day,
                                completion = month.completion(day),
                                isToday = month.month.atDay(day) == today,
                                isFuture = month.month.atDay(day).isAfter(today),
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Text(
            text = if (month.habitCount == 0) {
                "还没有习惯,先加一个。"
            } else {
                "打卡 ${month.activeDays} 天 · 全勤 ${month.perfectDays} 天"
            },
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayCell(
    day: Int,
    completion: Float?,
    isToday: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // Empty days stay near the surface colour so the month reads as texture, not as a wall of
    // failure; done days fade up through the "done" accent.
    val fill = when {
        isFuture -> scheme.surface
        completion == null || completion == 0f -> scheme.surfaceVariant
        else -> scheme.secondary.copy(alpha = 0.25f + 0.75f * completion)
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isFuture -> scheme.outline
                completion != null && completion > 0.5f -> scheme.onSecondary
                else -> scheme.onSurfaceVariant
            },
            // Today is marked by weight rather than another colour — the grid already uses colour
            // to mean "how much did I do".
            fontWeight = if (isToday) androidx.compose.ui.text.font.FontWeight.Bold else null,
        )
    }
}
