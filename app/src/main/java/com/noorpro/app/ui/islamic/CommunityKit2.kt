package com.noorpro.app.ui.islamic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import kotlinx.coroutines.tasks.await

/*
 * 1.2.0 (115) community refresh (Halaqa, Ask a Scholar, Dua requests) in the Noor AI language:
 * calm surfaces, one clear primary action, clean empty states, and a verified-scholar badge.
 */

/** Compact intro: gold accent bar, short hadith/ayah, source, one primary action. */
@Composable
internal fun IkIntroCard(eyebrow: String, quote: String, source: String, actionLabel: String, actionIcon: ImageVector, actionTag: String,
                         modifier: Modifier = Modifier, onAction: () -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(20.dp)).padding(16.dp)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(IkGold))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(eyebrow.uppercase(), color = IkGoldDeep, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(4.dp))
                Text(quote, color = stitchText(), fontFamily = LibreCaslon, fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(4.dp))
                Text(source, color = stitchMutedText(), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(IkGreen).clickable(onClick = onAction).padding(vertical = 12.dp).testTag(actionTag),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(actionIcon, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/** Empty state: icon in a soft circle, a title, one helpful line and an optional action. The [tag] node carries the body text. */
@Composable
internal fun IkEmptyState(icon: ImageVector, title: String, body: String, tag: String, modifier: Modifier = Modifier,
                          actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(18.dp))
        .border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).padding(vertical = 22.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(ikSoft()), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = ikAccentText(), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(body, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.testTag(tag))
        if (actionLabel != null) {
            Spacer(Modifier.height(12.dp))
            Text(actionLabel, color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(ikSoft()).clickable(onClick = onAction).padding(horizontal = 14.dp, vertical = 7.dp))
        }
    }
}

/** Section title with an optional count bubble ("Live now  2"). */
@Composable
internal fun IkCountSection(text: String, count: Int?, modifier: Modifier = Modifier, live: Boolean = false) {
    Row(modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (live) { Box(Modifier.size(8.dp).clip(CircleShape).background(IkRed)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (count != null && count > 0) {
            Spacer(Modifier.width(8.dp))
            Text("$count", color = ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(ikSoft()).padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}

/** Gold "Verified scholar" badge (scholars/{uid}.verified == true). */
@Composable
internal fun VerifiedScholarBadge(modifier: Modifier = Modifier, compact: Boolean = false, tag: String = "verified_scholar_badge") {
    if (compact) {
        Icon(Icons.Filled.Verified, "Verified scholar", tint = IkGold, modifier = modifier.size(15.dp).testTag(tag))
        return
    }
    Row(modifier.clip(RoundedCornerShape(50)).background(IkGold.copy(alpha = 0.16f)).padding(horizontal = 7.dp, vertical = 2.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Verified, null, tint = IkGoldDeep, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text("Verified scholar", color = IkGoldDeep, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** Session cache of verified scholar uids (public scholars collection; no new rules). */
object ScholarDirectory {
    @Volatile private var cache: Set<String>? = null
    @Volatile private var at = 0L
    suspend fun verifiedUids(): Set<String> {
        val c = cache
        if (c != null && System.currentTimeMillis() - at < 10 * 60_000) return c
        val db = runCatching { com.google.firebase.firestore.FirebaseFirestore.getInstance() }.getOrNull() ?: return emptySet()
        val s = runCatching {
            db.collection("scholars").whereEqualTo("verified", true).limit(500).get().await().documents.map { it.id }.toSet()
        }.getOrNull() ?: return c ?: emptySet()
        cache = s; at = System.currentTimeMillis()
        return s
    }
}
