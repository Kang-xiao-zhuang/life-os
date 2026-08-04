package com.zk.lifeos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.zk.lifeos.model.ThemeMode

private val LifeOsDarkScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = AccentBlueDeep,
    primaryContainer = AccentBlueDeep,
    onPrimaryContainer = AccentBlue,

    secondary = AccentMint,
    onSecondary = AccentMintDeep,
    secondaryContainer = AccentMintDeep,
    onSecondaryContainer = AccentMint,

    tertiary = AccentLavender,
    onTertiary = AccentLavenderDeep,
    tertiaryContainer = AccentLavenderDeep,
    onTertiaryContainer = AccentLavender,

    error = AccentRose,
    onError = AccentRoseDeep,

    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant,
)

private val LifeOsLightScheme = lightColorScheme(
    primary = AccentBlueDeep,
    onPrimary = LightSurface,
    primaryContainer = AccentBlue,
    onPrimaryContainer = AccentBlueDeep,

    secondary = AccentMintDeep,
    onSecondary = LightSurface,
    secondaryContainer = AccentMint,
    onSecondaryContainer = AccentMintDeep,

    tertiary = AccentLavenderDeep,
    onTertiary = LightSurface,
    tertiaryContainer = AccentLavender,
    onTertiaryContainer = AccentLavenderDeep,

    error = AccentRoseDeep,
    onError = LightSurface,

    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightSurfaceVariant,
)

/** 柔和圆角 —— generous but not pill-shaped. */
private val LifeOsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Deliberately NOT using dynamic colour (Material You): the palette is part of the product's
 * calm identity, and it should look the same on every phone.
 */
@Composable
fun LifeOsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) LifeOsDarkScheme else LifeOsLightScheme,
        typography = LifeOsTypography,
        shapes = LifeOsShapes,
        content = content,
    )
}
