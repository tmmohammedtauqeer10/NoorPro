package com.noorpro.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.ui.components.QuranArabicFont
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon

/*
 * 1.1.5 NoorPro Reels v2 — the stateless UI. NoorReels2Screen.kt wires it to the repositories and
 * ExoPlayer; V109 render tests drive it with sample data. Original NoorPro style: deep green
 * #0B6243, cream #FDF9F0, gold accents, Libre Caslon headings, mint pill tabs.
 */

internal val R2Green = Color(0xFF0B6243)
internal val R2Cream = Color(0xFFFDF9F0)
internal val R2Gold = Color(0xFFD4AF37)
internal val R2GoldDeep = Color(0xFFA9842A)
internal val R2Mint = Color(0xFFE6F2EC)
internal val R2Ink = Color(0xFF0D1C2F)
private val Glass = Color(0x33FFFFFF)
private val GlassDark = Color(0x66000000)

data class Reels2Ui(
    val post: UmmahPost,
    val liked: Boolean = false,
    val saved: Boolean = false,
    val following: Boolean = false,
    val own: Boolean = false,
    val verified: Boolean = false,
    val reaction: HalalReaction? = null,
    val likeCount: Long = post.likeCount,
    val commentCount: Long = post.commentCount,
    val shareCount: Long = post.shareCount,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val captionsOn: Boolean = false,
    val captionLang: String = "EN",
    val soundTitle: String = "Original audio · ${post.creatorName.ifBlank { "creator" }}",
    val nextPart: UmmahPost? = null,
    val refCount: Int = 0,
    /** Decoded frame aspect (ExoPlayer videoSize) when known, else the post's stored geometry. */
    val measuredAspect: Float = 0f,
    /** Live caption line (text track / AI transcript) shown when captions are on. */
    val captionLine: String = "",
) {
    val frame: ReelFrame get() = if (measuredAspect > 0f) reelFrameForAspect(measuredAspect)
        else reelFrameFor(post.mediaWidth, post.mediaHeight, post.mediaRotationDegrees)
    val chapters: List<ReelChapter> get() = parseReelChapters(post.caption)
    val series: ReelSeriesInfo? get() = parseReelSeries(post.caption)
}

class Reels2Actions(
    val onLike: () -> Unit = {},
    val onReact: (HalalReaction) -> Unit = {},
    /** Long-press on like: open the halal reaction picker (flagged). */
    val onReactions: () -> Unit = {},
    val onComment: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onRemix: () -> Unit = {},
    val onSound: () -> Unit = {},
    val onFollow: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onDoubleTap: () -> Unit = {},
    val onTogglePlay: () -> Unit = {},
    val onHold: (Boolean) -> Unit = {},
    val onSpeed: () -> Unit = {},
    val onCaptions: () -> Unit = {},
    val onCaptionLang: (String) -> Unit = {},
    val onFullScreen: () -> Unit = {},
    val onSeries: () -> Unit = {},
    val onNextPart: () -> Unit = {},
    val onVerify: () -> Unit = {},
    val onAskAi: () -> Unit = {},
    val onMenu: () -> Unit = {},
    val onSeek: (Long) -> Unit = {},
)

/** What the page shows on top of the video (tests force one; the screen drives it from state). */
enum class Reels2Overlay { NONE, HEART, HOLD, REACTIONS }

data class Reels2Features(
    val series: Boolean = Reels2Flags.isOn(Reels2Flags.SERIES),
    val soundPages: Boolean = Reels2Flags.isOn(Reels2Flags.SOUND_PAGES),
    val reactions: Boolean = Reels2Flags.isOn(Reels2Flags.REACTIONS),
    val aiAsk: Boolean = Reels2Flags.isOn(Reels2Flags.AI_ASK),
    val aiTranscripts: Boolean = Reels2Flags.isOn(Reels2Flags.AI_TRANSCRIPTS),
    val voiceComments: Boolean = Reels2Flags.isOn(Reels2Flags.VOICE_COMMENTS),
    val creatorLiked: Boolean = Reels2Flags.isOn(Reels2Flags.CREATOR_LIKED),
    val live: Boolean = Reels2Flags.isOn(Reels2Flags.LIVE),
) { companion object { val ALL_ON = Reels2Features(true, true, true, true, true, true, true, true) } }

// ================================================================ top chrome

