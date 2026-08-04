package com.zk.lifeos.ui.screen.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.CaptureItem
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.PhaseNote
import com.zk.lifeos.ui.components.SectionCard
import java.time.LocalDate

/**
 * 快速记录 — the inbox. Deliberately unstructured: anything typed in one tap lands here and is
 * sorted out later.
 */
@Composable
fun CaptureScreen(modifier: Modifier = Modifier) {
    val viewModel: CaptureViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val inbox by viewModel.inbox.collectAsStateWithLifecycle()

    LifeOsScreen(title = "记录", modifier = modifier) {
        SectionCard(
            title = "待整理",
            trailing = if (inbox.isEmpty()) null else "${inbox.size} 条",
        ) {
            if (inbox.isEmpty()) {
                EmptyHint("这里放你随手记下的东西 —— 一个待办、一个想法、一句话、一个创意。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    inbox.forEach { CaptureRow(it) }
                }
            }
        }

        PhaseNote("Phase 3 会接上:一键记录输入框,以及把记录整理成任务。")
    }
}

@Composable
private fun CaptureRow(item: CaptureItem) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = timeLabel(item),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/** Time for today's items, date for older ones — the useful half of the timestamp. */
private fun timeLabel(item: CaptureItem): String {
    val date = item.createdAt.toLocalDate()
    return if (date == LocalDate.now()) {
        "%02d:%02d".format(item.createdAt.hour, item.createdAt.minute)
    } else {
        "${date.monthValue}/${date.dayOfMonth}"
    }
}
