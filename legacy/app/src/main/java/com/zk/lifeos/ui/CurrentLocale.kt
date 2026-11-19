package com.zk.lifeos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * The locale the interface is actually using.
 *
 * **Not** `Locale.getDefault()` — that is the *system* locale, so anything formatted with it
 * (month names, weekday names) would ignore the in-app language switch and come out in the wrong
 * language. [LifeOsLocalization] overrides the configuration, and this reads from there.
 */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
