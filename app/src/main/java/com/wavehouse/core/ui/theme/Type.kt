package com.wavehouse.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Organic Ledger Typography
 *
 * DESIGN.MD spec:
 * - Display & Headlines: Manrope (geometric precision)
 * - Body & Labels: Work Sans (maximum legibility in high-velocity logistics)
 *
 * Current: Using system sans-serif (Roboto) as fallback.
 * TODO: Replace with actual Manrope + Work Sans font files
 *       when downloaded via Android Studio > File > New > Font Resource.
 *
 * Visual Rhythm rule:
 *   Always pair headline-sm + body-md with 4px or 8px gap.
 */

// TODO: Replace with actual font families when ttf files are available:
// val ManropeFontFamily = FontFamily(
//     Font(R.font.manrope_regular, FontWeight.Normal),
//     Font(R.font.manrope_medium, FontWeight.Medium),
//     Font(R.font.manrope_semibold, FontWeight.SemiBold),
//     Font(R.font.manrope_bold, FontWeight.Bold),
// )
// val WorkSansFontFamily = FontFamily(
//     Font(R.font.worksans_regular, FontWeight.Normal),
//     Font(R.font.worksans_medium, FontWeight.Medium),
//     Font(R.font.worksans_semibold, FontWeight.SemiBold),
//     Font(R.font.worksans_bold, FontWeight.Bold),
// )

// Temporary: use system default (Roboto) — visually close to Work Sans
private val HeadlineFontFamily = FontFamily.Default   // → Manrope when available
private val BodyFontFamily = FontFamily.Default        // → Work Sans when available

val WaveHouseTypography = Typography(
    // ── Display (Manrope / Headlines) ─────────────────────────
    displayLarge = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // ── Headline (Manrope) ────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // ── Title (Manrope) ───────────────────────────────────────
    titleLarge = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ── Body (Work Sans — workhorse for logistics data) ───────
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ── Label (Work Sans — SKUs, weights, timestamps) ─────────
    labelLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
)
