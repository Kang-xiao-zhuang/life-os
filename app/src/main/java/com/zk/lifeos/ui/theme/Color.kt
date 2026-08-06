package com.zk.lifeos.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette for LifeOS: Calm / Minimal / Modern / Clean.
 *
 * Dark is the primary target — muted, low-contrast surfaces with soft desaturated accents,
 * so the app reads as quiet rather than loud. Light mode exists but is the secondary case.
 *
 * Keep [DarkBackground] in sync with `window_background` in res/values/colors.xml, otherwise
 * launch flashes a different colour than the first Compose frame.
 */

// ---- dark (primary) ----
val DarkBackground = Color(0xFF12131A)
/** One step below [DarkSurface], for cards that should sit behind the screen's main point. */
val DarkSurfaceQuiet = Color(0xFF15161E)
val DarkSurface = Color(0xFF171922)
val DarkSurfaceVariant = Color(0xFF1F2230)
val DarkOutline = Color(0xFF3A3F52)
val DarkOnBackground = Color(0xFFE6E8F0)
val DarkOnSurfaceVariant = Color(0xFF9EA3B5)

// ---- light (secondary) ----
val LightBackground = Color(0xFFF7F8FC)
/**
 * Quiet cards in light mode. White-on-#F7F8FC is already only a few percent of contrast, so a third
 * tone can't carry hierarchy here the way it does in the dark palette — this sits *between* the
 * background and white, and the real distinction comes from type size and padding.
 */
val LightSurfaceQuiet = Color(0xFFFBFCFE)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDEFF6)
val LightOutline = Color(0xFFC9CEDD)
val LightOnBackground = Color(0xFF1A1C24)
val LightOnSurfaceVariant = Color(0xFF5A6076)

// ---- accents (shared, tuned to sit calmly on both) ----
/** Soft periwinkle — primary actions. */
val AccentBlue = Color(0xFF93B7F5)
val AccentBlueDeep = Color(0xFF2F5DA8)

/** Soft mint — progress / streaks / "done". */
val AccentMint = Color(0xFF9FD8CB)
val AccentMintDeep = Color(0xFF2C7364)

/** Soft lavender — journal / reflection. */
val AccentLavender = Color(0xFFC4B5FD)
val AccentLavenderDeep = Color(0xFF5B4CA8)

/** Muted rose — destructive only. Deliberately not loud red. */
val AccentRose = Color(0xFFF0A0A8)
val AccentRoseDeep = Color(0xFF9B2C39)
