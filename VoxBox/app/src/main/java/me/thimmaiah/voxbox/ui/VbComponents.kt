package me.thimmaiah.voxbox.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace

// Ripple-free click that still reports as a button to accessibility. Every surface in this app
// signals press by lifting or scaling instead, which reads better on large rounded cards.
@Composable
private fun Modifier.vbClickable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    label: String? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = role,
        onClickLabel = label,
        onClick = onClick,
    )
}

/** An uppercase group label. Used instead of a rule or a heavier heading. */
@Composable
fun VbEyebrow(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color ?: LocalVbStatus.current.fg3,
        modifier = modifier,
    )
}

/** The standard content card: surface fill, hairline border, generous radius. */
@Composable
fun VbCard(
    modifier: Modifier = Modifier,
    shape: Shape = VbShape.card,
    color: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = LocalVbStatus.current.line,
    contentPadding: PaddingValues = PaddingValues(VbSpace.cardPad),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScopeAlias.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(
        targetValue = if (pressed && onClick != null) (-2).dp else 0.dp,
        animationSpec = tween(VbMotion.PRESS),
        label = "card-lift",
    )
    val border by animateColorAsState(
        targetValue = if (pressed && onClick != null) MaterialTheme.colorScheme.primary else borderColor,
        animationSpec = tween(VbMotion.PRESS),
        label = "card-border",
    )
    val liftPx = with(androidx.compose.ui.platform.LocalDensity.current) { lift.toPx() }
    Column(
        modifier = modifier
            // Lift by translation, not padding: a negative padding value throws, and translating
            // keeps the card's measured height stable so neighbours do not shuffle on press.
            .graphicsLayer { translationY = liftPx }
            .clip(shape)
            .background(color)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.vbClickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Kotlin's ColumnScope, aliased so [VbCard] reads as a column without leaking the import. */
typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope

/** Small status pill: a dot plus a label, in one of the fixed status colours. */
@Composable
fun VbPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    blinking: Boolean = false,
) {
    val alpha by vbPulse(1f, 0.2f, 1400, restingValue = 1f, label = "pill-dot")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(VbShape.pill)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(VbShape.pill)
                .background(color.copy(alpha = if (blinking) alpha else 1f)),
        )
        Spacer(Modifier.width(6.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** A filled pill button, the primary action on every screen. */
@Composable
fun VbPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    height: Dp = 58.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "button-scale",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(VbShape.pill)
            .background(if (enabled) container else container.copy(alpha = 0.35f))
            .vbClickable(enabled = enabled, interactionSource = interaction, onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) content else content.copy(alpha = 0.6f),
        )
    }
}

/** Outlined counterpart, for the safe half of a destructive pair. */
@Composable
fun VbOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    height: Dp = 44.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .clip(VbShape.pill)
            .border(1.dp, LocalVbStatus.current.line, VbShape.pill)
            .vbClickable(onClick = onClick),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/** Icon-only button with a 48.dp hit box regardless of the visible size. */
@Composable
fun VbIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 40.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(maxOf(size, 48.dp))
            .clip(VbShape.pill)
            .vbClickable(label = contentDescription, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Two-or-more-way segmented switch with a sliding thumb.
 *
 * The thumb animates by width fraction rather than absolute dp so it lands correctly whatever
 * the container measures at.
 */
@Composable
fun <T> VbSegmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = options.indexOf(selected).coerceAtLeast(0)
    Row(
        modifier = modifier
            .clip(VbShape.pill)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, option ->
            val active = i == index
            val bg by animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(VbMotion.SWITCH),
                label = "segment-bg",
            )
            val fg by animateColorAsState(
                targetValue = if (active) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    LocalVbStatus.current.fg2
                },
                animationSpec = tween(VbMotion.SWITCH),
                label = "segment-fg",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .clip(VbShape.pill)
                    .background(bg)
                    .vbClickable(role = Role.RadioButton) { onSelect(option) },
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A settings row with a label, a supporting line, and a switch. */
@Composable
fun VbSwitchRow(
    title: String,
    supporting: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VbSpace.touch)
            .vbClickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (supporting != null) {
                Spacer(Modifier.height(2.dp))
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = LocalVbStatus.current.fg2)
            }
        }
        Spacer(Modifier.width(12.dp))
        VbSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** The switch itself: a 46x28 track with a knob that springs across. */
