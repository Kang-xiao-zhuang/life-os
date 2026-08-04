package com.zk.lifeos.ui.screen.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.CaptureItem
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import java.time.LocalDate

/**
 * 快速记录 — the inbox. One field, one tap, no structure: the moment it asks which project or
 * when it's due, the thought is already gone.
 */
@Composable
fun CaptureScreen(
    modifier: Modifier = Modifier,
    /** True when opened from the home-screen widget or launcher shortcut. */
    autoFocus: Boolean = false,
    onAutoFocusConsumed: () -> Unit = {},
) {
    val viewModel: CaptureViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val inbox by viewModel.inbox.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Coming from the home screen should land on a cursor, not on a screen you still have to tap.
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            onAutoFocusConsumed()
        }
    }

    LifeOsScreen(title = "记录", modifier = modifier) {
        SectionCard(title = "记一笔") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("一个待办、一个想法、一句话…") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.capture(text)
                            // Cleared immediately so the next thought can go straight in.
                            text = ""
                        },
                        enabled = text.isNotBlank(),
                    ) { Text("记下来") }
                    if (text.isNotBlank()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "清空")
                        }
                    }
                }
            }
        }

        SectionCard(
            title = "待整理",
            trailing = if (inbox.isEmpty()) null else "${inbox.size} 条",
        ) {
            if (inbox.isEmpty()) {
                EmptyHint("这里放你随手记下的东西 —— 记完之后再决定它是任务还是想法。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    inbox.forEach { item ->
                        CaptureRow(
                            item = item,
                            onConvert = { viewModel.convertToTask(item) },
                            onDelete = { viewModel.delete(item.id) },
                        )
                    }
                    EmptyHint("→ 变成任务(未归类);✕ 删掉。")
                }
            }
        }
    }
}

@Composable
private fun CaptureRow(
    item: CaptureItem,
    onConvert: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = timeLabel(item),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onConvert) {
            Icon(
                imageVector = Icons.Outlined.AddTask,
                contentDescription = "变成任务",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
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
