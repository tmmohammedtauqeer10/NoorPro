package com.noorpro.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.ComposerDraft
import com.noorpro.app.data.ComposerDraftStore
import com.noorpro.app.data.ComposerLaunch
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.data.StoryRepository
import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.data.UmmahStory
import com.noorpro.app.data.creator.*
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1.1.4 Creator Studio: Insights · Content · Comments · AI helper. Stateless UI ([NoorCreatorStudio])
 * driven by [CreatorStudioState]; [NoorCreatorStudioScreen] wires it to Firestore, local drafts and Noor AI.
 */
enum class StudioTab(val label: String, val icon: ImageVector) {
    INSIGHTS("Insights", Icons.Default.Insights), CONTENT("Content", Icons.Default.GridView),
    COMMENTS("Comments", Icons.Outlined.ChatBubbleOutline), AI("AI helper", Icons.Default.AutoAwesome)
}

class CreatorAiState(initialText: String = "", initialTopic: String = "Reminder") {
    var task by mutableStateOf(CreatorAiTask.CAPTIONS)
    var input by mutableStateOf(initialText)
    var topic by mutableStateOf(initialTopic)
    var lang by mutableStateOf("ar")
    var busy by mutableStateOf(false)
    var results by mutableStateOf<List<String>>(emptyList())
    var hashtags by mutableStateOf<List<String>>(emptyList())
    var refs by mutableStateOf<List<RefCheck>>(emptyList())
    var error by mutableStateOf<String?>(null)
    var ran by mutableStateOf(false)
    fun clear() { results = emptyList(); hashtags = emptyList(); refs = emptyList(); error = null; ran = false }
}

class CreatorStudioState(initialTab: StudioTab = StudioTab.INSIGHTS) {
    var tab by mutableStateOf(initialTab)
    var range by mutableStateOf(InsightRange.D28)
    var now by mutableStateOf(System.currentTimeMillis())
    var loading by mutableStateOf(true)
    var posts by mutableStateOf<List<UmmahPost>>(emptyList())
    var followerTimes by mutableStateOf<List<Long>>(emptyList())
    var prayer by mutableStateOf<PrayerClock>(emptyMap())
    var daily by mutableStateOf<List<Pair<String, Long>>>(emptyList())
    var stories by mutableStateOf<List<UmmahStory>>(emptyList())
    var drafts by mutableStateOf<List<ComposerDraft>>(emptyList())
    var kind by mutableStateOf(StudioKind.POST)
    var sort by mutableStateOf(StudioSort.NEWEST)
    var query by mutableStateOf("")
    var showArchived by mutableStateOf(false)
    var comments by mutableStateOf<List<UmmahComment>>(emptyList())
    var hidden by mutableStateOf<Set<String>>(emptySet())
    var keywords by mutableStateOf<Set<String>>(emptySet())
    var restricted by mutableStateOf<Set<String>>(emptySet())
    var blocked by mutableStateOf<Set<String>>(emptySet())
    var pins by mutableStateOf<Map<String, String>>(emptyMap())
    var inboxFilter by mutableStateOf(InboxFilter.ALL)
    var replyTo by mutableStateOf<String?>(null)
    var myUid by mutableStateOf("")
    val ai = CreatorAiState()

    val items: List<StudioItem> get() = posts.map(StudioContent::of) + stories.map(StudioContent::of) + drafts.map(StudioContent::of)
    fun insights() = CreatorInsightsCalc.compute(posts, followerTimes, range, now, prayer)
    fun inbox(): List<InboxComment> = CommentInbox.build(comments, myUid, posts.associate { it.id to it.caption.ifBlank { if (CreatorInsightsCalc.isReel(it)) "Your reel" else "Your post" } },
        hidden, keywords, restricted, blocked, pins + posts.filter { it.pinnedCommentId.isNotBlank() }.associate { it.id to it.pinnedCommentId })
}

class CreatorStudioActions(
    val onBack: () -> Unit = {},
    val onNewPost: () -> Unit = {},
    val onEditCaption: (UmmahPost, String) -> Unit = { _, _ -> },
    val onTogglePin: (UmmahPost) -> Unit = {},
    val onToggleArchive: (UmmahPost) -> Unit = {},
    val onDelete: (StudioItem) -> Unit = {},
    val onOpenDraft: (ComposerDraft) -> Unit = {},
    val onReply: (UmmahComment, String) -> Unit = { _, _ -> },
    val onHide: (InboxComment) -> Unit = {},
    val onDeleteComment: (InboxComment) -> Unit = {},
    val onPinComment: (InboxComment) -> Unit = {},
    val onRestrict: (InboxComment) -> Unit = {},
    val onBlock: (InboxComment) -> Unit = {},
    val onAddKeyword: (String) -> Unit = {},
    val onRemoveKeyword: (String) -> Unit = {},
    val onRunAi: () -> Unit = {},
    val onInsert: (String) -> Unit = {},
    val onCopy: (String) -> Unit = {},
)