@Composable
fun VbSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "switch-knob",
    )
    val track by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(VbMotion.SWITCH),
        label = "switch-track",
    )
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(VbShape.pill)
            .background(track)
            .vbClickable(role = Role.Switch) { onCheckedChange(!checked) }
            .clearAndSetSemantics { },
    ) {
        Box(
            Modifier
                .padding(start = knobOffset)
                .align(Alignment.CenterStart)
                .size(22.dp)
                .clip(VbShape.pill)
                .background(if (checked) MaterialTheme.colorScheme.onPrimary else Color.White),
        )
    }
}

/**
 * A collapsed disclosure whose header keeps showing a live summary while closed.
 *
 * The summary is the point: camera options stay out of the way without hiding what they are
 * currently set to, so nobody starts a lecture on last week's sensitivity by accident.
 */
@Composable
fun VbExpandableCard(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScopeAlias.() -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(),
        label = "disclosure-chevron",
    )
    VbCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VbSpace.touch)
                .vbClickable { onExpandedChange(!expanded) },
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(summary, style = MaterialTheme.typography.bodySmall, color = LocalVbStatus.current.fg2)
            }
            Icon(
                painter = painterResource(VbIcons.ChevronDown),
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = LocalVbStatus.current.fg2,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(VbMotion.EXPAND)) + fadeIn(tween(VbMotion.EXPAND)),
            exit = shrinkVertically(tween(VbMotion.EXPAND)) + fadeOut(tween(VbMotion.EXPAND)),
        ) {
            Column(Modifier.padding(top = 14.dp), content = content)
        }
    }
}

/** A tappable list row with a title, supporting line, optional badge and a chevron. */
@Composable
fun VbNavRow(
    title: String,
    supporting: String? = null,
    badge: String? = null,
    badgeColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .vbClickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (supporting != null) {
                Spacer(Modifier.height(2.dp))
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = LocalVbStatus.current.fg2)
            }
        }
        if (badge != null && badgeColor != null) {
            VbPill(badge, badgeColor)
            Spacer(Modifier.width(8.dp))
        }
        trailing?.invoke()
        Icon(
            painter = painterResource(VbIcons.ChevronRight),
            contentDescription = null,
            tint = LocalVbStatus.current.fg3,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** A tinted notice. [tone] must be one of the fixed status colours, never the accent. */
@Composable
fun VbNotice(
    title: String,
    body: String,
    tone: Color,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(VbShape.card)
            .background(tone.copy(alpha = 0.10f))
            .border(1.dp, tone.copy(alpha = 0.35f), VbShape.card)
            .padding(VbSpace.cardPad),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = tone)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        if (action != null) {
            Spacer(Modifier.height(12.dp))
            action()
        }
    }
}

/** Empty-state block: a title and one line explaining what would fill the space. */
@Composable
fun VbEmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = VbSpace.screenH),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalVbStatus.current.fg2,
        )
    }
}

/** A circle holding the first letter of a note title, used in every note list row. */
@Composable
fun VbInitial(title: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(VbShape.pill)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { contentDescription = "" },
    ) {
        Text(
            text = title.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Section header used between groups on a screen. No card chrome, no rule. */
@Composable
fun VbSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

/** A choice tile, used for mode and note-style selection. */
@Composable
fun VbChoiceTile(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null,
) {
    val border by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            LocalVbStatus.current.line
        },
        animationSpec = tween(VbMotion.EXPAND),
        label = "tile-border",
    )
    val fill by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(VbMotion.EXPAND),
        label = "tile-fill",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(VbMotion.EXPAND),
        label = "tile-content",
    )
    Column(
        modifier = modifier
            .clip(VbShape.tile)
            .background(fill)
            .border(if (selected) 1.5.dp else 1.dp, border, VbShape.tile)
            .vbClickable(role = Role.RadioButton, onClick = onClick)
            .padding(VbSpace.cardPad),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(10.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = content)
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) content.copy(alpha = 0.82f) else LocalVbStatus.current.fg2,
        )
    }
}

/** Rounded-rect helper for places that need a specific radius rather than a token. */
fun vbRadius(dp: Int): RoundedCornerShape = RoundedCornerShape(dp.dp)
