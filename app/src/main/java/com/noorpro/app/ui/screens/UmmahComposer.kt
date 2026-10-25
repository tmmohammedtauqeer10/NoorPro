package com.noorpro.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.ComposerDraft
import com.noorpro.app.data.ComposerFlags
import com.noorpro.app.data.ComposerLimits
import com.noorpro.app.data.ComposerPoll
import com.noorpro.app.data.ComposerTagSource
import com.noorpro.app.data.ComposerText
import com.noorpro.app.data.composerBlocker
import com.noorpro.app.data.composerTopics
import com.noorpro.app.data.formatClock
import com.noorpro.app.data.search.PeopleRanker
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon

/*
 * 1.1.1 — NoorPro composer. One screen for Ummah feed posts, reels and stories.
 * Stateless (NoorComposer) so it renders in tests; NoorCreateScreen wires pickers + upload.
 */

/** Which optional, backend-dependent controls are shown (Remote Config; see ComposerFlags). */
data class ComposerFeatures(
    val polls: Boolean = ComposerFlags.polls,
    val visibility: Boolean = ComposerFlags.visibility,
    val location: Boolean = ComposerFlags.location,
    val aiLabel: Boolean = ComposerFlags.aiLabel
)

@Stable
class ComposerUiState(destination: ComposerDestination) {
    var destination by mutableStateOf(destination)
    var text by mutableStateOf(TextFieldValue(""))
    var topic by mutableStateOf("Reflections")
    var photos by mutableStateOf<List<Uri>>(emptyList())
    var video by mutableStateOf<Uri?>(null)
    var videoDurationMs by mutableStateOf(0L)
    var trimStartMs by mutableStateOf(0L)
    var trimEndMs by mutableStateOf(0L)
    var coverMs by mutableStateOf(0L)
    var coverTouched by mutableStateOf(false)
    var coverFrame by mutableStateOf<Bitmap?>(null)
    /** 1.1.3 editor: edited photo uri -> (original uri, edit) so re-editing starts from the original. */
    var photoOrigins by mutableStateOf<Map<Uri, Pair<Uri, com.noorpro.app.data.media.edit.PhotoEdit>>>(emptyMap())
    /** 1.1.3 editor: original clip when [video] is an exported edit; the edit that produced it. */
    var reelSource by mutableStateOf<Uri?>(null)
    var reelEdit by mutableStateOf<com.noorpro.app.data.media.edit.ReelEdit?>(null)
    var verseOpen by mutableStateOf(false)
    var arabic by mutableStateOf("")
    var reference by mutableStateOf("")
    var credit by mutableStateOf("")
    var pollOpen by mutableStateOf(false)
    var poll by mutableStateOf(ComposerPoll())
    var aiLabel by mutableStateOf(false)
    var location by mutableStateOf("")
    var followersOnly by mutableStateOf(false)
    var draftId by mutableStateOf<String?>(null)
    var draftsOpen by mutableStateOf(false)
    var submitting by mutableStateOf(false)
    var progress by mutableStateOf(-1f)
    var stage by mutableStateOf("")
    var error by mutableStateOf<String?>(null)

    val hasContent get() = text.text.isNotBlank() || arabic.isNotBlank() || photos.isNotEmpty() || video != null ||
        poll.cleanOptions.isNotEmpty() || location.isNotBlank()

    fun blocker(features: ComposerFeatures) = composerBlocker(
        destination, text.text, if (verseOpen) arabic else "", photos.size, video != null,
        pollOpen, poll, trimStartMs, trimEndMs, pollsEnabled = features.polls
    )

    fun toDraft(id: String) = ComposerDraft(
        id = id, destination = destination, text = text.text, topic = topic,
        arabicText = if (verseOpen) arabic else "", reference = if (verseOpen) reference else "",
        media = if (video != null) listOf(video.toString()) else photos.map { it.toString() }, isVideo = video != null,
        pollOptions = if (pollOpen) poll.options else emptyList(), pollHours = poll.durationHours,
        aiLabel = aiLabel, location = location, followersOnly = followersOnly,
        trimStartMs = trimStartMs, trimEndMs = trimEndMs, coverMs = coverMs,
        editJson = com.noorpro.app.data.media.edit.EditCodec.draft(com.noorpro.app.data.media.edit.EditCodec.DraftEdits(
            photos = photoOrigins.filterKeys { it in photos }.entries.associate { (k, v) -> k.toString() to (v.first.toString() to v.second) },
            reelSource = if (video != null) reelSource?.toString() else null,
            reel = if (video != null) reelEdit else null
        ))
    )

