package com.zk.lifeos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zk.lifeos.model.AppLanguage
import com.zk.lifeos.model.ThemeMode
import com.zk.lifeos.ui.LifeOsApp
import com.zk.lifeos.ui.LifeOsLocalization
import com.zk.lifeos.ui.navigation.LaunchRequest
import com.zk.lifeos.ui.navigation.LaunchTarget
import com.zk.lifeos.ui.theme.LifeOsTheme

/**
 * The single activity. It reads the persisted theme and language directly from the settings
 * service so the whole tree is themed and translated before any screen composes — a screen-level
 * ViewModel would be too late and would flash the wrong colours or the wrong language.
 */
class MainActivity : ComponentActivity() {

    /**
     * Where the launch that brought us here wants to go. Carries a counter rather than being a
     * plain target so a second tap on the widget — or on the same notification — re-triggers the
     * jump even when the app is already showing that screen.
     */
    private var launchRequest by mutableStateOf(LaunchRequest())

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
                    LifeOsApp(launchRequest = launchRequest)
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
        val target = when (intent?.action) {
            LifeOsIntents.ACTION_QUICK_CAPTURE -> LaunchTarget.CAPTURE
            LifeOsIntents.ACTION_OPEN_TODAY -> LaunchTarget.TODAY
            LifeOsIntents.ACTION_OPEN_REVIEW -> LaunchTarget.REVIEW
            else -> return
        }
        launchRequest = LaunchRequest(target = target, serial = launchRequest.serial + 1)
    }
}