@Composable
fun NoorCreatorStudio(state: CreatorStudioState, actions: CreatorStudioActions, editEnabled: Boolean, dailyEnabled: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().testTag("creator_studio")) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText()) }
            Column(Modifier.weight(1f)) {
                Text("Creator Studio", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("Private to you · grow with ikhlas", color = stitchMutedText(), fontSize = 12.sp)
            }
            Row(
                Modifier.clip(RoundedCornerShape(999.dp)).background(feedAccentStrongStudio()).clickable(onClick = actions.onNewPost)
                    .padding(horizontal = 14.dp, vertical = 9.dp).testTag("studio_new"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp)); Text("Create", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(feedMint()).padding(4.dp)
        ) {
            StudioTab.entries.forEach { t ->
                val sel = t == state.tab
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(if (sel) stitchSurface() else Color.Transparent)
                        .clickable { state.tab = t }.padding(vertical = 9.dp).testTag("studio_tab_${t.name}"),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(t.icon, null, tint = if (sel) feedAccent() else stitchMutedText(), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(t.label, color = if (sel) stitchText() else stitchMutedText(), fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (state.tab) {
                StudioTab.INSIGHTS -> InsightsTab(state, dailyEnabled)
                StudioTab.CONTENT -> ContentTab(state, actions, editEnabled)
                StudioTab.COMMENTS -> CommentsTab(state, actions, editEnabled)
                StudioTab.AI -> LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp)) {
                    item { CreatorAiPanel(state.ai, actions.onRunAi, actions.onInsert, actions.onCopy, insertLabel = "Use in a new post") }
                }
            }
        }
    }
}

@Composable private fun feedAccentStrongStudio(): Color = if (isStitchLight()) StitchEmerald else Color(0xFF1F7A5A)

@Composable
private fun StudioCard(modifier: Modifier = Modifier, tag: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(stitchSurface())
            .border(1.dp, stitchLine().copy(alpha = 0.7f), RoundedCornerShape(24.dp)).padding(18.dp)
            .let { if (tag != null) it.testTag(tag) else it },
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content
    )
}

@Composable
private fun SectionLabel(text: String) = Text(text.uppercase(), color = feedGold(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)

@Composable
private fun Chip(text: String, selected: Boolean, tag: String? = null, onClick: () -> Unit) {
    Text(
        text, color = if (selected) Color.White else stitchText(), fontSize = 12.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (selected) feedAccentStrongStudio() else feedMint())
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp).let { if (tag != null) it.testTag(tag) else it }
    )
}

// ---------------------------------------------------------------- Insights