    fun load(d: ComposerDraft) {
        draftId = d.id; destination = d.destination
        text = TextFieldValue(d.text, TextRange(d.text.length)); topic = d.topic
        arabic = d.arabicText; reference = d.reference; verseOpen = d.arabicText.isNotBlank() || d.reference.isNotBlank()
        if (d.isVideo) { video = d.media.firstOrNull()?.let(Uri::parse); photos = emptyList() }
        else { photos = d.media.map(Uri::parse); video = null }
        pollOpen = d.pollOptions.isNotEmpty(); poll = ComposerPoll(d.pollOptions.ifEmpty { listOf("", "") }, d.pollHours)
        aiLabel = d.aiLabel; location = d.location; followersOnly = d.followersOnly
        trimStartMs = d.trimStartMs; trimEndMs = d.trimEndMs; coverMs = d.coverMs; coverTouched = d.coverMs > 0
        val edits = com.noorpro.app.data.media.edit.EditCodec.draft(d.editJson)
        photoOrigins = edits.photos.entries.associate { (k, v) -> Uri.parse(k) to (Uri.parse(v.first) to v.second) }.filterKeys { it in photos }
        reelSource = edits.reelSource?.let(Uri::parse); reelEdit = edits.reel?.takeIf { video != null }
        error = null
    }
}

class ComposerActions(
    val onBack: () -> Unit = {},
    val onPickPhotos: () -> Unit = {},
    val onPickVideo: () -> Unit = {},
    val onRecordVideo: () -> Unit = {},
    val onSaveDraft: () -> Unit = {},
    val onOpenDraft: (ComposerDraft) -> Unit = {},
    val onDeleteDraft: (ComposerDraft) -> Unit = {},
    val onPost: () -> Unit = {},
    /** 1.1.3: open the photo editor for photo #index / the reel editor for the video. */
    val onEditPhoto: (Int) -> Unit = {},
    val onEditVideo: () -> Unit = {},
    /** 1.1.3 editor pro: lay 2–6 photos out as one collage. */
    val onCollage: () -> Unit = {}
)

/** Render-test hook: Uri string -> bitmap (tests can't load content:// images). */
internal object ComposerPreview {
    @Volatile var images: Map<String, Bitmap> = emptyMap()
}

@Composable
fun NoorComposer(
    state: ComposerUiState,
    actions: ComposerActions,
    userName: String,
    userPhotoUrl: String,
    people: List<PeopleRanker.Person> = emptyList(),
    drafts: List<ComposerDraft> = emptyList(),
    features: ComposerFeatures = ComposerFeatures(),
    modifier: Modifier = Modifier
) {
    val blocker = state.blocker(features)
    Box(modifier.fillMaxSize().background(stitchBackground()).testTag("composer")) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            ComposerTopBar(state, actions, drafts.size, canPost = blocker == null && !state.submitting)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp).testTag("composer_scroll"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DestinationPicker(state.destination) { d ->
                    state.destination = d
                    if (d != ComposerDestination.FEED) state.pollOpen = false
                    if (d == ComposerDestination.REEL) state.photos = emptyList()
                    if (d == ComposerDestination.STORY && state.photos.size > 1) state.photos = state.photos.take(1)
                }
                ComposerEditorCard(state, userName, userPhotoUrl, people, features)
                ComposerMediaSection(state, actions)
                if (state.verseOpen) VerseBlock(state)
                if (state.pollOpen && features.polls && state.destination == ComposerDestination.FEED) PollEditor(state)
                if (state.destination != ComposerDestination.STORY) TopicPicker(state)
                ComposerSettings(state, features)
                val err = state.error
                if (err != null) Text(err, color = Color(0xFFBA1A1A), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
                else if (blocker != null && state.hasContent) Text(blocker, color = stitchMutedText(), fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 20.dp))
            }
            ComposerToolbar(state, actions, features)
        }
        if (state.submitting) ComposerProgress(state, Modifier.align(Alignment.BottomCenter))
        if (state.draftsOpen) DraftsPanel(drafts, state, actions)
    }
}

