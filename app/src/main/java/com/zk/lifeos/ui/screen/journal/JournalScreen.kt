package com.zk.lifeos.ui.screen.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    history.forEach { entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${entry.date.monthValue}/${entry.date.dayOfMonth}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = entry.win.ifBlank { entry.done }.take(80),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
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
