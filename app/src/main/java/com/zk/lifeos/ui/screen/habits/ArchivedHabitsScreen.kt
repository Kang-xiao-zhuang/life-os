package com.zk.lifeos.ui.screen.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.ArchivedHabit
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.ConfirmDialog
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard

/**
 * 已归档的习惯 — retired, not lost.
 *
 * Stopping a habit used to mean deleting it, which took every check-in with it. Now stopping is
 * archiving, and this is the only place a habit can actually be destroyed — with its history
 * count spelled out, because「删除」alone hides what it costs.
 */
@Composable
fun ArchivedHabitsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ArchivedHabitsViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<ArchivedHabit?>(null) }

    LifeOsScreen(
        title = "已归档",
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
    ) {
        if (habits.isEmpty()) {
            SectionCard(title = "没有归档的习惯") {
                EmptyHint("长按一个习惯可以归档它。归档后打卡记录都还在,以后想继续随时恢复。")
            }
            return@LifeOsScreen
        }

        habits.forEach { habit ->
            SectionCard(
                title = if (habit.emoji.isEmpty()) habit.name else "${habit.emoji}  ${habit.name}",
                trailing = "${habit.checkCount} 次打卡",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.restore(habit.id) }) { Text("恢复") }
                    TextButton(onClick = { deleting = habit }) {
                        Text("彻底删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EmptyHint("恢复后它会重新回到今天的清单里,连续天数按原来的记录继续算。")
            EmptyHint("彻底删除会把这个习惯的全部打卡记录一起删掉,没法撤销。")
        }
    }

    deleting?.let { habit ->
        ConfirmDialog(
            title = "彻底删除「${habit.name}」?",
            message = if (habit.checkCount == 0) {
                "这个习惯会被永久删除,无法恢复。"
            } else {
                "这个习惯和它的 ${habit.checkCount} 次打卡记录会被永久删除,无法恢复。" +
                    "如果只是暂时不做了,留在归档里就好。"
            },
            confirmText = "彻底删除",
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.deletePermanently(habit.id)
                deleting = null
            },
        )
    }
}
