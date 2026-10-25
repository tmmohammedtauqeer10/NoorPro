package com.noorpro.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.recs.PeopleRecommender
import com.noorpro.app.data.recs.PersonSuggestion
import com.noorpro.app.data.recs.SuggestionKind
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.theme.LibreCaslon

/**
 * 1.1.4 — "Recommended for you": a NoorPro-styled carousel of people cards inserted into the
 * For You feed. Cream/surface cards with a gold avatar ring, serif heading, mint reason chip and
 * an emerald Follow pill that flips to "Following" instantly (optimistic, like the post cards).
 */
@Composable
internal fun UmmahSuggestionsRow(
    suggestions: List<PersonSuggestion>,
    following: Set<String>,
    onFollow: (PersonSuggestion) -> Unit,
    onDismiss: (PersonSuggestion) -> Unit,
    onHideRow: () -> Unit,
    onOpen: (PersonSuggestion) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    Column(
        modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)).background(stitchSurface())
            .border(1.dp, stitchLine().copy(alpha = if (isStitchLight()) 0.55f else 0.35f), RoundedCornerShape(24.dp))
            .padding(top = 14.dp, bottom = 14.dp)
            .testTag("ummah_suggestions")
    ) {
        Row(Modifier.padding(start = 18.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 14.dp, height = 2.dp).background(feedGold()))
                    Spacer(Modifier.width(6.dp))
                    Text("FOR YOUR CIRCLE", color = feedGold(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                Text("Recommended for you", color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
            }
            Text("See all", color = feedAccent(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onSeeAll).padding(horizontal = 10.dp, vertical = 6.dp))
            Box(
                Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onHideRow).testTag("ummah_suggestions_hide"),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Close, "Hide recommendations", tint = stitchMutedText(), modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(suggestions, key = { it.uid }) { s ->
                SuggestionCard(s, s.uid in following, { onFollow(s) }, { onDismiss(s) }, { onOpen(s) })
            }
        }
    }
}

@Composable
private fun SuggestionCard(s: PersonSuggestion, isFollowing: Boolean, onFollow: () -> Unit, onDismiss: () -> Unit, onOpen: () -> Unit) {
    val light = isStitchLight()
    Box(
        Modifier.width(158.dp).clip(RoundedCornerShape(20.dp))
            .background(if (light) Color(0xFFFDF9F0) else Color(0xFF102A21))
            .border(1.dp, if (light) Color(0xFFEADFC4) else Color(0xFF214A3B), RoundedCornerShape(20.dp))
            .testTag("suggestion_${s.uid}")
    ) {
        Column(
            Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UmmahAvatar(s.name, s.photoUrl, 70.dp, ring = StitchGold)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.name, color = stitchText(), fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (s.verified) {
                    Spacer(Modifier.width(3.dp))
                    Icon(Icons.Default.Verified, "Verified", tint = feedGold(), modifier = Modifier.size(15.dp))
                }
            }
            Text(
                listOfNotNull(s.handle.ifBlank { null }, s.followerCount?.let { "${PeopleRecommender.formatCount(it)} followers" }).joinToString(" · "),
                color = stitchMutedText(), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(999.dp)).background(feedMint()).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(reasonIcon(s.kind), null, tint = feedAccent(), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(s.reason, color = feedAccent(), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))
            val bg by animateColorAsState(if (isFollowing) Color.Transparent else feedAccent(), label = "followBg")
            Row(
                Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(999.dp)).background(bg)
                    .border(1.dp, if (isFollowing) stitchLine() else Color.Transparent, RoundedCornerShape(999.dp))
                    .clickable(onClick = onFollow).testTag("suggestion_follow_${s.uid}"),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd, null,
                    tint = if (isFollowing) stitchText() else (if (light) Color.White else Color(0xFF06291D)), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (isFollowing) "Following" else "Follow", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (isFollowing) stitchText() else (if (light) Color.White else Color(0xFF06291D)))
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(CircleShape).background(feedMint().copy(alpha = 0.9f))
                .clickable(onClick = onDismiss).testTag("suggestion_dismiss_${s.uid}"),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Close, "Dismiss ${s.name}", tint = stitchMutedText(), modifier = Modifier.size(14.dp)) }
    }
}

private fun reasonIcon(kind: SuggestionKind) = when (kind) {
    SuggestionKind.MUTUAL -> Icons.Default.Groups
    SuggestionKind.INTEREST -> Icons.Default.MenuBook
    SuggestionKind.NEARBY -> Icons.Default.LocationOn
    SuggestionKind.ACTIVE, SuggestionKind.POPULAR -> Icons.Default.TrendingUp
}
