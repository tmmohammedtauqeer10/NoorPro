package com.noorpro.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * 1.1.6: premium placeholder art for the Official channel screen (no cartoon artwork):
 *  - [OcShimmer]: soft skeleton sweep while a thumbnail / the list is loading.
 *  - [OcPatternBackdrop]: soft emerald gradient with low-opacity Islamic 8-point-star line-art,
 *    used when a thumbnail can't load and behind the feed / player error panels.
 */

internal class OcArtColors(val light: Boolean) {
    val base = if (light) Color(0xFFE9EFEC) else Color(0xFF1B2421)
    val sheen = if (light) Color(0xFFFBFDFC) else Color(0xFF2E3D37)
    val gradTop = if (light) Color(0xFFDDEBE4) else Color(0xFF0E2A22)
    val gradBottom = if (light) Color(0xFFC9DDD3) else Color(0xFF07110F)
    val line = if (light) Color(0xFF0B6243).copy(alpha = 0.13f) else Color(0xFFD4AF37).copy(alpha = 0.12f)
    val glyphBg = if (light) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.10f)
    val glyphRing = if (light) Color(0xFF0B6243).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.16f)
    val glyph = if (light) Color(0xFF0B6243) else Color(0xFFFDF9F0)
}

/** Moving highlight over a neutral base (skeleton). */
@Composable
internal fun Modifier.ocShimmer(light: Boolean): Modifier {
    val c = OcArtColors(light)
    val tr = rememberInfiniteTransition(label = "ocShimmer")
    val p by tr.animateFloat(-1f, 2f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "p")
    return this.drawWithCache {
        val w = size.width
        val brush = Brush.linearGradient(listOf(c.base, c.sheen, c.base), start = Offset(w * p - w * 0.6f, 0f), end = Offset(w * p, size.height))
        onDrawBehind { drawRect(brush) }
    }
}

@Composable
internal fun OcShimmer(light: Boolean, modifier: Modifier = Modifier) { Box(modifier.ocShimmer(light).testTag("oc_shimmer")) }

/** Gradient + 8-point-star (khatam) lattice line-art at low opacity. */
@Composable
internal fun OcPatternBackdrop(light: Boolean, modifier: Modifier = Modifier, cell: Dp = 34.dp) {
    val c = OcArtColors(light)
    Canvas(modifier.background(Brush.verticalGradient(listOf(c.gradTop, c.gradBottom)))) {
        val s = cell.toPx(); val r = s * 0.42f; val stroke = Stroke(width = 1.dp.toPx())
        fun star(cx: Float, cy: Float) {
            val path = Path()
            for (k in 0 until 8) { // two overlapping squares = 8-point star
                val a = Math.toRadians(45.0 * k).toFloat(); val b = Math.toRadians(45.0 * k + 135.0).toFloat()
                path.moveTo(cx + r * cos(a), cy + r * sin(a)); path.lineTo(cx + r * cos(b), cy + r * sin(b))
            }
            drawPath(path, c.line, style = stroke)
            drawCircle(c.line, r * 0.30f, Offset(cx, cy), style = stroke)
        }
        var y = 0f; var row = 0
        while (y < size.height + s) {
            var x = if (row % 2 == 0) 0f else s / 2f
            while (x < size.width + s) { star(x, y); x += s }
            y += s * 0.866f; row++
        }
        // soft vignette toward the bottom for depth
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, c.gradBottom.copy(alpha = 0.55f)), startY = size.height * 0.45f))
    }
}

/** Small refined play glyph (frosted circle + thin ring). */
@Composable
internal fun OcPlayGlyph(light: Boolean, size: Dp = 34.dp) {
    val c = OcArtColors(light)
    Box(Modifier.size(size).clip(CircleShape).background(c.glyphBg).border(1.dp, c.glyphRing, CircleShape), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * 0.42f)) {
            val w = this.size.width; val h = this.size.height; val m = min(w, h)
            drawPath(Path().apply { moveTo(w * 0.22f, 0f); lineTo(w * 0.22f + m * 0.86f, h / 2f); lineTo(w * 0.22f, h); close() }, c.glyph)
        }
    }
}

/** Thumbnail that couldn't load: pattern backdrop + play glyph. */
@Composable
internal fun OcThumbFallback(light: Boolean, modifier: Modifier = Modifier, glyph: Dp = 30.dp, showGlyph: Boolean = true) {
    Box(modifier.testTag("oc_thumb_fallback"), contentAlignment = Alignment.Center) {
        OcPatternBackdrop(light, Modifier.matchParentSize(), cell = 26.dp)
        if (showGlyph) OcPlayGlyph(light, glyph)
    }
}

/** Skeleton row for the list while the feed loads. */
@Composable
internal fun OcSkeletonRow(light: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OcShimmer(light, Modifier.width(132.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            OcShimmer(light, Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)))
            OcShimmer(light, Modifier.fillMaxWidth(0.75f).height(12.dp).clip(RoundedCornerShape(6.dp)))
            OcShimmer(light, Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(5.dp)))
        }
    }
}