@Composable
fun Reels2TopBar(
    tab: Reels2Tab, onTab: (Reels2Tab) -> Unit, topic: String, onTopic: (String) -> Unit,
    onSearch: () -> Unit = {}, onCreate: () -> Unit = {}, modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent)))
        .statusBarsPadding().padding(top = 6.dp, bottom = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Reels", color = Color.White, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Row(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(999.dp)).background(Color(0x33FFFFFF)).padding(3.dp)
                .testTag("reels2_tabs")) {
                Reels2Tab.entries.forEach { t ->
                    val on = t == tab
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(999.dp))
                        .background(if (on) R2Mint else Color.Transparent).clickable { onTab(t) }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (on) { Box(Modifier.size(5.dp).clip(CircleShape).background(R2Gold)); Spacer(Modifier.width(5.dp)) }
                            Text(t.label, color = if (on) R2Green else Color.White, fontSize = 12.5.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            GlassIcon(Icons.Default.Search, "Search reels", onSearch)
            Spacer(Modifier.width(6.dp))
            GlassIcon(Icons.Default.Add, "Create reel", onCreate)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp)) {
            items(reels2Topics) { t ->
                val on = t == topic
                Text(t, color = if (on) R2Green else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) R2Gold else Color(0x29FFFFFF))
                        .border(1.dp, if (on) Color.Transparent else Color(0x44FFFFFF), RoundedCornerShape(999.dp))
                        .clickable { onTopic(t) }.padding(horizontal = 14.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun GlassIcon(icon: ImageVector, cd: String, onClick: () -> Unit, size: Dp = 36.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0x33FFFFFF)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, cd, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

// ================================================================ media layer

/**
 * Frames the clip. FULL_BLEED: fills the screen. FIT_BLUR: the same frame blurred + darkened behind,
 * the clip fitted to width and centred (never cropped), plus a 'Full screen' pill for landscape.
 * [background] / [foreground] draw the frame (a test bitmap, or thumbnail + PlayerView).
 */
@Composable
fun Reels2Media(
    frame: ReelFrame,
    background: @Composable (Modifier) -> Unit,
    foreground: @Composable (Modifier) -> Unit,
    onFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        if (frame.mode == ReelFrameMode.FULL_BLEED) {
            foreground(Modifier.fillMaxSize())
        } else {
            background(Modifier.fillMaxSize().blur(36.dp).scale(1.25f))
            Box(Modifier.fillMaxSize().background(Color(0x99000000)))
            val h = fittedReelHeightDp(maxWidth.value, maxHeight.value, frame.aspect).dp
            val w = minOf(maxWidth, h * frame.aspect)
            Column(Modifier.align(Alignment.Center).offset(y = (-24).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(w).height(h).testTag("reels2_fit_${frame.label}")) { foreground(Modifier.fillMaxSize()) }
                if (frame.showFullScreenPill) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x33FFFFFF)).border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(999.dp))
                        .clickable(onClick = onFullScreen).padding(horizontal = 14.dp, vertical = 7.dp).testTag("reels2_fullscreen"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ScreenRotation, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Full screen", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** True in the app (thumbnails/avatars load with Coil); false in render tests that pass bitmaps. */
val LocalReels2RemoteImages = staticCompositionLocalOf { false }

/** A post thumbnail: the given bitmap (tests) or the post's preview loaded with Coil (app). */
@Composable
fun Reels2Thumb(post: UmmahPost, bmp: ImageBitmap?, modifier: Modifier, scale: ContentScale = ContentScale.Crop) {
    Box(modifier) {
        Reels2Bitmap(bmp, Modifier.matchParentSize(), scale)
        if (bmp == null && LocalReels2RemoteImages.current) post.previewModel()?.let { m ->
            coil.compose.AsyncImage(m, null, contentScale = scale, modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
fun Reels2Bitmap(bmp: ImageBitmap?, modifier: Modifier, scale: ContentScale = ContentScale.Crop) {
    if (bmp != null) Image(bmp, null, contentScale = scale, modifier = modifier)
    else Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFF0E3B2C), Color(0xFF06291D)))))
}

// ================================================================ page

@Composable
fun Reels2Page(
    ui: Reels2Ui,
    actions: Reels2Actions,
    background: @Composable (Modifier) -> Unit,
    foreground: @Composable (Modifier) -> Unit,
    overlay: Reels2Overlay = Reels2Overlay.NONE,
    features: Reels2Features = Reels2Features(),
    captionExpanded: Boolean = false,
    onExpandCaption: () -> Unit = {},
    recitationIndex: Int = -1,
    topPadding: Dp = 132.dp,
    /** Space kept free at the bottom (the app's floating nav bar when it is shown). */
    bottomInset: Dp = 0.dp,
    /** Extra card above the caption (e.g. the series 'Next part' card). */
    aboveInfo: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var heart by remember { mutableStateOf(overlay == Reels2Overlay.HEART) }
    val heartScale = remember { Animatable(if (overlay == Reels2Overlay.HEART) 1f else 0f) }
    LaunchedEffect(heart) {
        if (heart && overlay != Reels2Overlay.HEART) {
            heartScale.snapTo(0.4f); heartScale.animateTo(1.15f, spring(dampingRatio = 0.45f, stiffness = 380f))
            kotlinx.coroutines.delay(350); heartScale.animateTo(0f, tween(250)); heart = false
        }
    }
    Box(modifier.fillMaxSize().background(Color.Black).testTag("reels2_page").pointerInput(ui.post.id) {
        detectTapGestures(
            onDoubleTap = { heart = true; actions.onDoubleTap() },
            onTap = { actions.onTogglePlay() },
            onLongPress = { actions.onHold(true) },
        )
    }) {
        Reels2Media(ui.frame, background, foreground, actions.onFullScreen)
        // bottom scrim for legibility
        Box(Modifier.fillMaxWidth().height(360.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))

        if (recitationIndex >= 0 && isRecitationReel(ui.post)) RecitationOverlay(ui.post, recitationIndex, Modifier.align(Alignment.TopCenter).padding(top = topPadding + 8.dp))
        if (ui.captionsOn && (ui.captionLine.isNotBlank() || features.aiTranscripts)) LiveCaptionStrip(ui.captionLine, ui.captionLang, actions.onCaptionLang, features.aiTranscripts,
            Modifier.align(Alignment.Center).offset(y = 120.dp))

        Reels2Rail(ui, actions, features, Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 92.dp + bottomInset))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 84.dp, bottom = 34.dp + bottomInset)) {
            if (aboveInfo != null) { aboveInfo(); Spacer(Modifier.height(10.dp)) }
            Reels2Info(ui, actions, features, captionExpanded, onExpandCaption, Modifier)
        }
        Reels2Scrubber(ui.positionMs, ui.durationMs, ui.chapters, actions.onSeek, Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp + bottomInset, start = 14.dp, end = 14.dp))

        if (heart || overlay == Reels2Overlay.HEART) Icon(Icons.Filled.Favorite, "Liked", tint = R2Gold,
            modifier = Modifier.align(Alignment.Center).size(120.dp).scale(heartScale.value.coerceAtLeast(if (overlay == Reels2Overlay.HEART) 1f else 0f)).testTag("reels2_heart"))
        if (overlay == Reels2Overlay.HOLD) HoldOverlay(ui, actions, Modifier.fillMaxSize())
        if (overlay == Reels2Overlay.REACTIONS) ReactionPicker(ui.reaction, actions.onReact, Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 400.dp + bottomInset))
    }
}