@Composable
private fun InsightsTab(state: CreatorStudioState, dailyEnabled: Boolean) {
    val ins = remember(state.posts, state.followerTimes, state.range, state.now, state.prayer) { state.insights() }
    LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.testTag("studio_insights")) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightRange.entries.forEach { r -> Chip(r.label, r == state.range, "range_${r.days}") { state.range = r } }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(StitchEmerald, Color(0xFF07402C)))).padding(20.dp)
            ) {
                Text("VIEWS · LAST ${ins.range.label.uppercase()}", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(compactCount(ins.views), color = Color.White, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 42.sp, modifier = Modifier.testTag("insight_views"))
                    Spacer(Modifier.width(10.dp))
                    ins.viewsChangePct?.let { pct ->
                        Text("${if (pct >= 0) "▲" else "▼"} ${kotlin.math.abs(pct)}% vs previous", color = if (pct >= 0) Color(0xFF9BE7C4) else Color(0xFFFFB4A9),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
                Text("On ${ins.posts} post${if (ins.posts == 1) "" else "s"} and ${ins.reels} reel${if (ins.reels == 1) "" else "s"} you shared in this period", color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Likes" to ins.likes, "Comments" to ins.comments, "Shares" to ins.shares, "Saves" to ins.saves).forEach { (l, v) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.1f)).padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(compactCount(v), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(l, color = Color.White.copy(alpha = 0.75f), fontSize = 10.5.sp)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Reach", compactCount(ins.uniqueReelViewers), "unique reel viewers", Icons.Outlined.Visibility, Modifier.weight(1f))
                MetricTile("Engagement", "${"%.1f".format(Locale.US, ins.engagementRate * 100)}%", "${compactCount(ins.interactions)} interactions", Icons.Outlined.FavoriteBorder, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Followers", compactCount(ins.followers.toLong()), "${if (ins.followerGain >= 0) "+" else ""}${ins.followerGain} this period", Icons.Default.Groups, Modifier.weight(1f))
                MetricTile("Shared", "${ins.posts + ins.reels}", "${ins.posts} posts · ${ins.reels} reels", Icons.Default.GridView, Modifier.weight(1f))
            }
        }
        item {
            StudioCard(tag = "insight_views_chart") {
                SectionLabel(if (dailyEnabled && state.daily.size > 1) "Daily views" else "Views by period")
                val pts = if (dailyEnabled && state.daily.size > 1) CreatorInsightsCalc.dailyDeltas(state.daily).takeLast(ins.range.days.coerceAtMost(30)) else ins.viewsSeries
                BarChart(pts, StitchGold, feedAccent())
                Text(
                    if (dailyEnabled && state.daily.size > 1) "New views each day across all your content." else "Lifetime views of the content you published in each period.",
                    color = stitchMutedText(), fontSize = 11.5.sp
                )
            }
        }
        item {
            StudioCard(tag = "insight_followers_chart") {
                SectionLabel("Follower growth")
                Text("+${ins.followerGain} new followers · ${compactCount(ins.followers.toLong())} total", color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                BarChart(ins.followerSeries, feedAccent(), feedAccent().copy(alpha = 0.55f))
            }
        }
        if (ins.topPosts.isNotEmpty() || ins.topReels.isNotEmpty()) item {
            StudioCard(tag = "insight_top") {
                SectionLabel("Top content")
                ins.topReels.forEachIndexed { i, p -> TopRow(i + 1, p, "Reel") }
                ins.topPosts.forEachIndexed { i, p -> TopRow(i + 1, p, "Post") }
            }
        }
        item {
            StudioCard(tag = "insight_best_time") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = feedGold(), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    SectionLabel("Best time to post")
                }
                ins.bestSlots.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (i == 0) feedMint() else Color.Transparent).padding(10.dp)) {
                        Text("${i + 1}", color = feedAccent(), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.width(22.dp))
                        Column(Modifier.weight(1f)) {
                            Text(CreatorInsightsCalc.hourLabel(s.hour), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(s.prayerNote.replaceFirstChar { it.uppercase() } + if (s.posts > 0) " · ${s.posts} of your posts" else "", color = stitchMutedText(), fontSize = 12.sp)
                        }
                        if (i == 0) Text("Best", color = feedGold(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    if (ins.bestFromHistory) "From how your posts did in the last 90 days, in your local time."
                    else "Not enough history yet — these are the moments the Ummah is usually online (after Maghrib and Isha, and after Fajr). Never let posting come before salah.",
                    color = stitchMutedText(), fontSize = 11.5.sp, lineHeight = 16.sp
                )
            }
        }
        item {
            StudioCard(tag = "insight_watch_time") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, tint = stitchMutedText(), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Watch time & retention", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text("Coming soon", color = feedGold(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, feedGold(), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Text("Average watch time and where viewers drop off in your reels will appear here once private watch receipts are summarised on the server.", color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun MetricTile(title: String, value: String, sub: String, icon: ImageVector, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(stitchSurface()).border(1.dp, stitchLine().copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = feedAccent(), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text(title, color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(sub, color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BarChart(points: List<ChartPoint>, top: Color, bottom: Color) {
    val max = (points.maxOfOrNull { it.value } ?: 0L).coerceAtLeast(1L)
    Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(if (points.size > 10) 3.dp else 8.dp), verticalAlignment = Alignment.Bottom) {
        points.forEach { p ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (points.size <= 10 && p.value > 0) Text(compactCount(p.value), color = stitchMutedText(), fontSize = 9.5.sp, maxLines = 1)
                val frac = (p.value.toFloat() / max).coerceIn(0.03f, 1f)
                Box(Modifier.fillMaxWidth().height((frac * 96).dp).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(Brush.verticalGradient(listOf(top, bottom))))
                Spacer(Modifier.height(4.dp))
                Text(if (points.size > 14) "" else p.label, color = stitchMutedText(), fontSize = 9.5.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Thumb(url: String, size: androidx.compose.ui.unit.Dp, icon: ImageVector) {
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(feedMint()), contentAlignment = Alignment.Center) {
        val preview = UmmahFeedPreview.images[url]
        when {
            preview != null -> Image(preview.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            url.startsWith("http") || url.startsWith("file") || url.startsWith("/") || url.startsWith("content") ->
                AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> Icon(icon, null, tint = feedAccent(), modifier = Modifier.size(size / 2.6f))
        }
    }
}

@Composable
private fun TopRow(rank: Int, p: UmmahPost, kind: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$rank", color = feedGold(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.width(20.dp))
        Thumb(p.thumbnailUrl.ifBlank { p.mediaUrl.takeIf { p.type == "image" }.orEmpty() }, 48.dp, if (kind == "Reel") Icons.Default.SmartDisplay else Icons.Default.Article)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.caption.ifBlank { kind }, color = stitchText(), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$kind · ${compactCount(p.viewCount)} views · ${compactCount(p.likeCount)} likes", color = stitchMutedText(), fontSize = 11.5.sp)
        }
    }
}

// ---------------------------------------------------------------- Content manager

private fun shortDate(t: Long) = if (t <= 0) "" else SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(t))

@Composable
private fun ContentTab(state: CreatorStudioState, actions: CreatorStudioActions, editEnabled: Boolean) {
    val all = state.items
    val shown = StudioContent.filter(all, state.kind, state.sort, state.query, state.showArchived)
    var editing by remember { mutableStateOf<UmmahPost?>(null) }
    var deleting by remember { mutableStateOf<StudioItem?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("studio_content")) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StudioKind.entries) { k ->
                    val n = all.count { it.kind == k && (!it.archived || k == StudioKind.DRAFT) }
                    Chip("${k.label} $n", k == state.kind, "kind_${k.name}") { state.kind = k }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = stitchMutedText(), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (state.query.isEmpty()) Text("Search your ${state.kind.label.lowercase()}", color = stitchMutedText(), fontSize = 13.5.sp)
                        BasicTextField(state.query, { state.query = it }, singleLine = true, textStyle = TextStyle(color = stitchText(), fontSize = 13.5.sp),
                            cursorBrush = SolidColor(feedAccent()), modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.width(8.dp))
                var sortMenu by remember { mutableStateOf(false) }
                Box {
                    Row(Modifier.clip(RoundedCornerShape(999.dp)).background(feedMint()).clickable { sortMenu = true }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sort, null, tint = feedAccent(), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                        Text(state.sort.label, color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        StudioSort.entries.forEach { s -> DropdownMenuItem(text = { Text(s.label) }, onClick = { state.sort = s; sortMenu = false }) }
                    }
                }
            }
        }
        if (editEnabled && state.kind in setOf(StudioKind.POST, StudioKind.REEL)) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip("Live", !state.showArchived) { state.showArchived = false }
                Spacer(Modifier.width(8.dp))
                Chip("Archived ${all.count { it.archived && it.kind == state.kind }}", state.showArchived, "show_archived") { state.showArchived = true }
            }
        }
        if (shown.isEmpty()) item {
            StudioCard {
                Text(
                    when {
                        state.loading -> "Loading your content…"
                        state.query.isNotBlank() -> "Nothing matches “${state.query}”."
                        state.kind == StudioKind.DRAFT -> "No drafts on this phone. Drafts you save in the composer appear here."
                        state.kind == StudioKind.STORY -> "No active stories. Stories disappear after 24 hours."
                        state.showArchived -> "Nothing archived."
                        else -> "Nothing here yet — tap Create to share your first ${state.kind.label.lowercase().removeSuffix("s")}."
                    }, color = stitchMutedText(), fontSize = 13.sp
                )
            }
        }
        items(shown, key = { it.kind.name + it.id }) { item -> ContentRow(item, editEnabled, actions, onEdit = { editing = it }, onDelete = { deleting = it }) }
    }
    editing?.let { p ->
        var text by remember(p.id) { mutableStateOf(p.caption) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit caption") },
            text = { OutlinedTextField(text, { text = it.take(2200) }, minLines = 3, maxLines = 8, modifier = Modifier.testTag("edit_caption_field")) },
            confirmButton = { TextButton(onClick = { actions.onEditCaption(p, text); editing = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
    deleting?.let { it ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this ${it.kind.label.lowercase().removeSuffix("s")}?") },
            text = { Text(if (it.kind == StudioKind.DRAFT) "The draft and its media are removed from this phone." else "It's removed from NoorPro for everyone, with its likes and comments. This can't be undone.") },
            confirmButton = { TextButton(onClick = { actions.onDelete(it); deleting = null }, modifier = Modifier.testTag("confirm_delete")) { Text("Delete", color = Color(0xFFBA1A1A)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ContentRow(item: StudioItem, editEnabled: Boolean, actions: CreatorStudioActions, onEdit: (UmmahPost) -> Unit, onDelete: (StudioItem) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(stitchSurface()).border(1.dp, stitchLine().copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .clickable { item.draft?.let(actions.onOpenDraft) }.padding(10.dp).testTag("content_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumb(item.thumbUrl, 66.dp, when (item.kind) { StudioKind.REEL -> Icons.Default.SmartDisplay; StudioKind.STORY -> Icons.Default.AutoStories; StudioKind.DRAFT -> Icons.Default.EditNote; else -> Icons.Default.Article })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.pinned) { Icon(Icons.Default.PushPin, "Pinned", tint = feedGold(), modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                if (item.archived) { Icon(Icons.Default.Archive, "Archived", tint = stitchMutedText(), modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                Text(item.title, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                when (item.kind) {
                    StudioKind.DRAFT -> "Draft · edited ${shortDate(item.time)}"
                    StudioKind.STORY -> "Story · ${shortDate(item.time)} · disappears in ${(((item.story?.expiresAt ?: 0L) - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(0)}h"
                    else -> "${compactCount(item.views)} views · ${compactCount(item.likes)} likes · ${compactCount(item.comments)} comments · ${shortDate(item.time)}"
                }, color = stitchMutedText(), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.testTag("content_menu_${item.id}")) { Icon(Icons.Default.MoreVert, "More", tint = stitchMutedText()) }
            DropdownMenu(menu, { menu = false }) {
                val p = item.post
                if (item.draft != null) DropdownMenuItem(text = { Text("Open in composer") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menu = false; actions.onOpenDraft(item.draft) })
                if (p != null && editEnabled) {
                    DropdownMenuItem(text = { Text("Edit caption") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menu = false; onEdit(p) })
                    DropdownMenuItem(text = { Text(if (p.pinnedToProfile) "Unpin from profile" else "Pin to profile") }, leadingIcon = { Icon(Icons.Default.PushPin, null) }, onClick = { menu = false; actions.onTogglePin(p) })
                    DropdownMenuItem(text = { Text(if (p.archived) "Unarchive" else "Archive") }, leadingIcon = { Icon(Icons.Default.Archive, null) }, onClick = { menu = false; actions.onToggleArchive(p) })
                }
                DropdownMenuItem(text = { Text("Delete", color = Color(0xFFBA1A1A)) }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFBA1A1A)) }, onClick = { menu = false; onDelete(item) })
            }
        }
    }
}

// ---------------------------------------------------------------- Comments manager

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentsTab(state: CreatorStudioState, actions: CreatorStudioActions, editEnabled: Boolean) {
    val inbox = remember(state.comments, state.hidden, state.keywords, state.restricted, state.blocked, state.pins, state.posts) { state.inbox() }
    val shown = CommentInbox.filter(inbox, state.inboxFilter)
    var newKeyword by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("studio_comments")) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(InboxFilter.entries) { f -> Chip("${f.label} ${CommentInbox.filter(inbox, f).size}", f == state.inboxFilter, "inbox_${f.name}") { state.inboxFilter = f } }
            }
        }
        item {
            StudioCard(tag = "keyword_filter") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterAlt, null, tint = feedGold(), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp))
                    SectionLabel("Keyword filter")
                }
                Text("Comments containing these words go to Filtered, out of your main inbox.", color = stitchMutedText(), fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.keywords.sorted().forEach { k ->
                        Row(Modifier.clip(RoundedCornerShape(999.dp)).background(feedMint()).padding(start = 11.dp, end = 6.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(k, color = stitchText(), fontSize = 12.sp); Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Close, "Remove $k", tint = stitchMutedText(), modifier = Modifier.size(14.dp).clickable { actions.onRemoveKeyword(k) })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).border(1.dp, stitchLine(), RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 9.dp)) {
                        if (newKeyword.isEmpty()) Text("Add a word or phrase", color = stitchMutedText(), fontSize = 13.sp)
                        BasicTextField(newKeyword, { newKeyword = it.take(40) }, singleLine = true, textStyle = TextStyle(color = stitchText(), fontSize = 13.sp),
                            cursorBrush = SolidColor(feedAccent()), modifier = Modifier.fillMaxWidth().testTag("keyword_field"))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(feedAccentStrongStudio()).clickable { actions.onAddKeyword(newKeyword); newKeyword = "" }
                            .padding(horizontal = 14.dp, vertical = 9.dp).testTag("keyword_add"))
                }
            }
        }
        if (shown.isEmpty()) item {
            StudioCard {
                Text(when (state.inboxFilter) {
                    InboxFilter.ALL -> if (state.loading) "Loading comments…" else "No comments yet. When people comment on your posts and reels they'll appear here."
                    InboxFilter.UNREPLIED -> "You're all caught up, masha'Allah."
                    InboxFilter.FILTERED -> "Nothing filtered."
                    InboxFilter.HIDDEN -> "No hidden comments."
                }, color = stitchMutedText(), fontSize = 13.sp)
            }
        }
        items(shown, key = { it.comment.id }) { c -> CommentCard(c, state, actions, editEnabled) }
    }
}

