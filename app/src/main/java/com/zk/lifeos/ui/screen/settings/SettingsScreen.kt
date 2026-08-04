package com.zk.lifeos.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.model.ThemeMode
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.ConfirmDialog
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.widget.WidgetPinning

/**
 * 设置. Not a bottom-bar tab — reached from the Dashboard top bar, per spec.
 *
 * Export and import go through the system file picker, which is why the app still declares
 * **zero permissions**: the user picks the file and grants access to that one file only.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(factory = LifeOsViewModelFactory.Factory)
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var confirmImport by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::import) }

    LifeOsScreen(
        title = "设置",
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
    ) {
        ThemeCard(themeMode = themeMode, onSelect = viewModel::setThemeMode)

        SectionCard(title = "数据") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EmptyHint("备份是一个 LifeOS_Backup.zip,里面有数据库、附件和设置。存到哪由你选,不联网。")

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                        enabled = !busy,
                    ) { Text("导出") }
                    OutlinedButton(
                        onClick = { confirmImport = true },
                        enabled = !busy,
                    ) { Text("导入") }
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.CenterVertically),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                status?.let { current ->
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (current.isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    )
                }
            }
        }

        HomeScreenCard()

        AboutCard()
    }

    if (confirmImport) {
        ConfirmDialog(
            title = "导入备份?",
            // Plain text — Compose renders no Markdown, so asterisks would just show up.
            message = "现在的项目、任务、习惯、打卡、记录和复盘,会被备份里的内容全部替换。" +
                "如果当前数据还有用,先导出一份再继续。",
            confirmText = "选择文件",
            onDismiss = { confirmImport = false },
            onConfirm = {
                confirmImport = false
                // Some file managers label zips as octet-stream, so accept both rather than
                // hiding a perfectly good backup from the picker.
                importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            },
        )
    }
}

/** 桌面快捷记录 — offered here because nobody discovers widgets by browsing the widget picker. */
@Composable
private fun HomeScreenCard() {
    val context = LocalContext.current
    val supported = remember { WidgetPinning.isSupported(context) }

    SectionCard(title = "桌面") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EmptyHint("放一个「记一笔」小组件在桌面上,想到什么一步就能记下来。长按 App 图标也有同样的入口。")
            if (supported) {
                OutlinedButton(onClick = { WidgetPinning.requestPin(context) }) {
                    Text("添加到桌面")
                }
            } else {
                EmptyHint("这个桌面不支持一键添加,请长按桌面空白处,从小组件列表里找 LifeOS。")
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }
    SectionCard(title = "关于", trailing = "v$version") {
        EmptyHint("LifeOS · 本地优先的个人工作台。数据只存在这台设备上,没有账号,不联网,零权限。")
    }
}

@Composable
private fun ThemeCard(themeMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to "跟随系统",
        ThemeMode.LIGHT to "浅色",
        ThemeMode.DARK to "深色",
    )
    SectionCard(title = "外观") {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label) },
                )
            }
        }
    }
}