@Composable
private fun Reels2Rail(ui: Reels2Ui, a: Reels2Actions, f: Reels2Features, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val likeIcon = if (ui.reaction != null && ui.reaction != HalalReaction.LIKE) reactionIcon(ui.reaction) else if (ui.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
        val likeTint = ui.reaction?.let { Color(it.color) }?.takeIf { ui.reaction != HalalReaction.LIKE } ?: if (ui.liked) Color(0xFFE0485C) else Color.White
        RailButton(likeIcon, ui.reaction?.label ?: reelCount(ui.likeCount), likeTint, "Like", a.onLike,
            onLong = if (f.reactions) a.onReactions else null, tag = "reels2_like")
        RailButton(Icons.Outlined.ChatBubbleOutline, reelCount(ui.commentCount), Color.White, "Comments", a.onComment)
        RailButton(if (ui.saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, if (ui.saved) "Saved" else "Save", if (ui.saved) R2Gold else Color.White, "Save", a.onSave)
        RailButton(Icons.Outlined.Send, reelCount(ui.shareCount), Color.White, "Share", a.onShare)
        RailButton(Icons.Outlined.Autorenew, "Remix", Color.White, "Remix", a.onRemix)
        SoundDisc(ui.post, a.onSound)
    }
}

@Composable
private fun RailButton(icon: ImageVector, label: String, tint: Color, cd: String, onClick: () -> Unit, onLong: (() -> Unit)? = null, tag: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.then(if (tag.isNotBlank()) Modifier.testTag(tag) else Modifier)
        .pointerInput(onLong) { detectTapGestures(onTap = { onClick() }, onLongPress = onLong?.let { l -> { _ -> l() } }) }) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x2EFFFFFF)), contentAlignment = Alignment.Center) {
            Icon(icon, cd, tint = tint, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun SoundDisc(post: UmmahPost, onClick: () -> Unit, spin: Boolean = true) {
    // Slow spin (infinite transition: cancelled automatically by the Compose test clock policy).
    val angle = if (spin) rememberInfiniteTransition(label = "disc").animateFloat(0f, 360f,
        infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "a").value else 0f
    Box(Modifier.size(42.dp).rotate(angle).clip(CircleShape)
        .background(Brush.sweepGradient(listOf(Color(0xFF1B1B1B), Color(0xFF3A3A3A), Color(0xFF1B1B1B), Color(0xFF3A3A3A), Color(0xFF1B1B1B))))
        .border(2.dp, R2Gold, CircleShape).clickable(onClick = onClick).testTag("reels2_disc"), contentAlignment = Alignment.Center) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(R2Green), contentAlignment = Alignment.Center) {
            Text(post.creatorName.take(1).uppercase().ifBlank { "N" }, color = R2Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Reels2Info(ui: Reels2Ui, a: Reels2Actions, f: Reels2Features, expanded: Boolean, onExpand: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (f.series) ui.series?.let { s ->
            Row(Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x33D4AF37)).border(1.dp, R2Gold.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                .clickable(onClick = a.onSeries).padding(horizontal = 10.dp, vertical = 5.dp).testTag("reels2_series"), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlaylistPlay, null, tint = R2Gold, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp))
                Text("${s.title} · Part ${s.part}${s.total?.let { "/$it" } ?: ""}", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).border(2.dp, R2Gold, CircleShape).padding(2.dp).clip(CircleShape).background(R2Green).clickable(onClick = a.onProfile),
                contentAlignment = Alignment.Center) {
                Text(ui.post.creatorName.take(1).uppercase().ifBlank { "N" }, color = R2Gold, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold)
                if (ui.post.creatorPhotoUrl.isNotBlank() && LocalReels2RemoteImages.current) coil.compose.AsyncImage(ui.post.creatorPhotoUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f, fill = false)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ui.post.creatorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (ui.verified) { Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Verified, "Verified", tint = R2Gold, modifier = Modifier.size(15.dp)) }
                }
                Text(ui.post.creatorHandle, color = Color(0xCCFFFFFF), fontSize = 11.sp, maxLines = 1)
            }
            if (!ui.own) {
                Spacer(Modifier.width(10.dp))
                Text(if (ui.following) "Following" else "Follow", color = if (ui.following) Color.White else R2Green, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (ui.following) Color(0x33FFFFFF) else R2Mint)
                        .clickable(onClick = a.onFollow).padding(horizontal = 14.dp, vertical = 6.dp).testTag("reels2_follow"))
            }
        }
        val caption = captionWithoutChapters(ui.post.caption)
        Text(caption + if (!expanded && caption.length > 90) "" else "", color = Color.White, fontSize = 13.sp, lineHeight = 18.sp,
            maxLines = if (expanded) 8 else 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable(onClick = onExpand).testTag("reels2_caption"))
        if (!expanded && caption.length > 90) Text("more", color = Color(0xCCFFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onExpand))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f, fill = false).clip(RoundedCornerShape(999.dp)).background(Color(0x29FFFFFF)).clickable(onClick = a.onSound).padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp))
                Text(ui.soundTitle, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (ui.refCount > 0) MiniChip(Icons.Default.FactCheck, "Verify", R2Mint, R2Green, a.onVerify, "reels2_verify")
            if (f.aiAsk) MiniChip(Icons.Default.AutoAwesome, "Ask Noor AI", Color(0x33D4AF37), R2Gold, a.onAskAi, "reels2_ask")
        }
    }
}

