package com.noorpro.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.noorpro.app.data.halal.*
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 1.1.5 Halal Guard UI: guidelines, text warnings, blocked-post notice, safety settings, moderator panel. */

private val Amber = Color(0xFFB7791F)
private val AmberDark = Color(0xFFE9B85A)
private val Danger = Color(0xFFB3261E)
private val DangerDark = Color(0xFFF2B8B5)
@Composable private fun amber() = if (isStitchLight()) Amber else AmberDark
@Composable private fun danger() = if (isStitchLight()) Danger else DangerDark

// ------------------------------------------------------------------ text warning
/** Small inline note under a text field. Severe = red ("can't send"), mild = amber. */
@Composable
fun HalalTextWarning(text: String, modifier: Modifier = Modifier) {
    if (!HalalFlags.textWarnings) return
    val ctx = LocalContext.current
    var loaded by remember { mutableStateOf(HalalTextFilter.isLoaded) }
    LaunchedEffect(Unit) {
        if (!loaded) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { HalalTextFilter.ensureLoaded(ctx) }
            loaded = HalalTextFilter.isLoaded
        }
    }
    val r = remember(text, loaded) { HalalTextFilter.check(text) }
    if (r.action == HalalTextFilter.Action.ALLOW) return
    val color = if (r.blocked) danger() else amber()
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp).testTag(if (r.blocked) "halal_block" else "halal_warning"),
        verticalAlignment = Alignment.Top
    ) {
        Icon(if (r.blocked) Icons.Default.Block else Icons.Default.Info, null, tint = color, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Spacer(Modifier.width(8.dp))
        Text(r.message, color = stitchText(), fontSize = 12.5.sp, lineHeight = 17.sp)
    }
}

/** True when the on-device filter would block [text] (severe). Send/post buttons use this. */
fun halalBlocks(text: String): Boolean = HalalFlags.textWarnings && HalalTextFilter.check(text).blocked

// ------------------------------------------------------------------ guidelines
data class GuidelineSection(val icon: ImageVector, val title: String, val body: String, val ref: String = "")

val HALAL_GUIDELINES = listOf(
    GuidelineSection(Icons.Default.Favorite, "Modesty (haya) first",
        "Share modest photos and reels. No nudity, revealing outfits, suggestive poses or dancing clips. Respect awrah for brothers and sisters alike.",
        "\u201CHaya is part of faith.\u201D \u2014 Bukhari 24"),
    GuidelineSection(Icons.Default.RecordVoiceOver, "Speak with kindness",
        "No swearing, insults, mocking or bullying \u2014 in any language. Disagree with adab; correct privately when you can.",
        "\u201CTell My servants to say that which is best.\u201D \u2014 Al-Isra 17:53"),
    GuidelineSection(Icons.Default.DoNotDisturbOn, "No haram promotion",
        "Don't promote alcohol, drugs, gambling, interest-based schemes, zina or anything whose purpose is calling to haram."),
    GuidelineSection(Icons.Default.MenuBook, "Quran & hadith with care",
        "Quote ayat exactly and give the reference. Share hadith with their source and grading. No fabricated or altered texts.",
        "Wrong Quran or hadith can be reported and is corrected or removed."),
    GuidelineSection(Icons.Default.Diversity3, "Unity, not sectarian hate",
        "No takfir of people or groups, no insults to the Sahaba, Ahl al-Bayt or scholars. Different madhhabs are welcome here."),
    GuidelineSection(Icons.Default.VerifiedUser, "Honesty & trust",
        "No scams, fake charity appeals, money doubling, impersonation or asking anyone for OTPs or passwords."),
    GuidelineSection(Icons.Default.Shield, "Privacy & safety",
        "Never post someone else's photos (especially sisters') without consent. No unwanted DMs. Report anything that makes you uneasy."),
    GuidelineSection(Icons.Default.Gavel, "How we enforce",
        "Posts are checked automatically and by moderators. Breaking a rule earns a strike; 3 strikes suspend the account for a while, serious harm means a ban. Appeals: ${UgcTerms.MODERATION_EMAIL}.")
)

