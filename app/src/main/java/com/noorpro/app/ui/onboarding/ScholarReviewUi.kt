package com.noorpro.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.onboarding.ScholarApplication
import com.noorpro.app.data.onboarding.ScholarRules
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.islamic.IkAvatar
import com.noorpro.app.ui.islamic.IkButton
import com.noorpro.app.ui.islamic.IkCard
import com.noorpro.app.ui.islamic.IkField
import com.noorpro.app.ui.islamic.IkGold
import com.noorpro.app.ui.islamic.IkGreen
import com.noorpro.app.ui.islamic.IkHeader
import com.noorpro.app.ui.islamic.IkNotice
import com.noorpro.app.ui.islamic.IkRed
import com.noorpro.app.ui.islamic.IkSegments
import com.noorpro.app.ui.islamic.IkTag
import com.noorpro.app.ui.islamic.ikAccentText
import com.noorpro.app.ui.islamic.ikCard
import com.noorpro.app.ui.islamic.ikScreen
import com.noorpro.app.ui.islamic.ikSoft
import com.noorpro.app.ui.theme.LibreCaslon

data class ScholarReviewUiState(
    /** null = still checking admin access. */
    val isModerator: Boolean? = null,
    val tab: Int = 0,
    val pending: List<ScholarApplication> = emptyList(),
    val approved: List<ScholarApplication> = emptyList(),
    val selected: ScholarApplication? = null,
    val certUrl: String? = null,
    val certIsPdf: Boolean = false,
    /** "reject" / "revoke" while the reason box is open. */
    val decision: String? = null,
    val reason: String = "",
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val now: Long = System.currentTimeMillis(),
)

class ScholarReviewActions(
    val onBack: () -> Unit = {},
    val onTab: (Int) -> Unit = {},
    val onOpen: (ScholarApplication) -> Unit = {},
    val onCloseDetail: () -> Unit = {},
    val onOpenCertificate: () -> Unit = {},
    val onApprove: () -> Unit = {},
    val onStartDecision: (String?) -> Unit = {},
    val onReason: (String) -> Unit = {},
    val onConfirmDecision: () -> Unit = {},
)

private fun ago(ms: Long, now: Long): String {
    if (ms <= 0) return ""
    val m = (now - ms) / 60000
    return when { m < 60 -> "${m.coerceAtLeast(1)}m ago"; m < 1440 -> "${m / 60}h ago"; else -> "${m / 1440}d ago" }
}

@Composable
fun ScholarReviewContent(s: ScholarReviewUiState, a: ScholarReviewActions, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().statusBarsPadding().imePadding().testTag("scholar_review")) {
        val sel = s.selected
        IkHeader(if (sel == null) "Scholar applications" else "Review application", if (sel == null) "Admins only" else "${sel.title} ${sel.name}".trim(),
            onBack = if (sel == null) a.onBack else a.onCloseDetail)
        when {
            s.isModerator == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = IkGreen) }
            s.isModerator == false -> IkNotice("Admins only", "This page is for Noor Pro admins. Your account isn't an admin.", Modifier.padding(18.dp), tag = "review_denied")
            sel != null -> Detail(s, sel, a)
            else -> ListPane(s, a)
        }
    }
}

