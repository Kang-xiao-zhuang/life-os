package com.zk.lifeos.ui.screen.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.JournalEntry
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.PhaseNote
import com.zk.lifeos.ui.components.SectionCard

/** 每日复盘 — one entry per day, four fixed prompts. */
@Composable
fun JournalScreen(modifier: Modifier = Modifier) {
    val viewModel: JournalViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val today by viewModel.today.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()

    // Today's entry is shown separately above, so it isn't repeated in the history list.
    val history = recent.filter { it.date != today.date }

    LifeOsScreen(title = "复盘", modifier = modifier) {
        SectionCard(
            title = "今天",
            trailing = if (today.isEmpty) "未写" else "已写",
        ) {
            if (today.isEmpty) {
                EmptyHint("今天还没有复盘。四个问题,几分钟就能写完。")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Prompt("今天完成了什么")
                    Prompt("今天最大的收获")
                    Prompt("今天遇到的问题")
                    Prompt("明天最重要的一件事")
                }
            } else {
                EntryBody(today)
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

        PhaseNote("Phase 3 会接上:写入与编辑复盘(支持 Markdown)。")
    }
}

@Composable
private fun Prompt(text: String) {
    Text(
        text = "· $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun EntryBody(entry: JournalEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("今天完成了什么", entry.done)
        Field("今天最大的收获", entry.win)
        Field("今天遇到的问题", entry.problems)
        Field("明天最重要的一件事", entry.tomorrowMit)
    }
}

@Composable
private fun Field(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
