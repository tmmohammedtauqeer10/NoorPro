package com.noorpro.app.ui.islamic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.islamic.community.CommunityReports
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText

/* 1.1.6 shared pieces for the community screens (dua requests, halaqa, Ask a Scholar, challenges). */

/** Initials avatar in the NoorNotify style (emerald gradient, gold ring); a quiet silhouette for anonymous. */
@Composable
internal fun IkAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp, anonymous: Boolean = false) {
    Box(modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF0B3D31), Color(0xFF0E8C73))))
        .border(1.5.dp, IkGold.copy(alpha = 0.85f), CircleShape), contentAlignment = Alignment.Center) {
        if (anonymous || name.isBlank()) Icon(Icons.Outlined.PersonOutline, null, tint = IkCream, modifier = Modifier.size(size * 0.55f))
        else Text(com.noorpro.app.notify.NoorNotify.initials(name), color = IkCream, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp, maxLines = 1)
    }
}

/** Small rounded tag (category, "Answered", "Scholar"…). */
@Composable
internal fun IkTag(text: String, modifier: Modifier = Modifier, bg: Color = ikSoft(), fg: Color = ikAccentText()) {
    Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, letterSpacing = 0.3.sp,
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** Horizontally scrolling pill row. */
@Composable
internal fun <T> IkPillRow(items: List<T>, selected: T, label: (T) -> String, modifier: Modifier = Modifier, tag: (T) -> String? = { null }, onPick: (T) -> Unit) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { IkPill(label(it), it == selected, tag = tag(it)) { onPick(it) } }
    }
}

/** Two-way segmented switch used for "Community / Mine"-style tabs. */
@Composable
internal fun IkSegments(options: List<String>, selected: Int, modifier: Modifier = Modifier, tagPrefix: String = "seg", onPick: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ikSoft()).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (i == selected) ikCard() else Color.Transparent)
                .clickable { onPick(i) }.padding(vertical = 9.dp).testTag("${tagPrefix}_$i"), contentAlignment = Alignment.Center) {
                Text(o, color = if (i == selected) stitchText() else stitchMutedText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

/** Report sheet that feeds the existing Halal Guard pipeline (ummah_reports → Moderator panel). */
@Composable
internal fun IkReportDialog(what: String, onDismiss: () -> Unit, onReport: (String) -> Unit) {
    var reason by remember { mutableStateOf(CommunityReports.REASONS.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this $what", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Our moderators review every report. The person isn't told who reported.", fontSize = 13.sp, color = stitchMutedText())
                Spacer(Modifier.height(8.dp))
                CommunityReports.REASONS.forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable { reason = r }.testTag("report_reason_$r"), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = r == reason, onClick = { reason = r })
                        Text(r, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onReport(reason) }, modifier = Modifier.testTag("report_send")) { Text("Report") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** One Ummah shortcut (shown only while its flag is on). */
internal data class UmmahLink(val flag: String, val title: String, val sub: String, val icon: androidx.compose.ui.graphics.vector.ImageVector,
                              val screen: com.noorpro.app.ui.viewmodel.DeenScreen)

object UmmahCommunityLinks {
    internal val all: List<UmmahLink> get() = listOf(
        UmmahLink(com.noorpro.app.islamic.IslamicFlags.DUA_REQUESTS, "Dua requests", "Say Ameen", androidx.compose.material.icons.Icons.Outlined.VolunteerActivism,
            com.noorpro.app.ui.viewmodel.DeenScreen.UMMAH_DUA_REQUESTS),
    )
    internal fun enabled() = all.filter { com.noorpro.app.islamic.IslamicFlags.isOn(it.flag) }
    fun any() = enabled().isNotEmpty()
}

/** "Gather" row in the Ummah feed: dua requests, halaqa rooms, Ask a Scholar, challenges. */
@Composable
fun UmmahCommunityShortcuts(modifier: Modifier = Modifier, onOpen: (com.noorpro.app.ui.viewmodel.DeenScreen) -> Unit) {
    val links = UmmahCommunityLinks.enabled()
    if (links.isEmpty()) return
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp).testTag("ummah_community"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        links.forEach { l ->
            Row(Modifier.width(176.dp).clip(RoundedCornerShape(18.dp)).background(ikCard()).border(1.dp, com.noorpro.app.ui.components.stitchLine(), RoundedCornerShape(18.dp))
                .clickable { onOpen(l.screen) }.padding(10.dp).testTag("ummah_link_${l.screen.name}"), verticalAlignment = Alignment.CenterVertically) {
                IkIconTile(l.icon, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(l.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    Text(l.sub, color = stitchMutedText(), fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}
