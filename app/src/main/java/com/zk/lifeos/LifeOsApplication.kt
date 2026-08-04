package com.zk.lifeos

import android.app.Application
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** Holds the app-wide object graph, and registers the launcher shortcut. */
class LifeOsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        registerCaptureShortcut()
    }

    /**
     * Long-press the launcher icon → 记一笔.
     *
     * Registered at runtime rather than declared in `res/xml/shortcuts.xml` because a static
     * shortcut has to hard-code `targetPackage`, and resource files get no `${applicationId}`
     * substitution — so it would point at the release package and silently do nothing in the
     * `.debug` build. Setting it here works for whatever id the variant actually has.
     */
    private fun registerCaptureShortcut() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = LifeOsIntents.ACTION_QUICK_CAPTURE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val shortcut = ShortcutInfoCompat.Builder(this, "quick_capture")
            .setShortLabel(getString(R.string.widget_capture_label))
            .setLongLabel(getString(R.string.shortcut_capture_long_label))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_capture))
            .setIntent(intent)
            .build()

        // Failing to register a shortcut must never stop the app from starting.
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(this, listOf(shortcut)) }
    }
}
