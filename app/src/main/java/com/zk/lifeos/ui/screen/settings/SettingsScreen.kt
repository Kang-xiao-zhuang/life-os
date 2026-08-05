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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zk.lifeos.R
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.model.BackupCounts
import com.zk.lifeos.model.BackupFailure
import com.zk.lifeos.model.ThemeMode
import com.zk.lifeos.ui.LifeOsViewModelFactory
import com.zk.lifeos.ui.components.ConfirmDialog
import com.zk.lifeos.ui.components.EmptyHint
import com.zk.lifeos.ui.components.LifeOsScreen
import com.zk.lifeos.ui.components.SectionCard
import com.zk.lifeos.ui.currentLocale
import com.zk.lifeos.widget.WidgetPinning
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

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
    val language by viewModel.language.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
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
        title = stringResource(R.string.nav_settings),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
    ) {
        ThemeCard(themeMode = themeMode, onSelect = viewModel::setThemeMode)

        LanguageCard(language = language, onSelect = viewModel::setLanguage)

        SectionCard(title = stringResource(R.string.settings_data)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EmptyHint(stringResource(R.string.settings_data_hint))

                LastBackupLine(lastBackupAt)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                        enabled = !busy,
                    ) { Text(stringResource(R.string.settings_export)) }
                    OutlinedButton(
                        onClick = { confirmImport = true },
                        enabled = !busy,
                    ) { Text(stringResource(R.string.settings_import)) }
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
                        text = statusText(current),
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
            title = stringResource(R.string.settings_import_title),
            message = stringResource(R.string.settings_import_message),
            confirmText = stringResource(R.string.settings_choose_file),
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

/**
 * How stale the backup is.
 *
 * Everything lives on one device with no sync, so the honest thing to show is not a button but the
 * date: "上次备份:12 天前" is what makes someone tap 导出. It turns amber once the gap is wide
 * enough to hurt, and when there has never been a backup at all.
 */
@Composable
private fun LastBackupLine(lastBackupAt: Instant?) {
    val stale = lastBackupAt == null ||
        ChronoUnit.DAYS.between(lastBackupAt.atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()) >= 14
    Text(
        text = if (lastBackupAt == null) {
            stringResource(R.string.settings_never_backed_up)
        } else {
            stringResource(R.string.settings_last_backup, relativeDay(lastBackupAt))
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (stale) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 今天 / 昨天 / N 天前 for the recent past, an absolute date once "N 天前" stops meaning anything. */
@Composable
private fun relativeDay(instant: Instant): String {
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(date, LocalDate.now())
    return when {
        days <= 0L -> stringResource(R.string.label_today)
        days == 1L -> stringResource(R.string.label_yesterday)
        days < 30L -> pluralStringResource(R.plurals.count_days_ago, days.toInt(), days.toInt())
        else -> stringResource(
            R.string.journal_date_full,
            date.month.getDisplayName(TextStyle.FULL, currentLocale()),
            date.dayOfMonth,
            date.year,
            date.monthValue,
        )
    }
}

/** The typed [BackupStatus] rendered into the current language. */
@Composable
private fun statusText(status: BackupStatus): String = when (status) {
    is BackupStatus.Exported -> stringResource(R.string.backup_exported, summary(status.counts))
    is BackupStatus.Restored -> stringResource(R.string.backup_restored, summary(status.counts))
    is BackupStatus.Failed -> when (val failure = status.failure) {
        BackupFailure.CannotWrite -> stringResource(R.string.backup_error_cannot_write)
        BackupFailure.CannotRead -> stringResource(R.string.backup_error_cannot_read)
        BackupFailure.NotABackup -> stringResource(R.string.backup_error_not_a_backup)
        is BackupFailure.SchemaMismatch -> stringResource(
            R.string.backup_error_schema_mismatch,
            failure.backupVersion,
            failure.appVersion,
        )
        is BackupFailure.Unexpected -> failure.message
            ?.let { stringResource(R.string.backup_error_unexpected, it) }
            ?: stringResource(R.string.backup_error_unknown)
    }
}

@Composable
private fun summary(counts: BackupCounts): String = stringResource(
    R.string.backup_summary,
    counts.projects,
    counts.tasks,
    counts.habits,
    counts.habitChecks,
    counts.captures,
    counts.journalEntries,
)

/** 桌面快捷记录 — offered here because nobody discovers widgets by browsing the widget picker. */
@Composable
private fun HomeScreenCard() {
    val context = LocalContext.current
    val supported = remember { WidgetPinning.isSupported(context) }

    SectionCard(title = stringResource(R.string.settings_home_screen)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EmptyHint(stringResource(R.string.settings_widget_hint))
            if (supported) {
                OutlinedButton(onClick = { WidgetPinning.requestPin(context) }) {
                    Text(stringResource(R.string.settings_add_widget))
                }
            } else {
                EmptyHint(stringResource(R.string.settings_widget_unsupported))
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
    SectionCard(
        title = stringResource(R.string.settings_about),
        trailing = stringResource(R.string.settings_version, version),
    ) {
        EmptyHint(stringResource(R.string.settings_about_text))
    }
}

@Composable
private fun ThemeCard(themeMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark),
    )
    SectionCard(title = stringResource(R.string.settings_appearance)) {
        Segmented(options = options, selected = themeMode, onSelect = onSelect)
    }
}

/**
 * 语言 — an in-app switch, not a jump to the system settings.
 *
 * The names are written in their own language on purpose: someone who opened this card because the
 * app is in a language they can't read still has to be able to find their way out of it.
 */
@Composable
private fun LanguageCard(language: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val options = listOf(
        AppLanguage.SYSTEM to stringResource(R.string.language_system),
        AppLanguage.CHINESE to stringResource(R.string.language_chinese),
        AppLanguage.ENGLISH to stringResource(R.string.language_english),
    )
    SectionCard(title = stringResource(R.string.settings_language)) {
        Segmented(options = options, selected = language, onSelect = onSelect)
    }
}

/** The three-way picker both cards above use. */
@Composable
private fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) },
            )
        }
    }
}