@Composable
private fun MiniChip(icon: ImageVector, label: String, bg: Color, fg: Color, onClick: () -> Unit, tag: String) {
    Row(Modifier.clip(RoundedCornerShape(999.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 4.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp))
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun Reels2Scrubber(positionMs: Long, durationMs: Long, chapters: List<ReelChapter>, onSeek: (Long) -> Unit, modifier: Modifier = Modifier, showLabels: Boolean = false) {
    val frac = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Column(modifier.fillMaxWidth()) {
        if (showLabels && chapters.isNotEmpty()) {
            val i = chapterIndexAt(positionMs, chapters)
            Text("Chapter ${i + 1} · ${chapters[i].title}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(10.dp).pointerInput(durationMs) {
            detectTapGestures { o -> if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong()) }
        }, contentAlignment = Alignment.CenterStart) {
            val w = maxWidth
            if (chapters.isEmpty() || durationMs <= 0) {
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x55FFFFFF)))
                Box(Modifier.width(w * frac).height(3.dp).clip(RoundedCornerShape(2.dp)).background(R2Gold))
            } else {
                // Chapter segments with small gaps (like a book's sections)
                val bounds = chapters.map { it.startMs } + durationMs
                bounds.zipWithNext().forEach { (s, e) ->
                    val x = w * (s.toFloat() / durationMs); val sw = w * ((e - s).toFloat() / durationMs) - 3.dp
                    val filled = ((positionMs - s).toFloat() / (e - s)).coerceIn(0f, 1f)
                    Box(Modifier.offset(x = x).width(sw.coerceAtLeast(2.dp)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x55FFFFFF)))
                    Box(Modifier.offset(x = x).width((sw * filled).coerceAtLeast(0.dp)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(R2Gold))
                }
            }
            Box(Modifier.offset(x = w * frac - 5.dp).size(10.dp).clip(CircleShape).background(Color.White))
        }
    }
}

// ================================================================ overlays

@Composable
private fun HoldOverlay(ui: Reels2Ui, a: Reels2Actions, modifier: Modifier) {
    Box(modifier.background(Color(0x77000000)).testTag("reels2_hold"), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Pause, "Paused", tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("Paused — release to play", color = Color.White, fontSize = 12.5.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickControl(Icons.Default.Speed, speedLabel(ui.speed), a.onSpeed)
                QuickControl(if (ui.captionsOn) Icons.Filled.ClosedCaption else Icons.Outlined.ClosedCaptionOff, if (ui.captionsOn) "CC on" else "CC off", a.onCaptions)
                QuickControl(Icons.Default.MoreHoriz, "More", a.onMenu)
            }
        }
    }
}

@Composable
private fun QuickControl(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x33FFFFFF)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp)); Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal fun reactionIcon(r: HalalReaction): ImageVector = when (r) {
    HalalReaction.LIKE -> Icons.Filled.Favorite
    HalalReaction.MASHAALLAH -> Icons.Filled.NightlightRound
    HalalReaction.SUBHANALLAH -> Icons.Filled.AutoAwesome
    HalalReaction.AMEEN -> Icons.Filled.FrontHand
    HalalReaction.LOVE -> Icons.Filled.VolunteerActivism
    HalalReaction.INSIGHTFUL -> Icons.Filled.Lightbulb
}

