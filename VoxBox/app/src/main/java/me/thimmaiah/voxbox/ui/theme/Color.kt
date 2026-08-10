package me.thimmaiah.voxbox.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Five hues carry the whole interface: four accents plus a near-black ground.
 *
 * Status meaning is fixed and never follows the accent — see [VbStatus]. The literals here are
 * mirrored in `res/values/colors.xml`, which paints the window before Compose starts; change one
 * and change the other.
 */

// Accent hues. The *Dark* variants are the light-theme values, darkened to stay legible on white.
val VbBlue = Color(0xFF4C7DFF)
val VbBlueDark = Color(0xFF2A5BE0)
val VbGreen = Color(0xFF22B77E)
val VbGreenDark = Color(0xFF0E8A57)
val VbOrange = Color(0xFFF79320)
val VbOrangeDark = Color(0xFFC96A00)
val VbRed = Color(0xFFF2555B)
val VbRedDark = Color(0xFFD8353C)

// On-accent foregrounds. Orange is light enough that it needs a dark foreground to pass contrast.
val VbOnAccentLight = Color(0xFFFFFFFF)
val VbOnAccentDark = Color(0xFF1B1206)

// Dark ground
val VbBgDark = Color(0xFF0B0C0F)
val VbSfDark = Color(0xFF15171C)
val VbSf2Dark = Color(0xFF1E222A)
val VbFgDark = Color(0xFFF3F5F8)
val VbFg2Dark = Color(0xFFA6AEBB)
val VbFg3Dark = Color(0xFF6C7480)
val VbLineDark = Color(0x1AFFFFFF)

// Light ground
val VbBgLight = Color(0xFFF4F5F3)
val VbSfLight = Color(0xFFFFFFFF)
val VbSf2Light = Color(0xFFEAECF0)
val VbFgLight = Color(0xFF12141A)
val VbFg2Light = Color(0xFF5A6270)
val VbFg3Light = Color(0xFF8A929E)
val VbLineLight = Color(0x1C0C0E12)

// The live capture surface is always dark, in both themes: it is a camera and a stage, and a
// white page behind a lecture-hall photo is unreadable.
val VbLiveBg = Color(0xFF07080A)
val VbLiveSf = Color(0xFF0F1116)
val VbLiveLine = Color(0x14FFFFFF)
val VbLiveFg = Color(0xFFF3F5F8)
val VbLiveFg2 = Color(0xFF8E96A3)
val VbLiveFgBody = Color(0xFFDDE2EA)
