package com.zk.lifeos.ui

import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.zk.lifeos.model.AppLanguage

/**
 * The chosen language, carried separately from the locale itself.
 *
 * Needed because a `Dialog` or `ModalBottomSheet` is its own subcomposition, and Compose re-provides
 * [LocalContext] and [LocalConfiguration] inside it from the dialog's own window — silently undoing
 * the override below. Overlays therefore have to re-apply the language, and to do that they need to
 * know what it is. A *static* local survives that reset because Compose only replaces the specific
 * locals it provides itself.
 */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.DEFAULT }

/**
 * Applies the chosen interface language to everything below it.
 *
 * Done by overriding [LocalConfiguration] and [LocalContext] rather than through
 * `AppCompatDelegate.setApplicationLocales` or the API 33 per-app language API:
 *
 * - it needs **no new dependency** (appcompat would drag a whole widget toolkit into a
 *   Compose-only app, and this project keeps 「不引入复杂依赖」),
 * - it works the same on every supported API level instead of branching at 33, and
 * - it switches **without recreating the activity**, so the screen doesn't flash and nothing
 *   in-flight is lost.
 *
 * `stringResource` resolves against [LocalContext], so recomposition picks the new language up
 * immediately. [AppLanguage.SYSTEM] provides the unmodified pair.
 */
@Composable
fun LifeOsLocalization(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseConfiguration = LocalConfiguration.current
    val baseContext = LocalContext.current
    val locale = language.toLocale()

    // Keyed on the locale AND the incoming configuration, so a rotation or font-size change still
    // produces a fresh, correct configuration instead of a stale copy.
    val localized = remember(locale, baseConfiguration, baseContext) {
        if (locale == null) {
            baseConfiguration to baseContext
        } else {
            val configuration = Configuration(baseConfiguration).apply { setLocale(locale) }
            configuration to baseContext.createConfigurationContext(configuration)
        }
    }

    // Both of these normally find the Activity by walking up from LocalContext. Once LocalContext is
    // a bare configuration context that walk fails, and the failure is not subtle: the file picker
    // in 设置 crashed with "No ActivityResultRegistryOwner was provided", and system back would have
    // stopped travelling through the nav graph. Resolved here — above the override, where they still
    // see the real Activity — and handed down explicitly.
    val activityResultOwner = LocalActivityResultRegistryOwner.current
    val backPressedOwner = LocalOnBackPressedDispatcherOwner.current

    val providers = buildList {
        add(LocalAppLanguage provides language)
        add(LocalConfiguration provides localized.first)
        add(LocalContext provides localized.second)
        activityResultOwner?.let { add(LocalActivityResultRegistryOwner provides it) }
        backPressedOwner?.let { add(LocalOnBackPressedDispatcherOwner provides it) }
    }

    // Always one CompositionLocalProvider call, never an early `content()` return for SYSTEM.
    // Switching between those two shapes changes the structure of the composition and throws away
    // everything remembered below it — including the navigation back stack, so picking 中文 while in
    // 设置 bounced you back to 首页. Only the provided *values* may differ.
    CompositionLocalProvider(
        values = providers.toTypedArray<ProvidedValue<*>>(),
        content = content,
    )
}

/**
 * Re-applies the interface language inside a `Dialog` or `ModalBottomSheet`.
 *
 * **Every slot of every overlay needs this.** An overlay opens its own subcomposition, and Compose
 * fills that subcomposition's [LocalContext] and [LocalConfiguration] from the overlay's window —
 * so a sheet or dialog reverts to the *phone's* language, however the app is set. It is not a
 * corner case: the whole task sheet came up in English while the app was in Chinese.
 *
 * Text resolved in the parent composition and passed in as a `String` is already correct; this is
 * for anything looked up inside a slot lambda, including Material's own strings (the date picker's
 * title and month names).
 */
@Composable
fun LifeOsOverlayLocalization(content: @Composable () -> Unit) {
    LifeOsLocalization(language = LocalAppLanguage.current, content = content)
}
