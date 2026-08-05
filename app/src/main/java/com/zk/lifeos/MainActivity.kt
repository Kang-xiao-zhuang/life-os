package com.zk.lifeos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.model.ThemeMode
import com.zk.lifeos.ui.LifeOsApp
import com.zk.lifeos.ui.LifeOsLocalization
import com.zk.lifeos.ui.theme.LifeOsTheme

/**
 * The single activity. It reads the persisted theme and language directly from the settings
 * service so the whole tree is themed and translated before any screen composes — a screen-level
 * ViewModel would be too late and would flash the wrong colours or the wrong language.
 */
class MainActivity : ComponentActivity() {

    /**
     * Bumped every time a quick-capture entry point fires. A counter rather than a boolean so a
     * second tap on the widget re-opens the capture field even if the app is already showing it.
     */
    private var captureRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        val settingsService = (application as LifeOsApplication).container.settingsService

        setContent {
            // Start from the product defaults so the very first frame is already right —
            // DataStore's first emission arrives a moment later.
            val themeMode by settingsService.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.DEFAULT)
            val language by settingsService.language
                .collectAsStateWithLifecycle(initialValue = AppLanguage.DEFAULT)

            LifeOsLocalization(language = language) {
                LifeOsTheme(themeMode = themeMode) {
                    LifeOsApp(captureRequest = captureRequest)
                }
            }
        }
    }

    /** The activity is `singleTop`, so repeat launches land here instead of in [onCreate]. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == LifeOsIntents.ACTION_QUICK_CAPTURE) captureRequest++
    }
}