@Composable
fun ReactionPicker(selected: HalalReaction?, onPick: (HalalReaction) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(999.dp)).background(R2Cream).border(1.dp, R2Gold.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
        .padding(horizontal = 8.dp, vertical = 6.dp).testTag("reels2_reactions"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HalalReaction.entries.forEach { r ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(46.dp).clickable { onPick(r) }) {
                Box(Modifier.size(if (r == selected) 38.dp else 34.dp).clip(CircleShape).background(Color(r.color)), contentAlignment = Alignment.Center) {
                    Icon(reactionIcon(r), r.label, tint = if (r == HalalReaction.AMEEN) R2Cream else Color.White, modifier = Modifier.size(19.dp))
                }
                Text(r.label, color = R2Ink, fontSize = 8.5.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RecitationOverlay(post: UmmahPost, index: Int, modifier: Modifier) {
    val ayahs = splitAyahs(post.arabicText)
    if (ayahs.isEmpty()) return
    val i = index.coerceIn(0, ayahs.size - 1)
    Column(modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0x99062A1E))
        .border(1.dp, R2Gold.copy(alpha = 0.55f), RoundedCornerShape(22.dp)).padding(16.dp).testTag("reels2_recitation"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MenuBook, null, tint = R2Gold, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(5.dp))
            Text("Recitation mode · Ayah ${i + 1} of ${ayahs.size}", color = R2Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text(ayahs[i], color = Color.White, fontFamily = QuranArabicFont, fontSize = 24.sp, lineHeight = 42.sp, textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl))
        if (post.sourceReference.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(post.sourceReference, color = Color(0xCCFFFFFF), fontSize = 11.sp) }
    }
}

@Composable
private fun LiveCaptionStrip(line: String, lang: String, onLang: (String) -> Unit, aiLanguages: Boolean, modifier: Modifier) {
    Column(modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (line.isNotBlank()) Text(line, color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xAA000000)).padding(horizontal = 10.dp, vertical = 6.dp).testTag("reels2_caption_line"))
        if (!aiLanguages) return@Column
        Spacer(Modifier.height(6.dp))
        Row(Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x66000000)).padding(3.dp).testTag("reels2_cc_lang")) {
            reels2CaptionLanguages.forEach { l ->
                Text(l, color = if (l == lang) R2Green else Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (l == lang) R2Mint else Color.Transparent).clickable { onLang(l) }.padding(horizontal = 9.dp, vertical = 3.dp))
            }
            Text(" AI", color = R2Gold, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
        }
    }
}

// ================================================================ sheets (content only; hosts wrap them)

@Composable
fun Reels2SheetFrame(title: String, subtitle: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(stitchBackground())
        .navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(stitchLine()))
        Spacer(Modifier.height(12.dp))
        Text(title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (subtitle != null) Text(subtitle, color = stitchMutedText(), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable private fun accent() = if (isStitchLight()) R2Green else Color(0xFF7FD3AE)
@Composable private fun mint() = if (isStitchLight()) R2Mint else Color(0xFF173A2F)

@Composable
fun Reels2CommentsSheet(
    comments: List<Reels2Comment>, total: Long, creatorName: String, features: Reels2Features,
    draft: String = "", onDraft: (String) -> Unit = {}, onSend: () -> Unit = {}, onLikeComment: (UmmahComment) -> Unit = {},
    likedIds: Set<String> = emptySet(), recording: Boolean = false, onVoice: () -> Unit = {}, modifier: Modifier = Modifier,
) {
    Reels2SheetFrame("Comments", "${reelCount(total)} reflections · be kind, cite sources", modifier.testTag("reels2_comments")) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            if (comments.isEmpty()) Text("No comments yet. Start with a kind word.", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.padding(vertical = 18.dp))
            comments.forEach { c -> CommentRow(c, creatorName, features, c.comment.id in likedIds, onLikeComment) }
            if (features.voiceComments) VoiceCommentRow()
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(999.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(999.dp)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.text.BasicTextField(draft, onDraft, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = stitchText(), fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(accent()), modifier = Modifier.weight(1f),
                    decorationBox = { inner -> if (draft.isEmpty()) Text("Add a comment…", color = stitchMutedText(), fontSize = 14.sp); inner() })
                if (draft.isNotBlank()) Text("Post", color = accent(), fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onSend))
            }
            if (features.voiceComments) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(46.dp).clip(CircleShape).background(if (recording) Color(0xFFD9475A) else accent()).clickable(onClick = onVoice), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, "Voice comment", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(c: Reels2Comment, creatorName: String, f: Reels2Features, liked: Boolean, onLike: (UmmahComment) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(if (isStitchLight()) Color(0xFFF3E6C4) else Color(0xFF3A3220)), contentAlignment = Alignment.Center) {
            Text(c.comment.creatorName.take(1).uppercase(), color = if (isStitchLight()) Color(0xFF7A5A12) else StitchGold, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (c.pinned) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PushPin, null, tint = R2GoldDeep, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(3.dp))
                Text("Pinned by $creatorName", color = R2GoldDeep, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
            Text(c.comment.creatorName, color = stitchText(), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(c.comment.text, color = stitchText(), fontSize = 13.5.sp, lineHeight = 19.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reply", color = stitchMutedText(), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                if (f.creatorLiked && c.creatorLiked) {
                    Spacer(Modifier.width(12.dp))
                    Row(Modifier.clip(RoundedCornerShape(999.dp)).background(mint()).padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, null, tint = Color(0xFFD9475A), modifier = Modifier.size(11.dp)); Spacer(Modifier.width(3.dp))
                        Text("Liked by creator", color = accent(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onLike(c.comment) }) {
            Icon(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Like comment", tint = if (liked) Color(0xFFD9475A) else stitchMutedText(), modifier = Modifier.size(17.dp))
            Text(reelCount(c.comment.likeCount + if (liked) 1 else 0), color = stitchMutedText(), fontSize = 10.sp)
        }
    }
}

@Composable
private fun VoiceCommentRow() {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(18.dp)).background(mint()).padding(10.dp).testTag("reels2_voice"),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(accent()), contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, "Play voice comment", tint = Color.White) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Bilal · voice reflection", color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(6, 12, 18, 9, 14, 20, 11, 7, 16, 10, 5, 13, 19, 8, 12, 6, 15, 9, 4, 11).forEach { h ->
                    Box(Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(2.dp)).background(accent().copy(alpha = 0.8f)))
                }
            }
        }
        Text("0:14", color = stitchMutedText(), fontSize = 11.sp)
    }
}

