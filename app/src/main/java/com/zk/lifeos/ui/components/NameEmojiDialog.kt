package com.zk.lifeos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zk.lifeos.R

/** Emoji options offered when creating a project. Picking one is optional. */
val projectEmojis = listOf("💼", "📚", "🏋", "🎥", "✍", "💰", "🏠", "🎯", "🧑‍💻", "🌱")

/** Emoji options for habits — the spec's own examples first. */
val habitEmojis = listOf("📚", "🏋", "✍", "🎥", "🇬🇧", "🧘", "💧", "🌅", "🏃", "🎸")

/**
 * One dialog for both「新建/重命名 项目」and「新建/编辑 习惯」— same shape, so one component.
 *
 * The emoji row is a picker rather than a free text field: typing an emoji on Android is fiddly,
 * and a fixed set keeps the lists visually consistent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NameEmojiDialog(
    title: String,
    label: String,
    emojiOptions: List<String>,
    initialName: String = "",
    initialEmoji: String = "",
    confirmText: String = stringResource(R.string.action_save),
    /** Optional third action when editing — 「删除」for habits, 「归档」for projects. */
    destructiveText: String? = null,
    onDestructive: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.label_icon_optional),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    emojiOptions.forEach { option ->
                        FilterChip(
                            selected = emoji == option,
                            // Tapping the selected one clears it, so "no icon" stays reachable.
                            onClick = { emoji = if (emoji == option) "" else option },
                            label = { Text(option) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, emoji) },
                enabled = name.isNotBlank(),
            ) { Text(confirmText) }
        },
        dismissButton = {
            if (destructiveText != null && onDestructive != null) {
                TextButton(onClick = onDestructive) {
                    Text(destructiveText, color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

/** Confirmation for anything that destroys data. Never used for reversible actions. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.action_delete),
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
