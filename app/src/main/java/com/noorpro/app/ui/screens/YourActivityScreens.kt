package com.noorpro.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.ui.components.StitchCard
import com.noorpro.app.ui.components.StitchScreen
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchSoftSurface
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

/** Resolved activity row: live post or an Unavailable placeholder the user can remove. */
internal sealed class ActivityRow {
    abstract val id: String
    data class Available(val post: UmmahPost) : ActivityRow() { override val id: String get() = post.id }
    data class Unavailable(override val id: String) : ActivityRow()
}

internal data class ActivityResolveState(
    val rows: List<ActivityRow> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false
)

/**
 * Lazily resolve liked/saved ids against ummah_posts (chunked whereIn ≤30).
 * Keeps collections and interaction sets as the source of truth for membership.
 */
@Composable
internal fun rememberActivityResolvedRows(
    ids: Collection<String>,
    pageSize: Int = YourActivityLogic.PAGE_SIZE
): Pair<ActivityResolveState, () -> Unit> {
    val repository = remember { UmmahRepository() }
    val orderedIds = remember(ids) { ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct() }
    var found by remember(orderedIds) { mutableStateOf<Map<String, UmmahPost>>(emptyMap()) }
    var offset by remember(orderedIds) { mutableStateOf(0) }
    var loading by remember(orderedIds) { mutableStateOf(true) }
    var loadingMore by remember(orderedIds) { mutableStateOf(false) }
    var requested by remember(orderedIds) { mutableStateOf<Set<String>>(emptySet()) }

    fun loadNext(reset: Boolean) {
        val start = if (reset) 0 else offset
        val page = YourActivityLogic.pageIds(orderedIds, start, pageSize)
        if (page.isEmpty()) {
            loading = false
            loadingMore = false
            return
        }
        if (reset) loading = true else loadingMore = true
        repository.fetchPostsByIds(page) { map ->
            found = if (reset) map else found + map
            requested = if (reset) page.toSet() else requested + page
            offset = start + page.size
            loading = false
            loadingMore = false
        }
    }

    LaunchedEffect(orderedIds) { loadNext(reset = true) }

    val pageIds = orderedIds.take(offset.coerceAtLeast(0))
    val refs = YourActivityLogic.markAvailability(pageIds, found.keys)
    val rows = refs.map { ref ->
        val post = found[ref.id]
        if (ref.available && post != null) ActivityRow.Available(post) else ActivityRow.Unavailable(ref.id)
    }
    val state = ActivityResolveState(
        rows = rows,
        loading = loading,
        loadingMore = loadingMore,
        hasMore = offset < orderedIds.size
    )
    return state to { if (!loadingMore && !loading) loadNext(reset = false) }
}

@Composable
internal fun ActivityUnavailableTile(id: String, onRemove: () -> Unit) {
    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2A3A34), Color(0xFF1A2420))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.HideImage, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Unavailable", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("This post was deleted or is no longer public.", color = stitchMutedText(), fontSize = 12.sp, maxLines = 2)
                Text(id.take(10) + "…", color = stitchMutedText().copy(alpha = 0.7f), fontSize = 10.sp)
            }
            TextButton(onClick = onRemove) {
                Text("Remove", color = Color(0xFFC0392B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun StitchUmmahWatchHistoryScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val store = remember { ReelWatchStore(context.applicationContext) }
    var entries by remember { mutableStateOf(YourActivityLogic.orderWatchHistory(store.all().map {
        WatchProgressEntry(it.postId, it.positionMs, it.durationMs, it.updatedAt)
    })) }
    val ids = entries.map { it.postId }
    val (resolved, loadMore) = rememberActivityResolvedRows(ids)
    val byId = remember(resolved.rows) {
        resolved.rows.mapNotNull { row -> (row as? ActivityRow.Available)?.post?.let { it.id to it } }.toMap()
    }
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear watch history?", color = stitchText(), fontWeight = FontWeight.Bold) },
            text = { Text("Removes resume points stored on this device only.", color = stitchMutedText()) },
            confirmButton = {
                TextButton(onClick = {
                    store.clearAll()
                    entries = emptyList()
                    confirmClear = false
                }) { Text("Clear all", color = Color(0xFFC0392B), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = stitchMutedText()) } },
            containerColor = stitchSurface()
        )
    }

    StitchScreen {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText())
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Watch history", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
                    Spacer(Modifier.weight(1f))
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) {
                            Text("Clear", color = stitchPrimary(), fontWeight = FontWeight.Bold)
                        }
                    } else Spacer(Modifier.size(48.dp))
                }
            }
            item {
                StitchCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(stitchSoftSurface()),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.History, null, tint = stitchPrimary())
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Continue where you left off", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("Saved on this device · ${entries.size} reel${if (entries.size == 1) "" else "s"}", color = stitchMutedText(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            when {
                entries.isEmpty() -> item {
                    StitchUmmahEmptyCard(
                        title = "No watch history yet",
                        message = "Reels you watch will appear here so you can resume later.",
                        action = "Watch Reels",
                        onAction = { viewModel.navigateTo(DeenScreen.REELS) }
                    )
                }
                resolved.loading && resolved.rows.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = stitchPrimary())
                    }
                }
                else -> {
                    items(entries, key = { it.postId }) { entry ->
                        val post = byId[entry.postId]
                        if (post != null) {
                            WatchHistoryCard(
                                post = post,
                                progress = entry,
                                onOpen = { viewModel.openReel(post.id) },
                                onRemove = {
                                    store.remove(entry.postId)
                                    entries = YourActivityLogic.removeWatchEntry(entries, entry.postId)
                                }
                            )
                        } else if (!resolved.loading) {
                            ActivityUnavailableTile(entry.postId) {
                                store.remove(entry.postId)
                                entries = YourActivityLogic.removeWatchEntry(entries, entry.postId)
                            }
                        }
                    }
                    if (resolved.hasMore) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (resolved.loadingMore) CircularProgressIndicator(color = stitchPrimary(), modifier = Modifier.size(28.dp))
                                else TextButton(onClick = loadMore) { Text("Load more", color = stitchPrimary(), fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchHistoryCard(
    post: UmmahPost,
    progress: WatchProgressEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val fraction = if (progress.durationMs <= 0) 0f else (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
    StitchCard(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(stitchSoftSurface())) {
                val thumb = post.thumbnailUrl.ifBlank { post.mediaUrl }
                if (thumb.startsWith("http")) {
                    coil.compose.AsyncImage(thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayCircle, null, tint = stitchPrimary(), modifier = Modifier.size(32.dp))
                    }
                }
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                    color = stitchPrimary(),
                    trackColor = Color.Black.copy(alpha = 0.25f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    post.caption.lineSequence().firstOrNull().orEmpty().ifBlank { post.creatorName },
                    color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${post.creatorName} · ${(fraction * 100).toInt()}% watched",
                    color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Remove", tint = stitchMutedText())
            }
        }
    }
}
