package com.zk.lifeos.ui.screen.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.R
import com.zk.lifeos.model.ArchivedHabit
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.ConfirmDialog
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.checkinCount

/**
 * 已归档的习惯 — retired, not lost.
 *
 * Stopping a habit used to mean deleting it, which took every check-in with it. Now stopping is
 * archiving, and this is the only place a habit can actually be destroyed — with its history
 * count spelled out, because「删除」alone hides what it costs.
 */
@Composable
fun ArchivedHabitsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ArchivedHabitsViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<ArchivedHabit?>(null) }

    LifeOsScreen(
        title = stringResource(R.string.archived_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
    ) {
        if (habits.isEmpty()) {
            SectionCard(title = stringResource(R.string.archived_habits_empty_title)) {
                EmptyHint(stringResource(R.string.archived_habits_empty_hint))
            }
            return@LifeOsScreen
        }

        habits.forEach { habit ->
            SectionCard(
                title = if (habit.emoji.isEmpty()) habit.name else "${habit.emoji}  ${habit.name}",
                trailing = checkinCount(habit.checkCount),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.restore(habit.id) }) { Text(stringResource(R.string.action_restore)) }
                    TextButton(onClick = { deleting = habit }) {
                        Text(stringResource(R.string.action_delete_forever), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EmptyHint(stringResource(R.string.archived_habits_restore_note))
            EmptyHint(stringResource(R.string.archived_habits_delete_note))
        }
    }

    deleting?.let { habit ->
        ConfirmDialog(
            title = stringResource(R.string.archived_habit_delete_title, habit.name),
            message = if (habit.checkCount == 0) {
                stringResource(R.string.archived_habit_delete_message_empty)
            } else {
                stringResource(R.string.archived_habit_delete_message, habit.checkCount)
            },
            confirmText = stringResource(R.string.action_delete_forever),
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.deletePermanently(habit.id)
                deleting = null
            },
        )
    }
}
