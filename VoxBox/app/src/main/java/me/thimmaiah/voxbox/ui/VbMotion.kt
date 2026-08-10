package me.thimmaiah.voxbox.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import me.thimmaiah.voxbox.ui.theme.LocalVbReducedMotion

/**
 * Motion vocabulary, in one place so the whole app decelerates the same way.
 *
 * Every entry animation and every looping animation asks [LocalVbReducedMotion] first. When the
 * user has turned animations off, entries become a short fade and loops hold still — a blinking
 * dot that cannot be switched off is a problem for people with vestibular or attention
 * conditions, and a lecture app is used for an hour at a time.
 */
object VbMotion {
    val decelerate: Easing = CubicBezierEasing(0.2f, 0.85f, 0.25f, 1f)
    val sheet: Easing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
    val sweep: Easing = CubicBezierEasing(0.5f, 0f, 0.5f, 1f)

    const val SCREEN_ENTER = 380
    const val BLOCK_ENTER = 500
    const val BLOCK_STAGGER = 60
    const val SWITCH = 280
    const val EXPAND = 300
    const val PRESS = 200
    const val ITEM_ARRIVE = 450
}

/** Screen-level entry: rise a little, scale a hair, fade in. */
@Composable
fun vbScreenEnter(): EnterTransition {
    if (LocalVbReducedMotion.current) return fadeIn(tween(100))
    return fadeIn(tween(VbMotion.SCREEN_ENTER, easing = VbMotion.decelerate)) +
        slideInVertically(tween(VbMotion.SCREEN_ENTER, easing = VbMotion.decelerate)) { 14 } +
        scaleIn(tween(VbMotion.SCREEN_ENTER, easing = VbMotion.decelerate), initialScale = 0.985f)
}

/** Staggered block entry for a column of cards. */
@Composable
fun vbBlockEnter(index: Int): EnterTransition {
    if (LocalVbReducedMotion.current) return fadeIn(tween(100))
    val spec = tween<Float>(
        durationMillis = VbMotion.BLOCK_ENTER,
        delayMillis = index * VbMotion.BLOCK_STAGGER,
        easing = VbMotion.decelerate,
    )
    return fadeIn(spec) + slideInVertically(
        tween(VbMotion.BLOCK_ENTER, index * VbMotion.BLOCK_STAGGER, VbMotion.decelerate),
    ) { 18 }
}

/**
 * A value that oscillates forever, or holds at [restingValue] under reduced motion.
 *
 * The resting value matters: a LIVE dot that stops blinking must stay fully opaque, because it
 * still carries the fact that recording is happening.
 */
@Composable
fun vbPulse(
    from: Float,
    to: Float,
    durationMillis: Int,
    easing: Easing = VbMotion.decelerate,
    repeatMode: RepeatMode = RepeatMode.Reverse,
    restingValue: Float = to,
    label: String = "pulse",
): State<Float> {
    if (LocalVbReducedMotion.current) {
        return remember(restingValue) { mutableFloatStateOf(restingValue) }
    }
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = InfiniteRepeatableSpec(tween(durationMillis, easing = easing), repeatMode),
        label = label,
    )
}

/** A value that ramps from [from] to [to] and restarts, for sweeps and expanding rings. */
@Composable
fun vbLoop(
    from: Float,
    to: Float,
    durationMillis: Int,
    delayMillis: Int = 0,
    easing: Easing = VbMotion.sweep,
    restingValue: Float = from,
    label: String = "loop",
): State<Float> {
    if (LocalVbReducedMotion.current) {
        return remember(restingValue) { mutableFloatStateOf(restingValue) }
    }
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, delayMillis = delayMillis, easing = easing),
            repeatMode = RepeatMode.Restart,
        ),
        label = label,
    )
}