data class Reels2Sound(val title: String, val artist: String, val reelsCount: Long, val fromAlNoor: Boolean)

@Composable
fun Reels2SoundPage(sound: Reels2Sound, reels: List<Pair<UmmahPost, ImageBitmap?>>, onUse: () -> Unit, onPreview: () -> Unit = {}, onOpen: (UmmahPost) -> Unit = {}, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().padding(16.dp).testTag("reels2_sound")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(R2Green, Color(0xFF0E8C73)))), contentAlignment = Alignment.Center) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF151515)).border(2.dp, R2Gold, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.GraphicEq, null, tint = R2Gold)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (sound.fromAlNoor) Text("AL-NOOR AUDIO", color = R2GoldDeep, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(sound.title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
                Text(sound.artist, color = stitchMutedText(), fontSize = 12.5.sp)
                Text("${reelCount(sound.reelsCount)} reels", color = stitchMutedText(), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(999.dp)).background(accent()).clickable(onClick = onUse), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Videocam, null, tint = Color.White); Spacer(Modifier.width(6.dp))
                Text("Use this sound", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(46.dp).clip(CircleShape).background(mint()).clickable(onClick = onPreview), contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, "Preview", tint = accent()) }
            Box(Modifier.size(46.dp).clip(CircleShape).background(mint()), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.BookmarkBorder, "Save sound", tint = accent()) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Reels using this sound", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        reels.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                row.forEach { (p, bmp) ->
                    Box(Modifier.weight(1f).aspectRatio(9f / 14f).clip(RoundedCornerShape(10.dp)).clickable { onOpen(p) }) {
                        Reels2Thumb(p, bmp, Modifier.fillMaxSize())
                        Row(Modifier.align(Alignment.BottomStart).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(reelCount(p.viewCount), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun Reels2LongPressMenu(
    onNotInterested: () -> Unit, onWhy: () -> Unit, onWatchLater: () -> Unit, onReport: (String) -> Unit,
    showReasons: Boolean = false, onShowReasons: () -> Unit = {}, modifier: Modifier = Modifier,
) {
    Reels2SheetFrame(if (showReasons) "Report to Halal Guard" else "Reel options", if (showReasons) "Reports go to Islamic-content reviewers. The creator isn't told who reported." else null,
        modifier.testTag("reels2_menu")) {
        if (!showReasons) {
            MenuRow(Icons.Outlined.VisibilityOff, "Not interested", "See fewer reels like this", onNotInterested)
            MenuRow(Icons.Outlined.Info, "Why am I seeing this?", "How For You picked this reel", onWhy)
            MenuRow(Icons.Outlined.WatchLater, "Watch later", "Save to your Watch later collection", onWatchLater)
            MenuRow(Icons.Outlined.Flag, "Report", "Halal Guard review", onShowReasons, danger = true)
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                UgcTerms.REPORT_REASONS.take(7).forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable { onReport(r) }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Shield, null, tint = accent(), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(12.dp))
                        Text(r, color = stitchText(), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = stitchMutedText())
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (danger) Color(0x22D9475A) else mint()), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (danger) Color(0xFFD9475A) else accent(), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) Color(0xFFD9475A) else stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            Text(sub, color = stitchMutedText(), fontSize = 12.sp)
        }
    }
}

@Composable
fun Reels2WhySheet(reasons: List<String>, onManage: () -> Unit = {}, modifier: Modifier = Modifier) {
    Reels2SheetFrame("Why you're seeing this", "Your For You is built on device from what you follow and watch.", modifier.testTag("reels2_why")) {
        reasons.forEach { r ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 6.dp).size(7.dp).clip(CircleShape).background(R2Gold)); Spacer(Modifier.width(10.dp))
                Text(r, color = stitchText(), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Manage interests", color = accent(), fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(mint()).clickable(onClick = onManage).padding(horizontal = 16.dp, vertical = 9.dp))
    }
}

