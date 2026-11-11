package com.noorpro.app.ui.islamic

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.GoogleMapsIntentProvider
import com.noorpro.app.data.PilgrimMapProvider
import com.noorpro.app.islamic.masjid.*
import com.noorpro.app.ui.components.QuranArabicFont
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/* 1.1.6 Masjid pages UI (masjid_pages_enabled). Stateless contents + one wired screen. */

data class MasjidHomeUi(
    val masjids: List<Masjid> = emptyList(),
    val followed: Masjid? = null,
    val query: String = "",
    val loading: Boolean = false,
    val error: MasjidError? = null,
    val moderator: Boolean = false,
    val pendingCount: Int = 0,
    val nowMinutes: Int = 0,
)

data class MasjidProfileUi(
    val masjid: Masjid,
    val announcements: List<Announcement> = emptyList(),
    val following: Boolean = false,
    val canManage: Boolean = false,
    val myClaim: MasjidClaim? = null,
    val nowMinutes: Int = 0,
    val isFriday: Boolean = false,
    val error: MasjidError? = null,
    val busy: Boolean = false,
    val toast: String? = null,
)

class MasjidActions(
    val onBack: () -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onOpen: (Masjid) -> Unit = {},
    val onFollow: (Boolean) -> Unit = {},
    val onDirections: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onClaim: () -> Unit = {},
    val onReview: () -> Unit = {},
)

private fun nowMinutes(): Int = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
private fun isFriday(): Boolean = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

