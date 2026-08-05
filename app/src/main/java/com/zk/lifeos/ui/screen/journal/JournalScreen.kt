package com.zk.lifeos.ui.screen.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.R
import com.zk.lifeos.model.JournalEntry
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.entryCount

/**
 * 每日复盘 — one entry per day, four fixed prompts, saved as written (Markdown is stored
 * verbatim; rendering it is a later polish item, not part of V1).
 */
@Composable
fun JournalScreen(modifier: Modifier = Modifier) {
    val viewModel: JournalViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val dirty by viewModel.dirty.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    var viewing by remember { mutableStateOf<JournalEntry?>(null) }

    // Today's entry is edited above, so it isn't repeated in the history list.
    val history = recent.filter { it.date != draft.date }

    LifeOsScreen(title = stringResource(R.string.journal_title), modifier = modifier) {
        SectionCard(
            title = stringResource(R.string.label_today),
            trailing = when {
                dirty -> stringResource(R.string.journal_unsaved)
                draft.isEmpty -> stringResource(R.string.journal_not_written)
                else -> stringResource(R.string.journal_written)
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(stringResource(R.string.journal_q_done), draft.done, viewModel::setDone)
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
            SectionCard(
                title = stringResource(R.string.journal_history),
                trailing = entryCount(history.size),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    history.forEach { entry ->
                        HistoryRow(entry = entry, onClick = { viewing = entry })
                    }
                    EmptyHint(stringResource(R.string.journal_history_hint))
                }
            }
        }
    }

    viewing?.let { entry ->
        JournalEntrySheet(entry = entry, onDismiss = { viewing = null })
    }
}

/** One line of history. Tappable — writing a review is only worth it if you can read it back. */
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
