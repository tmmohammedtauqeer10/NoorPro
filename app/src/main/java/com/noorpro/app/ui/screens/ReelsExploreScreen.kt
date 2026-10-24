package com.noorpro.app.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.data.search.PeopleRanker
import com.noorpro.app.data.search.ReelSearchKeys
import com.noorpro.app.data.search.ReelSearchRanker
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.delay

/** Preview-only overrides (Robolectric can't run Firestore/Coil). */
internal object ReelsExplorePreview {
    var posts: List<UmmahPost>? = null
    var art: Map<String, ImageBitmap> = emptyMap()
    var query: String = ""
    var tab: Int = 0
    var tag: String = ""
    var profiles: Map<String, com.noorpro.app.data.UmmahProfile> = emptyMap()
    var following: Set<String> = emptySet()
    var followers: Set<String> = emptySet()
}

private class ReelRecentSearches(context: Context) {
    private val prefs = context.getSharedPreferences("noor_reel_search", Context.MODE_PRIVATE)
    fun load(): List<String> = prefs.getString("recent", "").orEmpty().split('\n').filter { it.isNotBlank() }
    fun add(q: String): List<String> = (listOf(q) + load().filterNot { it.equals(q, true) }).take(12).also { save(it) }
    fun remove(q: String): List<String> = load().filterNot { it == q }.also { save(it) }
    fun clear(): List<String> = emptyList<String>().also { save(it) }
    private fun save(l: List<String>) { prefs.edit().putString("recent", l.joinToString("\n")).apply() }
}

private val TABS = listOf("Top", "Reels", "Accounts", "Hashtags")