@Composable
fun MasjidPagesScreen(viewModel: DeenViewModel, backend: MasjidBackend = remember { FirestoreMasjidBackend() }, maps: PilgrimMapProvider = remember { GoogleMapsIntentProvider() }) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { MasjidLocalStore(ctx) }
    val followed by FollowedMasjid.flow(ctx).collectAsState()
    var page by remember { mutableStateOf("home") }
    var home by remember { mutableStateOf(MasjidHomeUi(masjids = store.cached(), nowMinutes = nowMinutes())) }
    var profile by remember { mutableStateOf<MasjidProfileUi?>(null) }
    var claims by remember { mutableStateOf<List<MasjidClaim>>(emptyList()) }
    var myClaims by remember { mutableStateOf<List<MasjidClaim>>(emptyList()) }
    val name = viewModel.userDisplayName

    fun gate(needUser: Boolean): MasjidError? = when {
        !backend.available -> MasjidError.OFFLINE_BACKEND
        needUser && backend.uid() == null -> MasjidError.SIGN_IN
        else -> null
    }
    fun refresh() {
        gate(false)?.let { home = home.copy(error = it); return }
        home = home.copy(loading = true)
        scope.launch {
            runCatching { backend.list() }
                .onSuccess { l -> store.cache(l); home = home.copy(masjids = l, loading = false, error = null)
                    followed?.let { f -> l.firstOrNull { it.id == f.id }?.let(store::setFollowed) } }
                .onFailure { home = home.copy(loading = false, error = MasjidError.from(it)) }
            if (backend.uid() != null) {
                myClaims = runCatching { backend.myClaims() }.getOrDefault(emptyList())
                if (backend.isModerator()) {
                    claims = runCatching { backend.pendingClaims() }.getOrDefault(emptyList())
                    home = home.copy(moderator = true, pendingCount = claims.size)
                }
            }
        }
    }
    fun open(m: Masjid) {
        profile = MasjidProfileUi(m, following = followed?.id == m.id, canManage = MasjidLogic.canManage(m, backend.uid()),
            myClaim = myClaims.firstOrNull { it.masjidId == m.id }, nowMinutes = nowMinutes(), isFriday = isFriday())
        page = "profile"
        if (gate(false) == null) scope.launch {
            runCatching { backend.announcements(m.id) }.onSuccess { a -> profile = profile?.copy(announcements = a) }
                .onFailure { e -> profile = profile?.copy(error = MasjidError.from(e)) }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    when (page) {
        "profile" -> {
            val ui = profile ?: return
            BackHandler { page = "home" }
            MasjidProfileContent(ui, MasjidActions(
                onBack = { page = "home" },
                onFollow = { on ->
                    store.setFollowed(if (on) ui.masjid else null)
                    profile = ui.copy(following = on, toast = if (on) "Jamaat times will show on your prayer screen" else null)
                    if (backend.available && backend.uid() != null) scope.launch { runCatching { backend.follow(ui.masjid, on) } }
                },
                onDirections = { maps.openDirections(ctx, MasjidLogic.directionsQuery(ui.masjid), ui.masjid.lat, ui.masjid.lng) },
                onShare = { runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, MasjidLogic.shareText(ui.masjid)), "Share masjid")) } },
                onEdit = { page = "edit" },
                onClaim = { gate(true)?.let { profile = ui.copy(error = it) } ?: run { page = "claim" } },
            ))
        }
        "edit" -> {
            val ui = profile ?: return
            BackHandler { page = "profile" }
            MasjidEditContent(ui.masjid, ui.busy, ui.error, onBack = { page = "profile" },
                onSave = { j, jt, jb ->
                    profile = ui.copy(busy = true)
                    scope.launch {
                        runCatching { backend.saveTimes(ui.masjid.id, j, jt, jb) }
                            .onSuccess { val m = ui.masjid.copy(jamaat = j, jummahTime = jt, jummahBayan = jb)
                                if (followed?.id == m.id) store.setFollowed(m)
                                home = home.copy(masjids = home.masjids.map { if (it.id == m.id) m else it })
                                profile = ui.copy(masjid = m, busy = false, error = null, toast = "Times updated"); page = "profile" }
                            .onFailure { profile = ui.copy(busy = false, error = MasjidError.from(it)) }
                    }
                },
                onAnnounce = { t ->
                    profile = ui.copy(busy = true)
                    scope.launch {
                        runCatching { backend.announce(ui.masjid.id, t); backend.announcements(ui.masjid.id) }
                            .onSuccess { a -> profile = ui.copy(announcements = a, busy = false, error = null, toast = "Announcement posted"); page = "profile" }
                            .onFailure { profile = ui.copy(busy = false, error = MasjidError.from(it)) }
                    }
                })
        }
        "claim" -> {
            val ui = profile ?: return
            BackHandler { page = "profile" }
            MasjidClaimContent(ui.masjid, name, ui.busy, ui.error, onBack = { page = "profile" }) { n, role, contact, note ->
                profile = ui.copy(busy = true)
                scope.launch {
                    runCatching { backend.requestClaim(ui.masjid, n, role, contact, note) }
                        .onSuccess { val c = MasjidClaim("${ui.masjid.id}_${backend.uid()}", ui.masjid.id, ui.masjid.name, backend.uid().orEmpty(), n, role, contact, note)
                            myClaims = myClaims.filter { it.masjidId != ui.masjid.id } + c
                            profile = ui.copy(busy = false, error = null, myClaim = c, toast = "Request sent for review"); page = "profile" }
                        .onFailure { profile = ui.copy(busy = false, error = MasjidError.from(it)) }
                }
            }
        }
        "review" -> {
            BackHandler { page = "home" }
            MasjidReviewContent(claims, onBack = { page = "home" }) { c, ok ->
                scope.launch {
                    runCatching { backend.decide(c, ok) }.onSuccess {
                        claims = claims.filter { it.id != c.id }; home = home.copy(pendingCount = claims.size)
                    }
                }
            }
        }
        else -> MasjidHomeContent(home.copy(followed = followed, nowMinutes = nowMinutes()), MasjidActions(
            onBack = { viewModel.goBack() }, onQuery = { home = home.copy(query = it) }, onOpen = ::open, onReview = { page = "review" },
        ))
    }
}

