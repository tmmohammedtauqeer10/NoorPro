package com.noorpro.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.onboarding.ApplicationStatus
import com.noorpro.app.data.onboarding.Maslak
import com.noorpro.app.data.onboarding.ScholarApplication
import com.noorpro.app.data.onboarding.ScholarRules
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.islamic.IkAmber
import com.noorpro.app.ui.islamic.IkButton
import com.noorpro.app.ui.islamic.IkField
import com.noorpro.app.ui.islamic.IkGold
import com.noorpro.app.ui.islamic.IkGreen
import com.noorpro.app.ui.islamic.IkHeader
import com.noorpro.app.ui.islamic.IkHero
import com.noorpro.app.ui.islamic.IkIconTile
import com.noorpro.app.ui.islamic.IkNotice
import com.noorpro.app.ui.islamic.IkPill
import com.noorpro.app.ui.islamic.IkRed
import com.noorpro.app.ui.islamic.ikAccentText
import com.noorpro.app.ui.islamic.ikCard
import com.noorpro.app.ui.islamic.ikScreen
import com.noorpro.app.ui.islamic.ikSoft
import com.noorpro.app.ui.theme.LibreCaslon

data class ScholarApplyUiState(
    val form: ScholarApplication = ScholarApplication(),
    /** The applicant's saved application (null = none / still loading). */
    val existing: ScholarApplication? = null,
    val editing: Boolean = false,
    val certLabel: String? = null,
    /** Local preview of the chosen certificate (Uri / url), images only. */
    val certPreview: Any? = null,
    val showErrors: Boolean = false,
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = true,
)

class ScholarApplyActions(
    val onBack: () -> Unit = {},
    val onForm: (ScholarApplication) -> Unit = {},
    val onPickCertificate: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onApplyAgain: () -> Unit = {},
)

private val TITLES = listOf("Mufti", "Maulana", "Shaykh", "Alimah", "Hafiz", "Qari")

@Composable
fun ScholarApplyContent(s: ScholarApplyUiState, a: ScholarApplyActions, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().statusBarsPadding().imePadding().testTag("scholar_apply")) {
        IkHeader("Apply as a scholar", "Verified scholars answer in Ask a Scholar", onBack = a.onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp).navigationBarsPadding()) {
            when {
                !s.signedIn -> IkNotice("Sign in first", "Sign in to apply as a scholar.")
                s.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = IkGreen) }
                s.existing != null && s.existing.status != ApplicationStatus.NONE && !s.editing -> StatusCard(s.existing, a)
                else -> Form(s, a)
            }
        }
    }
}

