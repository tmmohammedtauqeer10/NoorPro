package com.noorpro.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.noorpro.app.data.messaging.GroupTopic
import com.noorpro.app.data.messaging.GroupVisibility
import com.noorpro.app.data.messaging.NoorGroup
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon
import kotlin.math.cos
import kotlin.math.sin

// -------------------------------------------------------------------------------------------
// 1.1.3 sample: NoorPro messaging design kit — brand green #0B6243, cream #FDF9F0, gold, serif
// headings, mint pill tabs and gold-hairline cards (same language as Home 1.0.99 / Ummah 1.1.x).
// -------------------------------------------------------------------------------------------

internal val MsgGreen = Color(0xFF0B6243)
internal val MsgCream = Color(0xFFFDF9F0)

@Composable internal fun msgBg(): Color = if (isStitchLight()) MsgCream else StitchDarkBackground
@Composable internal fun msgCard(): Color = if (isStitchLight()) Color.White else StitchDarkSurface
@Composable internal fun msgGreen(): Color = if (isStitchLight()) MsgGreen else Color(0xFF7FD3AE)
@Composable internal fun msgHair(): Color = if (isStitchLight()) Color(0x33D4AF37) else Color(0x2ED4AF37)
@Composable internal fun msgMint(): Color = feedMint()
@Composable internal fun msgGold(): Color = feedGold()

internal val MsgHeroBrush = Brush.linearGradient(listOf(Color(0xFF0B6243), Color(0xFF064E3B), Color(0xFF033D2E)))

@Composable
internal fun MsgTitle(text: String, size: Int = 30, modifier: Modifier = Modifier) =
    Text(text, color = msgGreen(), fontSize = size.sp, lineHeight = (size + 3).sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon, modifier = modifier)

@Composable
internal fun MsgRoundButton(icon: ImageVector, description: String, modifier: Modifier = Modifier, badge: Int = 0, onClick: () -> Unit = {}) {
    Box(modifier) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(msgMint()).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = msgGreen(), modifier = Modifier.size(20.dp))
        }
        if (badge > 0) MsgBadge(badge, Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp))
    }
}

@Composable
internal fun MsgBadge(count: Int, modifier: Modifier = Modifier, muted: Boolean = false) {
    Box(
        modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp).clip(CircleShape)
            .background(if (muted) stitchMutedText().copy(alpha = 0.5f) else MsgGreen)
            .border(1.5.dp, if (muted) Color.Transparent else StitchGold.copy(alpha = 0.7f), CircleShape)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center
    ) { Text(if (count > 99) "99+" else "$count", color = MsgCream, fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
}

/** Mint segmented control with a surface pill and a gold dot on the selected tab (as in Ummah). */
@Composable
internal fun MsgPillTabs(tabs: List<String>, selected: Int, modifier: Modifier = Modifier, badges: Map<Int, Int> = emptyMap(), onSelect: (Int) -> Unit = {}) {
    Row(modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(999.dp)).background(msgMint()).padding(4.dp).testTag("msg_tabs")) {
        tabs.forEachIndexed { i, t ->
            val on = i == selected
            Row(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(999.dp))
                    .background(if (on) (if (isStitchLight()) Color.White else MsgGreen) else Color.Transparent)
                    .clickable { onSelect(i) }.testTag("msg_tab_$t"),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                if (on) { Box(Modifier.size(5.dp).clip(CircleShape).background(StitchGold)); Spacer(Modifier.width(6.dp)) }
                Text(t, color = if (on) (if (isStitchLight()) MsgGreen else MsgCream) else stitchMutedText(), fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                badges[i]?.takeIf { it > 0 }?.let { Spacer(Modifier.width(6.dp)); MsgBadge(it) }
            }
        }
    }
}

@Composable
internal fun MsgSearchPill(value: String, placeholder: String, modifier: Modifier = Modifier, onValue: (String) -> Unit = {}, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(999.dp)).background(msgCard())
            .border(1.dp, msgHair(), RoundedCornerShape(999.dp)).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = msgGreen(), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = stitchMutedText(), fontSize = 14.5.sp, maxLines = 1)
            BasicTextField(value, onValue, singleLine = true, textStyle = TextStyle(color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(msgGreen()), modifier = Modifier.fillMaxWidth())
        }
        trailing?.invoke()
    }
}

