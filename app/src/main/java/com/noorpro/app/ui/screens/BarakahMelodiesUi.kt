package com.noorpro.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.audio.catalog.BarakahMelodies
import com.noorpro.app.ui.theme.LibreCaslon

/* 1.2.0 (117): Barakah Melodies (licensed by Safar Media) in Al-Noor Audio: shelf card, page, credit, Spotify link. */

internal val BarakahTint = Color(0xFF7A4B1E)

@Composable
internal fun BarakahArt(m: Modifier, label: String = "Barakah\nMelodies") {
    Box(m.clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Color(0xFF3B2410), BarakahTint, Color(0xFFC9A24A)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicNote, null, tint = Color(0xFFF6E7C1), modifier = Modifier.size(26.dp))
            Text(label, color = Color(0xFFF6E7C1), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 17.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** Home / Nasheeds shelf entry. */
@Composable
internal fun BarakahShelfCard(t: SpTheme, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(t.elevated).clickable(onClick = onOpen)
        .padding(12.dp).testTag("barakah_shelf"), verticalAlignment = Alignment.CenterVertically) {
        BarakahArt(Modifier.size(72.dp), "BM")
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Barakah Melodies", color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(if (BarakahMelodies.available) "${BarakahMelodies.tracks.size} nasheeds · licensed by Safar Media" else "Nasheeds · licensed by Safar Media · arriving soon",
                color = t.muted, fontSize = 12.sp)
        }
        Icon(Icons.Default.PlayArrow, "Open", tint = t.accent, modifier = Modifier.size(28.dp))
    }
}

/** Credit + Spotify link block (shown on the page and under every Barakah track). */
@Composable
internal fun BarakahCredit(t: SpTheme, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, BarakahTint.copy(alpha = 0.45f), RoundedCornerShape(14.dp)).padding(12.dp).testTag("barakah_credit")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Verified, null, tint = Color(0xFFC9A24A), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(BarakahMelodies.CREDIT, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(BarakahMelodies.TERMS, color = t.muted, fontSize = 11.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.clip(CircleShape).background(Color(0xFF1DB954)).clickable {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BarakahMelodies.SPOTIFY_URL))) }
        }.padding(horizontal = 14.dp, vertical = 8.dp).testTag("barakah_spotify"), verticalAlignment = Alignment.CenterVertically) {
            Text("Follow Barakah Melodies on Spotify", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.OpenInNew, null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

/** Page body (inside SpDetailPage). Tracks play in the normal Al-Noor player with the credit; no share-out. */
internal fun LazyListScope.barakahPageItems(t: SpTheme, onPlay: (Int) -> Unit) {
    item(key = "bm_credit") { BarakahCredit(t, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
    val tracks = BarakahMelodies.tracks
    if (tracks.isEmpty()) item(key = "bm_soon") {
        Column(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(16.dp)).background(t.elevated).padding(16.dp).testTag("barakah_soon")) {
            Text("The full catalogue is on its way", color = t.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Safar Media is sending NoorPro the official audio files. Nasheeds will appear here, ready to play in the background, in Car mode and with a credit on every track.",
                color = t.muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    } else itemsIndexed(tracks, key = { _, tr -> tr.id }) { i, tr ->
        SpTrackRow(t, i + 1, tr.title, BarakahMelodies.credit(tr) ?: BarakahMelodies.CREDIT, false, { onPlay(i) }, art = { m -> BarakahArt(m, "") })
    }
}
