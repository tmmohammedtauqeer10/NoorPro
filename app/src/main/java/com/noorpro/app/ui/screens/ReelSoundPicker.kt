package com.noorpro.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.noorpro.app.data.media.edit.LibrarySound
import com.noorpro.app.data.media.edit.ReelSound
import com.noorpro.app.data.media.edit.ReelSoundLibrary
import com.noorpro.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Short preview player shared by the sound library rows (one sound at a time, like Instagram). */
@Composable
internal fun rememberSoundPreview(): Pair<String?, (ReelSound?) -> Unit> {
    val ctx = LocalContext.current
    val player = remember { runCatching { ExoPlayer.Builder(ctx).build() }.getOrNull() }
    var playing by remember { mutableStateOf<String?>(null) }
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(s: Int) { if (s == Player.STATE_ENDED) playing = null }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playing = null; Toast.makeText(ctx, "Couldn't play this sound — check your connection", Toast.LENGTH_SHORT).show()
            }
        }
        player?.addListener(l)
        onDispose { player?.removeListener(l); player?.release() }
    }
    return playing to { s ->
        val p = player
        if (p == null || s == null || playing == s.trackId) { p?.pause(); playing = null }
        else {
            // Al-Noor Audio may be playing in the background; the preview takes focus briefly.
            p.setMediaItem(MediaItem.fromUri(s.url)); p.prepare(); p.play(); playing = s.trackId
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelSoundPickerSheet(selected: ReelSound?, creatorName: String, onPick: (ReelSound?) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var all by remember { mutableStateOf<List<LibrarySound>?>(null) }
    LaunchedEffect(Unit) { all = withContext(Dispatchers.IO) { ReelSoundLibrary.load(ctx) } }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ReelSoundLibrary.ALL) }
    val shown = remember(all, query, category) { all?.let { ReelSoundLibrary.filter(it, query, category).take(300) } }
    val (playing, toggle) = rememberSoundPreview()
    var pendingOwn by remember { mutableStateOf<ReelSound?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } }.getOrNull()
            } ?: "Original sound"
            // Copy into the app cache so the export (and drafts) can read it after the picker grant ends.
            val copy = withContext(Dispatchers.IO) {
                runCatching {
                    val f = java.io.File(ctx.cacheDir, "reel_sounds/own_${System.currentTimeMillis()}.${name.substringAfterLast('.', "m4a").take(5)}").apply { parentFile?.mkdirs() }
                    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { i.copyTo(it) } } ?: return@runCatching null
                    if (f.length() > 25L * 1024 * 1024) { f.delete(); null } else f
                }.getOrNull()
            }
            if (copy == null) Toast.makeText(ctx, "Couldn't use that file (max 25 MB audio).", Toast.LENGTH_SHORT).show()
            else pendingOwn = ReelSoundLibrary.original(Uri.fromFile(copy).toString(), name, creatorName)
        }
    }
    val green = feedAccent()
    ModalBottomSheet(onDismissRequest = { toggle(null); onDismiss() }, containerColor = stitchSurface()) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 16.dp).testTag("reel_sound_picker")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Add sound", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text("From Al-Noor Audio · voice only, free to reuse with credit", color = stitchMutedText(), fontSize = 12.sp)
                }
                if (selected != null) TextButton(onClick = { toggle(null); onPick(null) }) { Text("Remove", color = Color(0xFFD9475A)) }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchBackground()).padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = stitchMutedText(), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search duas, azkar, tafsir…", color = stitchMutedText(), fontSize = 14.sp)
                    BasicTextField(query, { query = it.take(60) }, singleLine = true, cursorBrush = SolidColor(green),
                        textStyle = androidx.compose.ui.text.TextStyle(color = stitchText(), fontSize = 14.sp), modifier = Modifier.fillMaxWidth().testTag("reel_sound_search"))
                }
                if (query.isNotEmpty()) Icon(Icons.Default.Close, "Clear", tint = stitchMutedText(), modifier = Modifier.size(18.dp).clickable { query = "" })
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReelSoundLibrary.categories.forEach { c ->
                    val on = c == category
                    Text(c, color = if (on) Color.White else stitchText(), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) green else stitchBackground()).clickable { category = c }.padding(horizontal = 13.dp, vertical = 7.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            // Own original sound
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, green.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .clickable { picker.launch("audio/*") }.padding(12.dp).testTag("reel_sound_upload"), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(green.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.UploadFile, null, tint = green)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Use your own sound", color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Your recitation, nasheed (voice only) or talk — audio you recorded or own", color = stitchMutedText(), fontSize = 11.5.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                shown == null -> Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = green) }
                shown.isEmpty() -> Text(if (all.isNullOrEmpty()) "The sound library couldn't load." else "No sounds match \"$query\".",
                    color = stitchMutedText(), modifier = Modifier.padding(24.dp))
                else -> LazyColumn(Modifier.weight(1f)) {
                    items(shown, key = { it.sound.trackId + it.sound.url }) { s ->
                        SoundRow(s, playing == s.sound.trackId, s.sound.trackId == selected?.trackId, onPreview = { toggle(s.sound) }, onUse = { toggle(null); onPick(s.sound) })
                    }
                    item { Spacer(Modifier.navigationBarsPadding().height(16.dp)) }
                }
            }
        }
    }
    pendingOwn?.let { own ->
        AlertDialog(
            onDismissRequest = { pendingOwn = null },
            title = { Text("Use your own sound?") },
            text = { Text("Only use audio you recorded or have the rights to. No music with instruments and no clips from YouTube or other apps. Reels that break this are removed by Halal Guard.") },
            confirmButton = { TextButton(onClick = { pendingOwn = null; toggle(null); onPick(own) }) { Text("I own this audio") } },
            dismissButton = { TextButton(onClick = { pendingOwn = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SoundRow(s: LibrarySound, playing: Boolean, selected: Boolean, onPreview: () -> Unit, onUse: () -> Unit) {
    val green = feedAccent()
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onPreview).padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF0E8C73)))), contentAlignment = Alignment.Center) {
            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause preview" else "Play preview", tint = Color(0xFFD4AF37))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.sound.title, color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${s.sound.artist} · ${s.group}", color = stitchMutedText(), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 3.dp)) {
                MiniTag(s.tag); MiniTag(s.sound.licenseShort)
                if (s.durationMs > 0) MiniTag(formatSoundClock(s.durationMs))
                if (playing) Icon(Icons.Default.GraphicEq, null, tint = green, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (selected) green.copy(alpha = 0.15f) else green).clickable(onClick = onUse)
            .padding(horizontal = 14.dp, vertical = 8.dp).testTag("reel_sound_use"), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Default.Check, "Selected", tint = green, modifier = Modifier.size(16.dp))
            else Text("Use", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MiniTag(t: String) = Text(t, color = stitchMutedText(), fontSize = 10.sp,
    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(stitchBackground()).padding(horizontal = 6.dp, vertical = 1.dp))

internal fun formatSoundClock(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

/** Composer row replacing the 1.1.x free-text credit field. */
@Composable
fun ReelSoundRow(sound: ReelSound?, mixed: Boolean, onOpen: () -> Unit) {
    val green = feedAccent()
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp).testTag("composer_sound"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(green.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, tint = green, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(sound?.title ?: "Add sound", color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    sound == null -> "Duas, azkar & tafsir from Al-Noor Audio, or your own"
                    mixed -> "${sound.artist} · mixed into your reel"
                    else -> "${sound.artist} · ${sound.licenseShort} · mixed in when you share"
                }, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(if (sound == null) "Browse" else "Change", color = green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
