package com.zk.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zk.lifeos.model.ThemeMode
import com.zk.lifeos.ui.LifeOsApp
import com.zk.lifeos.ui.theme.LifeOsTheme

/**
 * The single activity. It reads the persisted theme mode directly from the settings service so
 * the whole tree can be themed before any screen composes — a screen-level ViewModel would be
 * too late and would flash the wrong colours.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsService = (application as LifeOsApplication).container.settingsService

        setContent {
            // Start from the product default so the very first frame is already the right
            // colour — DataStore's first emission arrives a moment later.
            val themeMode by settingsService.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.DEFAULT)

            LifeOsTheme(themeMode = themeMode) {
                LifeOsApp()
            }
        }
    }
}