@Composable
private fun CommentCard(c: InboxComment, state: CreatorStudioState, actions: CreatorStudioActions, editEnabled: Boolean) {
    var menu by remember { mutableStateOf(false) }
    var reply by remember(c.comment.id) { mutableStateOf("") }
    val replying = state.replyTo == c.comment.id
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(stitchSurface())
            .border(1.dp, if (c.pinned) feedGold().copy(alpha = 0.6f) else stitchLine().copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(14.dp)
            .testTag("inbox_${c.comment.id}")
    ) {
        Row(verticalAlignment = Alignment.Top) {
            UmmahAvatar(c.comment.creatorName, "", 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.comment.creatorName, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Text(" · ${relativeAgo(c.comment.createdAt, state.now)}", color = stitchMutedText(), fontSize = 11.5.sp)
                    if (c.pinned) { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.PushPin, "Pinned", tint = feedGold(), modifier = Modifier.size(13.dp)) }
                }
                Text("on “${c.postTitle.take(40)}${if (c.postTitle.length > 40) "…" else ""}”", color = stitchMutedText(), fontSize = 11.sp, maxLines = 1)
                Spacer(Modifier.height(5.dp))
                Text(c.comment.text, color = stitchText(), fontSize = 14.sp, lineHeight = 19.sp)
                if (c.keywordHit != null || c.restricted) {
                    Spacer(Modifier.height(5.dp))
                    Text(if (c.restricted) "Restricted account" else "Matched “${c.keywordHit}”", color = feedGold(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "Comment options", tint = stitchMutedText(), modifier = Modifier.size(18.dp)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(if (c.pinned) "Unpin" else "Pin to top") }, leadingIcon = { Icon(Icons.Default.PushPin, null) }, onClick = { menu = false; actions.onPinComment(c) })
                    if (editEnabled) DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null) }, onClick = { menu = false; actions.onDeleteComment(c) })
                    DropdownMenuItem(text = { Text(if (c.restricted) "Unrestrict ${c.comment.creatorName}" else "Restrict ${c.comment.creatorName}") }, leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null) }, onClick = { menu = false; actions.onRestrict(c) })
                    DropdownMenuItem(text = { Text("Block ${c.comment.creatorName}", color = Color(0xFFBA1A1A)) }, leadingIcon = { Icon(Icons.Default.Block, null, tint = Color(0xFFBA1A1A)) }, onClick = { menu = false; actions.onBlock(c) })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(start = 48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallAction(Icons.AutoMirrored.Filled.Reply, if (c.replied) "Replied" else "Reply", tag = "reply_${c.comment.id}") { state.replyTo = if (replying) null else c.comment.id }
            SmallAction(if (c.hidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, if (c.hidden) "Unhide" else "Hide", tag = "hide_${c.comment.id}") { actions.onHide(c) }
        }
        if (replying) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.padding(start = 48.dp).clip(RoundedCornerShape(999.dp)).background(feedMint()).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (reply.isEmpty()) Text("Reply to ${c.comment.creatorName}…", color = stitchMutedText(), fontSize = 13.sp)
                    BasicTextField(reply, { reply = it.take(500) }, textStyle = TextStyle(color = stitchText(), fontSize = 13.sp), cursorBrush = SolidColor(feedAccent()),
                        modifier = Modifier.fillMaxWidth().testTag("reply_field_${c.comment.id}"))
                }
                IconButton(onClick = { if (reply.isNotBlank()) { actions.onReply(c.comment, reply); reply = ""; state.replyTo = null } }, modifier = Modifier.size(36.dp).testTag("reply_send_${c.comment.id}")) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send reply", tint = feedAccent(), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, stitchLine(), RoundedCornerShape(999.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = feedAccent(), modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
        Text(label, color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun relativeAgo(t: Long, now: Long): String {
    val m = ((now - t) / 60_000L).coerceAtLeast(0)
    return when { m < 1 -> "now"; m < 60 -> "${m}m"; m < 1440 -> "${m / 60}h"; m < 10080 -> "${m / 1440}d"; else -> shortDate(t) }
}

// ---------------------------------------------------------------- AI helper (also used inside the composer)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatorAiPanel(ai: CreatorAiState, onRun: () -> Unit, onInsert: (String) -> Unit, onCopy: (String) -> Unit, insertLabel: String = "Insert") {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("creator_ai")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Brush.linearGradient(listOf(StitchGold, StitchEmerald))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Noor AI for creators", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text("Captions, hashtags, reference checks, translation", color = stitchMutedText(), fontSize = 12.sp)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CreatorAiTask.entries.forEach { t -> Chip(t.label, t == ai.task, "ai_task_${t.name}") { ai.task = t; ai.clear() } }
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(18.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).padding(14.dp)
        ) {
            if (ai.input.isEmpty()) Text(
                when (ai.task) {
                    CreatorAiTask.CAPTIONS -> "What's your post about? (optional — or paste your draft)"
                    CreatorAiTask.HASHTAGS -> "Paste your caption"
                    CreatorAiTask.CHECK -> "Paste text with references, e.g. “Al-Baqarah 2:286” or “Bukhari 6018”"
                    CreatorAiTask.TRANSLATE -> "Text to translate"
                }, color = stitchMutedText(), fontSize = 13.5.sp
            )
            BasicTextField(ai.input, { ai.input = it.take(2000) }, textStyle = TextStyle(color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp),
                cursorBrush = SolidColor(feedAccent()), modifier = Modifier.fillMaxWidth().testTag("ai_input"))
        }
        if (ai.task == CreatorAiTask.CAPTIONS || ai.task == CreatorAiTask.HASHTAGS) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Reminder", "Quran", "Dua", "Reflection", "Hadith", "Community").forEach { t -> Chip(t, t == ai.topic) { ai.topic = t } }
        }
        if (ai.task == CreatorAiTask.TRANSLATE) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CreatorAiPrompts.languages.forEach { (code, name) -> Chip(name, code == ai.lang, "ai_lang_$code") { ai.lang = code } }
        }
        Button(
            onClick = onRun, enabled = !ai.busy && (ai.task == CreatorAiTask.CAPTIONS || ai.input.isNotBlank()),
            colors = ButtonDefaults.buttonColors(containerColor = feedAccentStrongStudio()), shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ai_run")
        ) {
            if (ai.busy) { CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Noor AI is thinking…") }
            else Text(if (ai.task == CreatorAiTask.CHECK) "Check against offline Qur'an & hadith" else "Ask Noor AI", fontWeight = FontWeight.Bold)
        }
        ai.error?.let { Text(it, color = Color(0xFFBA1A1A), fontSize = 12.5.sp) }
        ai.results.forEach { r -> ResultCard(r, insertLabel, onInsert, onCopy) }
        if (ai.hashtags.isNotEmpty()) StudioCard(tag = "ai_hashtags") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ai.hashtags.forEach { h -> Text(h, color = feedAccent(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(feedMint()).padding(horizontal = 10.dp, vertical = 5.dp)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction(Icons.Default.AddCircleOutline, "Add all to post", "ai_insert_tags") { onInsert(ai.hashtags.joinToString(" ")) }
                SmallAction(Icons.Default.ContentCopy, "Copy", "ai_copy_tags") { onCopy(ai.hashtags.joinToString(" ")) }
            }
        }
        ai.refs.forEach { r -> RefCard(r) }
        if (ai.task == CreatorAiTask.CHECK && ai.ran && ai.refs.isEmpty() && !ai.busy) Text("No ayah or hadith references found. Write them like “Al-Baqarah 2:286” or “Sahih Muslim 2564”.", color = stitchMutedText(), fontSize = 12.5.sp)
        Text("AI suggestions can be wrong — read before posting, and never attribute words to Allah or the Prophet ﷺ without a checked source.", color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun ResultCard(text: String, insertLabel: String, onInsert: (String) -> Unit, onCopy: (String) -> Unit) {
    StudioCard(tag = "ai_result") {
        Text(text, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction(Icons.Default.AddCircleOutline, insertLabel, "ai_insert") { onInsert(text) }
            SmallAction(Icons.Default.ContentCopy, "Copy", "ai_copy") { onCopy(text) }
        }
    }
}

@Composable
private fun RefCard(r: RefCheck) {
    val good = r.ok
    val tint = if (good) feedAccent() else Color(0xFFBA1A1A)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (good) feedMint() else Color(0xFFBA1A1A).copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(18.dp)).padding(14.dp).testTag("ref_${if (good) "ok" else "bad"}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (good) Icons.Default.Verified else Icons.Default.ErrorOutline, null, tint = tint, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(r.raw, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(r.message, color = stitchText(), fontSize = 13.sp, lineHeight = 18.sp)
        if (r.text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(r.text, color = stitchMutedText(), fontSize = 12.5.sp, lineHeight = 19.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Runs the selected AI task (askNoorAi with client-side framing; reference checks fully offline). */
fun runCreatorAi(ai: CreatorAiState, context: Context, scope: CoroutineScope) {
    if (ai.busy) return
    ai.clear(); ai.busy = true
    val app = context.applicationContext
    scope.launch {
        try {
            when (ai.task) {
                CreatorAiTask.CHECK -> {
                    val found = ReferenceChecker.find(ai.input)
                    ai.refs = found
                    ai.refs = withContext(Dispatchers.IO) { found.map { ReferenceChecker.withText(app, it) } }
                }
                CreatorAiTask.CAPTIONS -> {
                    val answer = AiFeatureService.generateResponse(CreatorAiPrompts.captions(ai.input, ai.topic), NoorAiMode.AskNoor)
                    val list = CreatorAiPrompts.parseList(answer)
                    if (list.isEmpty()) ai.error = "Noor AI couldn't write captions right now. Check your connection and try again."
                    ai.results = list
                }
                CreatorAiTask.HASHTAGS -> {
                    val local = CreatorAiPrompts.localHashtags(ai.input, ai.topic)
                    ai.hashtags = local
                    val answer = AiFeatureService.generateResponse(CreatorAiPrompts.hashtags(ai.input, ai.topic), NoorAiMode.AskNoor)
                    val tags = CreatorAiPrompts.parseHashtags(answer)
                    ai.hashtags = (tags + local).distinctBy { it.lowercase() }.take(12)
                }
                CreatorAiTask.TRANSLATE -> {
                    val before = AiFeatureService.answerLanguage
                    AiFeatureService.answerLanguage = ai.lang
                    val answer = try { AiFeatureService.generateResponse(CreatorAiPrompts.translate(ai.input, ai.lang), NoorAiMode.AskNoor) } finally { AiFeatureService.answerLanguage = before }
                    if (answer == AiFeatureService.UNAVAILABLE_MESSAGE) ai.error = "Translation needs Noor AI online. Please try again in a moment."
                    else ai.results = listOf(CreatorAiPrompts.cleanTranslation(answer))
                }
            }
        } catch (e: Exception) {
            ai.error = "Something went wrong: ${e.localizedMessage ?: "try again"}"
        } finally {
            ai.ran = true; ai.busy = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorAiSheet(initialText: String, topic: String, onInsert: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ai = remember { CreatorAiState(initialText, topic) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = stitchBackground()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 32.dp)) {
            item {
                CreatorAiPanel(ai, { runCreatorAi(ai, context, scope) }, { onInsert(it); onDismiss() },
                    { clipboard.setText(androidx.compose.ui.text.AnnotatedString(it)); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }, insertLabel = "Insert into post")
            }
        }
    }
}

// ---------------------------------------------------------------- screen wiring

@Composable
fun NoorCreatorStudioScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { CreatorStudioState() }
    val repo = remember { UmmahRepository() }
    val stories = remember { StoryRepository() }
    val drafts = remember { ComposerDraftStore(context) }
    val mod = remember { CreatorModerationStore(context) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var refresh by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { CreatorFlags.load() }
    val prayers by viewModel.prayers.collectAsState()
    LaunchedEffect(prayers) { state.prayer = prayers.associate { it.name to it.time } }
    state.blocked = rememberBlockedUids()
    fun toast(s: String) = Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    fun syncLocal() { state.hidden = mod.hidden(); state.keywords = mod.keywords(); state.restricted = mod.restricted(); state.pins = mod.pins(); state.drafts = drafts.all() }
    LaunchedEffect(refresh) {
        state.myUid = repo.currentUserUid().orEmpty(); state.now = System.currentTimeMillis()
        syncLocal()
        repo.fetchMyPosts { posts ->
            state.posts = posts; state.loading = false
            repo.fetchCommentsForPosts(posts.take(60).map { it.id }) { state.comments = it }
        }
        repo.fetchFollowerTimes { state.followerTimes = it }
        if (CreatorFlags.daily) repo.fetchDailyStats { state.daily = it }
    }
    DisposableEffect(Unit) {
        val reg = stories.observeActive { list -> state.stories = list.filter { it.authorUid == state.myUid } }
        onDispose { reg?.remove() }
    }
    if (!viewModel.isLoggedIn) {
        StitchSignInRequiredScreen(title = "Creator Studio", message = "Sign in to see your insights, manage your content and reply to comments.",
            onBack = { viewModel.goBack() }, onSignIn = { viewModel.navigateTo(DeenScreen.LOGIN) })
        return
    }
    fun replacePost(p: UmmahPost) { state.posts = state.posts.map { if (it.id == p.id) p else it } }
    fun edit(p: UmmahPost, fields: Map<String, Any>, updated: UmmahPost, done: String) {
        replacePost(updated)
        repo.updateMyPost(p.id, fields) { ok -> if (ok) toast(done) else { replacePost(p); toast("Couldn't update — try again later") } }
    }
    NoorCreatorStudio(
        state = state,
        editEnabled = CreatorFlags.edit,
        dailyEnabled = CreatorFlags.daily,
        actions = CreatorStudioActions(
            onBack = { viewModel.goBack() },
            onNewPost = { viewModel.navigateTo(DeenScreen.UMMAH_CREATE_POST) },
            onEditCaption = { p, t -> edit(p, mapOf("caption" to t), p.copy(caption = t.trim()), "Caption updated") },
            onTogglePin = { p ->
                // One pinned post at a time: unpin the previous one.
                state.posts.firstOrNull { it.pinnedToProfile && it.id != p.id && !p.pinnedToProfile }?.let { old -> edit(old, mapOf("pinnedToProfile" to false), old.copy(pinnedToProfile = false), "") }
                edit(p, mapOf("pinnedToProfile" to !p.pinnedToProfile), p.copy(pinnedToProfile = !p.pinnedToProfile), if (p.pinnedToProfile) "Unpinned" else "Pinned to your profile")
            },
            onToggleArchive = { p -> edit(p, mapOf("archived" to !p.archived), p.copy(archived = !p.archived), if (p.archived) "Back on your profile" else "Archived — only you can see it") },
            onDelete = { item ->
                when {
                    item.draft != null -> { drafts.delete(item.draft.id); state.drafts = drafts.all(); toast("Draft deleted") }
                    item.story != null -> stories.delete(item.story) { ok -> if (ok) { state.stories = state.stories - item.story; toast("Story deleted") } else toast("Couldn't delete") }
                    item.post != null -> repo.deletePost(item.post) { ok -> if (ok) { state.posts = state.posts - item.post; toast("Deleted") } else toast("Couldn't delete") }
                }
            },
            onOpenDraft = { d -> ComposerLaunch.openDraft(d.id, d.destination); viewModel.navigateTo(if (d.destination == ComposerDestination.REEL) DeenScreen.UMMAH_CREATE_REEL else DeenScreen.UMMAH_CREATE_POST) },
            onReply = { c, text ->
                repo.submitComment(c.postId, text, parentCommentId = c.id, replyToName = c.creatorName) { ok ->
                    if (ok) { state.comments = state.comments + UmmahComment("local${System.nanoTime()}", c.postId, text.trim(), state.myUid, "You", System.currentTimeMillis(), c.id); toast("Reply posted") }
                    else toast("Couldn't send reply")
                }
            },
            onHide = { c ->
                val hide = !c.hidden
                mod.setHidden(c.comment.id, hide); state.hidden = mod.hidden()
                if (CreatorFlags.edit) repo.setCommentHidden(c.comment.id, hide) {}
                if (!hide) state.comments = state.comments.map { if (it.id == c.comment.id) it.copy(status = "published") else it }
                toast(if (hide) "Hidden" + if (CreatorFlags.edit) " for everyone except them" else " from your inbox" else "Unhidden")
            },
            onDeleteComment = { c -> repo.deleteCommentOnMyPost(c.comment) { ok -> if (ok) { state.comments = state.comments - c.comment; toast("Comment deleted") } else toast("Couldn't delete comment") } },
            onPinComment = { c ->
                val pin = if (c.pinned) null else c.comment.id
                mod.setPin(c.comment.postId, pin); state.pins = mod.pins()
                state.posts.firstOrNull { it.id == c.comment.postId }?.let { p ->
                    if (CreatorFlags.edit) edit(p, mapOf("pinnedCommentId" to (pin ?: "")), p.copy(pinnedCommentId = pin.orEmpty()), "") else replacePost(p.copy(pinnedCommentId = pin.orEmpty()))
                }
                toast(if (pin != null) "Pinned" else "Unpinned")
            },
            onRestrict = { c -> mod.setRestricted(c.comment.creatorUid, !c.restricted); state.restricted = mod.restricted(); toast(if (c.restricted) "Unrestricted" else "Restricted — their comments go to Filtered") },
            onBlock = { c -> repo.setBlocked(c.comment.creatorUid, true) { ok -> toast(if (ok) "${c.comment.creatorName} blocked" else "Couldn't block") } },
            onAddKeyword = { k -> mod.addKeyword(k); state.keywords = mod.keywords() },
            onRemoveKeyword = { k -> mod.removeKeyword(k); state.keywords = mod.keywords() },
            onRunAi = { runCreatorAi(state.ai, context, scope) },
            onInsert = { t -> ComposerLaunch.prefill(t); viewModel.navigateTo(DeenScreen.UMMAH_CREATE_POST) },
            onCopy = { t -> clipboard.setText(androidx.compose.ui.text.AnnotatedString(t)); toast("Copied") },
        )
    )
}
