package com.zk.lifeos.ui.screen.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.R
import com.zk.lifeos.model.HabitToday
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.ConfirmDialog
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.EmptyState
import com.zk.lifeos.ui.components.HabitRow
import com.zk.lifeos.ui.components.LifeOsFab
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.NameEmojiDialog
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.WeekColumnWidth
import com.zk.lifeos.ui.components.habitEmojis
import com.zk.lifeos.ui.components.pieceCount

/**
 * 习惯 — tap a row to check today off (tap again to undo), long-press to edit or delete.
 */
@Composable
fun HabitsScreen(
    onOpenArchived: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HabitsViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val archivedCount by viewModel.archivedCount.collectAsStateWithLifecycle()
    val checkedToday = habits.count { it.checkedToday }

    // Coming back to a resident app after midnight must not leave 今天 pointing at yesterday —
    // here that mismatch made a tap record a check-in for a day nothing had been done on.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose {}
    }

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<HabitToday?>(null) }
    var archiving by remember { mutableStateOf<HabitToday?>(null) }

    LifeOsScreen(
        title = stringResource(R.string.habits_title),
        modifier = modifier,
        floatingActionButton = { LifeOsFab(stringResource(R.string.habit_new)) { creating = true } },
    ) {
        if (habits.isEmpty()) {
            SectionCard(title = stringResource(R.string.habits_empty_title)) {
                EmptyState(
                    icon = Icons.Outlined.LocalFireDepartment,
                    text = stringResource(R.string.habits_empty_hint),
                )
            }
        } else {
            SectionCard(
                title = stringResource(R.string.habits_today),
                trailing = stringResource(R.string.habit_progress, checkedToday, habits.size),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekdayHeader()
                    habits.forEach { habit ->
                        HabitRow(
                            habit = habit,
                            onToggle = { viewModel.toggleToday(habit.id) },
                            onLongClick = { editing = habit },
                        )
                    }
                    EmptyHint(stringResource(R.string.habits_hint))
                }
            }

            // Neutral title: the card can be paged back to earlier months, so「这个月」would lie.
            // Quiet: today's check-ins are what you came for; the month is what you look at after.
            SectionCard(title = stringResource(R.string.habits_month), quiet = true) {
                HabitHeatmap(
                    month = month,
                    onPreviousMonth = viewModel::showPreviousMonth,
                    onNextMonth = viewModel::showNextMonth,
                )
            }
        }

        if (archivedCount > 0) {
            SectionCard(
                title = stringResource(R.string.archived_title),
                trailing = pieceCount(archivedCount),
                quiet = true,
                onClick = onOpenArchived,
            ) {
                EmptyHint(stringResource(R.string.habits_archived_hint))
            }
        }
    }

    if (creating) {
        NameEmojiDialog(
            title = stringResource(R.string.habit_create),
            label = stringResource(R.string.habit_name_label),
            emojiOptions = habitEmojis,
            confirmText = stringResource(R.string.action_create),
            onDismiss = { creating = false },
            onConfirm = { name, emoji ->
                viewModel.create(name, emoji)
                creating = false
            },
        )
    }

    editing?.let { habit ->
        NameEmojiDialog(
            title = stringResource(R.string.habit_edit),
            label = stringResource(R.string.habit_name_label),
            emojiOptions = habitEmojis,
            initialName = habit.name,
            initialEmoji = habit.emoji,
            // 归档, not 删除: stopping a habit shouldn't cost you its history. Permanent deletion
            // is only reachable from the archive screen.
            destructiveText = stringResource(R.string.action_archive),
            onDestructive = {
                editing = null
                archiving = habit
            },
            onDismiss = { editing = null },
            onConfirm = { name, emoji ->
                viewModel.rename(habit.id, name, emoji)
                editing = null
            },
        )
    }

    archiving?.let { habit ->
        ConfirmDialog(
            title = stringResource(R.string.habit_archive_title, habit.name),
            message = stringResource(R.string.habit_archive_message),
            confirmText = stringResource(R.string.action_archive),
            onDismiss = { archiving = null },
            onConfirm = {
                viewModel.archive(habit.id)
                archiving = null
            },
        )
    }
}

/** Column labels for the week dots, aligned to the right like the dots themselves. */
@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        // Same column geometry as the dots below, so each label sits over its own day — and so a
        // 汉字 has room to draw. It used to be boxed at the dot's 8dp and was clipped.
        Row {
            stringArrayResource(R.array.weekday_initials).forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(WeekColumnWidth),
                )
            }
        }
    }
}