@Composable
internal fun MsgChip(text: String, selected: Boolean = false, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        modifier.height(36.dp).clip(RoundedCornerShape(999.dp))
            .background(if (selected) MsgGreen else msgCard())
            .border(1.dp, if (selected) MsgGreen else stitchLine(), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, tint = if (selected) StitchGold else msgGreen(), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, color = if (selected) MsgCream else stitchText(), fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

/** Small topic tag (mint) used on group rows and cards. */
@Composable
internal fun MsgTopicTag(topic: GroupTopic?, modifier: Modifier = Modifier) {
    if (topic == null) return
    Row(modifier.clip(RoundedCornerShape(999.dp)).background(msgMint()).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(topicIcon(topic.id), null, tint = msgGreen(), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(topic.label, color = msgGreen(), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

internal fun topicIcon(id: String): ImageVector = when (id) {
    "quran" -> Icons.Default.MenuBook
    "hadith" -> Icons.Default.AutoStories
    "duas" -> Icons.Default.NightsStay
    "masjid" -> Icons.Default.Mosque
    "sisters" -> Icons.Default.Woman
    "brothers" -> Icons.Default.Man
    "youth" -> Icons.Default.School
    "reverts" -> Icons.Default.Favorite
    "hajj" -> Icons.Default.FlightTakeoff
    "ramadan" -> Icons.Default.Brightness3
    "arabic" -> Icons.Default.Translate
    "family" -> Icons.Default.FamilyRestroom
    "charity" -> Icons.Default.VolunteerActivism
    else -> Icons.Default.Groups
}

internal fun visibilityIcon(v: GroupVisibility): ImageVector = when (v) {
    GroupVisibility.PUBLIC_OPEN -> Icons.Default.Public
    GroupVisibility.PUBLIC_REQUEST -> Icons.Default.HowToReg
    GroupVisibility.PRIVATE -> Icons.Default.Lock
}

/** Image from the preview map (renders), else network, else [fallback]. */
@Composable
internal fun MsgImage(url: String, modifier: Modifier, fallback: @Composable () -> Unit) {
    val preview = UmmahFeedPreview.images[url]
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            preview != null -> Image(preview.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            url.startsWith("http") -> AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> fallback()
        }
    }
}

/** Person avatar with an optional emerald online dot (cream ring). */
@Composable
internal fun MsgPersonAvatar(name: String, photoUrl: String, size: Dp = 52.dp, online: Boolean = false, ring: Boolean = false) {
    Box {
        UmmahAvatar(name, photoUrl, size, ring = if (ring) StitchGold else null)
        if (online) Box(
            Modifier.align(Alignment.BottomEnd).size(size * 0.27f).clip(CircleShape).background(msgBg()).padding(2.dp)
                .clip(CircleShape).background(Color(0xFF1FA66A))
        )
    }
}

/** Group avatar: rounded-square photo, or the topic glyph on an emerald tile with a gold hairline. */
@Composable
internal fun MsgGroupAvatar(g: NoorGroup, size: Dp = 52.dp) {
    val shape = RoundedCornerShape(size * 0.3f)
    MsgImage(g.photoUrl, Modifier.size(size).clip(shape).background(MsgHeroBrush).border(1.dp, StitchGold.copy(alpha = 0.45f), shape)) {
        Icon(topicIcon(g.topicId), null, tint = StitchGold, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
internal fun MsgTopBar(title: String, subtitle: String = "", onBack: () -> Unit = {}, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 6.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = msgGreen())
        }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            MsgTitle(title, 24)
            if (subtitle.isNotBlank()) Text(subtitle, color = stitchMutedText(), fontSize = 12.5.sp)
        }
        actions()
    }
}

@Composable
internal fun MsgSectionLabel(text: String, modifier: Modifier = Modifier, trailing: String = "") {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(14.dp).height(2.dp).background(msgGold()))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), color = msgGold(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
        if (trailing.isNotBlank()) Text(trailing, color = msgGreen(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun MsgPrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, gold: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        modifier.height(46.dp).clip(RoundedCornerShape(999.dp))
            .background(if (gold) Brush.linearGradient(listOf(Color(0xFFE5C45A), StitchGold)) else Brush.linearGradient(listOf(Color(0xFF0E7A55), MsgGreen)))
            .clickable(onClick = onClick).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, tint = if (gold) StitchEmeraldDeep else StitchGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = if (gold) StitchEmeraldDeep else MsgCream, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
    }
}

/** Join / Request / Open pill for group cards. */
@Composable
internal fun MsgJoinButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val filled = label == "Join" || label == "Request" || label == "Follow"
    Box(
        modifier.height(34.dp).clip(RoundedCornerShape(999.dp))
            .background(if (filled) MsgGreen else msgMint())
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (filled) MsgCream else msgGreen(), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

/** Faint eight-point-star lattice — the NoorPro chat wallpaper (original, generated). */
internal fun Modifier.msgWallpaper(light: Boolean): Modifier = this
    .background(if (light) Color(0xFFF8F2E4) else Color(0xFF0A1512))
    .drawBehind {
        val step = 46.dp.toPx(); val r = 7.dp.toPx()
        val c = if (light) Color(0x14A9842A) else Color(0x12D4AF37)
        var y = step / 2; var row = 0
        while (y < size.height + step) {
            var x = if (row % 2 == 0) step / 2 else step
            while (x < size.width + step) {
                val p = Path()
                for (k in 0 until 16) {
                    val a = Math.PI / 8 * k
                    val rr = if (k % 2 == 0) r else r * 0.55f
                    val px = x + (rr * cos(a)).toFloat(); val py = y + (rr * sin(a)).toFloat()
                    if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
                }
                p.close(); drawPath(p, c)
                drawCircle(c, 1.2.dp.toPx(), Offset(x + step / 2, y))
                x += step
            }
            y += step * 0.8f; row++
        }
    }

internal fun msgTime(ts: Long, now: Long = System.currentTimeMillis()): String {
    val d = now - ts
    return when {
        d < 60_000 -> "now"
        d < 3_600_000 -> "${d / 60_000}m"
        android.text.format.DateUtils.isToday(ts) -> android.text.format.DateFormat.format("h:mm a", ts).toString()
        d < 2 * 86_400_000 -> "Yesterday"
        else -> android.text.format.DateFormat.format("d MMM", ts).toString()
    }
}

/** QR for "add me" / group invite — generated locally with ZXing (no backend). */
internal fun msgQrBitmap(content: String, size: Int = 480): android.graphics.Bitmap? = runCatching {
    val m = com.google.zxing.qrcode.QRCodeWriter().encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size,
        mapOf(com.google.zxing.EncodeHintType.MARGIN to 1))
    val px = IntArray(size * size) { i -> if (m.get(i % size, i / size)) 0xFF0B6243.toInt() else 0xFFFDF9F0.toInt() }
    android.graphics.Bitmap.createBitmap(px, size, size, android.graphics.Bitmap.Config.ARGB_8888)
}.getOrNull()