@Composable
private fun StatusCard(app: ScholarApplication, a: ScholarApplyActions) {
    val (icon, tint, title, body) = when (app.status) {
        ApplicationStatus.PENDING -> Quad(Icons.Outlined.HourglassTop, IkAmber, "Under review",
            "JazakAllahu khairan. An admin will check your ijazah and reference. You'll see the decision here.")
        ApplicationStatus.APPROVED -> Quad(Icons.Outlined.WorkspacePremium, IkGold, "You're a verified scholar",
            "Your scholar badge is on your profile, and questions waiting in Ask a Scholar appear in your inbox there.")
        ApplicationStatus.REJECTED -> Quad(Icons.Outlined.ErrorOutline, IkRed, "Not approved yet", app.reason.ifBlank { "Your application wasn't approved." })
        ApplicationStatus.REVOKED -> Quad(Icons.Outlined.ErrorOutline, IkRed, "Verification removed", app.reason.ifBlank { "Your scholar verification was removed." })
        ApplicationStatus.NONE -> Quad(Icons.Outlined.Description, IkGreen, "", "")
    }
    Spacer(Modifier.height(6.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(22.dp)).padding(18.dp).testTag("scholar_status_${app.status.code}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkIconTile(icon, size = 46.dp, tint = tint, bg = tint.copy(alpha = 0.12f))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("APPLICATION STATUS", color = stitchMutedText(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text(title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(body, color = stitchMutedText(), fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(14.dp))
        KV("Name", "${app.title} ${app.name}".trim())
        KV("Institution", app.institution)
        KV("Madhhab", app.maslak?.label.orEmpty())
        if (app.years != null) KV("Teaching", "${app.years} years")
    }
    if (ScholarRules.canApply(app.status)) {
        Spacer(Modifier.height(14.dp))
        IkButton("Apply again", Modifier.fillMaxWidth(), tag = "scholar_apply_again", onClick = a.onApplyAgain)
    }
}

private data class Quad(val a: ImageVector, val b: Color, val c: String, val d: String)

@Composable
internal fun KV(k: String, v: String) {
    if (v.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(k, color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.width(104.dp))
        Text(v, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FieldError(s: ScholarApplyUiState, key: String) {
    if (!s.showErrors) return
    val e = ScholarRules.problems(s.form, s.certLabel != null || s.form.certPath.isNotBlank())[key] ?: return
    Text(e, color = IkRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp).testTag("err_$key"))
}

@Composable
private fun Label(t: String, optional: Boolean = false) {
    Row(Modifier.padding(top = 16.dp, bottom = 6.dp)) {
        Text(t, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (optional) Text("  optional", color = stitchMutedText(), fontSize = 12.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Form(s: ScholarApplyUiState, a: ScholarApplyActions) {
    val f = s.form
    IkHero(Modifier.padding(top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WorkspacePremium, null, tint = IkGold, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(10.dp))
            Text("Share the knowledge you carry", color = Color.White, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text("An admin verifies every scholar by their ijazah and a reference before they can answer questions.", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 18.sp)
    }
    Label("Full name"); IkField(f.name, { a.onForm(f.copy(name = it)) }, "Full name", tag = "sa_name", maxLen = 80); FieldError(s, "name")
    Label("Title")
    IkField(f.title, { a.onForm(f.copy(title = it)) }, "Mufti, Maulana, Alimah…", tag = "sa_title", maxLen = 80)
    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TITLES.forEach { t -> IkPill(t, f.title.equals(t, true)) { a.onForm(f.copy(title = t)) } }
    }
    FieldError(s, "title")
    Label("Madrasa / Dar-ul-Ifta"); IkField(f.institution, { a.onForm(f.copy(institution = it)) }, "Where you studied or serve", tag = "sa_institution", maxLen = 120); FieldError(s, "institution")
    Label("Maslak")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Maslak.entries.forEach { m -> IkPill(m.label, f.maslak == m, tag = "sa_maslak_${m.code}") { a.onForm(f.copy(maslak = m)) } }
    }
    FieldError(s, "maslak")
    Label("Tasawwuf silsila", optional = true); IkField(f.silsila, { a.onForm(f.copy(silsila = it)) }, "e.g. Chishti, Naqshbandi, Qadiri", tag = "sa_silsila", maxLen = 120)
    Label("Years of teaching")
    IkField(f.years?.toString().orEmpty(), { v -> a.onForm(f.copy(years = v.filter { it.isDigit() }.take(2).toIntOrNull())) }, "Years", tag = "sa_years", maxLen = 2); FieldError(s, "years")
    Label("Ijazah / certificate")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikSoft()).border(1.dp, if (s.showErrors && s.certLabel == null && f.certPath.isBlank()) IkRed else stitchLine(), RoundedCornerShape(16.dp))
            .clickable(onClick = a.onPickCertificate).padding(12.dp).testTag("sa_cert"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(ikCard()), contentAlignment = Alignment.Center) {
            if (s.certPreview != null) coil.compose.AsyncImage(model = s.certPreview, contentDescription = "Certificate", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(if (s.certLabel != null) Icons.Outlined.Description else Icons.Outlined.AddPhotoAlternate, null, tint = ikAccentText())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.certLabel ?: if (f.certPath.isNotBlank()) "Certificate on file" else "Add a photo or PDF", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text("Clear photo of your ijazah or sanad · max 10 MB", color = stitchMutedText(), fontSize = 12.sp)
        }
        Text(if (s.certLabel != null || f.certPath.isNotBlank()) "Change" else "Choose", color = ikAccentText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
    FieldError(s, "cert")
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Lock, null, tint = stitchMutedText(), modifier = Modifier.size(13.dp)); Spacer(Modifier.width(5.dp))
        Text("Only you and Noor Pro admins can see your certificate.", color = stitchMutedText(), fontSize = 12.sp)
    }
    Label("Reference")
    IkField(f.refName, { a.onForm(f.copy(refName = it)) }, "Teacher or institution contact name", tag = "sa_ref_name", maxLen = 80); FieldError(s, "refName")
    Spacer(Modifier.height(8.dp))
    IkField(f.refPhone, { a.onForm(f.copy(refPhone = it)) }, "Phone with country code", tag = "sa_ref_phone", maxLen = 22); FieldError(s, "refPhone")
    Label("Short bio")
    IkField(f.bio, { a.onForm(f.copy(bio = it)) }, "Your studies, teachers and teaching", tag = "sa_bio", singleLine = false, maxLen = 600)
    Text("${f.bio.trim().length}/600", color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    FieldError(s, "bio")
    s.error?.let { Text(it, color = IkRed, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp).testTag("sa_error")) }
    Spacer(Modifier.height(18.dp))
    IkButton(if (s.submitting) "Submitting…" else "Submit application", Modifier.fillMaxWidth(), icon = Icons.Outlined.CheckCircle, enabled = !s.submitting, tag = "sa_submit", onClick = a.onSubmit)
}
