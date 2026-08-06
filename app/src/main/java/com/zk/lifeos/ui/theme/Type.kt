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

val LifeOsTypography = Typography(
    headlineMedium = cjk(fontSize = 26, lineHeight = 36, weight = FontWeight.SemiBold),
    headlineSmall = cjk(fontSize = 21, lineHeight = 30, weight = FontWeight.SemiBold),
    titleLarge = cjk(fontSize = 20, lineHeight = 28, weight = FontWeight.SemiBold),
    titleMedium = cjk(fontSize = 16, lineHeight = 24, weight = FontWeight.SemiBold),
    titleSmall = cjk(fontSize = 14, lineHeight = 21, weight = FontWeight.Medium),
    bodyLarge = cjk(fontSize = 16, lineHeight = 27, weight = FontWeight.Normal),
    bodyMedium = cjk(fontSize = 14, lineHeight = 23, weight = FontWeight.Normal),
    bodySmall = cjk(fontSize = 12, lineHeight = 20, weight = FontWeight.Normal),
    labelLarge = cjk(fontSize = 14, lineHeight = 20, weight = FontWeight.Medium),
    labelMedium = cjk(fontSize = 12, lineHeight = 17, weight = FontWeight.Medium),
    labelSmall = cjk(fontSize = 11, lineHeight = 16, weight = FontWeight.Medium),
)
