package com.zk.lifeos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * The app's only "add" button style, in one place.
 *
 * Material's default extended FAB uses `primaryContainer`, which in this dark palette is the deep
 * blue — the loudest thing on the screen and at odds with 「Calm · Minimal」. Using `primary`
 * (the soft periwinkle) with dark content keeps it obviously the main action while staying quiet.
 */
@Composable
fun LifeOsFab(text: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        // Described rather than decorative, so a screen reader announces what it adds.
        icon = { Icon(Icons.Filled.Add, contentDescription = text) },
        text = { Text(text) },
    )
}