@Composable
fun HalalGuidelinesContent(modifier: Modifier = Modifier) {
    Column(modifier.testTag("halal_guidelines"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(feedMint()).padding(18.dp)
        ) {
            Text("\u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u0670\u0647\u0650", color = feedAccent(), fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text("Halal community guidelines", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("NoorPro is a space for benefit, sincerity and good character. These rules keep it that way for everyone.",
                color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp)
        }
        HALAL_GUIDELINES.forEachIndexed { i, s ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface())
                    .border(1.dp, stitchLine(), RoundedCornerShape(16.dp)).padding(14.dp).testTag("guideline_$i")
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(feedMint()), contentAlignment = Alignment.Center) {
                    Icon(s.icon, null, tint = feedAccent(), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(s.body, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp)
                    if (s.ref.isNotBlank()) Text(s.ref, color = feedGold(), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun HalalTopBar(title: String, onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText()) }
        Text(title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun HalalGuidelinesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding()) {
        HalalTopBar("Community guidelines", onBack)
        HalalGuidelinesContent(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp))
    }
}

@Composable
fun HalalGuidelinesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = stitchBackground(), modifier = Modifier.fillMaxSize()) {
            Column(Modifier.statusBarsPadding()) {
                HalalTopBar("Community guidelines", onDismiss)
                HalalGuidelinesContent(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp))
            }
        }
    }
}

// ------------------------------------------------------------------ blocked-post notice
@Composable
fun HalalBlockedNoticeCard(reason: String, onGuidelines: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, queued: Boolean = false, title: String? = null) {
    val tone = if (queued) amber() else danger()
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(stitchSurface()).border(1.dp, stitchLine(), RoundedCornerShape(22.dp))
            .padding(20.dp).testTag("halal_blocked_notice"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(tone.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(if (queued) Icons.Default.HourglassTop else Icons.Default.Shield, null, tint = tone, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title ?: if (queued) "Your post is being reviewed" else "Your post wasn't published", color = stitchText(),
            fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 19.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(reason.ifBlank { if (queued) "A moderator will check it shortly and it will appear once approved, in shaa Allah." else "It didn't pass our Halal community guidelines." },
            color = stitchMutedText(), fontSize = 13.5.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(if (queued) "Only you can see it until then." else "If you think this is a mistake, email ${UgcTerms.MODERATION_EMAIL} and a moderator will look again.",
            color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onGuidelines, modifier = Modifier.testTag("notice_guidelines")) { Text("Read guidelines", color = feedAccent()) }
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = feedAccent()), modifier = Modifier.testTag("notice_ok")) {
                Text("OK", color = Color.White)
            }
        }
    }
}

fun noticeTitle(kind: String) = when (kind) {
    "post" -> "Your post wasn't published"
    "story" -> "Your story was hidden"
    "comment" -> "Your comment was hidden"
    "avatar" -> "Your profile photo was removed"
    "remove" -> "Your content was removed"
    "warn" -> "A note from the moderators"
    "suspend" -> "Your account is paused"
    else -> "Halal Guard notice"
}