data class Reels2AiAnswer(val summary: String, val reference: String, val referenceText: String, val simple: String, val loading: Boolean = false, val error: String? = null)

@Composable
fun Reels2AskAiSheet(answer: Reels2AiAnswer, simple: Boolean, onSimple: () -> Unit, keyPoints: List<ReelChapter> = emptyList(), modifier: Modifier = Modifier) {
    Reels2SheetFrame("Ask Noor AI about this reel", "Answers come from Noor AI on our server. Always check with a scholar.", modifier.testTag("reels2_ai")) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (answer.loading) Text("Noor AI is reading the reel…", color = stitchMutedText())
            answer.error?.let { Text(it, color = Color(0xFFD9475A), fontSize = 13.sp) }
            if (answer.summary.isNotBlank()) {
                AiLabel("Summary"); Text(answer.summary, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
            }
            if (answer.reference.isNotBlank()) {
                Spacer(Modifier.height(12.dp)); AiLabel("Referenced")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(mint()).border(1.dp, R2Gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(12.dp)) {
                    if (answer.referenceText.isNotBlank()) Text(answer.referenceText, color = stitchText(), fontFamily = QuranArabicFont, fontSize = 19.sp, lineHeight = 32.sp,
                        textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FactCheck, null, tint = accent(), modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp))
                        Text(answer.reference, color = accent(), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.clip(RoundedCornerShape(999.dp)).background(if (simple) accent() else mint()).clickable(onClick = onSimple).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChildCare, null, tint = if (simple) Color.White else accent(), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text("Explain simply", color = if (simple) Color.White else accent(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            if (simple && answer.simple.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(answer.simple, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp) }
            if (keyPoints.isNotEmpty()) {
                Spacer(Modifier.height(12.dp)); AiLabel("Key points")
                keyPoints.forEach { k -> Row(Modifier.padding(vertical = 3.dp)) {
                    Text(reelClock(k.startMs), color = R2GoldDeep, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                    Text(k.title, color = stitchText(), fontSize = 13.5.sp)
                } }
            }
        }
    }
}

@Composable
private fun AiLabel(t: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Icon(Icons.Default.AutoAwesome, null, tint = R2Gold, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp))
        Text(t.uppercase(), color = R2GoldDeep, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun Reels2VerifySheet(checks: List<com.noorpro.app.data.creator.RefCheck>, modifier: Modifier = Modifier) {
    Reels2SheetFrame("Verify reference", "Checked offline against the Qur'an and hadith bundled in NoorPro.", modifier.testTag("reels2_verify_sheet")) {
        checks.forEach { r ->
            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (r.ok) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, null, tint = if (r.ok) accent() else Color(0xFFD9475A), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text(r.raw, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
                Text(r.message, color = stitchMutedText(), fontSize = 12.5.sp)
                if (r.text.isNotBlank()) Text(r.text, color = stitchText(), fontSize = 13.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun Reels2CollectionSheet(collections: List<String>, selected: String?, onPick: (String) -> Unit, onNew: () -> Unit = {}, modifier: Modifier = Modifier) {
    Reels2SheetFrame("Save to collection", null, modifier.testTag("reels2_collections")) {
        (listOf("Watch later") + collections.filterNot { it.equals("Watch later", true) }).forEach { c ->
            Row(Modifier.fillMaxWidth().clickable { onPick(c) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(mint()), contentAlignment = Alignment.Center) {
                    Icon(if (c == "Watch later") Icons.Outlined.WatchLater else Icons.Outlined.Folder, null, tint = accent())
                }
                Spacer(Modifier.width(12.dp)); Text(c, color = stitchText(), fontSize = 14.5.sp, modifier = Modifier.weight(1f))
                if (c == selected) Icon(Icons.Filled.CheckCircle, null, tint = accent())
            }
        }
        Text("+ New collection", color = accent(), fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onNew).padding(vertical = 10.dp))
    }
}

// ================================================================ landscape player

@Composable
fun Reels2LandscapePlayer(
    ui: Reels2Ui, video: @Composable (Modifier) -> Unit, onBack: () -> Unit, onPlayPause: () -> Unit, playing: Boolean,
    onSpeed: () -> Unit, onCaptions: () -> Unit, onSeek: (Long) -> Unit, next: UmmahPost?, onNext: () -> Unit, modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Color.Black).testTag("reels2_landscape")) {
        video(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent, Color.Transparent, Color(0xAA000000)))))
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(Icons.Default.ArrowBack, "Exit full screen", onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(captionWithoutChapters(ui.post.caption).lineSequence().firstOrNull().orEmpty(), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ui.post.creatorName, color = Color(0xCCFFFFFF), fontSize = 12.sp)
            }
            Text(speedLabel(ui.speed), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Glass).clickable(onClick = onSpeed).padding(horizontal = 12.dp, vertical = 7.dp))
            Spacer(Modifier.width(8.dp))
            GlassIcon(if (ui.captionsOn) Icons.Filled.ClosedCaption else Icons.Outlined.ClosedCaptionOff, "Captions", onCaptions)
        }
        Box(Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(GlassDark).clickable(onClick = onPlayPause), contentAlignment = Alignment.Center) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Reels2Scrubber(ui.positionMs, ui.durationMs, ui.chapters, onSeek, showLabels = true)
            Spacer(Modifier.height(6.dp))
            Text("${reelClock(ui.positionMs)} / ${reelClock(ui.durationMs)}", color = Color.White, fontSize = 12.sp)
        }
        if (next != null) Row(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).clip(RoundedCornerShape(16.dp)).background(GlassDark)
            .border(1.dp, R2Gold.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).clickable(onClick = onNext).padding(horizontal = 12.dp, vertical = 9.dp).testTag("reels2_next_video"),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.KeyboardArrowUp, null, tint = R2Gold)
            Column { Text("Swipe up · Next video", color = R2Gold, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                Text(next.caption.lineSequence().firstOrNull().orEmpty(), color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp)) }
        }
    }
}

