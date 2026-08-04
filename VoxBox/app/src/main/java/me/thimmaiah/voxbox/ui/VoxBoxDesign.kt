package me.thimmaiah.voxbox.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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

    /** Vertical rhythm between top-level sections of a screen. */
    val sectionSpacing = 14.dp

    /** Leaves room under the last section for the bottom navigation bar. */
    val listBottomPadding = 32.dp
}

enum class VoxBoxStatusTone {
    Neutral,
    Accent,
    Success,
    Warning,
    Error,
}

@Composable
private fun toneColors(tone: VoxBoxStatusTone): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (tone) {
        VoxBoxStatusTone.Neutral -> colors.surfaceContainerHighest to colors.onSurfaceVariant
        VoxBoxStatusTone.Accent -> colors.primaryContainer to colors.onPrimaryContainer
        VoxBoxStatusTone.Success -> colors.secondaryContainer to colors.onSecondaryContainer
        VoxBoxStatusTone.Warning -> colors.tertiaryContainer to colors.onTertiaryContainer
        VoxBoxStatusTone.Error -> colors.errorContainer to colors.onErrorContainer
    }
}

/**
 * Compact status treatment for capture and processing states.
 *
 * A leading dot carries the tone so the label stays readable at small sizes, and [pulsing] marks a
 * state that is actively changing, such as a running capture session.
 */
@Composable
fun VoxBoxStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    tone: VoxBoxStatusTone = VoxBoxStatusTone.Accent,
    pulsing: Boolean = false,
) {
    val (containerColor, contentColor) = toneColors(tone)
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dotAlpha = if (pulsing) {
                val transition = rememberInfiniteTransition(label = "voxbox-status-pulse")
                val animated by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.25f,
                    animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
                    label = "voxbox-status-pulse-alpha",
                )
                animated
            } else {
                1f
            }
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(dotAlpha)
                    .clip(CircleShape)
                    .background(contentColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Default grouping surface used by note, transcript, and review sections. */
@Composable
fun VoxBoxSectionCard(
    modifier: Modifier = Modifier,
    tone: VoxBoxStatusTone = VoxBoxStatusTone.Neutral,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerColor = when (tone) {
        VoxBoxStatusTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> toneColors(tone).first
    }
    val contentColor = when (tone) {
        VoxBoxStatusTone.Neutral -> MaterialTheme.colorScheme.onSurface
        else -> toneColors(tone).second
    }
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
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

/**
 * One consistent section header: an optional step number, a title, supporting text, and a slot for
 * a status pill or action on the trailing edge.
 */
@Composable
fun VoxBoxSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    step: Int? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step != null) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$step", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * A selectable row with a leading radio or check indicator.
 *
 * This replaces the stacked full-width outlined buttons whose selection was previously encoded as a
 * "✓" prefix inside the label, which screen readers announced as part of the text.
 */
@Composable
fun VoxBoxChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = if (selected) "$label, selected" else label },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
        contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionIndicator(selected = selected)
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) {
                            colors.onSecondaryContainer
                        } else {
                            colors.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(20.dp),
            shape = CircleShape,
            color = if (selected) colors.primary else Color.Transparent,
            contentColor = if (selected) colors.onPrimary else colors.outline,
            border = if (selected) null else androidx.compose.foundation.BorderStroke(1.5.dp, colors.outline),
        ) {
            if (selected) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(VoxBoxIcons.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

/** Compact selectable chip used for folders, syllabi, and speaker labels. */
@Composable
fun VoxBoxChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = if (selected) "$label, selected" else label
        },
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) colors.primary else colors.surfaceContainerHigh,
        contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(VoxBoxIcons.Check, contentDescription = null, modifier = Modifier.size(15.dp))
            } else if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Wrapping chip group; wrapping avoids adding a second scrollable region to a screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoxBoxChipGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
        verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
        content = { content() },
    )
}

/** A single number-and-label tile used by the live frame and queue counters. */
@Composable
fun VoxBoxStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tone: VoxBoxStatusTone = VoxBoxStatusTone.Neutral,
) {
    val (containerColor, contentColor) = toneColors(tone)
    Surface(
        modifier = modifier.semantics { contentDescription = "$label: $value" },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Prominent, actionable message for permission prompts, provider failures, and retained evidence.
 *
 * Actions live inside the banner so a blocking condition and its remedy stay together.
 */
@Composable
fun VoxBoxBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: VoxBoxStatusTone = VoxBoxStatusTone.Warning,
    icon: ImageVector = VoxBoxIcons.Warning,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    val (containerColor, contentColor) = toneColors(tone)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(VoxBoxSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Text(message, style = MaterialTheme.typography.bodySmall)
            if (actions != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
                    content = actions,
                )
            }
        }
    }
}

/** Centred placeholder with one clear next step. */
@Composable
fun VoxBoxEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = VoxBoxSpacing.xxLarge, horizontal = VoxBoxSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}