/** Shows the newest unseen moderation notice as a dialog (mounted once at app level). */
@Composable
fun HalalNoticeHost() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { HalalNotices.init(ctx) }
    // Server notices inbox (removed comments, strikes, avatar) needs the 1.1.5 rules: flagged.
    val store = remember { if (HalalFlags.notices) runCatching { ModerationNoticeStore() }.getOrNull() else null }
    var notices by remember { mutableStateOf(emptyList<ModerationNotice>()) }
    var guidelines by remember { mutableStateOf(false) }
    DisposableEffect(store) {
        val reg = runCatching { store?.observe { notices = it } }.getOrNull()
        onDispose { reg?.remove() }
    }
    // Private safety settings (owner-only doc, already covered by users/{uid}/settings rules).
    DisposableEffect(Unit) {
        val reg = if (HalalFlags.safety) runCatching { SafetyStore().observe { HalalPrefs.blurFlagged = it.blurFlagged; HalalPrefs.gender = it.gender } }.getOrNull() else null
        onDispose { reg?.remove() }
    }
    val local = HalalNotices.pending.firstOrNull()
    val server = notices.firstOrNull()
    if (!guidelines && (local != null || server != null)) {
        val dismiss = { if (local != null) HalalNotices.dismiss(local.id) else server?.let { store?.markSeen(it.id) } }
        Dialog(onDismissRequest = { dismiss() }) {
            if (local != null) HalalBlockedNoticeCard(local.reason, onGuidelines = { guidelines = true }, onDismiss = { dismiss() }, queued = local.queued)
            else HalalBlockedNoticeCard(server!!.reason, onGuidelines = { guidelines = true }, onDismiss = { dismiss() },
                title = noticeTitle(server.kind))
        }
    }
    if (guidelines) HalalGuidelinesDialog { guidelines = false }
}

