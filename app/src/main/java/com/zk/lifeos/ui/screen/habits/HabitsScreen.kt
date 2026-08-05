package com.zk.lifeos.ui.screen.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.HabitToday
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.ConfirmDialog
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.HabitRow
import com.zk.lifeos.ui.components.LifeOsFab
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.NameEmojiDialog
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.components.habitEmojis

private val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 习惯 — tap a row to check today off (tap again to undo), long-press to edit or delete.
 */
@Composable
fun HabitsScreen(
    onOpenArchived: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HabitsViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val archivedCount by viewModel.archivedCount.collectAsStateWithLifecycle()
    val checkedToday = habits.count { it.checkedToday }

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<HabitToday?>(null) }
    var archiving by remember { mutableStateOf<HabitToday?>(null) }

    LifeOsScreen(
        title = "习惯",
        modifier = modifier,
        floatingActionButton = { LifeOsFab("新习惯") { creating = true } },
    ) {
        if (habits.isEmpty()) {
            SectionCard(title = "还没有习惯") {
                EmptyHint("每天坚持的小事:📚 阅读 · 🏋 健身 · ✍ 写作 · 🎥 内容创作 · 🇬🇧 英语学习。")
            }
        } else {
            SectionCard(title = "今天", trailing = "$checkedToday / ${habits.size}") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekdayHeader()
                    habits.forEach { habit ->
                        HabitRow(
                            habit = habit,
                            onToggle = { viewModel.toggleToday(habit.id) },
                            onLongClick = { editing = habit },
                        )
                    }
                    EmptyHint("点一下打卡,再点一下取消;长按可以编辑。")
                }
            }

            // Neutral title: the card can be paged back to earlier months, so「这个月」would lie.
            SectionCard(title = "月度打卡") {
                HabitHeatmap(
                    month = month,
                    onPreviousMonth = viewModel::showPreviousMonth,
                    onNextMonth = viewModel::showNextMonth,
                )
            }
        }

        if (archivedCount > 0) {
            SectionCard(title = "已归档", trailing = "$archivedCount 个", onClick = onOpenArchived) {
                EmptyHint("停下来的习惯在这里,打卡记录都留着,想继续随时恢复。")
            }
        }
    }

    if (creating) {
        NameEmojiDialog(
            title = "新建习惯",
            label = "习惯名称",
            emojiOptions = habitEmojis,
            confirmText = "创建",
            onDismiss = { creating = false },
            onConfirm = { name, emoji ->
                viewModel.create(name, emoji)
                creating = false
            },
        )
    }

    editing?.let { habit ->
        NameEmojiDialog(
            title = "编辑习惯",
            label = "习惯名称",
            emojiOptions = habitEmojis,
            initialName = habit.name,
            initialEmoji = habit.emoji,
            // 归档, not 删除: stopping a habit shouldn't cost you its history. Permanent deletion
            // is only reachable from the archive screen.
            destructiveText = "归档",
            onDestructive = {
                editing = null
                archiving = habit
            },
            onDismiss = { editing = null },
            onConfirm = { name, emoji ->
                viewModel.rename(habit.id, name, emoji)
                editing = null
            },
        )
    }

    archiving?.let { habit ->
        ConfirmDialog(
            title = "归档「${habit.name}」?",
            message = "它会从今天的清单里移走,但打卡记录都留着 —— 以后想继续,在「已归档」里恢复就行。",
            confirmText = "归档",
            onDismiss = { archiving = null },
            onConfirm = {
                viewModel.archive(habit.id)
                archiving = null
            },
        )
    }
}

/** Column labels for the week dots, aligned to the right like the dots themselves. */
@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(8.dp),
                )
            }
        }
    }
}
