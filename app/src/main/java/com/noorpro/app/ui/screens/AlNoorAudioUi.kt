package com.noorpro.app.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.Reciter
import com.noorpro.app.data.Surah
import com.noorpro.app.ui.components.QuranArabicFont
import com.noorpro.app.ui.components.StitchEmerald
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.isStitchLight
import java.util.Calendar

/* ---------- Spotify-style Al-Noor Audio (1.0.80) ---------- */

/** Clean Spotify-like palette (no pattern), light + dark. */
internal class AudioTheme(val light: Boolean) {
    val bg = if (light) Color(0xFFFAFAF7) else Color(0xFF0E1210)
    val card = if (light) Color(0xFFFFFFFF) else Color(0xFF1A1F1C)
    val cardHi = if (light) Color(0xFFEFEFEA) else Color(0xFF252B27)
    val text = if (light) Color(0xFF151A17) else Color(0xFFF3F5F2)
    val muted = if (light) Color(0xFF6A716C) else Color(0xFFA6AEA8)
    val accent = Color(0xFF1FB86B)
    val gold = StitchGold
}

@Composable internal fun rememberAudioTheme(): AudioTheme { val l = isStitchLight(); return remember(l) { AudioTheme(l) } }

/** Deterministic cover colours for generated surah art. */
private val COVER_COLORS = listOf(
    Color(0xFF0F5D46), Color(0xFF6B3FA0), Color(0xFF1E5AA8), Color(0xFFB0532A), Color(0xFF8C2F5B),
    Color(0xFF2E7D6B), Color(0xFF7A6A1E), Color(0xFF3B4C9B), Color(0xFF9E3D3D), Color(0xFF1F6E8C),
)
internal fun coverColor(key: Int): Color = COVER_COLORS[Math.floorMod(key * 7 + 3, COVER_COLORS.size)]

/** Dominant colour of a drawable cover (average of saturated pixels), cached per resource. */
private val dominantCache = mutableMapOf<Int, Color>()
internal fun dominantColorOf(ctx: Context, res: Int, fallback: Color): Color = dominantCache.getOrPut(res) {
    runCatching {
        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bmp = BitmapFactory.decodeResource(ctx.resources, res, opts) ?: return@runCatching fallback
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        val step = maxOf(1, bmp.width / 24)
        var x = 0
        while (x < bmp.width) {
            var y = 0
            while (y < bmp.height) {
                val c = bmp.getPixel(x, y)
                val cr = (c shr 16) and 0xFF; val cg = (c shr 8) and 0xFF; val cb = c and 0xFF
                val mx = maxOf(cr, cg, cb); val mn = minOf(cr, cg, cb)
                val w = 1 + (mx - mn) / 16 // weight saturated pixels more
                if (mx in 30..235) { r += cr * w; g += cg * w; b += cb * w; n += w }
                y += step
            }
            x += step
        }
        if (n == 0L) fallback else Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }.getOrDefault(fallback)
}

@Composable internal fun rememberCoverColor(res: Int?, fallback: Color): Color {
    val ctx = LocalContext.current
    return remember(res) { if (res == null) fallback else dominantColorOf(ctx, res, fallback) }
}

/** Press-scale feedback with spring physics. */
@Composable
internal fun Modifier.pressScale(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "press")
    return this.scale(s).clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Shimmer placeholder brush. */
@Composable
internal fun shimmerBrush(t: AudioTheme): Brush {
    val tr = rememberInfiniteTransition(label = "shimmer")
    val x by tr.animateFloat(-300f, 900f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "sx")
    return Brush.linearGradient(listOf(t.cardHi, t.card, t.cardHi), start = Offset(x, 0f), end = Offset(x + 300f, 300f))
}

internal fun greetingFor(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) = when (hour) {
    in 4..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Peaceful night"
}

/** Generated square surah artwork: tinted gradient, number and Arabic name. */
@Composable
internal fun SurahCover(surah: Surah, size: Dp, corner: Dp = 8.dp, showArabic: Boolean = true) {
    val c = coverColor(surah.id)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(lerp(c, Color.White, 0.18f), c, lerp(c, Color.Black, 0.45f)))),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.matchParentSize().drawBehind {
            drawCircle(Color.White.copy(alpha = 0.10f), radius = this.size.minDimension * 0.42f, center = Offset(this.size.width * 0.85f, this.size.height * 0.1f))
        })
        if (showArabic && size >= 56.dp) {
            Text(surah.nameArabic, color = Color.White, fontFamily = QuranArabicFont, fontSize = (size.value / 5.2f).sp, maxLines = 1, textAlign = TextAlign.Center)
        }
        Text("${surah.id}", color = Color.White.copy(alpha = 0.85f), fontSize = (size.value / 9f).coerceAtLeast(9f).sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(size / 14))
    }
}