/** Fine line-art mihrab (nested pointed arches + hanging lamp) — pattern, not an illustration. */
@Composable
internal fun MihrabMark(modifier: Modifier = Modifier, color: Color = IkGold) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        fun arch(inset: Float): Path = Path().apply {
            val l = inset; val r = w - inset; val b = h; val shoulder = h * 0.42f + inset * 0.6f; val apex = inset * 1.1f
            moveTo(l, b); lineTo(l, shoulder)
            cubicTo(l, shoulder - (shoulder - apex) * 0.55f, w / 2 - (w / 2 - l) * 0.25f, apex + (shoulder - apex) * 0.15f, w / 2, apex)
            cubicTo(w / 2 + (r - w / 2) * 0.25f, apex + (shoulder - apex) * 0.15f, r, shoulder - (shoulder - apex) * 0.55f, r, shoulder)
            lineTo(r, b)
        }
        drawPath(arch(1.dp.toPx()), color, style = Stroke(1.6.dp.toPx()))
        drawPath(arch(w * 0.14f), color.copy(alpha = 0.7f), style = Stroke(1.dp.toPx()))
        drawPath(arch(w * 0.26f), color.copy(alpha = 0.45f), style = Stroke(1.dp.toPx()))
        val lampTop = h * 0.30f; val lampY = h * 0.55f
        drawLine(color.copy(alpha = 0.8f), Offset(w / 2, lampTop), Offset(w / 2, lampY - h * 0.05f), 1.dp.toPx())
        val lamp = Path().apply {
            moveTo(w / 2 - w * 0.09f, lampY - h * 0.05f); lineTo(w / 2 + w * 0.09f, lampY - h * 0.05f)
            lineTo(w / 2 + w * 0.05f, lampY + h * 0.05f); lineTo(w / 2 - w * 0.05f, lampY + h * 0.05f); close()
        }
        drawPath(lamp, color)
        drawCircle(color.copy(alpha = 0.25f), w * 0.16f, Offset(w / 2, lampY))
    }
}

