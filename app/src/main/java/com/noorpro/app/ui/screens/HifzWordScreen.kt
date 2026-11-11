package com.noorpro.app.ui.screens

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.noorpro.app.quran.*
import com.noorpro.app.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Word-level hide modes for hifz. */
enum class HideMode(val label: String) { OFF("Show"), HIDE("Hide words"), HINTS("First letters") }

data class HifzConfig(
    val from: Int = 1, val to: Int = 7, val ayahRepeat: Int = 3, val rangeRepeat: Int = 2,
    val gapSec: Int = 2, val speed: Float = 1f, val hide: HideMode = HideMode.OFF
)

/** First Arabic letter plus its harakat, then a tatweel hint. */
internal fun firstLetterHint(word: String): String {
    if (word.isEmpty()) return word
    val sb = StringBuilder().append(word[0])
    var i = 1
    while (i < word.length && Character.getType(word[i]) == Character.NON_SPACING_MARK.toInt()) { sb.append(word[i]); i++ }
    return sb.append("ـ").toString()
}

/**
 * 1.0.76: Word-by-word + Hifz mode for the NORMAL reader (never the Indo-Pak mushaf).
 * [previewVerses]/[previewTiming]/[previewPosMs] let screenshot tests render without network.
 */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun HifzWordScreen(
    surahId: Int, surahName: String, versesCount: Int, startAyah: Int = 1, onBack: () -> Unit,
    initialTab: Int = 0,
    previewVerses: List<QVerse>? = null, previewAudio: QChapterAudio? = null, previewPosMs: Long? = null,
    previewConfig: HifzConfig? = null, previewPopup: QWord? = null
) {
    val context = LocalContext.current
    val repo = remember { WordByWordRepository(context) }
    val store = remember { HifzStore(context) }
    var tab by remember { mutableStateOf(initialTab) }
    var verses by remember { mutableStateOf(previewVerses ?: emptyList()) }
    var audio by remember { mutableStateOf(previewAudio) }
    var reciter by remember { mutableStateOf(HIFZ_RECITERS.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    var cfg by remember { mutableStateOf(previewConfig ?: HifzConfig(from = startAyah.coerceIn(1, versesCount), to = (startAyah + 4).coerceAtMost(versesCount))) }
    var posMs by remember { mutableStateOf(previewPosMs ?: -1L) }
    var playingAyah by remember { mutableStateOf<Int?>(previewAudio?.timings?.firstOrNull { (previewPosMs ?: -1) in it.fromMs until it.toMs }?.ayah) }
    var playing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var popup by remember { mutableStateOf(previewPopup) }
    var revealed by remember { mutableStateOf(setOf<String>()) }
    var gradeTick by remember { mutableStateOf(0) }

    LaunchedEffect(surahId) {
        if (previewVerses != null) return@LaunchedEffect
        runCatching { verses = repo.verses(surahId) }.onFailure { error = "Couldn't load word-by-word data. Connect once to download this surah." }
    }
    LaunchedEffect(surahId, reciter) {
        if (previewAudio != null) return@LaunchedEffect
        runCatching { audio = repo.chapterAudio(surahId, reciter.id) }.onFailure { if (error == null) error = "Recitation timings unavailable offline." }
    }
    val player = remember { if (previewVerses == null) ExoPlayer.Builder(context).build() else null }
    val wordPlayer = remember { if (previewVerses == null) ExoPlayer.Builder(context).build() else null }
    DisposableEffect(Unit) { onDispose { player?.release(); wordPlayer?.release() } }
    LaunchedEffect(audio?.audioUrl) { audio?.let { a -> player?.setMediaItem(MediaItem.fromUri(a.audioUrl)); player?.prepare() } }
    LaunchedEffect(cfg.speed) { player?.playbackParameters = PlaybackParameters(cfg.speed) }

    // Playback engine: repeat each ayah N times, the range M times, with a gap.
    LaunchedEffect(playing, audio, tab) {
        val p = player ?: return@LaunchedEffect
        val a = audio ?: return@LaunchedEffect
        if (!playing) { p.pause(); return@LaunchedEffect }
        val hifz = tab == 1
        val range = if (hifz) cfg.from..cfg.to else (playingAyah ?: startAyah).coerceIn(1, versesCount)..versesCount
        val ayahRep = if (hifz) cfg.ayahRepeat else 1
        val rangeRep = if (hifz) cfg.rangeRepeat else 1
        outer@ for (rr in 1..rangeRep) for (ay in range) for (r in 1..ayahRep) {
            val t = a.timings.firstOrNull { it.ayah == ay } ?: continue
            playingAyah = ay
            status = if (hifz) "Ayah $ay · repeat $r/$ayahRep · round $rr/$rangeRep" else "Ayah $ay"
            p.seekTo(t.fromMs); p.play()
            while (isActive) {
                delay(40)
                posMs = p.currentPosition
                if (posMs >= t.toMs || p.playbackState == androidx.media3.common.Player.STATE_ENDED) break
            }
            if (!isActive) break@outer
            if (hifz) { p.pause(); delay((cfg.gapSec * 1000L / cfg.speed).toLong()) }
        }
        p.pause(); playing = false; status = "Finished"
    }

    val listState = rememberLazyListState()
    LaunchedEffect(playingAyah) { playingAyah?.let { a -> val i = verses.indexOfFirst { it.ayah == a }; if (i >= 0 && previewVerses == null) listState.animateScrollToItem(i) } }

    fun playWord(w: QWord) {
        popup = w
        if (w.audioPath.isNotBlank()) wordPlayer?.run { setMediaItem(MediaItem.fromUri(WBW_AUDIO_BASE + w.audioPath)); prepare(); play() }
    }

    Box(Modifier.fillMaxSize().background(stitchBackground())) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText()) }
                Column(Modifier.weight(1f)) {
                    Text(surahName, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(if (tab == 1) "Hifz mode" else if (tab == 2) "Memorization tracker" else "Word by word", color = stitchMutedText(), fontSize = 12.sp)
                }
                val streak = remember(gradeTick) { store.streak() }
                if (streak > 0) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(StitchGold.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = StitchGold, modifier = Modifier.size(16.dp))
                    Text(" $streak day" + if (streak == 1) "" else "s", color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = stitchPrimary()) {
                listOf("Word by word", "Hifz", "Progress").forEachIndexed { i, l -> Tab(selected = tab == i, onClick = { playing = false; tab = i }, text = { Text(l, fontWeight = FontWeight.SemiBold) }) }
            }
            if (tab != 2) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HIFZ_RECITERS.forEach { r -> FilterChip(selected = r == reciter, onClick = { playing = false; reciter = r }, label = { Text(r.name, fontSize = 12.sp) }) }
                }
            }
            if (tab == 1) HifzControls(cfg, versesCount) { cfg = it }
            error?.let { Text(it, color = Color(0xFFBA1A1A), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (tab == 2) HifzProgress(store, surahId, versesCount, gradeTick, verses)
            else if (verses.isEmpty() && error == null) Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(70.dp), 14.dp) }
            } else LazyColumn(state = listState, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 170.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                val shown = if (tab == 1) verses.filter { it.ayah in cfg.from..cfg.to } else verses
                items(shown, key = { it.key }) { v ->
                    val t = audio?.timings?.firstOrNull { it.ayah == v.ayah }
                    val active = if (playingAyah == v.ayah) WordByWordRepository.activeWord(t, posMs) else null
                    val card = remember(gradeTick, v.ayah) { store.card(surahId, v.ayah) }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (playingAyah == v.ayah) stitchPrimary().copy(alpha = 0.07f) else stitchSurface())
                        .border(1.dp, if (playingAyah == v.ayah) stitchPrimary().copy(alpha = 0.4f) else stitchLine(), RoundedCornerShape(16.dp)).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${surahId}:${v.ayah}", color = stitchPrimary(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            if (card != null) Text(if (HifzSchedule.memorised(card)) "Memorised" else if (card.lastGood) "Review in ${card.intervalDays}d" else "Needs work",
                                color = if (card.lastGood) stitchPrimary() else Color(0xFFC2410C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (tab == 0) playingAyah = v.ayah; playing = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.PlayArrow, "Play ayah ${v.ayah}", tint = stitchPrimary())
                            }
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                v.words.forEach { w ->
                                    val id = "${v.ayah}:${w.position}"
                                    val hidden = tab == 1 && cfg.hide != HideMode.OFF && id !in revealed && active != w.position
                                    WordChip(w, active = active == w.position, hidden = hidden, hint = cfg.hide == HideMode.HINTS, showMeaning = tab == 0) {
                                        if (hidden) revealed = revealed + id else playWord(w)
                                    }
                                }
                            }
                        }
                        if (tab == 1) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedButton(onClick = { store.grade(surahId, v.ayah, false, v.juz); gradeTick++ }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Needs work", fontSize = 12.sp) }
                            Button(onClick = { store.grade(surahId, v.ayah, true, v.juz); gradeTick++; revealed = revealed.filterNot { it.startsWith("${v.ayah}:") }.toSet() },
                                colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary()), contentPadding = PaddingValues(horizontal = 12.dp)) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)); Text(" Correct", fontSize = 12.sp)
                            }
                            if (cfg.hide != HideMode.OFF) TextButton(onClick = { revealed = revealed + v.words.map { "${v.ayah}:${it.position}" } }) { Text("Reveal", fontSize = 12.sp) }
                        }
                    }
                }
                item { Text(QF_ATTRIBUTION, color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(8.dp)) }
            }
        }
        // Bottom player bar
        if (tab != 2) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(12.dp)
            .clip(RoundedCornerShape(20.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(20.dp)).padding(12.dp)) {
            popup?.let { w ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(stitchPrimary().copy(alpha = 0.08f)).padding(10.dp)) {
                    Text(w.text, fontFamily = QuranArabicFont, fontSize = 26.sp, color = stitchText())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.meaning.ifBlank { "—" }, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(w.transliteration, color = stitchMutedText(), fontSize = 12.sp)
                    }
                    IconButton(onClick = { playWord(w) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Hear word", tint = stitchPrimary()) }
                    IconButton(onClick = { popup = null }) { Icon(Icons.Default.Close, "Close", tint = stitchMutedText()) }
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(onClick = { if (audio == null) Toast.makeText(context, "Recitation not loaded yet", Toast.LENGTH_SHORT).show() else playing = !playing },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = stitchPrimary()), modifier = Modifier.size(48.dp)) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(status.ifBlank { if (tab == 1) "Ayahs ${cfg.from}–${cfg.to} · ×${cfg.ayahRepeat} each · ${cfg.rangeRepeat} rounds" else "Tap a word to hear it and see its meaning" },
                        color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(reciter.name + " · ${cfg.speed}x", color = stitchMutedText(), fontSize = 11.sp)
                }
                listOf(0.75f, 1f, 1.25f).forEach { s ->
                    Text("${s}x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (cfg.speed == s) Color.White else stitchText(),
                        modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(999.dp)).background(if (cfg.speed == s) stitchPrimary() else stitchSoftSurface())
                            .clickable { cfg = cfg.copy(speed = s) }.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun WordChip(w: QWord, active: Boolean, hidden: Boolean, hint: Boolean, showMeaning: Boolean, onClick: () -> Unit) {
    val bg by androidx.compose.animation.animateColorAsState(if (active) StitchGold.copy(alpha = 0.35f) else Color.Transparent, label = "wbg")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp)) {
        if (hidden && !hint) Box(Modifier.width((w.text.length * 9).coerceIn(28, 110).dp).height(30.dp).padding(vertical = 6.dp).clip(RoundedCornerShape(8.dp)).background(stitchLine()))
        else Text(if (hidden) firstLetterHint(w.text) else w.text, fontFamily = QuranArabicFont, fontSize = 26.sp, color = if (active) stitchPrimary() else stitchText())
        if (showMeaning && w.meaning.isNotBlank()) Text(w.meaning, fontSize = 10.sp, color = stitchMutedText(), maxLines = 2, lineHeight = 12.sp,
            modifier = Modifier.widthIn(max = 96.dp))
    }
}

@Composable
private fun HifzControls(cfg: HifzConfig, count: Int, onChange: (HifzConfig) -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stepper("From", cfg.from, 1, count, Modifier.weight(1f)) { onChange(cfg.copy(from = it, to = maxOf(it, cfg.to))) }
            Stepper("To", cfg.to, 1, count, Modifier.weight(1f)) { onChange(cfg.copy(to = it, from = minOf(it, cfg.from))) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stepper("Ayah ×", cfg.ayahRepeat, 1, 20, Modifier.weight(1f)) { onChange(cfg.copy(ayahRepeat = it)) }
            Stepper("Range ×", cfg.rangeRepeat, 1, 20, Modifier.weight(1f)) { onChange(cfg.copy(rangeRepeat = it)) }
            Stepper("Gap s", cfg.gapSec, 0, 15, Modifier.weight(1f)) { onChange(cfg.copy(gapSec = it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HideMode.values().forEach { m -> FilterChip(selected = cfg.hide == m, onClick = { onChange(cfg.copy(hide = m)) }, label = { Text(m.label, fontSize = 12.sp) }) }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    Row(modifier.clip(RoundedCornerShape(12.dp)).background(stitchSoftSurface()).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, color = stitchMutedText(), modifier = Modifier.padding(start = 6.dp).weight(1f))
        Icon(Icons.Default.Remove, "Decrease $label", tint = stitchPrimary(), modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onChange((value - 1).coerceAtLeast(min)) }.padding(6.dp))
        Text("$value", fontWeight = FontWeight.Bold, color = stitchText(), fontSize = 14.sp)
        Icon(Icons.Default.Add, "Increase $label", tint = stitchPrimary(), modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onChange((value + 1).coerceAtMost(max)) }.padding(6.dp))
    }
}

@Composable
private fun HifzProgress(store: HifzStore, surahId: Int, count: Int, tick: Int, verses: List<QVerse>) {
    val mem = remember(tick) { store.memorisedCount(surahId, count) }
    val started = remember(tick) { store.startedCount(surahId, count) }
    val due = remember(tick) { store.due(surahId, count) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Streak", "${store.streak()}d", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
                StatCard("Best", "${store.bestStreak()}d", Icons.Default.EmojiEvents, Modifier.weight(1f))
                StatCard("Due today", "${due.size}", Icons.Default.Schedule, Modifier.weight(1f))
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("This surah", color = stitchText(), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val prog = animatedProgress(mem / count.toFloat().coerceAtLeast(1f))
                LinearProgressIndicator(progress = { prog }, color = stitchPrimary(), trackColor = stitchSoftSurface(), modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)))
                Spacer(Modifier.height(6.dp))
                Text("$mem memorised · ${started - mem} in progress · ${count - started} not started", color = stitchMutedText(), fontSize = 12.sp)
                if (due.isNotEmpty()) Text("Review today: ayah " + due.take(12).joinToString(", ") + if (due.size > 12) "…" else "", color = Color(0xFFC2410C), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item { Text("By juz", color = stitchText(), fontWeight = FontWeight.Bold) }
        item {
            val juzCounts = remember(tick) { (1..30).associateWith { store.memorisedInJuz(it) } }
            @kotlin.OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..30).forEach { j ->
                    val total = com.noorpro.app.data.IslamicData.juzAyahsCount[j] ?: 1
                    val f = (juzCounts[j] ?: 0) / total.toFloat()
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(stitchSoftSurface()), contentAlignment = Alignment.Center) {
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(f.coerceIn(0f, 1f)).background(stitchPrimary().copy(alpha = 0.35f)))
                        Text("$j", color = stitchText(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Text("Spaced review: correct → 1d, 3d, then growing intervals; “needs work” resets to tomorrow. 3 correct reviews in a row = memorised.", color = stitchMutedText(), fontSize = 11.sp) }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Icon(icon, null, tint = StitchGold, modifier = Modifier.size(20.dp))
        Text(value, color = stitchText(), fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(label, color = stitchMutedText(), fontSize = 11.sp)
    }
}
