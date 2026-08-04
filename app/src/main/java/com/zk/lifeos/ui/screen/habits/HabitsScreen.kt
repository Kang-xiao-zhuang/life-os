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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.HabitRow
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.PhaseNote
import com.zk.lifeos.ui.components.SectionCard

private val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/** 习惯 — daily check-ins, current streak, and this week at a glance. */
@Composable
fun HabitsScreen(modifier: Modifier = Modifier) {
    val viewModel: HabitsViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val checkedToday = habits.count { it.checkedToday }

    LifeOsScreen(title = "习惯", modifier = modifier) {
        if (habits.isEmpty()) {
            SectionCard(title = "还没有习惯") {
                EmptyHint("每天坚持的小事:📚 阅读 · 🏋 健身 · ✍ 写作 · 🎥 内容创作 · 🇬🇧 英语学习。")
            }
        } else {
            SectionCard(title = "今天", trailing = "$checkedToday / ${habits.size}") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WeekdayHeader()
                    habits.forEach { HabitRow(habit = it) }
                }
            }
        }

        PhaseNote("Phase 3 会接上:新建习惯、每日打卡、编辑与删除。")
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
