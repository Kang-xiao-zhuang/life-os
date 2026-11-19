package com.zk.lifeos

import android.content.Context
import android.content.res.Configuration
import com.zk.lifeos.model.AppLanguage

/**
 * A [Context] whose resources answer in [language], for the code that runs outside Compose.
 *
 * Inside the UI, `LifeOsLocalization` handles this by overriding `LocalContext`. The launcher
 * shortcut and the home-screen widget are built with plain `getString`, so they need the same
 * treatment applied by hand — otherwise they keep the phone's language and quietly contradict the
 * app the user just switched.
 *
 * [AppLanguage.SYSTEM] returns the receiver unchanged.
 */
fun Context.localized(language: AppLanguage): Context {
    val locale = language.toLocale() ?: return this
    val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(configuration)
}