@Composable
private fun ComposerTopBar(state: ComposerUiState, actions: ComposerActions, draftCount: Int, canPost: Boolean) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = actions.onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText())
        }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                when (state.destination) { ComposerDestination.FEED -> "New post"; ComposerDestination.REEL -> "New reel"; ComposerDestination.STORY -> "New story" },
                color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 23.sp
            )
            Text(if (state.draftId != null) "Editing a draft" else "Share something beneficial", color = stitchMutedText(), fontSize = 12.sp)
        }
        Box(
            Modifier.size(42.dp).clip(CircleShape).clickable { state.draftsOpen = true }.testTag("composer_drafts"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.History, "Drafts", tint = stitchText())
            if (draftCount > 0) Text(
                "$draftCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 4.dp).clip(CircleShape).background(StitchGold)
                    .padding(horizontal = 4.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        val bg by animateColorAsState(if (canPost) feedAccentStrong() else stitchLine(), label = "postBg")
        Text(
            when (state.destination) { ComposerDestination.FEED -> "Post"; ComposerDestination.REEL -> "Share reel"; ComposerDestination.STORY -> "Share" },
            color = if (canPost) Color.White else stitchMutedText(), fontWeight = FontWeight.Bold, fontSize = 14.sp,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg)
                .clickable(enabled = canPost, onClick = actions.onPost).padding(horizontal = 18.dp, vertical = 10.dp).testTag("composer_post")
        )
    }
}

@Composable private fun feedAccentStrong(): Color = if (isStitchLight()) StitchEmerald else Color(0xFF1F7A5A)

@Composable
private fun DestinationPicker(selected: ComposerDestination, onSelect: (ComposerDestination) -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(feedMint()).padding(4.dp)
    ) {
        ComposerDestination.entries.forEach { d ->
            val on = d == selected
            val bg by animateColorAsState(if (on) stitchSurface() else Color.Transparent, label = "destBg")
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(bg).clickable { onSelect(d) }
                    .padding(vertical = 9.dp).testTag("dest_${d.name}"),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (d) { ComposerDestination.FEED -> Icons.Default.DynamicFeed; ComposerDestination.REEL -> Icons.Default.Movie; ComposerDestination.STORY -> Icons.Default.AddCircle },
                    null, tint = if (on) feedAccent() else stitchMutedText(), modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(d.label, color = if (on) feedAccent() else stitchMutedText(), fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun composerCard(): Modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
    .background(stitchSurface()).border(1.dp, stitchLine().copy(alpha = if (isStitchLight()) 0.6f else 0.35f), RoundedCornerShape(22.dp))

@Composable
private fun ComposerEditorCard(
    state: ComposerUiState, userName: String, userPhotoUrl: String,
    people: List<PeopleRanker.Person>, features: ComposerFeatures
) {
    val limit = ComposerLimits.chars(state.destination)
    Column(composerCard().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UmmahAvatar(userName, userPhotoUrl, 40.dp, ring = StitchGold)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(userName.ifBlank { "You" }, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when (state.destination) { ComposerDestination.FEED -> "Posting to the Ummah feed"; ComposerDestination.REEL -> "Posting a reel"; ComposerDestination.STORY -> "Story · visible for 24 hours" },
                    color = stitchMutedText(), fontSize = 12.sp
                )
            }
            if (features.visibility && state.destination != ComposerDestination.STORY) AudienceChip(state)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().heightIn(min = 130.dp)) {
            BasicTextField(
                value = state.text,
                onValueChange = { v -> state.text = if (v.text.length > limit + 200) v.copy(text = v.text.take(limit + 200)) else v; state.error = null },
                textStyle = TextStyle(color = stitchText(), fontSize = 18.sp, lineHeight = 27.sp),
                cursorBrush = SolidColor(feedAccent()),
                visualTransformation = rememberTagHighlighter(feedAccent()),
                modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp).testTag("composer_text")
            )
            if (state.text.text.isEmpty()) Text(
                when (state.destination) {
                    ComposerDestination.FEED -> "What would you like to share with the Ummah?"
                    ComposerDestination.REEL -> "Write a caption… add #tags and @mentions"
                    ComposerDestination.STORY -> "Add a short caption (optional)"
                },
                color = stitchMutedText().copy(alpha = 0.8f), fontSize = 18.sp, lineHeight = 27.sp
            )
        }
        ComposerSuggestions(state, people)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("#tags · @mentions", color = stitchMutedText().copy(alpha = 0.75f), fontSize = 11.5.sp, modifier = Modifier.weight(1f))
            CharCounter(state.text.text.length, limit)
        }
    }
}