@Composable
private fun ListPane(s: ScholarReviewUiState, a: ScholarReviewActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
        IkSegments(listOf("Pending (${s.pending.size})", "Verified (${s.approved.size})"), s.tab, tagPrefix = "review_tab") { a.onTab(it) }
        Spacer(Modifier.height(14.dp))
        if (s.loading) Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = IkGreen) }
        s.error?.let { IkNotice("Couldn't load", it, Modifier.padding(bottom = 12.dp)) }
        val list = if (s.tab == 0) s.pending else s.approved
        if (!s.loading && list.isEmpty()) IkNotice(if (s.tab == 0) "No pending applications" else "No verified scholars yet",
            if (s.tab == 0) "New applications will appear here." else "Approved scholars appear here; you can revoke verification.")
        list.forEach { app ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(18.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp))
                    .clickable { a.onOpen(app) }.padding(14.dp).testTag("review_item_${app.uid}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IkAvatar(app.name, size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${app.title} ${app.name}".trim(), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    Text(app.institution, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        app.maslak?.let { IkTag(it.label) }
                        app.years?.let { IkTag("$it yrs teaching", bg = IkGold.copy(alpha = 0.16f), fg = stitchText()) }
                    }
                }
                Text(ago(if (s.tab == 0) app.submittedAt else app.decidedAt, s.now), color = stitchMutedText(), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Detail(s: ScholarReviewUiState, app: ScholarApplication, a: ScholarReviewActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
        // Certificate
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).background(ikSoft()).border(1.dp, stitchLine(), RoundedCornerShape(20.dp))
                .clickable(onClick = a.onOpenCertificate).testTag("review_cert"),
            contentAlignment = Alignment.Center,
        ) {
            if (s.certUrl != null && !s.certIsPdf) coil.compose.AsyncImage(model = s.certUrl, contentDescription = "Ijazah certificate", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp))
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (s.certIsPdf) Icons.Outlined.Description else Icons.Outlined.Image, null, tint = ikAccentText(), modifier = Modifier.size(42.dp))
                Text(if (s.certIsPdf) "Certificate (PDF)" else if (app.certPath.isBlank()) "No certificate" else "Ijazah certificate", color = stitchText(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            }
            Row(Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.OpenInNew, null, tint = Color.White, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp))
                Text("Open full size", color = Color.White, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        IkCard {
            Text("${app.title} ${app.name}".trim(), color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            KV("Institution", app.institution)
            KV("Maslak", app.maslak?.label.orEmpty())
            KV("Silsila", app.silsila)
            KV("Teaching", app.years?.let { "$it years" }.orEmpty())
            KV("Reference", app.refName)
            KV("Ref. phone", app.refPhone)
            KV("Submitted", ago(app.submittedAt, s.now))
        }
        Spacer(Modifier.height(10.dp))
        IkCard {
            Text("Bio", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(app.bio, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(16.dp))
        s.error?.let { Text(it, color = IkRed, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp).testTag("review_error")) }
        val pending = s.tab == 0
        if (s.decision != null) {
            Text(if (s.decision == "reject") "Reason for rejection (shown to the applicant)" else "Reason for revoking (shown to the scholar)",
                color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
            IkField(s.reason, a.onReason, "Reason", tag = "review_reason", singleLine = false, maxLen = 500)
            ScholarRules.rejectProblem(s.reason)?.takeIf { s.reason.isNotEmpty() }?.let { Text(it, color = IkRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IkButton("Cancel", Modifier.weight(1f), primary = false, enabled = !s.busy) { a.onStartDecision(null) }
                IkButton(if (s.decision == "reject") "Reject" else "Revoke", Modifier.weight(1f), icon = Icons.Outlined.Close,
                    enabled = !s.busy && ScholarRules.rejectProblem(s.reason) == null, tag = "review_confirm", onClick = a.onConfirmDecision)
            }
        } else if (pending) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IkButton("Reject", Modifier.weight(1f), icon = Icons.Outlined.Close, primary = false, enabled = !s.busy, tag = "review_reject") { a.onStartDecision("reject") }
                IkButton(if (s.busy) "Approving…" else "Approve", Modifier.weight(1f), icon = Icons.Outlined.Verified, enabled = !s.busy, tag = "review_approve", onClick = a.onApprove)
            }
            Text("Approving adds them to verified scholars (they can answer in Ask a Scholar) and puts a scholar badge on their profile.",
                color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp))
        } else {
            IkButton("Revoke verification", Modifier.fillMaxWidth(), icon = Icons.Outlined.Block, primary = false, enabled = !s.busy, tag = "review_revoke") { a.onStartDecision("revoke") }
        }
    }
}
