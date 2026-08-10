package me.thimmaiah.voxbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Two families: a serif for screen titles, a sans for everything else. The contrast is what
 * makes a title read as a title without needing a rule under it.
 *
 * These are the platform families rather than Caprasimo and Figtree. Downloadable fonts need the
 * Google Fonts provider certificates and a preloaded-fonts manifest entry, and if either is
 * missing — or the device is offline on first run — text falls back mid-layout. A lecture app is
 * used in buildings with bad signal, so the families here always resolve.
 */
private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif

/** Formulas, clocks and timestamps. Tabular figures stop a running clock from jittering. */
val VbMono = FontFamily.Monospace

private val trimmed = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val VoxBoxTypography = Typography(
    // Screen titles
    displayLarge = TextStyle(
        fontFamily = Display, fontSize = 34.sp, lineHeight = 37.sp,
        fontWeight = FontWeight.Normal, lineHeightStyle = trimmed,
    ),
    // Sub-page titles
    displayMedium = TextStyle(
        fontFamily = Display, fontSize = 30.sp, lineHeight = 33.sp,
        fontWeight = FontWeight.Normal, lineHeightStyle = trimmed,
    ),
    displaySmall = TextStyle(
        fontFamily = Display, fontSize = 22.sp, lineHeight = 26.sp,
        fontWeight = FontWeight.Normal, lineHeightStyle = trimmed,
    ),
    // Card titles
    titleLarge = TextStyle(
        fontFamily = Body, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold,
    ),
    // Row titles
    titleMedium = TextStyle(
        fontFamily = Body, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontFamily = Body, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
    ),
    // Reader body. The reading-size setting scales this one style and nothing else.
    bodyLarge = TextStyle(
        fontFamily = Body, fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = Body, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontFamily = Body, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal,
    ),
    // Buttons
    labelLarge = TextStyle(
        fontFamily = Body, fontSize = 14.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontFamily = Body, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
    ),
    // Pills and uppercase eyebrows
    labelSmall = TextStyle(
        fontFamily = Body, fontSize = 11.sp, lineHeight = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.1.em,
    ),
)

/** Reading sizes offered in the reader, applied to `bodyLarge` only. */
enum class VbReadingSize(val label: String, val size: Int, val lineHeight: Int) {
    Small("Small", 14, 22),
    Default("Default", 16, 26),
    Large("Large", 18, 31),
    Huge("Huge", 20, 35);

    companion object {
        fun fromOrdinal(value: Int): VbReadingSize = entries.getOrElse(value) { Default }
    }
}

fun readerBodyStyle(size: VbReadingSize, serif: Boolean): TextStyle = TextStyle(
    fontFamily = if (serif) Display else Body,
    fontSize = size.size.sp,
    lineHeight = size.lineHeight.sp,
    fontWeight = FontWeight.Normal,
)
