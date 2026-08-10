package me.thimmaiah.voxbox.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** The four hues the user can pick between for primary actions and selection. */
enum class VbAccent { Blue, Green, Orange, Red }

/**
 * Colours whose meaning is fixed by what they say, not by the user's taste.
 *
 * Deliberately separate from [MaterialTheme.colorScheme]: if green followed the accent then a
 * user on the green accent could not tell "saved" from "primary action", and a user on the red
 * accent would see every button look like a warning. Dynamic colour is not supported for the
 * same reason.
 */
@Immutable
data class VbStatus(
    /** Saved, verified, permission granted. Never used for anything not yet on disk. */
    val saved: Color,
    /** Needs the student's attention. Never used for something the app changed by itself. */
    val review: Color,
    /** Recording, destructive, or failed. */
    val danger: Color,
    val line: Color,
    val fg2: Color,
    val fg3: Color,
)

val LocalVbStatus = staticCompositionLocalOf {
    VbStatus(VbGreen, VbOrange, VbRed, VbLineDark, VbFg2Dark, VbFg3Dark)
}

/**
 * True when the system animator scale is zero, i.e. the user has turned animations off in
 * developer options or an accessibility setting. Entry animations collapse to a short fade and
 * infinite animations stop; see `ui/VbMotion.kt`.
 */
val LocalVbReducedMotion = staticCompositionLocalOf { false }

private fun accentColor(accent: VbAccent, dark: Boolean) = when (accent) {
    VbAccent.Blue -> if (dark) VbBlue else VbBlueDark
    VbAccent.Green -> if (dark) VbGreen else VbGreenDark
    VbAccent.Orange -> if (dark) VbOrange else VbOrangeDark
    VbAccent.Red -> if (dark) VbRed else VbRedDark
}

private fun onAccent(accent: VbAccent) =
    if (accent == VbAccent.Orange) VbOnAccentDark else VbOnAccentLight

@Composable
fun VoxBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: VbAccent = VbAccent.Blue,
    content: @Composable () -> Unit,
) {
    val acc = accentColor(accent, darkTheme)
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = acc,
            onPrimary = onAccent(accent),
            primaryContainer = acc.copy(alpha = 0.18f),
            onPrimaryContainer = acc,
            background = VbBgDark,
            onBackground = VbFgDark,
            surface = VbSfDark,
            onSurface = VbFgDark,
            surfaceVariant = VbSf2Dark,
            onSurfaceVariant = VbFg2Dark,
            outlineVariant = VbLineDark,
            error = VbRed,
            onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = acc,
            onPrimary = onAccent(accent),
            primaryContainer = acc.copy(alpha = 0.12f),
            onPrimaryContainer = acc,
            background = VbBgLight,
            onBackground = VbFgLight,
            surface = VbSfLight,
            onSurface = VbFgLight,
            surfaceVariant = VbSf2Light,
            onSurfaceVariant = VbFg2Light,
            outlineVariant = VbLineLight,
            error = VbRedDark,
            onError = Color.White,
        )
    }
    val status = VbStatus(
        saved = if (darkTheme) VbGreen else VbGreenDark,
        review = if (darkTheme) VbOrange else VbOrangeDark,
        danger = if (darkTheme) VbRed else VbRedDark,
        line = if (darkTheme) VbLineDark else VbLineLight,
        fg2 = if (darkTheme) VbFg2Dark else VbFg2Light,
        fg3 = if (darkTheme) VbFg3Dark else VbFg3Light,
    )
    val resolver = LocalContext.current.contentResolver
    val reducedMotion = remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(
        LocalVbStatus provides status,
        LocalVbReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = VoxBoxTypography, content = content)
    }
}

object VbShape {
    val pill = RoundedCornerShape(percent = 50)
    val card = RoundedCornerShape(24.dp)
    val cardL = RoundedCornerShape(28.dp)
    val row = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val media = RoundedCornerShape(18.dp)
    val tile = RoundedCornerShape(26.dp)
}

object VbSpace {
    /** Horizontal screen padding. Every screen uses this and nothing else. */
    val screenH = 20.dp
    val gap = 12.dp
    val section = 20.dp
    val cardPad = 16.dp

    /** Minimum touch target. Anything tappable is at least this in both dimensions. */
    val touch = 44.dp
}
