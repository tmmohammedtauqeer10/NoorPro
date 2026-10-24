package com.noorpro.app.ui.components

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Noor Pro motion system (1.0.73). Subtle Material 3 motion: short durations, standard easing,
 * GPU-only properties (alpha/scale/translation via graphicsLayer) so low-end devices stay at 60fps.
 * Everything collapses to "no animation" when the system "Remove animations" setting is on.
 */
object NoorMotion {
    const val SHORT = 150
    const val MEDIUM = 260
    const val STAGGER_MS = 35
    const val STAGGER_MAX = 10

    /** Read Settings.Global.ANIMATOR_DURATION_SCALE; 0 = user removed animations. */
    fun enabled(ctx: android.content.Context): Boolean = runCatching {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }.getOrDefault(true)
}

@Composable
fun animationsEnabled(): Boolean {
    val ctx = LocalContext.current
    return remember { NoorMotion.enabled(ctx) }
}

/** Card/tile press feedback: scale to 0.97 while pressed. Does not consume the gesture. */
fun Modifier.pressScale(pressedScale: Float = 0.97f): Modifier = composed {
    if (!animationsEnabled()) return@composed this
    var pressed by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (pressed) pressedScale else 1f, spring(stiffness = 900f), label = "press")
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/** One-time staggered entrance (fade + 12dp rise). [seen] keeps it from replaying on scroll. */
fun Modifier.staggeredEntrance(index: Int, seen: MutableSet<Int>? = null, key: Int = index): Modifier = composed {
    val enabled = animationsEnabled()
    val first = remember { seen?.add(key) ?: true }
    if (!enabled || !first) return@composed this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index.coerceAtMost(NoorMotion.STAGGER_MAX) * NoorMotion.STAGGER_MS).toLong())
        progress.animateTo(1f, tween(NoorMotion.MEDIUM, easing = FastOutSlowInEasing))
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 12.dp.toPx()
    }
}

/** Light haptic tick for key taps. Honours the system haptics setting via View. */
@Composable
fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } }
}

/** Skeleton shimmer placeholder (replaces spinners for list loading). */
@Composable
fun ShimmerBox(modifier: Modifier, corner: Dp = 12.dp) {
    val light = isStitchLight()
    val base = if (light) Color(0xFFE9ECE6) else Color(0xFF1A2622)
    val hi = if (light) Color(0xFFF7F8F5) else Color(0xFF26352F)
    val x = if (animationsEnabled()) {
        val t = rememberInfiniteTransition(label = "shimmer")
        t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "x").value
    } else 0.5f
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = listOf(base, hi, base),
                start = androidx.compose.ui.geometry.Offset(x * 600f - 300f, 0f),
                end = androidx.compose.ui.geometry.Offset(x * 600f + 300f, 0f),
            ),
            RoundedCornerShape(corner),
        )
    )
}

/** Animated 0..1 progress that eases to new values. */
@Composable
fun animatedProgress(target: Float): Float {
    val enabled = animationsEnabled()
    return animateFloatAsState(target.coerceIn(0f, 1f), if (enabled) tween(600, easing = FastOutSlowInEasing) else tween(0), label = "progress").value
}

/** Scale that pops (1 -> 1.25 -> 1) whenever [active] flips to true; static if motion is off. */
@Composable
fun popScale(active: Boolean): Float {
    val enabled = animationsEnabled()
    val s = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(active) {
        if (first) { first = false; return@LaunchedEffect }
        if (enabled && active) {
            s.snapTo(0.8f)
            s.animateTo(1.18f, tween(120))
            s.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
        }
    }
    return s.value
}

/** Instagram-style story ring: gold -> emerald sweep. */
val NoorRingBrush: Brush = Brush.sweepGradient(listOf(Color(0xFFD4AF37), Color(0xFF0E8C73), Color(0xFF89E6C8), Color(0xFFD4AF37)))