@Composable
private fun AudienceChip(state: ComposerUiState) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, feedAccent().copy(alpha = 0.6f), RoundedCornerShape(999.dp))
            .clickable { state.followersOnly = !state.followersOnly }.padding(horizontal = 10.dp, vertical = 6.dp).testTag("composer_audience"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (state.followersOnly) Icons.Default.Group else Icons.Default.Public, null, tint = feedAccent(), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (state.followersOnly) "Followers" else "Everyone", color = feedAccent(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Default.ExpandMore, null, tint = feedAccent(), modifier = Modifier.size(14.dp))
    }
}

@Composable
internal fun CharCounter(count: Int, limit: Int) {
    val left = limit - count
    val color = when { left < 0 -> Color(0xFFD9475A); left <= limit / 10 -> feedGold(); else -> stitchMutedText() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(
                progress = { (count.toFloat() / limit).coerceIn(0f, 1f) }, color = color, trackColor = stitchLine().copy(alpha = 0.5f),
                strokeWidth = 2.dp, modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("$count/$limit", color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("composer_counter"))
    }
}

@Composable
private fun ComposerSuggestions(state: ComposerUiState, people: List<PeopleRanker.Person>) {
    val token = ComposerText.activeToken(state.text.text, state.text.selection.end) ?: return
    fun apply(completion: String) {
        val (t, c) = ComposerText.complete(state.text.text, token, completion)
        state.text = TextFieldValue(t, TextRange(c))
    }
    if (token.symbol == '#') {
        val tags = ComposerText.hashtagSuggestions(token.query, ComposerTagSource.captions)
        if (tags.isEmpty()) return
        Row(Modifier.padding(vertical = 8.dp).horizontalScroll(rememberScrollState()).testTag("composer_tag_suggestions"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag ->
                Text(tag, color = feedAccent(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(feedMint()).clickable { apply(tag) }.padding(horizontal = 12.dp, vertical = 7.dp))
            }
        }
    } else {
        val found = (if (token.query.isEmpty()) people.sortedByDescending { it.relation } else PeopleRanker.search(token.query, people)).take(4)
        if (found.isEmpty()) return
        Column(Modifier.padding(vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSoftSurface()).testTag("composer_mention_suggestions")) {
            found.forEach { p ->
                Row(Modifier.fillMaxWidth().clickable { apply("@" + p.handle.removePrefix("@")) }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    UmmahAvatar(p.name.ifBlank { p.handle }, p.photoUrl, 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name.ifBlank { p.handle }, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("@" + p.handle.removePrefix("@") + (if (p.subtitle.isNotBlank()) " · ${p.subtitle}" else ""), color = stitchMutedText(), fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumb(uri: Uri, modifier: Modifier) {
    val preview = ComposerPreview.images[uri.toString()]
    if (preview != null) Image(preview.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = modifier)
    else AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}

@Composable
private fun ComposerMediaSection(state: ComposerUiState, actions: ComposerActions) {
    val video = state.video
    when {
        state.photos.isNotEmpty() && video == null -> PhotoStrip(state, actions)
        video != null -> VideoEditor(state, actions)
        state.destination == ComposerDestination.REEL || state.destination == ComposerDestination.STORY -> MediaEmptyCard(state, actions)
        else -> Unit
    }
}

@Composable
private fun PhotoStrip(state: ComposerUiState, actions: ComposerActions) {
    val max = if (state.destination == ComposerDestination.STORY) 1 else ComposerLimits.MAX_PHOTOS
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()).testTag("composer_photos"), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            state.photos.forEachIndexed { i, uri ->
                Box(Modifier.size(if (state.photos.size == 1) 220.dp else 150.dp).clip(RoundedCornerShape(18.dp)).background(stitchSoftSurface())) {
                    MediaThumb(uri, Modifier.fillMaxSize())
                    Text("${i + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 7.dp, vertical = 2.dp))
                    RoundIcon(Icons.Default.Close, "Remove photo", Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                        state.photos = state.photos - uri; state.photoOrigins = state.photoOrigins - uri
                    }
                    EditBadge(uri in state.photoOrigins, Modifier.align(Alignment.BottomEnd).padding(6.dp).testTag("composer_edit_photo_$i")) { actions.onEditPhoto(i) }
                    if (i > 0) RoundIcon(Icons.Default.ChevronLeft, "Move earlier", Modifier.align(Alignment.BottomStart).padding(6.dp)) {
                        state.photos = state.photos.toMutableList().apply { add(i - 1, removeAt(i)) }
                    }
                    if (i == 0 && state.photos.size > 1) Text("Cover", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(999.dp)).background(StitchEmerald.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            if (state.photos.size < max) Box(
                Modifier.size(if (state.photos.size == 1) 220.dp else 150.dp).clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, feedAccent().copy(alpha = 0.45f), RoundedCornerShape(18.dp)).clickable(onClick = actions.onPickPhotos),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, null, tint = feedAccent())
                    Text("Add", color = feedAccent(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${state.photos.size}/$max photo${if (max == 1) "" else "s"}" + if (max > 1) " · use ‹ to reorder, first photo is the cover" else "",
                color = stitchMutedText(), fontSize = 11.5.sp, modifier = Modifier.weight(1f))
            if (state.photos.size >= 2) Row(
                Modifier.clip(RoundedCornerShape(999.dp)).background(feedMint()).border(1.dp, StitchGold.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                    .clickable(onClick = actions.onCollage).padding(horizontal = 10.dp, vertical = 6.dp).testTag("composer_collage"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Dashboard, null, tint = feedAccent(), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Layout", color = feedAccent(), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EditBadge(edited: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(999.dp)).background(if (edited) StitchGold else Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoAwesome, null, tint = if (edited) StitchEmeraldDeep else Color.White, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (edited) "Edited" else "Edit", color = if (edited) StitchEmeraldDeep else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, desc: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun MediaEmptyCard(state: ComposerUiState, actions: ComposerActions) {
    val reel = state.destination == ComposerDestination.REEL
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(feedMint())
            .border(BorderStroke(1.dp, StitchGold.copy(alpha = 0.5f)), RoundedCornerShape(22.dp)).padding(vertical = 26.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(if (reel) Icons.Default.Movie else Icons.Default.AddCircle, null, tint = feedAccent(), modifier = Modifier.size(34.dp))
        Text(if (reel) "Start your reel" else "Add to your story", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text(if (reel) "Pick a vertical video or record one. Edit it and choose a cover next." else "A photo or a short video, shared for 24 hours.",
            color = stitchMutedText(), fontSize = 13.sp, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(Icons.Default.Videocam, "Record", filled = true, onClick = actions.onRecordVideo)
            PillButton(Icons.Default.Movie, "Gallery", filled = false, onClick = actions.onPickVideo)
            if (!reel) PillButton(Icons.Default.Image, "Photo", filled = false, onClick = actions.onPickPhotos)
        }
    }
}

@Composable
private fun PillButton(icon: ImageVector, label: String, filled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(if (filled) feedAccentStrong() else stitchSurface())
            .border(1.dp, if (filled) Color.Transparent else feedAccent().copy(alpha = 0.5f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (filled) Color.White else feedAccent(), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (filled) Color.White else feedAccent(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VideoEditor(state: ComposerUiState, actions: ComposerActions) {
    val reel = state.destination == ComposerDestination.REEL
    val dur = state.videoDurationMs
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.then(if (reel) Modifier.width(150.dp).aspectRatio(9f / 16f) else Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                    .clip(RoundedCornerShape(18.dp)).background(StitchEmeraldDeep).testTag("composer_video")
            ) {
                state.coverFrame?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                Box(Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                }
                if (dur > 0) Text(formatClock(clipLength(state)), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 7.dp, vertical = 2.dp))
                if (!reel) EditBadge(state.reelEdit != null, Modifier.align(Alignment.BottomStart).padding(6.dp).testTag("composer_edit_video")) { actions.onEditVideo() }
                RoundIcon(Icons.Default.Close, "Remove video", Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                    state.reelSource = null; state.reelEdit = null
                    state.video = null; state.coverFrame = null; state.videoDurationMs = 0; state.trimStartMs = 0; state.trimEndMs = 0; state.coverMs = 0; state.coverTouched = false
                }
            }
            if (reel) Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Your reel")
                Text(if (state.reelEdit != null) "Edited · ${formatClock(state.videoDurationMs)}. Open the editor again any time — it starts from your original clip."
                    else "Vertical 9:16 looks best. Edit it — trim, speed, filters, text, stickers, sound — then pick a cover.", color = stitchMutedText(), fontSize = 12.5.sp, lineHeight = 18.sp)
                Box(Modifier.testTag("composer_edit_video")) { PillButton(Icons.Default.AutoAwesome, if (state.reelEdit != null) "Edit again" else "Edit reel", filled = true, onClick = actions.onEditVideo) }
                PillButton(Icons.Default.Movie, "Replace", filled = false, onClick = actions.onPickVideo)
                PillButton(Icons.Default.Videocam, "Re-record", filled = false, onClick = actions.onRecordVideo)
            }
        }
        if (reel && dur > 1000 && state.reelSource == null) {
            Column(composerCard().padding(horizontal = 16.dp, vertical = 12.dp).testTag("composer_trim")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Trim", Modifier.weight(1f))
                    Text("${formatClock(state.trimStartMs)} – ${formatClock(state.trimEndMs.takeIf { it > 0 } ?: dur)} · ${formatClock(clipLength(state))}",
                        color = feedAccent(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                RangeSlider(
                    value = state.trimStartMs.toFloat()..(state.trimEndMs.takeIf { it > 0 } ?: dur).toFloat(),
                    onValueChange = { r -> state.trimStartMs = r.start.toLong(); state.trimEndMs = r.endInclusive.toLong() },
                    valueRange = 0f..dur.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = StitchGold, activeTrackColor = feedAccent(), inactiveTrackColor = stitchLine())
                )
                SectionLabel("Cover")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 36.dp, height = 56.dp).clip(RoundedCornerShape(8.dp)).background(StitchEmeraldDeep)
                        .border(1.5.dp, StitchGold, RoundedCornerShape(8.dp))) {
                        state.coverFrame?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Slider(
                        value = state.coverMs.toFloat().coerceIn(0f, dur.toFloat()),
                        onValueChange = { state.coverMs = it.toLong(); state.coverTouched = true },
                        valueRange = 0f..dur.toFloat(), modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = StitchGold, activeTrackColor = feedAccent(), inactiveTrackColor = stitchLine())
                    )
                }
                Text(if (state.coverTouched) "Cover at ${formatClock(state.coverMs)}" else "Slide to choose the frame people see first",
                    color = stitchMutedText(), fontSize = 11.5.sp)
            }
        }
    }
}

private fun clipLength(state: ComposerUiState): Long =
    ((state.trimEndMs.takeIf { it > 0 } ?: state.videoDurationMs) - state.trimStartMs).coerceAtLeast(0)

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = feedGold(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = modifier)
}

@Composable
private fun ComposerField(value: String, onChange: (String) -> Unit, placeholder: String, tag: String, rtl: Boolean = false, big: Boolean = false, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(stitchSurface().copy(alpha = if (isStitchLight()) 0.75f else 0.5f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        val style = TextStyle(color = stitchText(), fontSize = if (big) 21.sp else 14.sp, lineHeight = if (big) 34.sp else 20.sp,
            textDirection = if (rtl) TextDirection.Rtl else TextDirection.Content, textAlign = if (rtl) TextAlign.Right else TextAlign.Start)
        BasicTextField(value, onChange, textStyle = style, cursorBrush = SolidColor(feedAccent()), modifier = Modifier.fillMaxWidth().testTag(tag))
        if (value.isEmpty()) Text(placeholder, style = style.copy(color = stitchMutedText().copy(alpha = 0.75f)), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VerseBlock(state: ComposerUiState) {
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(feedMint())
            .border(1.dp, StitchGold.copy(alpha = 0.45f), RoundedCornerShape(22.dp)).padding(14.dp).testTag("composer_verse"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MenuBook, null, tint = feedGold(), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            SectionLabel("Verse, hadith or dua", Modifier.weight(1f))
            Icon(Icons.Default.Close, "Remove verse", tint = stitchMutedText(), modifier = Modifier.size(18.dp).clickable { state.verseOpen = false; state.arabic = ""; state.reference = "" })
        }
        ComposerField(state.arabic, { state.arabic = it.take(2000) }, "اكتب الآية أو الدعاء هنا", "composer_arabic", rtl = true, big = true)
        ComposerField(state.reference, { state.reference = it.take(120) }, "Reference, e.g. Al-Baqarah 2:286", "composer_reference")
    }
}

@Composable
private fun PollEditor(state: ComposerUiState) {
    Column(composerCard().padding(14.dp).testTag("composer_poll"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Poll, null, tint = feedGold(), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            SectionLabel("Poll", Modifier.weight(1f))
            Icon(Icons.Default.Close, "Remove poll", tint = stitchMutedText(), modifier = Modifier.size(18.dp).clickable { state.pollOpen = false; state.poll = ComposerPoll() })
        }
        state.poll.options.forEachIndexed { i, opt ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).border(1.dp, stitchLine(), RoundedCornerShape(14.dp))) {
                    ComposerField(opt, { v -> state.poll = state.poll.copy(options = state.poll.options.toMutableList().also { it[i] = v.take(ComposerLimits.POLL_OPTION_CHARS) }) },
                        "Option ${i + 1}", "poll_option_$i")
                }
                if (i >= ComposerLimits.POLL_MIN) Icon(Icons.Default.Close, "Remove option", tint = stitchMutedText(),
                    modifier = Modifier.padding(start = 6.dp).size(18.dp).clickable { state.poll = state.poll.copy(options = state.poll.options.filterIndexed { j, _ -> j != i }) })
            }
        }
        if (state.poll.options.size < ComposerLimits.POLL_MAX) Text("+ Add option", color = feedAccent(), fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { state.poll = state.poll.copy(options = state.poll.options + "") }.padding(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ends in", color = stitchMutedText(), fontSize = 12.sp)
            listOf(24 to "1 day", 72 to "3 days", 168 to "1 week").forEach { (h, l) -> SmallChip(l, state.poll.durationHours == h) { state.poll = state.poll.copy(durationHours = h) } }
        }
    }
}

@Composable
private fun SmallChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) feedAccentStrong() else Color.Transparent, label = "chip")
    Text(label, color = if (selected) Color.White else stitchText(), fontSize = 12.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg)
            .border(1.dp, if (selected) Color.Transparent else stitchLine(), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp))
}

@Composable
private fun TopicPicker(state: ComposerUiState) {
    Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Topic")
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(end = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            composerTopics.forEach { t -> SmallChip(t, state.topic == t) { state.topic = t } }
        }
    }
}

@Composable
private fun ComposerSettings(state: ComposerUiState, features: ComposerFeatures) {
    val reel = state.destination == ComposerDestination.REEL
    val story = state.destination == ComposerDestination.STORY
    val showAi = features.aiLabel && !story
    val showLoc = features.location && !story
    if (!reel && !showAi && !showLoc) return
    Column(composerCard().animateContentSize()) {
        if (reel) SettingRow(Icons.Default.MusicNote, "Sound / recitation credit") {
            ComposerField(state.credit, { state.credit = it.take(80) }, "e.g. Recitation: Sheikh …  (optional)", "composer_credit", modifier = Modifier.padding(top = 6.dp))
        }
        if (showLoc) SettingRow(Icons.Default.Place, "Location") {
            ComposerField(state.location, { state.location = it.take(80) }, "Add a place (optional)", "composer_location", modifier = Modifier.padding(top = 6.dp))
        }
        if (showAi) Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = feedAccent(), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("AI-assisted label", color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Tell viewers that AI helped make this", color = stitchMutedText(), fontSize = 12.sp)
            }
            Switch(state.aiLabel, { state.aiLabel = it }, modifier = Modifier.testTag("composer_ai"),
                colors = SwitchDefaults.colors(checkedTrackColor = feedAccentStrong(), checkedThumbColor = Color.White))
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Icon(icon, null, tint = feedAccent(), modifier = Modifier.padding(top = 2.dp).size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ComposerToolbar(state: ComposerUiState, actions: ComposerActions, features: ComposerFeatures) {
    val feed = state.destination == ComposerDestination.FEED
    Column(Modifier.fillMaxWidth().background(stitchSurface()).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(stitchLine().copy(alpha = 0.6f)))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).testTag("composer_toolbar"), verticalAlignment = Alignment.CenterVertically) {
            if (state.destination != ComposerDestination.REEL) ToolIcon(Icons.Default.Image, "Photos", on = state.photos.isNotEmpty(), onClick = actions.onPickPhotos)
            ToolIcon(Icons.Default.Videocam, "Video", on = state.video != null, onClick = actions.onPickVideo)
            if (feed && features.polls) ToolIcon(Icons.Default.Poll, "Poll", on = state.pollOpen) {
                state.pollOpen = !state.pollOpen; if (!state.pollOpen) state.poll = ComposerPoll()
            }
            ToolIcon(Icons.Default.Tag, "Hashtag", on = false) {
                val (t, c) = ComposerText.insertSymbol(state.text.text, state.text.selection.end, '#'); state.text = TextFieldValue(t, TextRange(c))
            }
            ToolIcon(Icons.Default.AlternateEmail, "Mention", on = false) {
                val (t, c) = ComposerText.insertSymbol(state.text.text, state.text.selection.end, '@'); state.text = TextFieldValue(t, TextRange(c))
            }
            if (state.destination != ComposerDestination.STORY) ToolIcon(Icons.Default.MenuBook, "Verse", on = state.verseOpen) { state.verseOpen = !state.verseOpen }
            Spacer(Modifier.weight(1f))
            Text("Save draft", color = feedAccent(), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(enabled = state.hasContent, onClick = actions.onSaveDraft)
                    .padding(horizontal = 12.dp, vertical = 9.dp).testTag("composer_save_draft"))
        }
    }
}

@Composable
private fun ToolIcon(icon: ImageVector, desc: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(if (on) feedMint() else Color.Transparent).clickable(onClick = onClick).testTag("tool_$desc"),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = if (on) feedAccent() else stitchText().copy(alpha = 0.8f), modifier = Modifier.size(22.dp)) }
}

@Composable
private fun ComposerProgress(state: ComposerUiState, modifier: Modifier) {
    Column(
        modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(stitchSurface())
            .border(1.dp, StitchGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(state.stage.ifBlank { "Sharing…" }, color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (state.progress in 0f..1f) LinearProgressIndicator(progress = { state.progress }, color = feedAccent(), trackColor = stitchLine(),
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)))
        else LinearProgressIndicator(color = feedAccent(), trackColor = stitchLine(), modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)))
    }
}

@Composable
private fun DraftsPanel(drafts: List<ComposerDraft>, state: ComposerUiState, actions: ComposerActions) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { state.draftsOpen = false }) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(stitchBackground()).clickable(enabled = false) {}.padding(18.dp).navigationBarsPadding().testTag("composer_drafts_panel"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Drafts", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text("Saved on this phone only.", color = stitchMutedText(), fontSize = 12.sp)
            if (drafts.isEmpty()) Text("No drafts yet. Tap “Save draft” to keep a post for later.", color = stitchMutedText(), fontSize = 14.sp, modifier = Modifier.padding(vertical = 18.dp))
            drafts.take(8).forEach { d ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface()).clickable { actions.onOpenDraft(d) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(d.preview, color = stitchText(), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${d.destination.label} · ${timeAgo(d.updatedAt)}", color = stitchMutedText(), fontSize = 11.5.sp)
                    }
                    Icon(Icons.Default.Close, "Delete draft", tint = stitchMutedText(), modifier = Modifier.size(20.dp).clickable { actions.onDeleteDraft(d) })
                }
            }
        }
    }
}

/** Colours #tags and @mentions while typing (same matcher as the feed captions). */
@Composable
private fun rememberTagHighlighter(color: Color): androidx.compose.ui.text.input.VisualTransformation = remember(color) {
    androidx.compose.ui.text.input.VisualTransformation { text ->
        val styled = androidx.compose.ui.text.buildAnnotatedString {
            append(text.text)
            Regex("""(?<![\w#@])([#@][\p{L}\p{N}_.]{1,40})""").findAll(text.text).forEach { m ->
                addStyle(androidx.compose.ui.text.SpanStyle(color = color, fontWeight = FontWeight.SemiBold), m.range.first, m.range.last + 1)
            }
        }
        androidx.compose.ui.text.input.TransformedText(styled, androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}
