package com.zk.lifeos.ui.screen.journal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zk.lifeos.ui.components.PlaceholderScreen

@Composable
fun JournalScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "每日复盘",
        description = "每天一篇:完成了什么、最大的收获、遇到的问题、明天最重要的一件事。",
        modifier = modifier,
    )
}
