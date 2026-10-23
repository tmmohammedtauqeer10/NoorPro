package com.noorpro.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import com.noorpro.app.data.StoryRepository
import com.noorpro.app.data.UmmahMessage
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A person you can send a reel/post to. */
data class SharePerson(val uid: String, val name: String, val handle: String, val photoUrl: String, val recent: Boolean)

@Composable
internal fun ShareAvatar(name: String, photoUrl: String, size: Dp, selected: Boolean = false) {
    Box(Modifier.size(size)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFFC9A227)))), contentAlignment = Alignment.Center) {
            if (photoUrl.startsWith("https://")) AsyncImage(photoUrl, name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(name.trim().take(1).uppercase().ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value / 2.6f).sp)
        }
        val sc by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.45f), label = "chk")
        if (sc > 0.01f) Box(Modifier.align(Alignment.BottomEnd).size(size / 3.2f).graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(CircleShape).background(Color(0xFF0095F6)).border(2.dp, stitchSurface(), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.fillMaxSize(0.7f))
        }
    }
}

/** Stateless Instagram-style share sheet body (preview friendly). */
@Composable
internal fun ShareSheetContent(
    post: UmmahPost, people: List<SharePerson>, query: String, onQuery: (String) -> Unit,
    selected: Set<String>, onToggle: (String) -> Unit, note: String, onNote: (String) -> Unit,
    sending: Boolean, sent: Boolean, onSend: () -> Unit,
    onCopyLink: () -> Unit, onShareOut: () -> Unit, onAddStory: () -> Unit,
) {
    val shown = remember(people, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) people else people.filter { q in it.name.lowercase() || q in it.handle.lowercase() }
    }
    Column(Modifier.fillMaxWidth().background(stitchSurface()).navigationBarsPadding()) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).size(40.dp, 4.dp).clip(CircleShape).background(stitchMutedText().copy(alpha = 0.4f)))
        Row(Modifier.padding(16.dp, 12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(stitchSoftSurface()).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = stitchMutedText(), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search", color = stitchMutedText(), fontSize = 15.sp)
                BasicTextField(query, onQuery, singleLine = true, textStyle = LocalTextStyle.current.copy(color = stitchText(), fontSize = 15.sp),
                    cursorBrush = SolidColor(stitchPrimary()), modifier = Modifier.fillMaxWidth())
            }
        }
        if (shown.isEmpty()) {
            Text(if (people.isEmpty()) "Follow members or start a chat to share here." else "No matches", color = stitchMutedText(),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(36.dp))
        } else LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxWidth().heightIn(max = 330.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
            items(shown, key = { it.uid }) { p ->
                val on = p.uid in selected
                Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = !sent) { onToggle(p.uid) }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ShareAvatar(p.name, p.photoUrl, 68.dp, on)
                    Spacer(Modifier.height(6.dp))
                    Text(p.name, color = stitchText(), fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
                    Text(if (p.recent) "Recent" else p.handle, color = stitchMutedText(), fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        HorizontalDivider(color = stitchMutedText().copy(alpha = 0.15f))
        AnimatedContent(selected.isNotEmpty() || sent, label = "send-row") { picking ->
            if (picking) Column(Modifier.fillMaxWidth().padding(16.dp, 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp, 52.dp).clip(RoundedCornerShape(6.dp)).background(stitchSoftSurface())) {
                        if (post.thumbnailUrl.startsWith("https://")) AsyncImage(post.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (note.isEmpty()) Text("Write a message…", color = stitchMutedText(), fontSize = 14.sp)
                        BasicTextField(note, { if (it.length <= 500) onNote(it) }, enabled = !sent, textStyle = LocalTextStyle.current.copy(color = stitchText(), fontSize = 14.sp),
                            cursorBrush = SolidColor(stitchPrimary()), modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(10.dp))
                val bg by animateColorAsState(if (sent) Color(0xFF22A06B) else Color(0xFF0095F6), tween(300), label = "sb")
                val press by animateFloatAsState(if (sending) 0.96f else 1f, spring(dampingRatio = 0.5f), label = "sp")
                Box(Modifier.fillMaxWidth().height(46.dp).graphicsLayer { scaleX = press; scaleY = press }.clip(RoundedCornerShape(10.dp)).background(bg)
                    .clickable(enabled = !sending && !sent, onClick = onSend), contentAlignment = Alignment.Center) {
                    AnimatedContent(Triple(sending, sent, selected.size), transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) }, label = "st") { (sn, st, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when {
                                st -> { Icon(Icons.Default.Check, null, tint = Color.White); Spacer(Modifier.width(6.dp)); Text("Sent", color = Color.White, fontWeight = FontWeight.Bold) }
                                sn -> { val inf = rememberInfiniteTransition(label = "fly"); val x by inf.animateFloat(-10f, 14f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "x")
                                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.graphicsLayer { translationX = x; rotationZ = -20f }); Spacer(Modifier.width(6.dp)); Text("Sending…", color = Color.White, fontWeight = FontWeight.Bold) }
                                else -> Text(if (n > 1) "Send separately" else "Send", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ShareAction(Icons.Default.Link, "Copy link", onCopyLink)
                ShareAction(Icons.Default.Share, "Share to…", onShareOut)
                ShareAction(Icons.Default.AddCircleOutline, "Add to story", onAddStory)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ShareAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(stitchSoftSurface()), contentAlignment = Alignment.Center) { Icon(icon, label, tint = stitchText()) }
        Spacer(Modifier.height(6.dp)); Text(label, color = stitchText(), fontSize = 12.sp)
    }
}

/** App-wide host: shows the share sheet while [DeenViewModel.sharePost] is set. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToChatHost(viewModel: DeenViewModel) {
    val post = viewModel.sharePost ?: return
    val context = LocalContext.current
    val repo = remember { UmmahRepository() }
    val me = remember { repo.currentUserUid().orEmpty() }
    val blocked = rememberBlockedUids()
    var chats by remember { mutableStateOf(emptyList<com.noorpro.app.data.UmmahChat>()) }
    var following by remember { mutableStateOf(emptyList<com.noorpro.app.data.UmmahFollowUser>()) }
    var profiles by remember { mutableStateOf(emptyMap<String, com.noorpro.app.data.UmmahProfile>()) }
    DisposableEffect(Unit) {
        runCatching { repo.observeChats { c, _ -> chats = c }; repo.observeFollowingUsers { following = it }; repo.observeProfiles { profiles = it } }
        onDispose { runCatching { repo.close() } }
    }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var note by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val people = remember(chats, following, profiles, blocked, query, me) {
        val out = LinkedHashMap<String, SharePerson>()
        fun add(uid: String, name: String, handle: String, recent: Boolean) {
            if (uid.isBlank() || uid == me || uid in blocked || out.containsKey(uid)) return
            val p = profiles[uid]
            out[uid] = SharePerson(uid, p?.name?.ifBlank { null } ?: name.ifBlank { "Member" }, p?.handle?.ifBlank { null } ?: handle, p?.photoUrl.orEmpty(), recent)
        }
        chats.sortedByDescending { it.updatedAt }.forEach { c -> val o = c.participantUids.firstOrNull { it != me }.orEmpty(); add(o, c.participantNames[o].orEmpty(), "", true) }
        following.forEach { add(it.uid, it.name, it.handle, false) }
        if (query.isNotBlank()) profiles.values.forEach { add(it.uid, it.name, it.handle, false) }
        out.values.toList()
    }
    val close = { viewModel.sharePost = null }
    ModalBottomSheet(onDismissRequest = close, containerColor = stitchSurface(), dragHandle = null) {
        ShareSheetContent(post, people, query, { query = it }, selected, { uid -> selected = if (uid in selected) selected - uid else selected + uid },
            note, { note = it }, sending, sent,
            onSend = {
                if (me.isBlank()) { Toast.makeText(context, "Sign in to share", Toast.LENGTH_SHORT).show(); return@ShareSheetContent }
                sending = true
                val targets = people.filter { it.uid in selected }.map { it.uid to it.name }
                repo.sendPostShare(post, targets, note) { ok, err ->
                    sending = false
                    if (ok > 0) { sent = true; viewModel.shareSentCount++; scope.launch { delay(900); close() } }
                    else Toast.makeText(context, err ?: "Couldn't send", Toast.LENGTH_SHORT).show()
                }
            },
            onCopyLink = { copyStitchReelLink(context, post.id) },
            onShareOut = { shareStitchUmmahPost(context, post, repo); close() },
            onAddStory = {
                val thumb = post.thumbnailUrl.ifBlank { post.mediaUrls.firstOrNull().orEmpty() }
                if (me.isBlank() || !thumb.startsWith("https://")) { Toast.makeText(context, "This post can't be added to a story", Toast.LENGTH_SHORT).show(); return@ShareSheetContent }
                val mine = profiles[me]
                Toast.makeText(context, "Adding to your story…", Toast.LENGTH_SHORT).show()
                scope.launch {
                    val file = withContext(Dispatchers.IO) { runCatching {
                        val f = java.io.File(context.cacheDir, "story_share_${System.currentTimeMillis()}.jpg")
                        java.net.URL(thumb).openStream().use { i -> f.outputStream().use { o -> i.copyTo(o) } }; f }.getOrNull() }
                    if (file == null) { Toast.makeText(context, "Couldn't load the cover", Toast.LENGTH_SHORT).show(); return@launch }
                    StoryRepository().createImageStory(context, android.net.Uri.fromFile(file),
                        "▶ ${if (post.type == "reel") "Reel" else "Post"} by ${post.creatorName}".take(300),
                        mine?.name ?: "Member", mine?.handle.orEmpty(), mine?.photoUrl.orEmpty(), {}) { _, msg ->
                        file.delete(); Toast.makeText(context, msg ?: "Done", Toast.LENGTH_SHORT).show() }
                }
                close()
            })
    }
}

/** Rich preview card for a "post_share" chat message. */
@Composable
internal fun SharedPostCard(message: UmmahMessage, blocked: Set<String>, onOpen: (UmmahPost) -> Unit) {
    val repo = remember { UmmahRepository() }
    var state by remember(message.sharedPostId) { mutableStateOf<Int>(0) } // 0 loading, 1 ok, 2 gone
    var post by remember(message.sharedPostId) { mutableStateOf<UmmahPost?>(null) }
    LaunchedEffect(message.sharedPostId) {
        SharedPostCache.get(message.sharedPostId)?.let { post = it.orNull; state = if (it.orNull != null) 1 else 2; return@LaunchedEffect }
        repo.fetchPublishedPost(message.sharedPostId) { p -> SharedPostCache.put(message.sharedPostId, p); post = p; state = if (p != null) 1 else 2 }
    }
    val creatorBlocked = message.sharedCreatorUid in blocked || (post?.creatorUid?.let { it in blocked } == true)
    SharedPostCardContent(message, unavailable = state == 2 || creatorBlocked, onClick = { post?.takeIf { state == 1 && !creatorBlocked }?.let(onOpen) })
}

internal object SharedPostCache {
    class Entry(val orNull: UmmahPost?)
    private val map = java.util.concurrent.ConcurrentHashMap<String, Entry>()
    fun get(id: String) = map[id]
    fun put(id: String, p: UmmahPost?) { map[id] = Entry(p) }
}

@Composable
internal fun SharedPostCardContent(message: UmmahMessage, unavailable: Boolean, onClick: () -> Unit) {
    val reel = message.sharedPostKind != "post"
    Column(Modifier.width(220.dp).clip(RoundedCornerShape(14.dp)).background(stitchSoftSurface()).clickable(enabled = !unavailable, onClick = onClick)) {
        if (unavailable) {
            Column(Modifier.fillMaxWidth().height(120.dp).padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HideImage, null, tint = stitchMutedText())
                Spacer(Modifier.height(6.dp))
                Text(if (reel) "Reel unavailable" else "Post unavailable", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("It may have been deleted or the creator is blocked.", color = stitchMutedText(), fontSize = 11.sp, textAlign = TextAlign.Center)
            }
            return
        }
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ShareAvatar(message.sharedCreatorName, message.sharedCreatorPhoto, 26.dp)
            Spacer(Modifier.width(8.dp))
            Text(message.sharedCreatorName.ifBlank { "Noor Pro creator" }, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // 1.0.83: use the thumbnail's real aspect (clamped) and a sharp decode instead of a fixed crop.
        var ar by androidx.compose.runtime.remember(message.sharedThumb) { androidx.compose.runtime.mutableFloatStateOf(ChatMediaAspect.get(message.sharedThumb) ?: if (reel) 9f / 14f else 1f) }
        Box(Modifier.fillMaxWidth().aspectRatio(ar.coerceIn(0.56f, 1.6f)).background(Color(0xFF1B2A2A))) {
            if (message.sharedThumb.startsWith("https://")) AsyncImage(
                coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(message.sharedThumb).size(1080).crossfade(true).build(), null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                onSuccess = { st -> val z = st.painter.intrinsicSize; if (z.width > 0f && z.height > 0f) { ChatMediaAspect.put(message.sharedThumb, z.width / z.height); ar = z.width / z.height } })
            else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF14213D)))))
            if (reel) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.align(Alignment.Center).size(54.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).padding(8.dp))
                Icon(Icons.Default.SmartDisplay, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp))
            }
        }
        if (message.sharedCaption.isNotBlank()) Text(buildString { append(message.sharedCreatorName.substringBefore(' ')); append("  "); append(message.sharedCaption) },
            color = stitchText(), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(10.dp))
    }
}

/** Full-screen viewer for a shared Ummah (non-video) post. Back returns to the chat. */
@Composable
internal fun SharedPostViewer(post: UmmahPost, onDismiss: () -> Unit) {
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler(onBack = onDismiss)
        Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = stitchText()) }
                ShareAvatar(post.creatorName, post.creatorPhotoUrl, 32.dp); Spacer(Modifier.width(8.dp))
                Column { Text(post.creatorName, color = stitchText(), fontWeight = FontWeight.Bold); Text(post.creatorHandle, color = stitchMutedText(), fontSize = 12.sp) }
            }
            val img = post.mediaUrls.firstOrNull() ?: post.mediaUrl.ifBlank { post.thumbnailUrl }
            if (img.startsWith("https://")) ZoomableChatImage(img, null, modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 520.dp))
            Text(post.caption.ifBlank { post.arabicText }, color = stitchText(), fontSize = 15.sp, modifier = Modifier.padding(16.dp))
        }
    }
}
