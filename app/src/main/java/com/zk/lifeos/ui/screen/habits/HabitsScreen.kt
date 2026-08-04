package com.zk.lifeos.ui.screen.habits

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zk.lifeos.ui.components.PlaceholderScreen

@Composable
fun HabitsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "习惯",
        description = "每天坚持的事:打卡、连续天数、本周完成情况。",
        modifier = modifier,
    )
}
