package com.noorpro.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahProfile
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon

/*
 * 1.1.5 NoorPro Profile v2 — stateless UI. NoorProfile2Screen.kt wires it to Firestore / the view
 * model; V109 render tests drive it with sample data. Original NoorPro style (green, cream, gold,
 * Libre Caslon headings, mint pill tabs).
 */

data class Profile2Features(
    val deenCard: Boolean = Reels2Flags.isOn(Reels2Flags.DEEN_CARD),
    val extendedFields: Boolean = Reels2Flags.isOn(Reels2Flags.PROFILE_FIELDS),
    val highlights: Boolean = Reels2Flags.isOn(Reels2Flags.HIGHLIGHTS),
    val closeFriends: Boolean = Reels2Flags.isOn(Reels2Flags.CLOSE_FRIENDS),
    val aiRecap: Boolean = Reels2Flags.isOn(Reels2Flags.AI_RECAP),
    val series: Boolean = Reels2Flags.isOn(Reels2Flags.SERIES),
) { companion object { val ALL_ON = Profile2Features(true, true, true, true, true, true) } }

data class DeenCardUi(val streakDays: Int, val learning: String, val khatmPercent: Int, val hidden: Boolean = false)

data class Profile2Ui(
    val profile: UmmahProfile,
    val own: Boolean,
    val following: Boolean = false,
    val stats: List<Profile2Stat> = emptyList(),
    val hasStory: Boolean = false,
    val city: String = "",
    val learning: String = "",
    val tabs: List<Profile2Tab> = profile2Tabs(own, false),
    val tab: Profile2Tab = Profile2Tab.POSTS,
    val grid: List<UmmahPost> = emptyList(),
    val series: List<Pair<String, List<UmmahPost>>> = emptyList(),
    val locked: Boolean = false,
    val highlights: List<String> = emptyList(),
    val deenCard: DeenCardUi? = null,
    val recap: DeenRecap? = null,
    val totalViews: Long = 0,
    val loading: Boolean = false,
    val thumbs: Map<String, ImageBitmap> = emptyMap(),
    val avatar: ImageBitmap? = null,
    val accent: Color = R2Green,
    val cover: ImageBitmap? = null,
) {
    val bio: BioParts get() = splitBioLink(profile.bio)
    val handle: String get() = "@" + normalizeUsername(profile.handle).ifBlank { "member" }
}

class Profile2Actions(
    val onBack: () -> Unit = {},
    val onMenu: () -> Unit = {},
    val onQr: () -> Unit = {},
    val onStat: (String) -> Unit = {},
    val onFollow: () -> Unit = {},
    val onMessage: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onStudio: () -> Unit = {},
    val onTab: (Profile2Tab) -> Unit = {},
    val onOpen: (UmmahPost) -> Unit = {},
    val onLink: (String) -> Unit = {},
    val onHighlight: (String) -> Unit = {},
    val onToggleDeenCard: () -> Unit = {},
    val onAvatar: () -> Unit = {},
    val onCreate: () -> Unit = {},
)

@Composable
private fun p2Card() = if (isStitchLight()) Color.White else stitchSurface()