data class AudioQuickItem(val title: String, val surah: Surah? = null, val cover: Int? = null, val onClick: () -> Unit)
data class AudioPlaylistUi(val title: String, val subtitle: String, val count: Int, val cover: Int, val onOpen: () -> Unit)
data class AudioCategoryTile(val label: String, val color: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector)

internal val AUDIO_FILTERS = listOf("All", "Recitations", "Nasheeds", "Podcasts", "Dua Audio", "Tafsir", "Tarjuma", "Library")
internal val AUDIO_BROWSE = listOf(
    AudioCategoryTile("Recitations", Color(0xFF0F7A55), Icons.Default.GraphicEq),
    AudioCategoryTile("Nasheeds", Color(0xFF8E44AD), Icons.Default.MusicNote),
    AudioCategoryTile("Podcasts", Color(0xFF1E62C8), Icons.Default.Podcasts),
    AudioCategoryTile("Dua Audio", Color(0xFFC05A1B), Icons.Default.FrontHand),
    AudioCategoryTile("Tafsir", Color(0xFFA0306A), Icons.AutoMirrored.Filled.QueueMusic),
    AudioCategoryTile("Tarjuma", Color(0xFF3C7D2B), Icons.Default.Translate),
)

/* ---------- Home ---------- */

@Composable
internal fun AudioHomeHeader(t: AudioTheme, tint: Color, photoInitial: String, onBack: () -> Unit, onSearch: () -> Unit, onProfile: () -> Unit, filter: String, onFilter: (String) -> Unit, greeting: String = greetingFor()) {
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(tint.copy(alpha = if (t.light) 0.30f else 0.55f), t.bg)))) {
        Column(Modifier.statusBarsPadding().padding(top = 6.dp, bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.text) }
                Column(Modifier.weight(1f)) {
                    Text(greeting, color = t.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Al-Noor Audio", color = t.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search", tint = t.text) }
                Box(Modifier.padding(end = 8.dp).size(34.dp).clip(CircleShape).background(StitchEmerald).clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
                    Text(photoInitial, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AUDIO_FILTERS) { f ->
                    val sel = f == filter
                    val bg by animateColorAsState(if (sel) t.accent else t.cardHi, label = "chip")
                    Text(f.replace(" Audio", "s"), color = if (sel) Color.White else t.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).clickable { onFilter(f) }.padding(horizontal = 14.dp, vertical = 7.dp))
                }
            }
        }
    }
}

