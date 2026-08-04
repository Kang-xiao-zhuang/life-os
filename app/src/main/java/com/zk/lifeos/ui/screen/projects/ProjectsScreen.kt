package com.zk.lifeos.ui.screen.projects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zk.lifeos.ui.components.PlaceholderScreen

@Composable
fun ProjectsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "项目",
        description = "长期在做的事:工作、学习、阅读、健身、自媒体。每个项目下面挂任务。",
        modifier = modifier,
    )
}
