package me.thimmaiah.voxbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Reusable spacing values for compact Android layouts. */
object VoxBoxSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xLarge = 24.dp
    val xxLarge = 32.dp
}

/** Layout bounds that keep touch targets accessible and tablet content readable. */
object VoxBoxLayout {
    val minimumTouchTarget = 48.dp
    val compactScreenPadding = 16.dp
    val comfortableScreenPadding = 20.dp
    val contentMaxWidth = 640.dp
}

enum class VoxBoxStatusTone {
    Accent,
    Success,
    Warning,
    Error,
}

/** Compact, theme-aware status treatment for capture and processing states. */
@Composable
fun VoxBoxStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    tone: VoxBoxStatusTone = VoxBoxStatusTone.Accent,
) {
    val colors = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (tone) {
        VoxBoxStatusTone.Accent -> colors.primaryContainer to colors.onPrimaryContainer
        VoxBoxStatusTone.Success -> colors.secondaryContainer to colors.onSecondaryContainer
        VoxBoxStatusTone.Warning -> colors.tertiaryContainer to colors.onTertiaryContainer
        VoxBoxStatusTone.Error -> colors.errorContainer to colors.onErrorContainer
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Default elevated grouping surface used by note, transcript, and review sections. */
@Composable
fun VoxBoxSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(VoxBoxSpacing.large),
            verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
            content = content,
        )
    }
}