@Composable
fun ReelsExploreScreen(viewModel: DeenViewModel) = ReelsExploreContent(
    onBack = { viewModel.goBack() },
    onOpenReel = { p, list -> viewModel.openReelFromResults(p, list) },
    onOpenProfile = { a -> viewModel.openCreatorProfile(a.uid, a.name, a.handle) },
    onMessage = { uid, name -> viewModel.openCreatorChat(uid, name) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReelsExploreContent(onBack: () -> Unit, onOpenReel: (UmmahPost, List<UmmahPost>) -> Unit, onOpenProfile: (ReelSearchRanker.Account) -> Unit, onMessage: (String, String) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    val preview = ReelsExplorePreview.posts
    val feed = if (preview == null) rememberStitchUmmahFeed() else StitchUmmahFeedState(preview, null, false)
    val profiles = if (preview == null) rememberStitchProfiles() else ReelsExplorePreview.profiles
    val graph = if (preview == null) rememberPeopleGraph(true) else ReelsExplorePreview.following to ReelsExplorePreview.followers
    val blocked = if (preview == null) rememberBlockedUids() else emptySet()
    val repo = remember { if (preview == null) runCatching { UmmahRepository() }.getOrNull() else null }
    val uid = remember { repo?.currentUserUid().orEmpty() }
    val hidden = remember(uid) { if (preview == null) ReelFeedbackStore(context.applicationContext, uid).load().hiddenReelIds else emptySet() }
    val recentStore = remember { ReelRecentSearches(context.applicationContext) }
    var recent by remember { mutableStateOf(recentStore.load()) }

    var query by remember { mutableStateOf(ReelsExplorePreview.query) }
    var debounced by remember { mutableStateOf(ReelsExplorePreview.query) }
    var tab by remember { mutableStateOf(ReelsExplorePreview.tab) }
    var openTag by remember { mutableStateOf(ReelsExplorePreview.tag) }
    var serverHits by remember { mutableStateOf<List<UmmahPost>>(emptyList()) }
    var tagHits by remember { mutableStateOf<List<UmmahPost>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(query) { delay(300); debounced = query.trim() }
    LaunchedEffect(debounced) {
        serverHits = emptyList()
        val key = ReelSearchKeys.queryKey(debounced) ?: return@LaunchedEffect
        val r = repo ?: return@LaunchedEffect
        searching = true
        val isTag = debounced.startsWith("#")
        r.searchPublishedPosts(if (isTag) "hashtags" else "searchKeywords", if (isTag) debounced.drop(1).lowercase() else key) { serverHits = it; searching = false }
    }
    LaunchedEffect(openTag) {
        tagHits = emptyList()
        if (openTag.isNotBlank()) repo?.searchPublishedPosts("hashtags", openTag) { tagHits = it }
    }

    fun visible(list: List<UmmahPost>) = list.asSequence()
        .filter { it.opensInVideoViewer() && it.creatorUid !in blocked && it.id !in hidden }
        .filter { it.publicationStatus.isBlank() || it.publicationStatus.equals("published", true) }
        .distinctBy { it.id }.toList()
    val pool = remember(feed.posts, serverHits, blocked, hidden) {
        // Live feed copy wins (fresh counts + live profile overlay); server hits add older matches.
        visible(feed.posts + serverHits.filter { s -> feed.posts.none { it.id == s.id } })
    }
    val explore = remember(pool) { ReelSearchRanker.explore(pool) }
    val trending = remember(pool) { ReelSearchRanker.trending(pool) }
    val reelResults = remember(pool, debounced) { ReelSearchRanker.reels(debounced, pool) }
    val tags = remember(pool, debounced) { ReelSearchRanker.hashtags(debounced, pool) }
    fun commit(q: String) { if (q.isNotBlank()) recent = recentStore.add(q.trim()) }
    val allProfiles = rememberProfilesWithHits(profiles, debounced, preview == null)
    val everyone = remember(allProfiles, pool, graph, blocked) { buildPeople(allProfiles, pool, graph.first, graph.second, blocked) }
    val people = remember(everyone, debounced) { PeopleRanker.search(debounced, everyone, uid) }
    val suggested = remember(everyone) { PeopleRanker.suggested(everyone, uid) }
    val related = remember(reelResults, pool) { ReelSearchRanker.related(reelResults, pool) }
    val relatedSearches = remember(reelResults, debounced) { ReelSearchRanker.relatedSearches(debounced, reelResults) }
    // Explore never looks empty: top reels first, then recent photo/text posts with media as filler.
    val exploreFilled = remember(explore, feed.posts, blocked) {
        if (explore.size >= 18) explore else explore + feed.posts.asSequence()
            .filter { !it.opensInVideoViewer() && it.creatorUid !in blocked && (it.mediaUrl.isNotBlank() || it.thumbnailUrl.isNotBlank()) }
            .filter { p -> explore.none { it.id == p.id } }
            .sortedByDescending { it.likeCount * 3 + it.viewCount + it.publishedAt / 86_400_000L }
            .take(30 - explore.size).toList()
    }
    var viewPost by remember { mutableStateOf<UmmahPost?>(null) }
    viewPost?.let { vp -> SharedPostViewer(vp) { viewPost = null } }
    fun openAny(p: UmmahPost, list: List<UmmahPost>) { if (p.opensInVideoViewer()) { commit(debounced); onOpenReel(p, list.filter { it.opensInVideoViewer() }) } else viewPost = p }
    fun msg(p: PeopleRanker.Person) { commit(debounced); onMessage(p.uid, p.name) }
    fun prof(p: PeopleRanker.Person) { commit(debounced); onOpenProfile(ReelSearchRanker.Account(p.uid, p.name, p.handle, p.photoUrl, p.reels)) }

    fun play(post: UmmahPost, list: List<UmmahPost>) { commit(debounced); onOpenReel(post, list) }

    BackHandler(enabled = openTag.isNotBlank() || query.isNotEmpty()) {
        if (openTag.isNotBlank()) openTag = "" else query = ""
    }

    if (openTag.isNotBlank()) {
        val tagPosts = remember(pool, tagHits, openTag) {
            ReelSearchRanker.explore(visible(pool.filter { openTag in ReelSearchRanker.postHashtags(it) } + tagHits))
        }
        HashtagPage(openTag, tagPosts, onBack = { openTag = "" }, onOpen = { play(it, tagPosts) })
        return
    }

    Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText()) }
            TextField(
                value = query, onValueChange = { query = it.take(80) },
                placeholder = { Text("Search reels, #tags, @people", maxLines = 1) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear") } },
                singleLine = true, shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { commit(query) }),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedContainerColor = stitchSurface(), unfocusedContainerColor = stitchSurface()),
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
        }
        if (debounced.isBlank()) {
            if (recent.isNotEmpty() || trending.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 14.dp)) {
                    if (recent.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("Clear all", color = stitchPrimary(), fontSize = 13.sp, modifier = Modifier.clickable { recent = recentStore.clear() }.padding(6.dp))
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            recent.forEach { r -> ExploreChip(r, Icons.Default.History, onClick = { query = r; debounced = r }, onRemove = { recent = recentStore.remove(r) }) }
                        }
                    }
                    if (trending.isNotEmpty()) {
                        Text("Trending hashtags", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            trending.forEach { t -> ExploreChip("#${t.tag}", Icons.Default.Tag, onClick = { openTag = t.tag }) }
                        }
                    }
                }
            }
            when {
                feed.loading && exploreFilled.isEmpty() -> ShimmerGrid()
                exploreFilled.isEmpty() && suggested.isEmpty() -> EmptyState("Nothing here yet", "Reels and posts from the Ummah will appear here.")
                else -> LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(3), contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp), verticalItemSpacing = 2.dp, modifier = Modifier.fillMaxSize().padding(top = 4.dp)
                ) {
                    if (suggested.isNotEmpty()) item(span = StaggeredGridItemSpan.FullLine) { SuggestedPeopleRow(suggested, ::prof, ::msg) }
                    itemsIndexed(exploreFilled, key = { _, p -> p.id }) { i, p -> ReelTile(p, i) { openAny(p, exploreFilled) } }
                }
            }
            return@Column
        }
        TabRow(selectedTabIndex = tab, containerColor = stitchBackground(), contentColor = stitchPrimary()) {
            TABS.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) }) }
        }
        val nothing = reelResults.isEmpty() && people.isEmpty() && tags.isEmpty()
        when {
            searching && nothing -> ShimmerGrid()
            tab == 0 && nothing || tab == 1 && reelResults.isEmpty() || tab == 2 && people.isEmpty() || tab == 3 && tags.isEmpty() ->
                EmptyState("No results for \"$debounced\"", "Try another spelling, a #hashtag or an @username.")
            tab == 1 -> ReelGrid(reelResults + related) { play(it, reelResults + related) }
            tab == 2 -> LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(people, key = { it.uid }) { p -> PersonRow(p, onOpen = { prof(p) }, onMessage = { msg(p) }) }
            }
            tab == 3 -> LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tags, key = { it.tag }) { t -> TagRow(t) { commit(debounced); openTag = t.tag } }
            }
            else -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(3), contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp), verticalItemSpacing = 2.dp, modifier = Modifier.fillMaxSize()
            ) {
                if (people.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) { Text("People", color = stitchText(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp, 8.dp)) }
                    people.take(3).forEach { p -> item(span = StaggeredGridItemSpan.FullLine, key = "pp_" + p.uid) { Box(Modifier.padding(horizontal = 6.dp)) { PersonRow(p, onOpen = { prof(p) }, onMessage = { msg(p) }) } } }
                }
                if (tags.isNotEmpty()) item(span = StaggeredGridItemSpan.FullLine) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) { tags.take(8).forEach { t -> ExploreChip("#${t.tag} · ${t.count}", Icons.Default.Tag, onClick = { commit(debounced); openTag = t.tag }) } }
                }
                if (reelResults.isNotEmpty()) item(span = StaggeredGridItemSpan.FullLine) { Text("Reels", color = stitchText(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp, 8.dp)) }
                itemsIndexed(reelResults, key = { _, p -> p.id }) { i, p -> ReelTile(p, i) { play(p, reelResults) } }
                if (relatedSearches.isNotEmpty()) item(span = StaggeredGridItemSpan.FullLine) {
                    Column(Modifier.padding(top = 10.dp)) {
                        Text("Related searches", color = stitchText(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp, 4.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                            relatedSearches.forEach { q -> ExploreChip(q, Icons.Default.Search, onClick = { commit(debounced); query = q; debounced = q }) }
                        }
                    }
                }
                if (related.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) { Text("Related reels", color = stitchText(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp, 8.dp)) }
                    itemsIndexed(related, key = { _, p -> "rel_" + p.id }) { i, p -> ReelTile(p, i + 1) { play(p, reelResults + related) } }
                }
            }
        }
    }
}

