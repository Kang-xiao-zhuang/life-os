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
import androidx.compose.material.icons.outlined.Inbox
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.R
import com.zk.lifeos.model.CaptureItem
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.EmptyState
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.noteCount
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

    LifeOsScreen(title = stringResource(R.string.capture_title), modifier = modifier) {
        SectionCard(title = stringResource(R.string.capture_card_title)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.capture_placeholder)) },
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
                    ) { Text(stringResource(R.string.capture_submit)) }
                    if (text.isNotBlank()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.capture_clear))
                        }
                    }
                }
            }
        }

        // Quiet: the field above is the point of this screen; the inbox is what accumulates from it.
        SectionCard(
            title = stringResource(R.string.capture_inbox),
            trailing = if (inbox.isEmpty()) null else noteCount(inbox.size),
            quiet = true,
        ) {
            if (inbox.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    text = stringResource(R.string.capture_inbox_empty),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    inbox.forEach { item ->
                        CaptureRow(
                            item = item,
                            onConvert = { viewModel.convertToTask(item) },
                            onDelete = { viewModel.delete(item.id) },
                        )
                    }
                    EmptyHint(stringResource(R.string.capture_actions_hint))
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
                contentDescription = stringResource(R.string.capture_to_task),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Time for today's items, date for older ones — the useful half of the timestamp. */
@Composable
private fun timeLabel(item: CaptureItem): String {
    val date = item.createdAt.toLocalDate()
    return if (date == LocalDate.now()) {
        "%02d:%02d".format(item.createdAt.hour, item.createdAt.minute)
    } else {
        stringResource(R.string.date_short, date.monthValue, date.dayOfMonth)
    }
}
