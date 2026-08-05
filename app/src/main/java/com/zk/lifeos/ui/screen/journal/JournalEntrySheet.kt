package com.zk.lifeos.ui.screen.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zk.lifeos.R
import com.zk.lifeos.model.JournalEntry
import com.zk.lifeos.ui.currentLocale
import java.time.format.TextStyle

/**
 * Reads back one past review in full.
 *
 * A sheet rather than a page: looking something up shouldn't cost you your place in the list.
 * Read-only — editing history is a separate decision, and silently letting old entries be
 * rewritten would undermine the point of keeping them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntrySheet(
    entry: JournalEntry,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = currentLocale()
    val weekday = entry.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val monthName = entry.date.month.getDisplayName(TextStyle.FULL, locale)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(
                        R.string.journal_date_full,
                        monthName,
                        entry.date.dayOfMonth,
                        entry.date.year,
                        entry.date.monthValue,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Field(stringResource(R.string.journal_q_done), entry.done)
            Field(stringResource(R.string.journal_q_win), entry.win)
            Field(stringResource(R.string.journal_q_problems), entry.problems)
            Field(stringResource(R.string.journal_q_tomorrow), entry.tomorrowMit)
        }
    }
}

/** Blank prompts are skipped — an empty heading tells the reader nothing. */
@Composable
private fun Field(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