// ------------------------------------------------------------------ sensitive media
/** Covers flagged (moderator-approved but sensitive) media until tapped. */
@Composable
fun SensitiveMediaCover(modifier: Modifier = Modifier, onReveal: () -> Unit) {
    Box(modifier.background(Color.Black.copy(alpha = 0.72f)).clickable(onClick = onReveal).testTag("sensitive_cover"), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VisibilityOff, null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(6.dp))
            Text("Sensitive content", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("A moderator flagged this. Tap to view.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

/** Media modifier: blurs when the item is flagged and the viewer keeps "Blur flagged media" on. */
fun Modifier.halalBlur(active: Boolean): Modifier = if (active) this.blur(28.dp) else this

/** Feed media wrapper: blurred + covered when a moderator marked it sensitive and the viewer keeps blur on. */
@Composable
fun SensitiveMedia(sensitive: Boolean, key: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var revealed by remember(key) { mutableStateOf(false) }
    val cover = sensitive && HalalFlags.blur && HalalPrefs.blurFlagged && !revealed
    Box(modifier) {
        Box(Modifier.fillMaxSize().halalBlur(cover)) { content() }
        if (cover) SensitiveMediaCover(Modifier.fillMaxSize()) { revealed = true }
    }
}

// ------------------------------------------------------------------ safety settings
@Composable
private fun SettingCard(title: String, subtitle: String = "", content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(stitchSurface())
            .border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).padding(16.dp)
    ) {
        Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (subtitle.isNotBlank()) Text(subtitle, color = stitchMutedText(), fontSize = 12.5.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ChoiceRow(label: String, blurb: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = feedAccent()))
        Column(Modifier.weight(1f)) {
            Text(label, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (blurb.isNotBlank()) Text(blurb, color = stitchMutedText(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SwitchRow(label: String, blurb: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(blurb, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = feedAccent()))
    }
}

@Composable
fun SafetySettingsContent(
    settings: SafetySettings,
    onChange: (SafetySettings) -> Unit,
    circlesEnabled: Boolean,
    onGuidelines: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.testTag("safety_settings"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingCard("Message requests", "Who lands in your main inbox. Everyone else waits in the Requests folder.") {
            MessageRequestMode.entries.forEach { m ->
                ChoiceRow(m.label, m.blurb, settings.messageRequests == m, "msgreq_${m.name}") { onChange(settings.copy(messageRequests = m)) }
            }
        }
        SettingCard("Comments") {
            SwitchRow("Hide comments from non-followers", "On your posts, only people who follow you are shown. Your own replies always show.",
                settings.hideNonFollowerComments, "hide_nonfollower") { onChange(settings.copy(hideNonFollowerComments = it)) }
        }
        SettingCard("Media") {
            SwitchRow("Blur flagged media", "Posts a moderator marked sensitive stay blurred until you tap.",
                settings.blurFlagged, "blur_flagged") { onChange(settings.copy(blurFlagged = it)) }
        }
        if (circlesEnabled) {
            SettingCard("Sisters & Brothers circles",
                "Optional. Lets you join Sisters-only or Brothers-only groups. Stored privately on your account \u2014 never shown on your profile.") {
                CircleGender.entries.forEach { g ->
                    ChoiceRow(g.label, "", settings.gender == g, "gender_${g.name}") { onChange(settings.copy(gender = g)) }
                }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lock, null, tint = stitchMutedText(), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Privacy: this is self-declared and not verified. Only you can read it; group owners see only whether you can join.",
                        color = stitchMutedText(), fontSize = 11.5.sp, lineHeight = 15.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(feedMint()).clickable(onClick = onGuidelines)
                .padding(14.dp).testTag("safety_guidelines"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MenuBook, null, tint = feedAccent())
            Spacer(Modifier.width(10.dp))
            Text("Halal community guidelines", color = stitchText(), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = stitchMutedText())
        }
    }
}

@Composable
fun SafetySettingsScreen(viewModel: DeenViewModel) {
    val store = remember { runCatching { SafetyStore() }.getOrNull() }
    var settings by remember { mutableStateOf(SafetySettings()) }
    val ctx = LocalContext.current
    DisposableEffect(store) {
        val reg = runCatching { store?.observe { settings = it; HalalPrefs.blurFlagged = it.blurFlagged; HalalPrefs.gender = it.gender } }.getOrNull()
        onDispose { reg?.remove() }
    }
    Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding()) {
        HalalTopBar("Safety", { viewModel.goBack() })
        SafetySettingsContent(settings, onChange = { s ->
            settings = s
            HalalPrefs.blurFlagged = s.blurFlagged
            HalalPrefs.gender = s.gender
            store?.save(s) { ok -> if (!ok) Toast.makeText(ctx, "Couldn't save. Check your connection.", Toast.LENGTH_SHORT).show() }
        }, circlesEnabled = HalalFlags.circles,
            onGuidelines = { viewModel.navigateTo(com.noorpro.app.ui.viewmodel.DeenScreen.HALAL_GUIDELINES) },
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp))
    }
}

// ------------------------------------------------------------------ moderator panel
enum class ModTab(val label: String) { QUEUE("Queue"), REPORTS("Reports"), AUDIT("Audit log") }

class ModeratorPanelState {
    var tab by mutableStateOf(ModTab.QUEUE)
    var queue by mutableStateOf(emptyList<ModerationItem>())
    var reports by mutableStateOf(emptyList<ModerationReport>())
    var audit by mutableStateOf(emptyList<AuditEntry>())
    var strikes by mutableStateOf(emptyMap<String, StrikeRecord>())
    var busyId by mutableStateOf<String?>(null)
    /** Item awaiting the suspend/ban sheet: (target, id, subjectUid). */
    var accountSheet by mutableStateOf<Triple<String, String, String>?>(null)
    var suspendDays by mutableStateOf("7")
}

data class ModeratorActions(
    val onBack: () -> Unit = {},
    val onDecide: (ModAction, String, String, Int, Boolean) -> Unit = { _, _, _, _, _ -> },
    val onLoadStrikes: (String) -> Unit = {}
)

private fun shortTime(ms: Long) = if (ms <= 0) "" else SimpleDateFormat("d MMM, HH:mm", Locale.ENGLISH).format(Date(ms))

@Composable
private fun Chip(text: String, color: Color) {
    Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
private fun ModPreview(url: String, type: String) {
    Box(Modifier.size(86.dp).clip(RoundedCornerShape(14.dp)).background(feedMint()), contentAlignment = Alignment.Center) {
        val preview = UmmahFeedPreview.images[url]
        when {
            preview != null -> Image(preview.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            url.startsWith("http") -> AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> Icon(when (type) { "comment" -> Icons.Default.ChatBubble; "reel", "video" -> Icons.Default.SmartDisplay; else -> Icons.Default.Article },
                null, tint = feedAccent(), modifier = Modifier.size(30.dp))
        }
        if (type == "reel" || type == "video") Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun ActionButton(label: String, color: Color, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Text(label, color = if (enabled) color else stitchMutedText(), fontWeight = FontWeight.Bold, fontSize = 13.sp,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, color.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp).testTag(tag))
}

@Composable
private fun StrikeLine(rec: StrikeRecord?) {
    if (rec == null) return
    val now = System.currentTimeMillis()
    val t = when {
        rec.banned -> "Banned"
        rec.suspendedUntilMs > now -> "${rec.count} strikes \u00B7 suspended until ${shortTime(rec.suspendedUntilMs)}"
        else -> "${rec.count}/3 strikes"
    }
    Text(t, color = if (rec.restricted(now)) danger() else stitchMutedText(), fontSize = 11.5.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QueueCard(item: ModerationItem, state: ModeratorPanelState, actions: ModeratorActions) {
    val busy = state.busyId == item.id
    LaunchedEffect(item.creatorUid) { if (item.creatorUid.isNotBlank() && item.creatorUid !in state.strikes) actions.onLoadStrikes(item.creatorUid) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(stitchSurface())
            .border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).padding(14.dp).testTag("queue_${item.id}")
    ) {
        Row {
            ModPreview(item.previewUrl, if (item.kind == "comment") "comment" else item.type)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.creatorName.ifBlank { "Member" }, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(6.dp))
                    Chip(item.kind.replace('_', ' '), feedAccent())
                }
                StrikeLine(state.strikes[item.creatorUid])
                if (item.caption.isNotBlank()) Text(item.caption, color = stitchText(), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.reasons.forEach { Chip(ModerationText.reason(it), amber()) }
                    item.scores.filterValues { it in setOf("POSSIBLE", "LIKELY", "VERY_LIKELY") }.forEach { (k, v) ->
                        Chip("$k: ${v.lowercase().replace('_', ' ')}", danger())
                    }
                }
                if (item.createdAt > 0) Text(shortTime(item.createdAt), color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionButton("Approve", feedAccent(), "approve_${item.id}", !busy) { actions.onDecide(ModAction.APPROVE, "queue", item.id, 0, false) }
            ActionButton("Remove", danger(), "remove_${item.id}", !busy) { actions.onDecide(ModAction.REMOVE, "queue", item.id, 0, false) }
            ActionButton("Warn", amber(), "warn_${item.id}", !busy) { actions.onDecide(ModAction.WARN, "queue", item.id, 0, false) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { state.accountSheet = Triple("user", item.creatorUid, item.creatorName) }, enabled = !busy && item.creatorUid.isNotBlank(),
                modifier = Modifier.testTag("account_${item.id}")) { Icon(Icons.Default.ManageAccounts, "Account actions", tint = stitchMutedText()) }
        }
        if (item.kind == "post" && item.reasons.any { it.startsWith("possible") }) {
            Text("Approve & blur", color = feedAccent(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp).clickable(enabled = !busy) { actions.onDecide(ModAction.APPROVE, "queue", item.id, 0, true) }
                    .testTag("approve_blur_${item.id}"))
        }
    }
}

@Composable
private fun ReportCard(r: ModerationReport, state: ModeratorPanelState, actions: ModeratorActions) {
    val busy = state.busyId == r.id
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(stitchSurface())
            .border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).padding(14.dp).testTag("report_${r.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip(r.targetType.ifBlank { "post" }, feedAccent())
            Spacer(Modifier.width(8.dp))
            Text(r.reason, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(shortTime(r.createdAt), color = stitchMutedText(), fontSize = 11.sp)
        }
        if (r.details.isNotBlank()) Text("\u201C${r.details}\u201D", color = stitchMutedText(), fontSize = 13.sp, maxLines = 3, modifier = Modifier.padding(top = 6.dp))
        StrikeLine(state.strikes[r.targetUid])
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionButton("Remove", danger(), "rremove_${r.id}", !busy) { actions.onDecide(ModAction.REMOVE, "report", r.id, 0, false) }
            ActionButton("Warn", amber(), "rwarn_${r.id}", !busy) { actions.onDecide(ModAction.WARN, "report", r.id, 0, false) }
            ActionButton("Dismiss", stitchMutedText(), "rdismiss_${r.id}", !busy) { actions.onDecide(ModAction.DISMISS, "report", r.id, 0, false) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { state.accountSheet = Triple("user", r.targetUid, "") }, enabled = !busy && r.targetUid.isNotBlank()) {
                Icon(Icons.Default.ManageAccounts, "Account actions", tint = stitchMutedText())
            }
        }
    }
}

@Composable
private fun AccountSheet(state: ModeratorPanelState, actions: ModeratorActions) {
    val (target, uid, name) = state.accountSheet ?: return
    AlertDialog(
        onDismissRequest = { state.accountSheet = null },
        title = { Text("Account: ${name.ifBlank { uid.take(10) }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StrikeLine(state.strikes[uid])
                Text("Suspension length (days)", fontSize = 13.sp)
                BasicTextField(state.suspendDays, { v -> state.suspendDays = v.filter { it.isDigit() }.take(3) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    textStyle = TextStyle(color = stitchText(), fontSize = 15.sp), cursorBrush = SolidColor(feedAccent()),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, stitchLine(), RoundedCornerShape(10.dp))
                        .padding(10.dp).testTag("suspend_days"))
                Text("3 strikes suspend automatically for this many days. Ban disables sign-in.", color = stitchMutedText(), fontSize = 12.sp)
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { actions.onDecide(ModAction.SUSPEND, target, uid, state.suspendDays.toIntOrNull() ?: 7, false); state.accountSheet = null },
                    modifier = Modifier.testTag("do_suspend")) { Text("Suspend") }
                TextButton(onClick = { actions.onDecide(ModAction.BAN, target, uid, 0, false); state.accountSheet = null },
                    modifier = Modifier.testTag("do_ban")) { Text("Ban", color = danger()) }
            }
        },
        dismissButton = {
            TextButton(onClick = { actions.onDecide(ModAction.UNBAN, target, uid, 0, false); state.accountSheet = null }) { Text("Lift") }
        }
    )
}

@Composable
fun ModeratorPanel(state: ModeratorPanelState, actions: ModeratorActions, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().testTag("moderator_panel")) {
        HalalTopBar("Moderation", actions.onBack) {
            Icon(Icons.Default.AdminPanelSettings, null, tint = feedGold(), modifier = Modifier.padding(end = 12.dp))
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModTab.entries.forEach { t ->
                val sel = state.tab == t
                val count = when (t) { ModTab.QUEUE -> state.queue.size; ModTab.REPORTS -> state.reports.size; else -> 0 }
                Text(t.label + if (count > 0) " $count" else "", color = if (sel) Color.White else stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (sel) feedAccent() else stitchSurface())
                        .border(1.dp, if (sel) feedAccent() else stitchLine(), RoundedCornerShape(999.dp))
                        .clickable { state.tab = t }.padding(horizontal = 14.dp, vertical = 8.dp).testTag("modtab_${t.name}"))
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (state.tab) {
                ModTab.QUEUE -> {
                    if (state.queue.isEmpty()) item { EmptyNote("Queue is clear, alhamdulillah.") }
                    items(state.queue, key = { it.id }) { QueueCard(it, state, actions) }
                }
                ModTab.REPORTS -> {
                    if (state.reports.isEmpty()) item { EmptyNote("No open reports.") }
                    items(state.reports, key = { it.id }) { ReportCard(it, state, actions) }
                }
                ModTab.AUDIT -> {
                    if (state.audit.isEmpty()) item { EmptyNote("No decisions yet.") }
                    items(state.audit, key = { it.id }) { a ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(stitchSurface()).padding(12.dp).testTag("audit_${a.id}"),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(when (a.action) { "approve" -> Icons.Default.CheckCircle; "ban" -> Icons.Default.Block; "remove", "auto_remove" -> Icons.Default.Delete; else -> Icons.Default.Gavel },
                                null, tint = feedAccent(), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${a.action.replace('_', ' ').replaceFirstChar { it.uppercase() }} \u00B7 ${a.kind}", color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("by ${if (a.actorUid == "system") "Halal Guard" else a.actorUid.take(8)}${if (a.reason.isNotBlank()) " \u2014 ${a.reason}" else ""}",
                                    color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(shortTime(a.at), color = stitchMutedText(), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
    AccountSheet(state, actions)
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = stitchMutedText(), fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(32.dp))
}

/** Screen wrapper. Non-moderators who reach it (deep link) only see a polite dead end. */
@Composable
fun ModeratorPanelScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val repo = remember { runCatching { ModeratorRepository() }.getOrNull() }
    var allowed by remember { mutableStateOf<Boolean?>(null) }
    val state = remember { ModeratorPanelState() }
    LaunchedEffect(repo) { repo?.checkModerator { allowed = it } ?: run { allowed = false } }
    if (allowed != true) {
        Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding()) {
            HalalTopBar("Moderation", { viewModel.goBack() })
            EmptyNote(if (allowed == null) "Checking access\u2026" else "This area is for NoorPro moderators.")
        }
        return
    }
    DisposableEffect(repo) {
        val regs = listOfNotNull(
            runCatching { repo?.observeQueue { state.queue = it } }.getOrNull(),
            runCatching { repo?.observeReports { state.reports = it } }.getOrNull(),
            runCatching { repo?.observeAudit { state.audit = it } }.getOrNull()
        )
        onDispose { regs.forEach { it.remove() } }
    }
    ModeratorPanel(state, ModeratorActions(
        onBack = { viewModel.goBack() },
        onDecide = { action, target, id, days, sensitive ->
            state.busyId = id
            repo?.decide(action, target, id, days = if (days > 0) days else state.suspendDays.toIntOrNull() ?: 7, sensitive = sensitive) { ok, msg ->
                state.busyId = null
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        },
        onLoadStrikes = { uid -> repo?.strikes(uid) { rec -> state.strikes = state.strikes + (uid to rec) } }
    ))
}

// ------------------------------------------------------------------ message requests folder
/** Inbox row that opens / closes the Requests ("stranger") folder. */
@Composable
fun StitchMessageRequestsRow(open: Boolean, count: Int, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(feedMint()).clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp).testTag("msg_requests_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(feedAccent()), contentAlignment = Alignment.Center) {
            Icon(if (open) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.MarkEmailUnread, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (open) "Back to chats" else "Message requests", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(if (open) "Requests from people you don't follow" else "$count from people you don't follow",
                color = stitchMutedText(), fontSize = 12.5.sp)
        }
        if (!open && count > 0) Text("$count", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(feedAccent()).padding(horizontal = 9.dp, vertical = 3.dp))
    }
}

// ------------------------------------------------------------------ sisters / brothers circles
/** Audience picker in "create group" (only shown with the circles flag). */
@Composable
fun CircleAudiencePicker(selected: CircleAudience, me: CircleGender, onSelect: (CircleAudience) -> Unit) {
    Column(Modifier.testTag("circle_audience"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Who can join", color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleAudience.entries.forEach { a ->
                val sel = a == selected
                val allowed = SafetyRules.canJoin(a, me)
                Text(a.label, color = if (sel) Color.White else if (allowed) stitchText() else stitchMutedText(), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (sel) feedAccent() else stitchSurface())
                        .border(1.dp, if (sel) feedAccent() else stitchLine(), RoundedCornerShape(999.dp))
                        .clickable(enabled = allowed) { onSelect(a) }.padding(horizontal = 12.dp, vertical = 7.dp).testTag("audience_${a.name}"))
            }
        }
        if (me == CircleGender.UNSET) Text("Set 'Sister' or 'Brother' privately in Settings \u203A Safety to create a sisters- or brothers-only circle.",
            color = stitchMutedText(), fontSize = 11.5.sp, lineHeight = 15.sp)
    }
}
