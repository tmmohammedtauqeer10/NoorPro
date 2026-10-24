package com.noorpro.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.audio.catalog.OfficialChannel
import com.noorpro.app.audio.catalog.OfficialChannels

private val YtRed = Color(0xFFFF0033)

/** Small red rounded-rect play glyph (generic "video" mark). */
@Composable
internal fun YouTubeGlyph(size: Dp = 18.dp, tint: Color = YtRed, play: Color = Color.White) {
    Canvas(Modifier.size(width = size * 1.4f, height = size)) {
        drawRoundRect(tint, cornerRadius = CornerRadius(this.size.height * 0.28f))
        val w = this.size.width; val h = this.size.height
        drawPath(Path().apply { moveTo(w * 0.40f, h * 0.28f); lineTo(w * 0.66f, h * 0.5f); lineTo(w * 0.40f, h * 0.72f); close() }, play)
    }
}

/** Neutral initials avatar — we never show scholars' photos without an official, permitted source. */
@Composable
internal fun ChannelInitials(ch: OfficialChannel, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF0F5D46), Color(0xFF2E7D6B)))), contentAlignment = Alignment.Center) {
        Text(ch.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp)
    }
}

/**
 * "Official channels" card: name, subtitle, and a Watch on YouTube button that opens the channel
 * externally. Colours are passed in so it fits both the Spotify-style and Stitch screens.
 */
@Composable
internal fun OfficialChannelCard(
    ch: OfficialChannel, bg: Color, text: Color, muted: Color, modifier: Modifier = Modifier,
    onOpen: ((String) -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val open = { onOpen?.invoke(ch.url) ?: OfficialChannels.open(ctx, ch.url) }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).border(1.dp, muted.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .clickable(onClick = open).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelInitials(ch, 54.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ch.name, color = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YouTubeGlyph(12.dp); Spacer(Modifier.width(6.dp))
                    Text(ch.subtitle, color = muted, fontSize = 13.sp)
                }
                Text("${ch.category} · ${ch.language}", color = muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(42.dp).clip(CircleShape).background(YtRed).clickable(onClick = open),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            YouTubeGlyph(14.dp, tint = Color.White, play = YtRed)
            Spacer(Modifier.width(8.dp))
            Text("Watch on YouTube", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text("Opens the official channel on YouTube. Noor Pro does not host this content.", color = muted, fontSize = 11.sp)
    }
}

/** Section block: header + one card per official channel. */
@Composable
internal fun OfficialChannelsBlock(bg: Color, text: Color, muted: Color, modifier: Modifier = Modifier, channels: List<OfficialChannel> = OfficialChannels.all) {
    if (channels.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Official channels", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        channels.forEach { OfficialChannelCard(it, bg, text, muted) }
    }
}