@Composable
fun NoorProfile2(ui: Profile2Ui, a: Profile2Actions, f: Profile2Features = Profile2Features(), modifier: Modifier = Modifier) {
    val accent = if (f.extendedFields) ui.accent else R2Green
    Column(
        modifier
            .fillMaxSize()
            .background(stitchBackground())
            .verticalScroll(rememberScrollState())
            .testTag("profile2")
    ) {
        // ---- cover + top bar
        Box(Modifier.fillMaxWidth().height(if (f.extendedFields) 150.dp else 104.dp)) {
            if (f.extendedFields && ui.cover != null) Image(ui.cover, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            else Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.78f), R2GoldDeep.copy(alpha = 0.55f)))))
            // subtle geometric arcs (NoorPro motif, not a photo)
            Box(Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp).size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)))
            Box(Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp).size(90.dp).clip(CircleShape).background(R2Gold.copy(alpha = 0.12f)))
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TopIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back", a.onBack)
                Spacer(Modifier.weight(1f))
                Text(ui.handle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                TopIcon(Icons.Default.QrCode2, "Profile QR", a.onQr)
                Spacer(Modifier.width(6.dp))
                TopIcon(if (ui.own) Icons.Outlined.Lock else Icons.Default.MoreVert, if (ui.own) "Privacy" else "More", a.onMenu)
            }
        }
        // ---- avatar row (overlaps the cover)
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp).offset(y = (-38).dp), verticalAlignment = Alignment.Bottom) {
            Box(
                Modifier.size(92.dp).clip(CircleShape)
                    .background(if (ui.hasStory) Brush.sweepGradient(listOf(R2Gold, accent, R2Gold)) else Brush.linearGradient(listOf(stitchBackground(), stitchBackground())))
                    .padding(3.dp).clip(CircleShape).background(stitchBackground()).padding(3.dp).clip(CircleShape).background(accent)
                    .clickable(onClick = a.onAvatar).testTag("profile2_avatar"),
                contentAlignment = Alignment.Center,
            ) {
                if (ui.avatar != null) Image(ui.avatar, "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else {
                    Text(ui.profile.name.take(1).uppercase().ifBlank { "N" }, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
                    if (ui.profile.photoUrl.isNotBlank() && LocalReels2RemoteImages.current) coil.compose.AsyncImage(ui.profile.photoUrl, "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ui.profile.name.ifBlank { "NoorPro member" }, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (ui.profile.verified) {
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Default.Verified, "Verified", tint = R2Gold, modifier = Modifier.size(19.dp).testTag("profile2_verified"))
                    }
                }
                Text(ui.handle, color = accentText(accent), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.fillMaxWidth().offset(y = (-26).dp).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ---- bio, link, city, learning
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val bio = ui.bio
                if (bio.text.isNotBlank()) Text(bio.text, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
                else if (ui.own) Text("Add a short bio so the Ummah knows you.", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.clickable(onClick = a.onEdit))
                bio.link?.let { link ->
                    Row(Modifier.clip(RoundedCornerShape(50)).background(R2Mint).clickable { a.onLink(linkHref(link)) }.padding(horizontal = 10.dp, vertical = 5.dp).testTag("profile2_link"), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, tint = R2Green, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(link.removePrefix("https://").removePrefix("http://"), color = R2Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (f.extendedFields && (ui.city.isNotBlank() || ui.learning.isNotBlank())) Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (ui.city.isNotBlank()) MetaLine(Icons.Outlined.LocationOn, ui.city)
                    if (ui.learning.isNotBlank()) MetaLine(Icons.AutoMirrored.Filled.MenuBook, "Learning: ${ui.learning}")
                }
            }
            // ---- stats
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p2Card()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).padding(vertical = 10.dp)) {
                ui.stats.forEachIndexed { i, s ->
                    if (i > 0) Box(Modifier.width(1.dp).height(34.dp).align(Alignment.CenterVertically).background(stitchLine()))
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(enabled = !s.hidden) { a.onStat(s.key) }.padding(vertical = 2.dp).testTag("profile2_stat_${s.key}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (s.key == "streak") { Icon(Icons.Default.LocalFireDepartment, null, tint = R2GoldDeep, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(2.dp)) }
                            Text(s.value, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = LibreCaslon)
                        }
                        Text(s.label, color = stitchMutedText(), fontSize = 11.sp)
                    }
                }
            }
            // ---- buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (ui.own) {
                    P2Button("Edit profile", Icons.Outlined.Edit, primary = false, modifier = Modifier.weight(1f), tag = "profile2_edit", onClick = a.onEdit)
                    P2Button("Share", Icons.Outlined.Share, primary = false, modifier = Modifier.weight(1f), tag = "profile2_share", onClick = a.onShare)
                    P2Button("Studio", Icons.Default.AutoGraph, primary = true, modifier = Modifier.weight(1f), tag = "profile2_studio", onClick = a.onStudio)
                } else {
                    P2Button(if (ui.following) "Following" else "Follow", if (ui.following) Icons.Default.Check else Icons.Default.PersonAdd, primary = !ui.following, modifier = Modifier.weight(1.3f), tag = "profile2_follow", onClick = a.onFollow)
                    P2Button("Message", Icons.Outlined.ChatBubbleOutline, primary = false, modifier = Modifier.weight(1f), tag = "profile2_message", onClick = a.onMessage)
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(p2Card()).border(1.dp, stitchLine(), RoundedCornerShape(14.dp)).clickable(onClick = a.onShare), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Share, "Share profile", tint = stitchText(), modifier = Modifier.size(19.dp))
                    }
                }
            }
            // ---- creator studio AI link (own)
            if (ui.own) Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(R2Green, Color(0xFF084A33)))).clickable(onClick = a.onStudio).padding(horizontal = 14.dp, vertical = 12.dp).testTag("profile2_ai_helper"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(R2Gold), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = R2Green, modifier = Modifier.size(19.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Noor AI caption & hashtag helper", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("In Creator Studio · ${reelCount(ui.totalViews)} total views", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
            }
            // ---- highlights (flag)
            if (f.highlights && ui.highlights.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (ui.own) item { Highlight("New", Icons.Default.Add, a.onCreate) }
                items(ui.highlights) { h -> Highlight(h, highlightIcon(h)) { a.onHighlight(h) } }
            }
            // ---- deen card (flag)
            if (f.deenCard) ui.deenCard?.let { DeenCard(it, ui.own, a.onToggleDeenCard) }
            // ---- weekly recap (flag, own)
            if (f.aiRecap && ui.own) ui.recap?.let { RecapCard(it) }
            // ---- tabs
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(if (isStitchLight()) R2Mint else Color(0xFF15302A)).padding(4.dp)) {
                ui.tabs.forEach { t ->
                    val on = t == ui.tab
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (on) R2Green else Color.Transparent).clickable { a.onTab(t) }.padding(vertical = 8.dp).testTag("profile2_tab_${t.name.lowercase()}"),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (t == Profile2Tab.SAVED) { Icon(Icons.Default.Lock, null, tint = if (on) Color.White else stitchMutedText(), modifier = Modifier.size(11.dp)); Spacer(Modifier.width(3.dp)) }
                        Text(t.label, color = if (on) Color.White else if (isStitchLight()) R2Green else stitchText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            // ---- content
            when {
                ui.locked -> LockedCard(ui.handle)
                ui.loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = R2Green) }
                ui.tab == Profile2Tab.SERIES -> SeriesList(ui.series, ui.thumbs, a.onOpen)
                else -> {
                    if (ui.tab == Profile2Tab.SAVED) Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = stitchMutedText(), modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp))
                        Text("Only you can see what you've saved", color = stitchMutedText(), fontSize = 12.sp)
                    }
                    if (ui.grid.isEmpty()) EmptyGrid(ui.tab, ui.own, a.onCreate) else ProfileGrid(ui.grid, ui.thumbs, a.onOpen)
                }
            }
            Spacer(Modifier.height(90.dp))
        }
    }
}