@Composable
internal fun AudioQuickGrid(t: AudioTheme, items: List<AudioQuickItem>, playingTitle: String?) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { q ->
                    Row(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(8.dp)).background(t.cardHi).pressScale(q.onClick),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            q.surah != null -> SurahCover(q.surah, 56.dp, 0.dp, showArabic = false)
                            q.cover != null -> Image(painterResource(q.cover), null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp))
                        }
                        Text(q.title, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                        if (playingTitle == q.title) EqualizerBars(t.accent, Modifier.padding(end = 10.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Animated "now playing" equalizer bars. */
@Composable
internal fun EqualizerBars(color: Color, modifier: Modifier = Modifier, animate: Boolean = true) {
    val tr = rememberInfiniteTransition(label = "eq")
    Row(modifier.height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        repeat(3) { i ->
            val h by tr.animateFloat(0.3f, 1f, infiniteRepeatable(tween(420 + i * 130), RepeatMode.Reverse), label = "eq$i")
            Box(Modifier.width(3.dp).fillMaxHeight(if (animate) h else 0.5f).clip(RoundedCornerShape(1.dp)).background(color))
        }
    }
}

@Composable
internal fun AudioSectionHeader(t: AudioTheme, title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = t.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = t.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
    }
}

@Composable
internal fun FeaturedHero(t: AudioTheme, surah: Surah, reciter: String, playing: Boolean, onPlay: () -> Unit, liked: Boolean, onLike: () -> Unit) {
    val c = coverColor(surah.id)
    Row(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(lerp(c, Color.Black, 0.25f), c.copy(alpha = 0.85f))))
            .pressScale(onPlay).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SurahCover(surah, 96.dp, 10.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("FEATURED TODAY", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("Surah ${surah.nameEnglish}", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(reciter, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like", tint = if (liked) t.accent else Color.White,
                    modifier = Modifier.size(22.dp).clickable(onClick = onLike))
                Spacer(Modifier.weight(1f))
                PlayFab(t, playing, 44.dp, onPlay)
            }
        }
    }
}

@Composable
internal fun PlayFab(t: AudioTheme, playing: Boolean, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(t.accent).pressScale(onClick), contentAlignment = Alignment.Center) {
        AnimatedContent(playing, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "pp") { p ->
            Icon(if (p) Icons.Default.Pause else Icons.Default.PlayArrow, if (p) "Pause" else "Play", tint = Color.Black, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
internal fun SurahCarousel(t: AudioTheme, surahs: List<Surah>, loading: Boolean, onPlay: (Surah) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (loading) items(5) { Box(Modifier.size(140.dp).clip(RoundedCornerShape(8.dp)).background(shimmerBrush(t))) }
        else items(surahs, key = { it.id }) { s ->
            Column(Modifier.width(140.dp).pressScale { onPlay(s) }) {
                SurahCover(s, 140.dp)
                Spacer(Modifier.height(8.dp))
                Text(s.nameEnglish, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${s.englishTranslation} · ${s.versesCount} ayahs", color = t.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
            }
        }
    }
}

/** Round reciter avatars; names wrap to 2 lines (no mid-word truncation) and stay aligned. */
@Composable
internal fun ReciterCarousel(t: AudioTheme, reciters: List<Reciter>, portraits: List<Int>, selectedId: Int, onOpen: (Reciter) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(reciters) { i, r ->
            Column(Modifier.width(100.dp).pressScale { onOpen(r) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).clip(CircleShape).then(if (r.id == selectedId) Modifier.border(3.dp, t.accent, CircleShape) else Modifier)) {
                    ReciterAvatarArt(r, portraits.getOrNull(i), 96.dp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    r.name, color = t.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp
                )
                Text("Reciter", color = t.muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
internal fun ReciterAvatarArt(r: Reciter, portrait: Int?, size: Dp) {
    if (portrait != null) Image(painterResource(portrait), r.name, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    else {
        val c = coverColor(r.id + 40)
        Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(lerp(c, Color.White, 0.2f), lerp(c, Color.Black, 0.35f)))), contentAlignment = Alignment.Center) {
            SpPattern(Modifier.fillMaxSize())
            Text(r.name.split(" ").filter { it.isNotBlank() && it.first().isUpperCase() }.take(2).joinToString("") { it.take(1) }.ifBlank { r.name.take(1) },
                color = Color.White, fontSize = (size.value / 3.2f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun PlaylistCarousel(t: AudioTheme, playlists: List<AudioPlaylistUi>) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(playlists) { p ->
            Column(Modifier.width(150.dp).pressScale(p.onOpen)) {
                Box(Modifier.size(150.dp).clip(RoundedCornerShape(6.dp))) {
                    Image(painterResource(p.cover), p.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("${p.count} tracks", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(p.title, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p.subtitle, color = t.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
            }
        }
    }
}

/** Spotify "Browse all" tiles with a tilted icon. */
@Composable
internal fun BrowseTiles(t: AudioTheme, onOpen: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AUDIO_BROWSE.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { c ->
                    Box(Modifier.weight(1f).height(92.dp).clip(RoundedCornerShape(10.dp)).background(c.color).pressScale { onOpen(c.label) }) {
                        Text(c.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(12.dp))
                        Icon(c.icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 6.dp).size(58.dp).rotate(22f))
                    }
                }
            }
        }
    }
}

/** Track row used by surah lists, library and reciter pages. */
@Composable
internal fun AudioTrackRow(t: AudioTheme, surah: Surah, subtitle: String, playing: Boolean, liked: Boolean, onPlay: () -> Unit, onLike: () -> Unit) {
    Row(Modifier.fillMaxWidth().pressScale(onPlay).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        SurahCover(surah, 52.dp, 4.dp, showArabic = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(surah.nameEnglish, color = if (playing) t.accent else t.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = t.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (playing) EqualizerBars(t.accent, Modifier.padding(end = 8.dp))
        Text(surah.nameArabic, color = t.muted, fontFamily = QuranArabicFont, fontSize = 16.sp, maxLines = 1, modifier = Modifier.padding(end = 4.dp))
        IconButton(onClick = onLike, modifier = Modifier.size(36.dp)) {
            Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (liked) "Unlike" else "Like", tint = if (liked) t.accent else t.muted, modifier = Modifier.size(20.dp))
        }
    }
}

/** Subtle geometric pattern kept only in the Recitations header (per 1.0.79 decision). */
internal fun Modifier.recitationPattern(color: Color): Modifier = drawBehind {
    val step = 64.dp.toPx(); val r = 20.dp.toPx(); var x = -step / 2
    while (x < size.width + step) {
        var y = -step / 2
        while (y < size.height + step) {
            drawCircle(color, r, Offset(x, y), style = Stroke(1.dp.toPx()))
            drawLine(color, Offset(x - r, y), Offset(x + r, y), 1.dp.toPx()); drawLine(color, Offset(x, y - r), Offset(x, y + r), 1.dp.toPx())
            y += step
        }
        x += step
    }
}

/* ---------- Collapsing detail page (reciter / recitations) ---------- */

@Composable
internal fun CollapsingDetailPage(
    t: AudioTheme, title: String, subtitle: String, tint: Color, art: @Composable (Dp) -> Unit,
    playing: Boolean, shuffle: Boolean, onBack: () -> Unit, onPlay: () -> Unit, onShuffle: () -> Unit,
    pattern: Boolean = false, listState: LazyListState = rememberLazyListState(),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val collapse by remember { derivedStateOf { if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / 600f).coerceIn(0f, 1f) } }
    Box(Modifier.fillMaxSize().background(t.bg)) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 200.dp)) {
            item(key = "hdr") {
                Box(
                    Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.55f), t.bg)))
                        .then(if (pattern) Modifier.recitationPattern(Color.White.copy(alpha = 0.07f)) else Modifier)
                ) {
                    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.graphicsLayer { val s = 1f - collapse * 0.35f; scaleX = s; scaleY = s; alpha = 1f - collapse }) { art(200.dp) }
                        Spacer(Modifier.height(16.dp))
                        Text(title, color = t.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                        Text(subtitle, color = t.muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                    }
                }
            }
            item(key = "actions") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) t.accent else t.muted, modifier = Modifier.size(28.dp).clickable(onClick = onShuffle))
                    Spacer(Modifier.weight(1f))
                    PlayFab(t, playing, 58.dp, onPlay)
                }
            }
            content()
        }
        // Collapsed top bar fades in as the header scrolls away.
        Row(
            Modifier.fillMaxWidth().background(lerp(t.bg.copy(alpha = 0f), lerp(tint, t.bg, 0.35f), collapse)).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.25f * (1f - collapse))), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.text)
                }
            }
            Text(title, color = t.text.copy(alpha = collapse), fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}

