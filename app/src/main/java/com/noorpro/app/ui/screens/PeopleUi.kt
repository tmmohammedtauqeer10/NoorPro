package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahProfile
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.data.search.PeopleRanker
import com.noorpro.app.ui.components.*

/** 1.0.92: my following + followers uid sets (live). */
@Composable
internal fun rememberPeopleGraph(enabled: Boolean): Pair<Set<String>, Set<String>> {
    val repo = remember(enabled) { if (enabled) runCatching { UmmahRepository() }.getOrNull() else null }
    var following by remember(enabled) { mutableStateOf<Set<String>>(emptySet()) }
    var followers by remember(enabled) { mutableStateOf<Set<String>>(emptySet()) }
    DisposableEffect(repo) {
        repo?.observeFollowing { following = it }
        repo?.observeFollowers { l -> followers = l.map { it.uid }.toSet() }
        onDispose { repo?.close() }
    }
    return following to followers
}

internal fun buildPeople(
    profiles: Map<String, UmmahProfile>, posts: List<UmmahPost>, following: Set<String>, followers: Set<String>, blocked: Set<String>
): List<PeopleRanker.Person> {
    val reels = posts.groupingBy { it.creatorUid }.eachCount()
    val views = posts.groupBy { it.creatorUid }.mapValues { e -> e.value.sumOf { it.viewCount } }
    val out = LinkedHashMap<String, PeopleRanker.Person>()
    fun add(uid: String, name: String, handle: String, photo: String) {
        if (uid.isBlank() || uid in blocked || uid in out) return
        out[uid] = PeopleRanker.Person(uid, name.ifBlank { "Member" }, handle, photo, reels[uid] ?: 0, views[uid] ?: 0, uid in following, uid in followers)
    }
    profiles.values.forEach { add(it.uid, it.name, it.handle, it.photoUrl) }
    posts.forEach { add(it.creatorUid, it.creatorName, it.creatorHandle, it.creatorPhotoUrl) }
    return out.values.toList()
}

@Composable
internal fun PersonAvatar(name: String, photoUrl: String, size: androidx.compose.ui.unit.Dp, ring: Boolean = false) {
    Box(
        Modifier.size(size).then(if (ring) Modifier.border(2.dp, NoorRingBrush, CircleShape).padding(3.dp) else Modifier).clip(CircleShape).background(StitchEmerald),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) AsyncImage(photoUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(name.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4f).sp)
    }
}

/** Follow button with optimistic state; writes via UmmahRepository.setFollowing. */
@Composable
internal fun FollowButton(p: PeopleRanker.Person, compact: Boolean = false, onChanged: (Boolean) -> Unit = {}) {
    val repo = remember { runCatching { UmmahRepository() }.getOrNull() }
    var following by remember(p.uid, p.following) { mutableStateOf(p.following) }
    val label = when { following -> "Following"; p.followsMe -> "Follow back"; else -> "Follow" }
    val filled = !following
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (filled) stitchPrimary() else stitchSoftSurface())
            .clickable {
                val next = !following; following = next; onChanged(next)
                repo?.setFollowing(p.uid, p.name, p.handle.ifBlank { "@ummah" }, next) { ok -> if (!ok) following = !next }
            }
            .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (filled) Color.White else stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@Composable
internal fun PersonRow(p: PeopleRanker.Person, onOpen: () -> Unit, onMessage: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onOpen).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonAvatar(p.name, p.photoUrl, 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.handle.removePrefix("@").ifBlank { p.name }, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOf(p.name, p.subtitle).filter { it.isNotBlank() }.joinToString(" · "), color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        FollowButton(p, compact = true)
        IconButton(onClick = onMessage) { Icon(Icons.AutoMirrored.Filled.Send, "Message ${p.name}", tint = stitchPrimary(), modifier = Modifier.size(20.dp)) }
    }
}

@Composable
internal fun SuggestedPeopleRow(people: List<PeopleRanker.Person>, onOpen: (PeopleRanker.Person) -> Unit, onMessage: (PeopleRanker.Person) -> Unit) {
    if (people.isEmpty()) return
    Column {
        Text("Suggested for you", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(people, key = { it.uid }) { p ->
                Column(
                    Modifier.width(140.dp).clip(RoundedCornerShape(16.dp)).background(stitchSurface()).clickable { onOpen(p) }.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PersonAvatar(p.name, p.photoUrl, 64.dp, ring = p.reels > 0)
                    Spacer(Modifier.height(8.dp))
                    Text(p.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(p.subtitle.ifBlank { "@" + p.handle.removePrefix("@") }, color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    FollowButton(p, compact = true)
                }
            }
        }
    }
}

/** Profiles matching [query] from the server index, merged on top of the live [base] map. */
@Composable
internal fun rememberProfilesWithHits(base: Map<String, UmmahProfile>, query: String, enabled: Boolean = true): Map<String, UmmahProfile> {
    val repo = remember(enabled) { if (enabled) runCatching { UmmahRepository() }.getOrNull() else null }
    var hits by remember { mutableStateOf<List<UmmahProfile>>(emptyList()) }
    LaunchedEffect(query) {
        hits = emptyList()
        val key = com.noorpro.app.data.search.ReelSearchKeys.key(query.trim().trimStart('@'))
        if (key.isNotEmpty()) repo?.searchProfiles(key) { hits = it }
    }
    return remember(base, hits) { if (hits.isEmpty()) base else base + hits.filter { it.uid !in base }.associateBy { it.uid } }
}