@Composable private fun accentText(accent: Color) = if (isStitchLight()) accent else R2Gold

@Composable
private fun TopIcon(icon: ImageVector, cd: String, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.18f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, cd, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MetaLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = stitchMutedText(), modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
        Text(text, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun P2Button(label: String, icon: ImageVector, primary: Boolean, modifier: Modifier, tag: String, onClick: () -> Unit) {
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(14.dp))
            .background(if (primary) R2Green else p2Card())
            .then(if (primary) Modifier else Modifier.border(1.dp, stitchLine(), RoundedCornerShape(14.dp)))
            .clickable(onClick = onClick).testTag(tag),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (primary) Color.White else stitchText(), modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (primary) Color.White else stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

private fun highlightIcon(h: String): ImageVector = when {
    h.contains("quran", true) -> Icons.AutoMirrored.Filled.MenuBook
    h.contains("hajj", true) || h.contains("umrah", true) -> Icons.Default.Mosque
    h.contains("ramadan", true) -> Icons.Default.NightsStay
    else -> Icons.Default.AutoAwesome
}

@Composable
private fun Highlight(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(66.dp).clickable(onClick = onClick)) {
        Box(Modifier.size(58.dp).clip(CircleShape).border(2.dp, R2Gold, CircleShape).padding(4.dp).clip(CircleShape).background(R2Mint), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = R2Green, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = stitchText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DeenCard(d: DeenCardUi, own: Boolean, onToggle: () -> Unit) {
    if (d.hidden && !own) return
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(R2Cream).border(1.dp, R2Gold.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(14.dp).testTag("profile2_deen_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Deen card", color = R2Green, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            if (own) Row(Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onToggle).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (d.hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = R2GoldDeep, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (d.hidden) "Hidden" else "Public", color = R2GoldDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DeenStat("${d.streakDays}d", "Quran streak", Modifier.weight(1f))
            DeenStat("${d.khatmPercent}%", "Khatm", Modifier.weight(1f))
            DeenStat(d.learning.ifBlank { "—" }, "Learning", Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun DeenStat(v: String, l: String, m: Modifier) {
    Column(m.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(v, color = R2Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(l, color = Color(0xFF5B6B63), fontSize = 10.sp)
    }
}

@Composable
private fun RecapCard(r: DeenRecap) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF2B2410), Color(0xFF4A3B12)))).padding(14.dp).testTag("profile2_recap"), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AutoAwesome, null, tint = R2Gold, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Your week in deen", color = R2Gold, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(r.headline, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
            Text("${r.streakDays}-day streak · ${r.postsThisWeek} posts · ${r.reelsWatched} reels", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun LockedCard(handle: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p2Card()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).padding(24.dp).testTag("profile2_locked"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(54.dp).clip(CircleShape).border(2.dp, R2Gold, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, null, tint = R2Green, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.height(10.dp))
        Text("This account is private", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Follow $handle to see their posts and reels.", color = stitchMutedText(), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmptyGrid(tab: Profile2Tab, own: Boolean, onCreate: () -> Unit) {
    val msg = when (tab) {
        Profile2Tab.SAVED -> "Posts and reels you save appear here."
        Profile2Tab.TAGGED -> if (own) "When someone mentions you, it shows here." else "No tagged posts yet."
        Profile2Tab.REELS -> if (own) "Share your first reel with the Ummah." else "No reels yet."
        else -> if (own) "Share your first reminder." else "No posts yet."
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.PhotoLibrary, null, tint = stitchMutedText(), modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(8.dp))
        Text(msg, color = stitchMutedText(), fontSize = 13.sp)
        if (own && (tab == Profile2Tab.POSTS || tab == Profile2Tab.REELS)) {
            Spacer(Modifier.height(10.dp))
            Text("Create", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(50)).background(R2Green).clickable(onClick = onCreate).padding(horizontal = 18.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun ProfileGrid(posts: List<UmmahPost>, thumbs: Map<String, ImageBitmap>, onOpen: (UmmahPost) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("profile2_grid")) {
        posts.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { p -> GridTile(p, thumbs[p.id], Modifier.weight(1f)) { onOpen(p) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GridTile(p: UmmahPost, bmp: ImageBitmap?, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.aspectRatio(0.8f).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)) {
        if (bmp == null && p.previewModel() == null) {
            Box(Modifier.matchParentSize().background(R2Cream).padding(8.dp)) {
                Text(p.caption.take(80), color = R2Green, fontFamily = LibreCaslon, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
            }
        } else Reels2Thumb(p, bmp, Modifier.matchParentSize())
        if (p.isReelLike()) Row(Modifier.align(Alignment.BottomStart).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text(reelCount(p.viewCount), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        if (p.pinnedToProfile) Icon(Icons.Default.PushPin, "Pinned", tint = R2Gold, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(14.dp))
    }
}

@Composable
private fun SeriesList(series: List<Pair<String, List<UmmahPost>>>, thumbs: Map<String, ImageBitmap>, onOpen: (UmmahPost) -> Unit) {
    if (series.isEmpty()) { EmptyGrid(Profile2Tab.SERIES, false) {}; return }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        series.forEach { (title, parts) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p2Card()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).clickable { onOpen(parts.first()) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Reels2Thumb(parts.first(), thumbs[parts.first().id], Modifier.size(54.dp, 68.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${parts.size} part${if (parts.size == 1) "" else "s"}", color = stitchMutedText(), fontSize = 12.sp)
                }
                Icon(Icons.Default.PlaylistPlay, null, tint = R2Green)
            }
        }
    }
}

// ================================================================ sheets

@Composable
fun Profile2QrCard(name: String, handle: String, url: String, qr: ImageBitmap?, onShare: () -> Unit = {}, onCopy: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(20.dp).testTag("profile2_qr"), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(R2Green, Color(0xFF084A33)))).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("NoorPro", color = R2Gold, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(210.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(12.dp), contentAlignment = Alignment.Center) {
                if (qr != null) Image(qr, "QR code for $handle", modifier = Modifier.fillMaxSize())
                else Icon(Icons.Default.QrCode2, null, tint = R2Green, modifier = Modifier.size(120.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(name, color = Color.White, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text(handle, color = R2Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(14.dp))
        Text(url, color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            P2Button("Copy link", Icons.Outlined.ContentCopy, false, Modifier.weight(1f), "profile2_qr_copy", onCopy)
            P2Button("Share", Icons.Outlined.Share, true, Modifier.weight(1f), "profile2_qr_share", onShare)
        }
    }
}

@Composable
fun Profile2PrivacySheet(
    private: Boolean, hideCounts: Boolean, hideDeen: Boolean, closeFriends: Int, f: Profile2Features,
    onPrivate: (Boolean) -> Unit, onHideCounts: (Boolean) -> Unit, onHideDeen: (Boolean) -> Unit, onCloseFriends: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Reels2SheetFrame("Privacy", "Choose who sees what on your profile", modifier.testTag("profile2_privacy")) {
        PrivacyRow(Icons.Outlined.Lock, "Private account", "Only followers see your posts and reels on your profile", private, onPrivate)
        PrivacyRow(Icons.Outlined.VisibilityOff, "Hide follower counts", "Others see — instead of your numbers", hideCounts, onHideCounts)
        if (f.deenCard) PrivacyRow(Icons.AutoMirrored.Filled.MenuBook, "Hide Deen card", "Keep your streak and learning private", hideDeen, onHideDeen)
        if (f.closeFriends) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onCloseFriends).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(R2Gold.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = R2GoldDeep, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Close friends", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("$closeFriends people · share stories with a smaller circle", color = stitchMutedText(), fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = stitchMutedText())
        }
    }
}

@Composable
private fun PrivacyRow(icon: ImageVector, title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(R2Mint), contentAlignment = Alignment.Center) { Icon(icon, null, tint = R2Green, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(sub, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = R2Green, checkedThumbColor = Color.White))
    }
}

val Profile2Accents = listOf(Color(0xFF0B6243), Color(0xFF1E4D7A), Color(0xFF7A4B1E), Color(0xFF5B2D6E), Color(0xFF2E2E2E))

/** Edit form: photo, name, username (live availability), bio with Noor AI suggestions; cover/accent behind a flag. */
@Composable
fun Profile2EditForm(
    name: String, onName: (String) -> Unit,
    username: String, onUsername: (String) -> Unit, status: UsernameStatus,
    bio: String, onBio: (String) -> Unit,
    suggestions: List<String>, aiLoading: Boolean, aiError: String?, onAskAi: () -> Unit, onPick: (String) -> Unit,
    avatar: ImageBitmap?, photoUrl: String, onPhoto: () -> Unit,
    accent: Color, onAccent: (Color) -> Unit, onCover: () -> Unit,
    f: Profile2Features, saving: Boolean, onSave: () -> Unit, onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(stitchBackground()).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp).testTag("profile2_edit_form")) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText(), modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(8.dp))
            Text("Edit profile", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 21.sp, modifier = Modifier.weight(1f).padding(start = 6.dp))
            val canSave = status.ok && name.isNotBlank() && !saving
            Text(if (saving) "Saving…" else "Save", color = if (canSave) Color.White else stitchMutedText(), fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (canSave) R2Green else stitchLine()).clickable(enabled = canSave, onClick = onSave).padding(horizontal = 16.dp, vertical = 8.dp).testTag("profile2_save"))
        }
        if (f.extendedFields) {
            Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f)))).clickable(onClick = onCover), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Image, null, tint = Color.White); Spacer(Modifier.width(6.dp)); Text("Change cover", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Accent", color = stitchMutedText(), fontSize = 12.sp)
                Profile2Accents.forEach { c ->
                    Box(Modifier.size(28.dp).clip(CircleShape).background(c).border(if (c == accent) 3.dp else 0.dp, R2Gold, CircleShape).clickable { onAccent(c) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp), contentAlignment = Alignment.BottomEnd) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(R2Green).clickable(onClick = onPhoto), contentAlignment = Alignment.Center) {
                if (avatar != null) Image(avatar, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else {
                    Text(name.take(1).uppercase().ifBlank { "N" }, color = Color.White, fontSize = 36.sp, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold)
                    if (photoUrl.isNotBlank() && LocalReels2RemoteImages.current) coil.compose.AsyncImage(photoUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Box(Modifier.size(32.dp).clip(CircleShape).background(R2Gold).border(2.dp, stitchBackground(), CircleShape).clickable(onClick = onPhoto), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PhotoCamera, "Change photo", tint = R2Green, modifier = Modifier.size(17.dp))
            }
        }
        P2Field("Name", name, onName, "profile2_name")
        P2Field("Username", username, onUsername, "profile2_username", prefix = "@")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp).testTag("profile2_username_status")) {
            Icon(if (status.ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null, tint = if (status.ok) R2Green else Color(0xFFC0392B), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(status.message, color = if (status.ok) R2Green else Color(0xFFC0392B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        P2Field("Bio", bio, onBio, "profile2_bio", singleLine = false)
        Text("${bio.length}/160 · add a link and it shows as a chip", color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        Spacer(Modifier.height(14.dp))
        // ---- Noor AI bio helper
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (isStitchLight()) R2Cream else Color(0xFF1C2A24)).border(1.dp, R2Gold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)).padding(14.dp).testTag("profile2_bio_ai")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = R2GoldDeep, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text("Noor AI bio helper", color = if (isStitchLight()) R2Green else R2Gold, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(if (aiLoading) "Thinking…" else if (suggestions.isEmpty()) "Suggest 3" else "Again", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(R2Green).clickable(enabled = !aiLoading, onClick = onAskAi).padding(horizontal = 12.dp, vertical = 6.dp))
            }
            aiError?.let { Text(it, color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            suggestions.forEachIndexed { i, s ->
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p2Card()).clickable { onPick(s) }.padding(10.dp).testTag("profile2_bio_suggestion_$i"), verticalAlignment = Alignment.CenterVertically) {
                    Text(s, color = stitchText(), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    Text("Use", color = R2Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            if (suggestions.isEmpty() && aiError == null) Text("Get three humble, short bio ideas based on what you've written.", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun P2Field(label: String, value: String, onChange: (String) -> Unit, tag: String, prefix: String? = null, singleLine: Boolean = true) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p2Card()).border(1.dp, stitchLine(), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (prefix != null) Text(prefix, color = stitchMutedText(), fontSize = 15.sp)
            androidx.compose.foundation.text.BasicTextField(
                value, onChange, singleLine = singleLine, maxLines = if (singleLine) 1 else 4,
                textStyle = androidx.compose.ui.text.TextStyle(color = stitchText(), fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(R2Green),
                modifier = Modifier.weight(1f).testTag(tag),
            )
        }
    }
}