// ================================================================ extras

@Composable
fun Reels2NextPartCard(next: UmmahPost, series: ReelSeriesInfo, thumb: ImageBitmap?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xE6062A1E)).border(1.dp, R2Gold.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
        .clickable(onClick = onOpen).padding(8.dp).testTag("reels2_next_part"), verticalAlignment = Alignment.CenterVertically) {
        Reels2Thumb(next, thumb, Modifier.size(width = 46.dp, height = 62.dp).clip(RoundedCornerShape(10.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.widthIn(max = 190.dp)) {
            Text("NEXT PART · ${series.part + 1}${series.total?.let { "/$it" } ?: ""}", color = R2Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Text(series.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Plays next · tap to jump", color = Color(0xCCFFFFFF), fontSize = 11.sp)
        }
        Spacer(Modifier.width(6.dp)); Icon(Icons.Default.SkipNext, null, tint = Color.White)
    }
}

@Composable
fun Reels2MiniPlayer(title: String, creator: String, thumb: ImageBitmap?, playing: Boolean, onToggle: () -> Unit, onClose: () -> Unit, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.width(260.dp).clip(RoundedCornerShape(18.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp))
        .clickable(onClick = onExpand).padding(6.dp).testTag("reels2_mini"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 54.dp, height = 72.dp).clip(RoundedCornerShape(12.dp))) { Reels2Bitmap(thumb, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(creator, color = stitchMutedText(), fontSize = 11.sp)
        }
        Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/pause", tint = accent(), modifier = Modifier.size(30.dp).clickable(onClick = onToggle))
        Icon(Icons.Default.Close, "Close", tint = stitchMutedText(), modifier = Modifier.size(24.dp).clickable(onClick = onClose))
    }
}

@Composable
fun Reels2LiveCard(title: String, host: String, whenText: String, onRemind: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(R2Green, Color(0xFF0E8C73))))
        .padding(14.dp).testTag("reels2_live"), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFD9475A)).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White)); Spacer(Modifier.width(4.dp))
                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp)); Text("Coming soon · $whenText", color = R2Cream, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(host, color = Color(0xCCFFFFFF), fontSize = 12.sp)
        }
        Text("Remind me", color = R2Green, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(R2Gold).clickable(onClick = onRemind).padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
fun Reels2ContinueRow(items: List<Triple<UmmahPost, WatchProgress, ImageBitmap?>>, onOpen: (UmmahPost) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.testTag("reels2_continue")) {
        Text("Continue watching", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            items.forEach { (p, w, bmp) ->
                Column(Modifier.width(150.dp).clickable { onOpen(p) }) {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(14.dp))) {
                        Reels2Thumb(p, bmp, Modifier.fillMaxSize())
                        Text(reelClock(w.durationMs - w.positionMs) + " left", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x99000000)).padding(horizontal = 5.dp, vertical = 2.dp))
                        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x55FFFFFF)))
                        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(w.fraction).height(4.dp).background(R2Gold))
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(captionWithoutChapters(p.caption).lineSequence().firstOrNull().orEmpty(), color = stitchText(), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(p.creatorName, color = stitchMutedText(), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun Reels2SmartSearch(text: String, onText: (String) -> Unit, parsed: SmartReelQuery?, onSubmit: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(modifier.testTag("reels2_smart_search")) {
        Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(999.dp)).background(stitchSurface()).border(1.5.dp, R2Gold.copy(alpha = 0.6f), RoundedCornerShape(999.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = R2GoldDeep, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.text.BasicTextField(text, onText, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = stitchText(), fontSize = 14.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(accent()), modifier = Modifier.weight(1f),
                decorationBox = { inner -> if (text.isEmpty()) Text("Find reels about patience in Urdu…", color = stitchMutedText(), fontSize = 14.sp); inner() })
            Icon(Icons.Default.Search, "Search", tint = accent(), modifier = Modifier.clickable(onClick = onSubmit))
        }
        if (parsed != null && parsed.topic.isNotBlank()) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SearchTag("Topic: ${parsed.topic}")
            parsed.language?.let { SearchTag("Language: ${it.uppercase()}") }
            SearchTag("Reels")
        }
    }
}

@Composable private fun SearchTag(t: String) = Text(t, color = accent(), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(mint()).padding(horizontal = 10.dp, vertical = 5.dp))
