package com.noorpro.app.ui.islamic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import kotlin.math.cos
import kotlin.math.sin

/*
 * 1.1.6 shared building blocks for the Islamic feature screens (calendar, group khatm, hifz coach,
 * masjid pages, kids, deen goals). Same NoorPro language as Reels v2 / Profile v2: emerald green,
 * cream, gold accents, Libre Caslon headings, mint pills, khatam (8-point star) line-art instead of
 * illustrations.
 */

internal val IkGreen = Color(0xFF0B6243)
internal val IkGreenDeep = Color(0xFF07402C)
internal val IkGold = Color(0xFFD4AF37)
internal val IkGoldDeep = Color(0xFFA9842A)
internal val IkMint = Color(0xFFE6F2EC)
internal val IkCream = Color(0xFFFDF9F0)
internal val IkAmber = Color(0xFFC77D12)
internal val IkRed = Color(0xFFB3261E)

@Composable internal fun ikCard(): Color = if (isStitchLight()) Color.White else stitchSurface()
@Composable internal fun ikSoft(): Color = if (isStitchLight()) IkMint else Color(0xFF15302A)
/** Green that stays readable on the dark background. */
@Composable internal fun ikAccentText(): Color = if (isStitchLight()) IkGreen else Color(0xFF7FD1AE)

/** Back arrow + Libre Caslon title + optional subtitle and trailing actions. */
@Composable
internal fun IkHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onBack).testTag("ik_back"), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText())
            }
            Spacer(Modifier.width(4.dp))
        } else Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

/** Low-opacity khatam lattice (two overlapping squares per cell). */
@Composable
internal fun IkLattice(modifier: Modifier, line: Color, cell: Dp = 30.dp) {
    Canvas(modifier) {
        val s = cell.toPx(); val r = s * 0.42f; val stroke = Stroke(width = 1.dp.toPx())
        var y = 0f; var row = 0
        while (y < size.height + s) {
            var x = if (row % 2 == 0) 0f else s / 2f
            while (x < size.width + s) {
                val path = Path()
                for (k in 0 until 8) {
                    val a = Math.toRadians(45.0 * k).toFloat(); val b = Math.toRadians(45.0 * k + 135.0).toFloat()
                    path.moveTo(x + r * cos(a), y + r * sin(a)); path.lineTo(x + r * cos(b), y + r * sin(b))
                }
                drawPath(path, line, style = stroke)
                x += s
            }
            y += s * 0.866f; row++
        }
    }
}

/** Emerald hero panel with gold lattice; content sits on top in cream. */
@Composable
internal fun IkHero(modifier: Modifier = Modifier, colors: List<Color> = listOf(IkGreen, IkGreenDeep), content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(colors))) {
        IkLattice(Modifier.matchParentSize(), IkGold.copy(alpha = 0.13f), 34.dp)
        Box(Modifier.align(Alignment.TopEnd).offset(x = 36.dp, y = (-36).dp).size(130.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
internal fun IkCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(20.dp)).padding(padding),
        content = content,
    )
}

@Composable
internal fun IkSection(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 16.dp).clip(RoundedCornerShape(2.dp)).background(IkGold))
        Spacer(Modifier.width(8.dp))
        Text(text, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
internal fun IkPill(text: String, selected: Boolean, modifier: Modifier = Modifier, icon: ImageVector? = null, tag: String? = null, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(if (selected) IkGreen else ikSoft())
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp).let { if (tag != null) it.testTag(tag) else it },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (selected) Color.White else ikAccentText(), modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)) }
        Text(text, color = if (selected) Color.White else ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun IkButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, primary: Boolean = true, enabled: Boolean = true, tag: String? = null, onClick: () -> Unit) {
    val bg = when { !enabled -> stitchLine(); primary -> IkGreen; else -> ikCard() }
    Row(
        modifier.height(46.dp).clip(RoundedCornerShape(14.dp)).background(bg)
            .let { if (!primary) it.border(1.dp, stitchLine(), RoundedCornerShape(14.dp)) else it }
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp).let { if (tag != null) it.testTag(tag) else it },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (primary && enabled) Color.White else if (!enabled) stitchMutedText() else stitchText()
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Circular progress ring (track + gold-to-green sweep) with centred content. */
@Composable
internal fun IkRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 120.dp, stroke: Dp = 10.dp, track: Color = Color.White.copy(alpha = 0.18f),
                    colors: List<Color> = listOf(IkGold, Color(0xFFF3D77A)), content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = stroke.toPx(); val d = this.size.minDimension - w
            val tl = Offset((this.size.width - d) / 2f, (this.size.height - d) / 2f)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w))
            val p = progress.coerceIn(0f, 1f)
            if (p > 0f) drawArc(Brush.sweepGradient(colors + colors.first()), -90f, 360f * p, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Calm notice used when a feature's backend isn't deployed / reachable, or for helper disclaimers. */
@Composable
internal fun IkNotice(title: String, body: String, modifier: Modifier = Modifier, offline: Boolean = false, tag: String = "ik_notice") {
    val light = isStitchLight()
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (light) Color(0xFFF4F8F5) else Color(0xFF12201B))
        .border(1.dp, if (light) IkGreen.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)).testTag(tag)) {
        IkLattice(Modifier.matchParentSize(), if (light) IkGreen.copy(alpha = 0.06f) else IkGold.copy(alpha = 0.07f), 26.dp)
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(if (light) Color.White else Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                Icon(if (offline) Icons.Outlined.CloudOff else Icons.Outlined.Info, null, tint = ikAccentText(), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(body, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

/** Rounded icon tile (mint in light, deep green in dark). */
@Composable
internal fun IkIconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 40.dp, tint: Color = ikAccentText(), bg: Color = ikSoft()) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/** Screen scaffold background. */
@Composable internal fun Modifier.ikScreen(): Modifier = this.fillMaxSize().background(stitchBackground())
