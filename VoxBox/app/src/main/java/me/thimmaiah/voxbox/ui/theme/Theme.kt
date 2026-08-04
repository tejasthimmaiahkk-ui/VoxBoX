package me.thimmaiah.voxbox.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val VoxBoxDarkColorScheme = darkColorScheme(
    primary = VoxIndigoDark,
    onPrimary = VoxOnIndigoDark,
    primaryContainer = VoxIndigoContainerDark,
    onPrimaryContainer = VoxOnIndigoContainerDark,
    secondary = VoxTealDark,
    onSecondary = VoxOnTealDark,
    secondaryContainer = VoxTealContainerDark,
    onSecondaryContainer = VoxOnTealContainerDark,
    tertiary = VoxGoldDark,
    onTertiary = VoxOnGoldDark,
    tertiaryContainer = VoxGoldContainerDark,
    onTertiaryContainer = VoxOnGoldContainerDark,
    error = VoxErrorDark,
    onError = VoxOnErrorDark,
    errorContainer = VoxErrorContainerDark,
    onErrorContainer = VoxOnErrorContainerDark,
    background = VoxNight,
    onBackground = VoxOnNight,
    surface = VoxNight,
    onSurface = VoxOnNight,
    surfaceVariant = VoxSurfaceVariantDark,
    onSurfaceVariant = VoxOnSurfaceVariantDark,
    outline = VoxOutlineDark,
    outlineVariant = VoxOutlineVariantDark,
    inverseSurface = VoxInverseSurfaceDark,
    inverseOnSurface = VoxInverseOnSurfaceDark,
    inversePrimary = VoxInversePrimaryDark,
    surfaceDim = VoxNight,
    surfaceBright = VoxSurfaceBrightDark,
    surfaceContainerLowest = VoxSurfaceLowestDark,
    surfaceContainerLow = VoxSurfaceLowDark,
    surfaceContainer = VoxSurfaceDark,
    surfaceContainerHigh = VoxSurfaceHighDark,
    surfaceContainerHighest = VoxSurfaceHighestDark,
)

private val VoxBoxLightColorScheme = lightColorScheme(
    primary = VoxIndigo,
    onPrimary = VoxOnIndigo,
    primaryContainer = VoxIndigoContainer,
    onPrimaryContainer = VoxOnIndigoContainer,
    secondary = VoxTeal,
    onSecondary = VoxOnTeal,
    secondaryContainer = VoxTealContainer,
    onSecondaryContainer = VoxOnTealContainer,
    tertiary = VoxGold,
    onTertiary = VoxOnGold,
    tertiaryContainer = VoxGoldContainer,
    onTertiaryContainer = VoxOnGoldContainer,
    error = VoxError,
    onError = VoxOnError,
    errorContainer = VoxErrorContainer,
    onErrorContainer = VoxOnErrorContainer,
    background = VoxPaper,
    onBackground = VoxOnPaper,
    surface = VoxPaper,
    onSurface = VoxOnPaper,
    surfaceVariant = VoxSurfaceVariant,
    onSurfaceVariant = VoxOnSurfaceVariant,
    outline = VoxOutline,
    outlineVariant = VoxOutlineVariant,
    inverseSurface = VoxInverseSurface,
    inverseOnSurface = VoxInverseOnSurface,
    inversePrimary = VoxInversePrimary,
    surfaceDim = VoxSurfaceDim,
    surfaceBright = VoxPaper,
    surfaceContainerLowest = VoxSurfaceLowest,
    surfaceContainerLow = VoxSurfaceLow,
    surfaceContainer = VoxSurface,
    surfaceContainerHigh = VoxSurfaceHigh,
    surfaceContainerHighest = VoxSurfaceHighest,
)

private val VoxBoxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun VoxBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> VoxBoxDarkColorScheme
        else -> VoxBoxLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VoxBoxTypography,
        shapes = VoxBoxShapes,
        content = content
    )
}
