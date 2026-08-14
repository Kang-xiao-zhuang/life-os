package com.zk.lifeos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Typography stays on the system font — no bundled font files, per「不引入复杂依赖」. A CJK subset
 * would cost several MB on a 2 MB APK and hand the project its first binary asset to look after.
 *
 * What *is* tuned is the metrics, for a UI that is mostly Chinese:
 *
 * - **Line height ≈ 1.6–1.75×**, not Material's ~1.4×. Latin text has ascenders and descenders that
 *   create visual gaps between lines; a wall of Chinese glyphs is a solid block of full-height boxes,
 *   so the same ratio reads as cramped. This is the single biggest difference.
 * - **`letterSpacing = 0.sp` everywhere, stated explicitly.** Material's scale adds tracking tuned
 *   for Latin (0.1–0.5sp); on Chinese it just pulls the characters of a word apart. It was already
 *   0 on the styles this file overrode — but only by accident, because an unspecified value resolves
 *   to 0. The styles further down that this file used to leave alone (titleLarge, labelMedium) really
 *   did inherit Latin tracking, which is why they are defined here now.
 * - **`LineHeightStyle` centred with trimmed edges**: distributes the extra leading evenly instead of
 *   dumping it under the glyphs, and drops the half-line of padding above the first line and below
 *   the last. Without it, generous line heights make a single-line label look vertically off-centre
 *   inside its row.
 */
private val CjkLineHeight = PlatformTextStyle(includeFontPadding = false)

private val CjkLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun cjk(
    fontSize: Int,
    lineHeight: Int,
    weight: FontWeight,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
    platformStyle = CjkLineHeight,
    lineHeightStyle = CjkLineHeightStyle,
)

/**
 * The scale had almost no contrast, and counting the call sites showed exactly how little:
 * **nine of the eleven styles sat between 11sp and 16sp**, the most-used style in the whole app was
 * the *smallest* one (`labelSmall`, 18 call sites at 11sp), `headlineMedium` appeared exactly once,
 * and `headlineSmall` / `titleLarge` were never used at all. `headlineSmall` 21 and `titleLarge` 20
 * were also a step apart on paper and indistinguishable on screen.
 *
 * So most of the interface was rendered at one of three nearly identical sizes in one muted colour —
 * an even grey texture with nothing leading it. The sizes below are spread so that the ones actually
 * on screen at once are clearly different: **30 / 24 / 17 / 15 / 14 / 13 / 12**.
 *
 * `headlineSmall` now has a job (every screen's title, via `LifeOsScreen`), which is what gives each
 * screen an anchor instead of opening straight into a stack of same-sized cards.
 *
 * Nothing here got smaller except the near-duplicates: `labelSmall` 11 → 12 and `bodySmall` 12 → 13,
 * because both carry real content (dates, notes, coaching lines) and 11sp is genuinely small for 汉字.
 */
val LifeOsTypography = Typography(
    // Dashboard's 今天 — the one place a number-of-the-day sized statement belongs.
    headlineMedium = cjk(fontSize = 30, lineHeight = 40, weight = FontWeight.Bold),
    // Screen titles.
    headlineSmall = cjk(fontSize = 24, lineHeight = 34, weight = FontWeight.SemiBold),
    titleLarge = cjk(fontSize = 19, lineHeight = 27, weight = FontWeight.SemiBold),
    // Card titles.
    titleMedium = cjk(fontSize = 17, lineHeight = 26, weight = FontWeight.SemiBold),
    // Quiet card titles — a real step below titleMedium, not a shade of it.
    titleSmall = cjk(fontSize = 14, lineHeight = 21, weight = FontWeight.Medium),
    bodyLarge = cjk(fontSize = 16, lineHeight = 27, weight = FontWeight.Normal),
    // Task and habit names: the text you actually read on every screen.
    bodyMedium = cjk(fontSize = 15, lineHeight = 25, weight = FontWeight.Normal),
    bodySmall = cjk(fontSize = 13, lineHeight = 21, weight = FontWeight.Normal),
    labelLarge = cjk(fontSize = 14, lineHeight = 20, weight = FontWeight.Medium),
    labelMedium = cjk(fontSize = 13, lineHeight = 19, weight = FontWeight.Medium),
    labelSmall = cjk(fontSize = 12, lineHeight = 18, weight = FontWeight.Medium),
)