@Composable
private fun HashtagPage(tag: String, posts: List<UmmahPost>, onBack: () -> Unit, onOpen: (UmmahPost) -> Unit) {
    Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText()) }
            Text("#$tag", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Row(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(StitchEmerald, StitchGold))), contentAlignment = Alignment.Center) {
                Text("#", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text("${compactCount(posts.size.toLong())} ${if (posts.size == 1) "post" else "posts"}", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Reels tagged #$tag", color = stitchMutedText(), fontSize = 13.sp)
            }
        }
        if (posts.isEmpty()) EmptyState("No reels for #$tag yet", "Be the first to use this hashtag.") else ReelGrid(posts, Modifier.padding(top = 8.dp), onOpen)
    }
}

@Composable
private fun ReelGrid(posts: List<UmmahPost>, modifier: Modifier = Modifier, onOpen: (UmmahPost) -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(3), contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalItemSpacing = 2.dp, modifier = modifier.fillMaxSize()
    ) { itemsIndexed(posts, key = { _, p -> p.id }) { i, p -> ReelTile(p, i) { onOpen(p) } } }
}

@Composable
private fun ReelTile(post: UmmahPost, index: Int, onClick: () -> Unit) {
    val tall = index % 5 == 1 || index % 5 == 3
    Box(Modifier.fillMaxWidth().height(if (tall) 250.dp else 165.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black).clickable(onClick = onClick)) {
        val art = ReelsExplorePreview.art[post.id]
        if (art != null) Image(art, post.caption, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) else StitchProfileTileImage(post)
        Box(Modifier.fillMaxWidth().height(44.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))))
        Row(Modifier.align(Alignment.BottomStart).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(compactCount(post.viewCount), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExploreChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    Row(
        Modifier.padding(end = 8.dp, bottom = 6.dp).clip(RoundedCornerShape(999.dp)).background(stitchSurface()).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = stitchPrimary(), modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp))
        Text(text, color = stitchText(), fontSize = 13.sp, maxLines = 1)
        if (onRemove != null) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Close, "Remove", tint = stitchMutedText(), modifier = Modifier.size(14.dp).clickable(onClick = onRemove)) }
    }
}

