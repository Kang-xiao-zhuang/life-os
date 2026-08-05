package com.zk.lifeos

import android.app.Application
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.widget.CaptureWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Holds the app-wide object graph, and keeps the launcher shortcut and widget in the right language. */
class LifeOsApplication : Application() {

    lateinit var container: AppContainer
        private set

    /**
     * The language everything outside Compose should use.
     *
     * Read from [CaptureWidgetProvider.onUpdate], which the system can invoke at any moment (widget
     * added, device rebooted) and cannot suspend to await a Flow.
     */
    @Volatile
    var currentLanguage: AppLanguage = AppLanguage.DEFAULT
        private set

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
                CaptureWidgetProvider.refreshAll(this@LifeOsApplication, language)
            }
        }
    }

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
