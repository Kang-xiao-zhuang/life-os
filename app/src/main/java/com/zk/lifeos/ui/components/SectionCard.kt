package com.zk.lifeos.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The one card shape the whole app uses: title row, optional trailing text, content below.
 * 卡片式布局 + 留白充足 lives here so no screen re-invents its own padding.
 *
 * [quiet] steps a card down a level. Dashboard used to be five cards of identical weight, so the
 * one that justifies the app (今日最重要) looked exactly like the one that is merely an entrance
 * (快速记录) — nothing told your eye where to start. The quiet variant is what everything *except*
 * the screen's main point uses.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    quiet: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (quiet) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(if (quiet) 16.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (quiet) 10.dp else 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    // The size step does most of the work; in light mode the surface tones are only
                    // a few percent apart and can't carry the hierarchy on their own.
                    style = if (quiet) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = if (quiet) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                if (trailing != null) {
                    // Animated because these counters change under your finger — ticking a task off
                    // used to make「3 项」snap to「2 项」with no sense that you'd caused it.
                    AnimatedContent(
                        targetState = trailing,
                        transitionSpec = {
                            (fadeIn(tween(180)) + slideInVertically { it / 3 }) togetherWith
                                (fadeOut(tween(120)) + slideOutVertically { -it / 3 })
                        },
                        label = "trailing",
                    ) { value ->
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

/** Muted one-liner for "nothing here yet". Never an error, just an empty state. */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * A screen's "there is nothing here yet" state: a faint mark, then the sentence.
 *
 * Distinct from [EmptyHint], which is an inline aside. This is what you see on your *first* visit to
 * a tab, and it used to be a grey paragraph — the one place the app most looked undesigned. The icon
 * is drawn at low opacity on purpose: it should give the empty screen a centre of gravity, not
 * announce itself.
 *
 * Use the tab's own icon, so an empty Projects screen still says「项目」without words.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
            modifier = Modifier.size(44.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Marks work that is deliberately not built yet, so a laid-out-but-inert screen can't be
 * mistaken for a finished one. Delete these as each Phase 3 feature lands.
 */
@Composable
fun PhaseNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier,
    )
}
