package com.zk.lifeos.ui.screen.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.R
import com.zk.lifeos.model.JournalEntry
import com.zk.lifeos.ui.LifeOsOverlayLocalization
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.entryCount
import com.zk.lifeos.ui.currentLocale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle

/**
 * 每日复盘 — one entry per day, four fixed prompts, saved as written (Markdown is stored
 * verbatim; rendering it is a later polish item, not part of V1).
 *
 * Any past day can be opened and edited, not just today. Reviews are written in the evening and
 * evenings get away from you; a form that only accepts today quietly loses the days you most
 * wanted to write about. History rows open into this same editor rather than a read-only sheet —
 * one place to read and change a day, not two.
 */
@Composable
fun JournalScreen(modifier: Modifier = Modifier) {
    val viewModel: JournalViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val dirty by viewModel.dirty.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val isToday = selectedDate == today
    // The day being edited above isn't repeated in the list below.
    val history = recent.filter { it.date != selectedDate }

    LifeOsScreen(title = stringResource(R.string.journal_title), modifier = modifier) {
        SectionCard(
            title = if (isToday) stringResource(R.string.label_today) else dateLine(selectedDate),
            trailing = when {
                dirty -> stringResource(R.string.journal_unsaved)
                draft.isEmpty -> stringResource(R.string.journal_not_written)
                else -> stringResource(R.string.journal_written)
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DayPicker(
                    isToday = isToday,
                    onPrevious = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                    onToday = viewModel::selectToday,
                    onPick = { showDatePicker = true },
                )

                if (!isToday) {
                    EmptyHint(stringResource(R.string.journal_editing_past))
                }

                Field(stringResource(R.string.journal_q_done), draft.done, viewModel::setDone)
                // Only offered when there is actually something to bring over — an empty day
                // shouldn't advertise a button that would do nothing.
                if (!completions.isEmpty) {
                    FillInCompleted(
                        count = completions.count,
                        onClick = viewModel::fillInCompleted,
                    )
                }
                Field(stringResource(R.string.journal_q_win), draft.win, viewModel::setWin)
                Field(stringResource(R.string.journal_q_problems), draft.problems, viewModel::setProblems)
                Field(stringResource(R.string.journal_q_tomorrow), draft.tomorrowMit, viewModel::setTomorrowMit)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = viewModel::save, enabled = dirty) { Text(stringResource(R.string.action_save)) }
                }
                if (dirty) {
                    EmptyHint(stringResource(R.string.journal_unsaved_hint))
                }
            }
        }

        if (history.isNotEmpty()) {
            // Quiet: the editor above is what you opened this tab to do.
            SectionCard(
                title = stringResource(R.string.journal_history),
                trailing = entryCount(history.size),
                quiet = true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    history.forEach { entry ->
                        HistoryRow(entry = entry, onClick = { viewModel.selectDate(entry.date) })
                    }
                    EmptyHint(stringResource(R.string.journal_history_hint))
                }
            }
        }
    }

    if (showDatePicker) {
        DayPickerDialog(
            selected = selectedDate,
            onDismiss = { showDatePicker = false },
            onPicked = { date ->
                showDatePicker = false
                viewModel.selectDate(date)
            },
        )
    }
}

/**
 * 「带出已经打过勾的 N 项」— the app filling in the one prompt it can actually answer.
 *
 * Sits under 今天完成了什么 rather than above it: the box is the thing, this is an offer. The hint
 * spells out that it appends, because a button that rewrites a box you already typed in is exactly
 * the kind of surprise this app doesn't allow.
 */
@Composable
private fun FillInCompleted(count: Int, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AssistChip(
            onClick = onClick,
            label = { Text(stringResource(R.string.journal_fill_done, count)) },
        )
        EmptyHint(stringResource(R.string.journal_fill_done_hint))
    }
}

/**
 * Arrows for「昨天忘了写」, a chip for anything further back, and a way home.
 *
 * 后一天 stops at today because a review of a day that hasn't happened is not a feature.
 */
@Composable
private fun DayPicker(
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.journal_prev_day),
            )
        }
        IconButton(onClick = onNext, enabled = !isToday) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.journal_next_day),
            )
        }
        AssistChip(onClick = onPick, label = { Text(stringResource(R.string.journal_choose_day)) })
        if (!isToday) {
            TextButton(onClick = onToday) { Text(stringResource(R.string.journal_back_to_today)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(
    selected: LocalDate,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    val todayMillis = remember {
        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli(),
        // Greying out the future is friendlier than accepting a tap and then ignoring it.
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis < todayMillis
                override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
            }
        },
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
        // Wrapped so Material's own strings follow the app language, not the phone's.
        LifeOsOverlayLocalization {
            DatePicker(state = state)
        }
    }
}

/** One line of history. Tappable — it opens that day in the editor above. */
@Composable
private fun HistoryRow(entry: JournalEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = stringResource(R.string.date_short, entry.date.monthValue, entry.date.dayOfMonth),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = entry.win.ifBlank { entry.done }.ifBlank { entry.problems },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 「8 月 3 日 · 星期一」— the weekday matters, it's what tells you which day you mean. */
@Composable
private fun dateLine(date: LocalDate): String = stringResource(
    R.string.date_month_day_weekday,
    date.monthValue,
    date.dayOfMonth,
    date.dayOfWeek.getDisplayName(TextStyle.FULL, currentLocale()),
)

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = 2,
        maxLines = 6,
        modifier = Modifier.fillMaxWidth(),
    )
}