@Composable
private fun AccountRow(a: ReelSearchRanker.Account, onClick: () -> Unit) {
    StitchSearchUserRow(StitchSearchUser(a.uid, a.name, a.handle, a.photoUrl), onClick)
}

@Composable
private fun TagRow(t: ReelSearchRanker.Tag, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(stitchSurface()).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(stitchSoftSurface()), contentAlignment = Alignment.Center) { Text("#", color = stitchPrimary(), fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("#${t.tag}", color = stitchText(), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${compactCount(t.count.toLong())} ${if (t.count == 1) "post" else "posts"}", color = stitchMutedText(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(top = 80.dp, start = 32.dp, end = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(stitchSoftSurface()), contentAlignment = Alignment.Center) { Icon(Icons.Default.Search, null, tint = stitchPrimary(), modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(body, color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ShimmerGrid() {
    val t = rememberInfiniteTransition(label = "shimmer")
    val a by t.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Fixed(3), contentPadding = PaddingValues(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalItemSpacing = 2.dp) {
        items(12) { i -> Box(Modifier.fillMaxWidth().height(if (i % 5 == 1 || i % 5 == 3) 250.dp else 165.dp).clip(RoundedCornerShape(4.dp)).background(stitchMutedText().copy(alpha = a * 0.3f))) }
    }
}
