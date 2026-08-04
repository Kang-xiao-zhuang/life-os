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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.JournalEntry
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard

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

    LifeOsScreen(title = "复盘", modifier = modifier) {
        SectionCard(
            title = "今天",
            trailing = when {
                dirty -> "未保存"
                draft.isEmpty -> "未写"
                else -> "已写"
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("今天完成了什么", draft.done, viewModel::setDone)
                Field("今天最大的收获", draft.win, viewModel::setWin)
                Field("今天遇到的问题", draft.problems, viewModel::setProblems)
                Field("明天最重要的一件事", draft.tomorrowMit, viewModel::setTomorrowMit)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = viewModel::save, enabled = dirty) { Text("保存") }
                }
                if (dirty) {
                    EmptyHint("改动还没保存。")
                }
            }
        }

        if (history.isNotEmpty()) {
            SectionCard(title = "以前", trailing = "${history.size} 篇") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    history.forEach { entry ->
                        HistoryRow(entry = entry, onClick = { viewing = entry })
                    }
                    EmptyHint("点一条可以看当天写的全部内容。")
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
            text = "${entry.date.monthValue}/${entry.date.dayOfMonth}",
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