@Composable
private fun JamaatStrip(m: Masjid, now: Int, onDark: Boolean) {
    val next = MasjidLogic.nextJamaat(m, now)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        JAMAAT_PRAYERS.forEach { p ->
            val on = p == next
            val bg = when { on -> IkGold; onDark -> Color.White.copy(alpha = 0.08f); else -> ikSoft() }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(bg).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(p, color = if (on) IkGreenDeep else if (onDark) IkCream.copy(alpha = 0.75f) else stitchMutedText(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(m.jamaat[p]?.let { MasjidLogic.display(it).removeSuffix(" AM").removeSuffix(" PM") } ?: "—",
                    color = if (on) IkGreenDeep else if (onDark) IkCream else stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
fun MasjidHomeContent(ui: MasjidHomeUi, actions: MasjidActions, modifier: Modifier = Modifier) {
    val results = MasjidLogic.search(ui.masjids, ui.query)
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("masjid_home")) {
        IkHeader("Masjid Pages", "Jamaat times, Jummah and notices", actions.onBack)
        val f = ui.followed
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).let { if (f != null) it.clickable { actions.onOpen(f) } else it }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (f != null) "YOUR MASJID" else "FOLLOW YOUR MASJID", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(f?.name ?: "Never miss the jamaat", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(if (f != null) listOf(f.address, f.city).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Jamaat times shown on your prayer screen" }
                         else "Follow a masjid to see its jamaat times on your prayer screen.", color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(10.dp))
                MihrabMark(Modifier.size(width = 62.dp, height = 82.dp))
            }
            if (f != null) { Spacer(Modifier.height(14.dp)); JamaatStrip(f, ui.nowMinutes, onDark = true) }
        }
        ui.error?.let { IkNotice(it.title, it.body, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), offline = it == MasjidError.OFFLINE_BACKEND || it == MasjidError.NETWORK, tag = "masjid_notice") }
        if (ui.moderator) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikSoft()).clickable(onClick = actions.onReview).padding(12.dp).testTag("masjid_review_open"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = ikAccentText(), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                Text("Manager requests to review", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${ui.pendingCount}", color = ikAccentText(), fontWeight = FontWeight.Bold)
            }
        }
        IkSection("Find a masjid", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        Box(Modifier.padding(horizontal = 16.dp)) { IkField(ui.query, actions.onQuery, "Search by name, area or city", tag = "masjid_search", maxLen = 60) }
        Spacer(Modifier.height(6.dp))
        if (results.isEmpty()) {
            Text(when { ui.loading -> "Loading masjids…"; ui.query.isNotBlank() -> "No masjid matches \"${ui.query.trim()}\"."
                else -> "Masjid listings appear here once they're published. Imams and committees can request to manage their masjid's page." },
                color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).testTag("masjid_empty"))
        }
        results.forEach { m ->
            val next = MasjidLogic.nextJamaat(m, ui.nowMinutes)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ikCard())
                .border(1.dp, if (m.id == ui.followed?.id) IkGold else stitchLine(), RoundedCornerShape(18.dp)).clickable { actions.onOpen(m) }.padding(12.dp).testTag("masjid_row_${m.id}"),
                verticalAlignment = Alignment.CenterVertically) {
                IkIconTile(Icons.Outlined.Mosque, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (m.adminUids.isNotEmpty()) { Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.VerifiedUser, "Managed by the masjid", tint = IkGoldDeep, modifier = Modifier.size(14.dp)) }
                    }
                    Text(listOf(m.address, m.city).filter { it.isNotBlank() }.joinToString(", "), color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (next != null) {
                        Text(next, color = stitchMutedText(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(MasjidLogic.display(m.jamaat.getValue(next)), color = ikAccentText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else if (m.id == ui.followed?.id) Text("Following", color = IkGoldDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Directions open in your maps app. NoorPro doesn't track your location for this page.", color = stitchMutedText(), fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(28.dp))
    }
}

private fun fmtDate(t: Long) = if (t <= 0) "" else SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(t))

@Composable
fun MasjidProfileContent(ui: MasjidProfileUi, actions: MasjidActions, modifier: Modifier = Modifier) {
    val m = ui.masjid
    val next = MasjidLogic.nextJamaat(m, ui.nowMinutes)
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("masjid_profile")) {
        IkHeader(m.name, m.city.ifBlank { null }, actions.onBack) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = actions.onShare).testTag("masjid_share"), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Share, "Share", tint = stitchText(), modifier = Modifier.size(20.dp))
            }
        }
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("بَيْتُ الله", color = IkGold, fontFamily = QuranArabicFont, fontSize = 20.sp)
                    Text(m.name, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp)
                    if (m.address.isNotBlank()) Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Place, null, tint = IkGold, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp))
                        Text(listOf(m.address, m.city).filter { it.isNotBlank() }.joinToString(", "), color = IkCream.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    if (m.adminUids.isNotEmpty()) Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, null, tint = IkGold, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                        Text("Times published by the masjid", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                MihrabMark(Modifier.size(width = 54.dp, height = 72.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp)).background(if (ui.following) Color.White.copy(alpha = 0.14f) else IkGold)
                    .clickable { actions.onFollow(!ui.following) }.testTag("masjid_follow"), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (ui.following) Icons.Default.Check else Icons.Outlined.NotificationsActive, null, tint = if (ui.following) IkCream else IkGreenDeep, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (ui.following) "Following" else "Follow", color = if (ui.following) IkCream else IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, IkCream.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable(onClick = actions.onDirections).testTag("masjid_directions"), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Directions, null, tint = IkCream, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text("Directions", color = IkCream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        ui.toast?.let { Text(it, color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).testTag("masjid_toast")) }
        ui.error?.let { IkNotice(it.title, it.body, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), offline = it == MasjidError.OFFLINE_BACKEND || it == MasjidError.NETWORK, tag = "masjid_notice") }

        IkSection("Jamaat times", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 2.dp).testTag("masjid_jamaat"), padding = 6.dp) {
            if (m.jamaat.isEmpty()) Text("This masjid hasn't published jamaat times yet.", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.padding(10.dp))
            JAMAAT_PRAYERS.filter { m.jamaat.isNotEmpty() }.forEachIndexed { i, p ->
                val on = p == next
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (on) ikSoft() else Color.Transparent).padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) IkGold else stitchLine()))
                    Spacer(Modifier.width(12.dp))
                    Text(p, color = stitchText(), fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
                    if (on) { Text("NEXT", color = IkGoldDeep, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Spacer(Modifier.width(10.dp)) }
                    Text(m.jamaat[p]?.let { MasjidLogic.display(it) } ?: "—", color = if (on) ikAccentText() else stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (i < 4) Box(Modifier.padding(start = 30.dp).fillMaxWidth().height(1.dp).background(stitchLine().copy(alpha = 0.5f)))
            }
        }

        IkSection("Jummah", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 2.dp).testTag("masjid_jummah")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.clip(RoundedCornerShape(14.dp)).background(if (isStitchLight()) IkGreen else Color(0xFF1C4636)).padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (ui.isFriday) "TODAY" else "FRIDAY", color = IkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(m.jummahTime.takeIf { it.isNotBlank() }?.let { MasjidLogic.display(it) } ?: "—", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("KHUTBAH / BAYAN", color = stitchMutedText(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(m.jummahBayan.ifBlank { "Topic not announced yet" }, color = if (m.jummahBayan.isBlank()) stitchMutedText() else stitchText(),
                        fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp)
                }
            }
        }

        IkSection("Announcements", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        if (ui.announcements.isEmpty()) Text("No announcements right now.", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        ui.announcements.forEach { a ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(12.dp).testTag("masjid_ann_${a.id}")) {
                IkIconTile(Icons.Outlined.Campaign, size = 34.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.text, color = stitchText(), fontSize = 14.sp, lineHeight = 19.sp)
                    if (a.at > 0) Text(fmtDate(a.at), color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        when {
            ui.canManage -> IkButton("Update times & announcements", Modifier.padding(horizontal = 16.dp).fillMaxWidth(), icon = Icons.Outlined.Edit, tag = "masjid_edit", onClick = actions.onEdit)
            ui.myClaim?.status == ClaimStatus.PENDING -> IkNotice("Request under review", "Your request to manage ${m.name} is with the NoorPro team. You'll be able to update times once it's approved.",
                Modifier.padding(horizontal = 16.dp), tag = "masjid_claim_pending")
            else -> Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, stitchLine(), RoundedCornerShape(16.dp))
                .clickable(onClick = actions.onClaim).padding(14.dp).testTag("masjid_claim"), verticalAlignment = Alignment.CenterVertically) {
                IkIconTile(Icons.Outlined.VerifiedUser, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Imam or committee member?", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Request to manage this page and keep its times up to date.", color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun MasjidEditContent(m: Masjid, busy: Boolean, error: MasjidError?, onBack: () -> Unit, onSave: (Map<String, String>, String, String) -> Unit, onAnnounce: (String) -> Unit, modifier: Modifier = Modifier) {
    val times = remember { mutableStateMapOf<String, String>().apply { JAMAAT_PRAYERS.forEach { put(it, m.jamaat[it]?.let { t -> MasjidLogic.display(t) } ?: "") } } }
    var jummah by remember { mutableStateOf(m.jummahTime.takeIf { it.isNotBlank() }?.let { MasjidLogic.display(it) } ?: "") }
    var bayan by remember { mutableStateOf(m.jummahBayan) }
    var ann by remember { mutableStateOf("") }
    val parsed = JAMAAT_PRAYERS.associateWith { MasjidLogic.normalizeTime(times[it].orEmpty()) }
    val bad = JAMAAT_PRAYERS.filter { times[it].orEmpty().isNotBlank() && parsed[it] == null }
    val jParsed = MasjidLogic.normalizeTime(jummah)
    val jBad = jummah.isNotBlank() && jParsed == null
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("masjid_edit_page")) {
        IkHeader("Update ${m.name}", "Visible to everyone who follows", onBack)
        error?.let { IkNotice(it.title, it.body, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), offline = true, tag = "masjid_notice") }
        IkSection("Jamaat times", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        IkCard(Modifier.padding(horizontal = 16.dp), padding = 12.dp) {
            JAMAAT_PRAYERS.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { p -> Box(Modifier.weight(1f)) { IkField(times[p].orEmpty(), { times[p] = it }, p, tag = "masjid_t_$p", maxLen = 8) } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text(if (bad.isEmpty()) "Use times like 5:30 AM or 13:15." else "Check: ${bad.joinToString()}", color = if (bad.isEmpty()) stitchMutedText() else IkRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        IkSection("Jummah", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        IkCard(Modifier.padding(horizontal = 16.dp), padding = 12.dp) {
            IkField(jummah, { jummah = it }, "Khutbah time", tag = "masjid_jummah_t", maxLen = 8)
            Spacer(Modifier.height(8.dp))
            IkField(bayan, { bayan = it }, "Bayan topic (optional)", tag = "masjid_bayan", maxLen = 140)
        }
        IkButton(if (busy) "Saving…" else "Save times", Modifier.padding(16.dp).fillMaxWidth(), icon = Icons.Default.Check, enabled = !busy && bad.isEmpty() && !jBad, tag = "masjid_save") {
            onSave(parsed.filterValues { it != null }.mapValues { it.value!! }, jParsed.orEmpty(), bayan.trim())
        }
        IkSection("Post an announcement", Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        IkCard(Modifier.padding(horizontal = 16.dp), padding = 12.dp) {
            IkField(ann, { ann = it }, "e.g. Tarawih begins after Isha tonight", singleLine = false, tag = "masjid_ann_text", maxLen = 500)
            Spacer(Modifier.height(10.dp))
            IkButton("Post", Modifier.fillMaxWidth(), icon = Icons.Outlined.Campaign, enabled = !busy && ann.isNotBlank(), tag = "masjid_post") { onAnnounce(ann.trim()); ann = "" }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun MasjidClaimContent(m: Masjid, defaultName: String, busy: Boolean, error: MasjidError?, onBack: () -> Unit, modifier: Modifier = Modifier, onSubmit: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    var role by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val problem = MasjidLogic.claimProblem(name, contact, role)
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("masjid_claim_page")) {
        IkHeader("Manage this masjid", m.name, onBack)
        IkNotice("How approval works", "The NoorPro team checks each request with the masjid before giving access. Once approved, you can update jamaat times, Jummah and announcements.",
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp), tag = "masjid_claim_info")
        error?.let { IkNotice(it.title, it.body, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), offline = true, tag = "masjid_notice") }
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = 12.dp) {
            IkField(name, { name = it }, "Your full name", tag = "claim_name", maxLen = 80)
            Spacer(Modifier.height(10.dp))
            Text("Your role", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MasjidLogic.ROLES.forEach { r -> IkPill(r, role == r, tag = "claim_role_${r.substringBefore(' ').lowercase()}") { role = r } }
            }
            Spacer(Modifier.height(10.dp))
            IkField(contact, { contact = it }, "Phone or email for verification", tag = "claim_contact", maxLen = 120)
            Spacer(Modifier.height(10.dp))
            IkField(note, { note = it }, "Anything that helps us verify (optional)", singleLine = false, tag = "claim_note", maxLen = 500)
        }
        if (tried && problem != null) Text(problem, color = IkRed, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp).testTag("claim_problem"))
        IkButton(if (busy) "Sending…" else "Send request", Modifier.padding(16.dp).fillMaxWidth(), icon = Icons.Outlined.VerifiedUser, enabled = !busy, tag = "claim_submit") {
            tried = true; if (problem == null) onSubmit(MasjidLogic.cleanName(name), role, contact.trim(), note.trim())
        }
    }
}

@Composable
fun MasjidReviewContent(claims: List<MasjidClaim>, onBack: () -> Unit, modifier: Modifier = Modifier, onDecide: (MasjidClaim, Boolean) -> Unit) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("masjid_review")) {
        IkHeader("Manager requests", "${claims.size} waiting", onBack)
        if (claims.isEmpty()) Text("No requests waiting.", color = stitchMutedText(), modifier = Modifier.padding(20.dp))
        claims.forEach { c ->
            IkCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).testTag("claim_${c.id}")) {
                Text(c.masjidName, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${c.name} · ${c.role}", color = stitchText(), fontSize = 13.sp)
                Text(c.contact, color = ikAccentText(), fontSize = 13.sp)
                if (c.note.isNotBlank()) Text(c.note, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IkButton("Decline", Modifier.weight(1f), icon = Icons.Outlined.Delete, primary = false, tag = "claim_reject_${c.id}") { onDecide(c, false) }
                    IkButton("Approve", Modifier.weight(1f), icon = Icons.Default.Check, tag = "claim_approve_${c.id}") { onDecide(c, true) }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

/** Compact card for the prayer screen: the followed masjid's jamaat (shown only when the flag is on). */
@Composable
fun FollowedMasjidJamaatCard(m: Masjid, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val now = remember { nowMinutes() }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp))
        .clickable(onClick = onOpen).padding(12.dp).testTag("prayer_masjid_jamaat")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Mosque, null, tint = ikAccentText(), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text("Jamaat at ${m.name}", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (m.jummahTime.isNotBlank()) Text("Jummah ${MasjidLogic.display(m.jummahTime)}", color = IkGoldDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        JamaatStrip(m, now, onDark = false)
    }
}

