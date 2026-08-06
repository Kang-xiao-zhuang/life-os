package com.zk.lifeos

import android.app.Application
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.widget.CaptureWidgetProvider
import com.zk.lifeos.widget.MitWidgetProvider
import com.zk.lifeos.widget.MitWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Holds the app-wide object graph, and keeps the launcher shortcut and widget in the right language. */
class LifeOsApplication : Application() {

    lateinit var container: AppContainer
        private set

    /**
     * The language everything outside Compose should use, for callers that cannot suspend.
     *
     * **Do not read this from a widget's `onUpdate`.** It is only correct once the settings Flow has
     * emitted, and `onUpdate` runs in a fresh process during `install -r` — before that. The widget
     * then rendered in the *system* language and, because the broadcast races the push below, could
     * land after it and win. Use [storedLanguage] from a `goAsync` block instead.
     */
    @Volatile
    var currentLanguage: AppLanguage = AppLanguage.DEFAULT
        private set

    /** The persisted language, awaited. Always correct, whatever stage of startup we are at. */
    suspend fun storedLanguage(): AppLanguage = container.settingsService.language.first()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Collects rather than reads once: the first emission publishes the shortcut at startup, and
        // every later one re-publishes it (and any placed widget) in the newly chosen language.
        scope.launch {
            container.settingsService.language.distinctUntilChanged().collect { language ->
                currentLanguage = language
                registerCaptureShortcut(language)
                // Both widgets carry translated labels; each re-reads the language for itself.
                CaptureWidgetProvider.requestUpdate(this@LifeOsApplication)
                MitWidgetProvider.requestUpdate(this@LifeOsApplication)
            }
        }

        // Same shape for reminders: the first emission re-arms whatever was stored (covering an
        // alarm lost to a reboot or a force-stop), and later ones apply the user's edits. Settings
        // therefore only has to write a preference — it never touches AlarmManager.
        scope.launch {
            container.reminderService.settings.distinctUntilChanged().collect {
                container.reminderService.sync()
            }
        }

        // The MIT widget is pushed to rather than polling: the system's own widget refresh floor is
        // 30 minutes, too slow to be right. distinctUntilChanged on the *rendered* state means
        // ticking an unrelated task doesn't redraw anyone's home screen.
        scope.launch {
            mitWidgetState()
                .distinctUntilChanged()
                .collect { MitWidgetProvider.requestUpdate(this@LifeOsApplication) }
        }
    }

    /** Today's MIT reduced to what the widget shows. */
    private fun mitWidgetState(): Flow<MitWidgetState> =
        container.taskService.observeMit().map { tasks ->
            MitWidgetState(
                openTitles = tasks.filterNot { it.done }.map { it.title },
                anyFlagged = tasks.isNotEmpty(),
            )
        }

    /** One-shot read, for [MitWidgetProvider.onUpdate] — it cannot wait on a Flow. */
    suspend fun currentMitState(): MitWidgetState = mitWidgetState().first()

    /**
     * Long-press the launcher icon → 记一笔.
     *
     * Registered at runtime rather than declared in `res/xml/shortcuts.xml` because a static
     * shortcut has to hard-code `targetPackage`, and resource files get no `${applicationId}`
     * substitution — so it would point at the release package and silently do nothing in the
     * `.debug` build. Setting it here works for whatever id the variant actually has, and lets the
     * labels follow the in-app language.
     */
    private fun registerCaptureShortcut(language: AppLanguage) {
        val strings = localized(language)
        val intent = Intent(this, MainActivity::class.java).apply {
            action = LifeOsIntents.ACTION_QUICK_CAPTURE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val shortcut = ShortcutInfoCompat.Builder(this, "quick_capture")
            .setShortLabel(strings.getString(R.string.widget_capture_label))
            .setLongLabel(strings.getString(R.string.shortcut_capture_long_label))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_capture))
            .setIntent(intent)
            .build()

        // Failing to register a shortcut must never stop the app from starting.
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(this, listOf(shortcut)) }
    }
}