/* ---------- Search ---------- */

@Composable
internal fun AudioSearchPage(t: AudioTheme, query: String, onQuery: (String) -> Unit, results: List<Surah>, onBack: () -> Unit, onPlay: (Surah) -> Unit, onCategory: (String) -> Unit, liked: Set<Int>, onLike: (Surah) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(t.bg), contentPadding = PaddingValues(bottom = 200.dp)) {
        item {
            Row(Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.text) }
                Text("Search", color = t.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (t.light) Color(0xFFEDEDE8) else Color.White).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF333333))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Surahs, reciters, nasheeds…", color = Color(0xFF777777), fontSize = 15.sp)
                    BasicTextField(query, onQuery, singleLine = true, textStyle = TextStyle(color = Color(0xFF111111), fontSize = 15.sp), cursorBrush = SolidColor(t.accent), modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (query.isBlank()) {
            item { AudioSectionHeader(t, "Browse all"); Spacer(Modifier.height(10.dp)); BrowseTiles(t, onCategory) }
        } else {
            if (results.isEmpty()) item { Text("No surahs match \"$query\".", color = t.muted, modifier = Modifier.padding(16.dp)) }
            items(results, key = { it.id }) { s -> AudioTrackRow(t, s, "Surah · ${s.englishTranslation}", false, s.id in liked, { onPlay(s) }, { onLike(s) }) }
        }
    }
}
