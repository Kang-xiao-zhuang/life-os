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
import androidx.compose.ui.unit.dp
import com.zk.lifeos.model.JournalEntry
import java.time.format.TextStyle
import java.util.Locale

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
    val weekday = entry.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)

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
                    text = "${entry.date.year} 年 ${entry.date.monthValue} 月 ${entry.date.dayOfMonth} 日",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Field("今天完成了什么", entry.done)
            Field("今天最大的收获", entry.win)
            Field("今天遇到的问题", entry.problems)
            Field("明天最重要的一件事", entry.tomorrowMit)
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
