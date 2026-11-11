package com.noorpro.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
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
import com.noorpro.app.data.onboarding.AgeRules
import com.noorpro.app.data.onboarding.OnbGender
import com.noorpro.app.data.onboarding.OnbStep
import com.noorpro.app.data.onboarding.OnboardingDraft
import com.noorpro.app.data.onboarding.OnboardingRules
import com.noorpro.app.data.onboarding.PRIMARY_LANGUAGES
import com.noorpro.app.data.onboarding.ProfileType
import com.noorpro.app.data.onboarding.SimpleDate
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.islamic.IkButton
import com.noorpro.app.ui.islamic.IkField
import com.noorpro.app.ui.islamic.IkGold
import com.noorpro.app.ui.islamic.IkGreen
import com.noorpro.app.ui.islamic.IkIconTile
import com.noorpro.app.ui.islamic.IkLattice
import com.noorpro.app.ui.islamic.IkNotice
import com.noorpro.app.ui.islamic.IkPill
import com.noorpro.app.ui.islamic.IkRed
import com.noorpro.app.ui.islamic.ikAccentText
import com.noorpro.app.ui.islamic.ikCard
import com.noorpro.app.ui.islamic.ikScreen
import com.noorpro.app.ui.islamic.ikSoft
import com.noorpro.app.ui.theme.LibreCaslon

/** Username availability shown under the field. */
enum class NameCheck { IDLE, CHECKING, AVAILABLE, TAKEN, OFFLINE }

data class OnboardingUiState(
    val step: OnbStep = OnbStep.NAME,
    val draft: OnboardingDraft = OnboardingDraft(),
    val today: SimpleDate = SimpleDate(2026, 11, 11),
    val nameCheck: NameCheck = NameCheck.IDLE,
    val suggestions: List<String> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
    /** Steps that were already complete when an existing user is asked again (shown as done). */
    val firstStep: OnbStep = OnbStep.NAME,
)

class OnboardingActions(
    val onDraft: (OnboardingDraft) -> Unit = {},
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onApplyScholar: () -> Unit = {},
)

private val COMMON_COUNTRIES = listOf("India", "Pakistan", "Bangladesh", "United Kingdom", "United States", "UAE", "Saudi Arabia", "Canada")

/** Full-screen, multi-step onboarding in the app's emerald / cream / gold language. Pure state -> UI (previewable). */
@Composable
fun OnboardingContent(s: OnboardingUiState, a: OnboardingActions, modifier: Modifier = Modifier) {
    val steps = OnboardingRules.STEPS
    val idx = steps.indexOf(s.step)
    Column(modifier.ikScreen().statusBarsPadding().navigationBarsPadding().imePadding().testTag("onboarding")) {
        // Top bar: back + segmented progress + step count
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val canBack = s.step != OnbStep.DONE && idx > steps.indexOf(s.firstStep)
            Box(Modifier.size(42.dp).clip(CircleShape).clickable(enabled = canBack, onClick = a.onBack).testTag("onb_back"), contentAlignment = Alignment.Center) {
                if (canBack) Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = stitchText())
            }
            Spacer(Modifier.width(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                steps.forEachIndexed { i, _ ->
                    val done = s.step == OnbStep.DONE || i < idx
                    Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp))
                        .background(when { done -> IkGreen; i == idx -> IkGold; else -> stitchLine() }))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(if (s.step == OnbStep.DONE) "Done" else "${idx + 1}/${steps.size}", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        AnimatedContent(s.step, label = "step", modifier = Modifier.weight(1f),
            transitionSpec = { (slideInHorizontally { it / 6 } + fadeIn()) togetherWith fadeOut() }) { step ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 14.dp)) {
                when (step) {
                    OnbStep.NAME -> NameStep(s, a)
                    OnbStep.USERNAME -> UsernameStep(s, a)
                    OnbStep.GENDER -> GenderStep(s, a)
                    OnbStep.TYPE -> TypeStep(s, a)
                    OnbStep.BIRTH -> BirthStep(s, a)
                    OnbStep.ADDRESS -> AddressStep(s, a)
                    OnbStep.LANGUAGE -> LanguageStep(s, a)
                    OnbStep.DONE -> DoneStep(s, a)
                }
            }
        }
        // Bottom: error + Continue
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)) {
            s.error?.let {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp).testTag("onb_error")) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = IkRed, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text(it, color = IkRed, fontSize = 13.sp)
                }
            }
            if (s.step == OnbStep.DONE) {
                IkButton("Go to Noor Pro", Modifier.fillMaxWidth(), tag = "onb_finish", onClick = a.onFinish)
            } else {
                val blocked = OnboardingRules.stepProblem(s.step, s.draft, s.today) != null ||
                    (s.step == OnbStep.USERNAME && s.nameCheck in setOf(NameCheck.TAKEN, NameCheck.CHECKING))
                val last = s.step == OnbStep.LANGUAGE
                Box {
                    IkButton(if (s.saving) "Saving…" else if (last) "Finish" else "Continue", Modifier.fillMaxWidth(), enabled = !blocked && !s.saving, tag = "onb_next", onClick = a.onNext)
                    if (s.saving) CircularProgressIndicator(Modifier.align(Alignment.CenterStart).padding(start = 16.dp).size(18.dp), color = Color.White, strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun StepTitle(eyebrow: String, title: String, body: String) {
    Text(eyebrow.uppercase(), color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
    Spacer(Modifier.height(6.dp))
    Text(title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 32.sp)
    Spacer(Modifier.height(6.dp))
    Text(body, color = stitchMutedText(), fontSize = 14.sp, lineHeight = 20.sp)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun PrivacyLine(text: String, tag: String? = null) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 10.dp).let { if (tag != null) it.testTag(tag) else it }) {
        Icon(Icons.Outlined.Lock, null, tint = stitchMutedText(), modifier = Modifier.size(14.dp).padding(top = 1.dp)); Spacer(Modifier.width(6.dp))
        Text(text, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun NameStep(s: OnboardingUiState, a: OnboardingActions) {
    Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(IkGreen, com.noorpro.app.ui.islamic.IkGreenDeep)))) {
        IkLattice(Modifier.matchParentSize(), IkGold.copy(alpha = 0.14f), 30.dp)
        Row(Modifier.align(Alignment.CenterStart).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).border(2.dp, IkGold, CircleShape), contentAlignment = Alignment.Center) {
                if (s.draft.photoUrl.isNotBlank()) coil.compose.AsyncImage(model = s.draft.photoUrl, contentDescription = "Your photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                else Text(s.draft.fullName.trim().take(1).uppercase().ifBlank { "؟" }, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Assalamu alaikum", color = IkGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(s.draft.fullName.ifBlank { "Welcome to Noor Pro" }, color = Color.White, fontFamily = LibreCaslon, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(22.dp))
    StepTitle("Let's set up your profile", "What's your name?", "This is how you'll appear in the Ummah community. You can change it later.")
    IkField(s.draft.fullName, { a.onDraft(s.draft.copy(fullName = it)) }, "Full name", tag = "onb_name", maxLen = 60)
    PrivacyLine("Shown on your public profile.")
}

@Composable
private fun UsernameStep(s: OnboardingUiState, a: OnboardingActions) {
    StepTitle("Your handle", "Pick a username", "3–20 characters: lowercase letters, numbers, dots and underscores. It must be unique.")
    IkField(s.draft.username, { a.onDraft(s.draft.copy(username = com.noorpro.app.data.onboarding.UsernameRules.normalize(it))) }, "@username", tag = "onb_username", maxLen = 20)
    Spacer(Modifier.height(8.dp))
    val problem = if (s.draft.username.isEmpty()) null else com.noorpro.app.data.onboarding.UsernameRules.problem(s.draft.username)
    val (icon, color, text) = when {
        problem != null -> Triple(Icons.Outlined.ErrorOutline, IkRed, problem)
        s.nameCheck == NameCheck.CHECKING -> Triple(Icons.Outlined.AlternateEmail, stitchMutedText(), "Checking…")
        s.nameCheck == NameCheck.AVAILABLE -> Triple(Icons.Outlined.CheckCircle, ikAccentText(), "@${s.draft.username} is available")
        s.nameCheck == NameCheck.TAKEN -> Triple(Icons.Outlined.ErrorOutline, IkRed, "@${s.draft.username} is taken")
        s.nameCheck == NameCheck.OFFLINE -> Triple(Icons.Outlined.ErrorOutline, stitchMutedText(), "Can't check right now; we'll confirm when you continue.")
        else -> Triple(Icons.Outlined.AlternateEmail, stitchMutedText(), "noorpro.app/@${s.draft.username.ifBlank { "you" }}")
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("onb_username_status")) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(text, color = color, fontSize = 13.sp)
    }
    if (s.suggestions.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text("Suggestions", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.suggestions.forEach { n -> IkPill("@$n", n == s.draft.username, tag = "onb_suggest_$n") { a.onDraft(s.draft.copy(username = n)) } }
        }
    }
}

@Composable
private fun ChoiceCard(icon: ImageVector, title: String, body: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (selected) ikSoft() else ikCard())
            .border(if (selected) 2.dp else 1.dp, if (selected) IkGreen else stitchLine(), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(14.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IkIconTile(icon, size = 42.dp, bg = if (selected) IkGreen else ikSoft(), tint = if (selected) Color.White else ikAccentText())
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (body.isNotBlank()) Text(body, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) IkGreen else stitchLine(), CircleShape).background(if (selected) IkGreen else Color.Transparent),
            contentAlignment = Alignment.Center) { if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun GenderStep(s: OnboardingUiState, a: OnboardingActions) {
    StepTitle("About you", "Are you a brother or a sister?", "Used for sisters-only and brothers-only circles and to address you respectfully.")
    ChoiceCard(Icons.Outlined.Person, "Male", "Brother", s.draft.gender == OnbGender.MALE, "onb_gender_male") { a.onDraft(s.draft.copy(gender = OnbGender.MALE)) }
    ChoiceCard(Icons.Outlined.Person, "Female", "Sister", s.draft.gender == OnbGender.FEMALE, "onb_gender_female") { a.onDraft(s.draft.copy(gender = OnbGender.FEMALE)) }
    ChoiceCard(Icons.Outlined.Lock, "Prefer not to say", "You can still join open circles", s.draft.gender == OnbGender.UNSPECIFIED, "onb_gender_none") { a.onDraft(s.draft.copy(gender = OnbGender.UNSPECIFIED)) }
    PrivacyLine("Private. Never shown on your profile.")
}

@Composable
private fun TypeStep(s: OnboardingUiState, a: OnboardingActions) {
    StepTitle("Your profile", "How will you use Noor Pro?", "This sets your profile badge and what we show you first.")
    val icons = mapOf(ProfileType.REGULAR to Icons.Outlined.Person, ProfileType.STUDENT to Icons.Outlined.School, ProfileType.SCHOLAR to Icons.Outlined.MenuBook,
        ProfileType.CREATOR to Icons.Outlined.Videocam, ProfileType.MASJID to Icons.Outlined.AccountBalance)
    ProfileType.entries.forEach { t ->
        ChoiceCard(icons.getValue(t), t.label, t.blurb, s.draft.profileType == t, "onb_type_${t.code}") { a.onDraft(s.draft.copy(profileType = t)) }
    }
    if (s.draft.profileType == ProfileType.SCHOLAR) IkNotice("Scholar verification", "After setup you'll fill a short application with your ijazah. Your scholar badge appears once an admin verifies it.",
        Modifier.padding(top = 4.dp), tag = "onb_scholar_note")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthStep(s: OnboardingUiState, a: OnboardingActions) {
    StepTitle("Date of birth", "When were you born?", "We keep your date of birth private. It helps us keep younger members safe.")
    var open by remember { mutableStateOf(false) }
    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp))
            .clickable { open = true }.padding(16.dp).testTag("onb_dob"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IkIconTile(Icons.Outlined.CalendarMonth, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Date of birth", color = stitchMutedText(), fontSize = 12.sp)
            Text(s.draft.dob?.let { "${it.day} ${months[it.month - 1]} ${it.year}" } ?: "Choose a date", color = stitchText(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text("Change", color = ikAccentText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
    s.draft.dob?.let { dob ->
        if (AgeRules.plausible(dob, s.today) && AgeRules.isChild(dob, s.today)) IkNotice("Family & Kids mode",
            "Because you're under 13, Noor Pro will switch on Kids mode: a safe space with Qur'an, duas and stars, and no social feeds or messages. A parent can manage it in Family & Kids.",
            Modifier.padding(top = 14.dp), tag = "onb_child_note")
    }
    PrivacyLine("Private. Only you can see it; your profile never shows your age.")
    if (open) {
        val utc = java.util.TimeZone.getTimeZone("UTC")
        val initial = (s.draft.dob ?: SimpleDate(s.today.year - 20, 1, 1)).let { d -> java.util.Calendar.getInstance(utc).apply { clear(); set(d.year, d.month - 1, d.day) }.timeInMillis }
        val maxMs = java.util.Calendar.getInstance(utc).apply { clear(); set(s.today.year, s.today.month - 1, s.today.day) }.timeInMillis
        val state = rememberDatePickerState(initialSelectedDateMillis = initial, yearRange = (s.today.year - 110)..s.today.year,
            selectableDates = object : androidx.compose.material3.SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMs })
        DatePickerDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = {
                state.selectedDateMillis?.let { ms -> val c = java.util.Calendar.getInstance(utc).apply { timeInMillis = ms }
                    a.onDraft(s.draft.copy(dob = SimpleDate(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH)))) }
                open = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }) { DatePicker(state) }
    }
}

@Composable
private fun AddressStep(s: OnboardingUiState, a: OnboardingActions) {
    StepTitle("Location", "Where do you live?", "Your city helps with prayer times, nearby masjids and your local community.")
    IkField(s.draft.country, { a.onDraft(s.draft.copy(country = it)) }, "Country", tag = "onb_country", maxLen = 60)
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        COMMON_COUNTRIES.forEach { c -> IkPill(c, s.draft.country.equals(c, true)) { a.onDraft(s.draft.copy(country = c)) } }
    }
    Spacer(Modifier.height(12.dp))
    IkField(s.draft.state, { a.onDraft(s.draft.copy(state = it)) }, "State / province", tag = "onb_state", maxLen = 60)
    Spacer(Modifier.height(10.dp))
    IkField(s.draft.city, { a.onDraft(s.draft.copy(city = it)) }, "City or town", tag = "onb_city", maxLen = 60)
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Place, null, tint = ikAccentText(), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Show city & country on my profile", color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Never shown for members under 13.", color = stitchMutedText(), fontSize = 12.sp)
        }
        Switch(s.draft.showLocation, { a.onDraft(s.draft.copy(showLocation = it)) }, modifier = Modifier.testTag("onb_show_location"),
            colors = SwitchDefaults.colors(checkedTrackColor = IkGreen, checkedThumbColor = Color.White))
    }
    Spacer(Modifier.height(18.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ikSoft()).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = ikAccentText(), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text("Full address (optional, private)", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text("Only you can see this. Never shown on your profile.", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
        IkField(s.draft.street, { a.onDraft(s.draft.copy(street = it)) }, "Street address", tag = "onb_street", maxLen = 200)
        Spacer(Modifier.height(8.dp))
        IkField(s.draft.postalCode, { a.onDraft(s.draft.copy(postalCode = it)) }, "Postal / PIN code", tag = "onb_postal", maxLen = 20)
    }
}

@Composable
private fun LanguageStep(s: OnboardingUiState, a: OnboardingActions) {
    StepTitle("Language", "Your primary language", "Noor AI will answer in this language by default, and the app will use it where available.")
    PRIMARY_LANGUAGES.forEach { l ->
        ChoiceCard(Icons.Outlined.Translate, l.label, if (l.appUi != "EN" || l.code == "en") "App and Noor AI" else "Noor AI answers · app in English", s.draft.language == l.code, "onb_lang_${l.code}") {
            a.onDraft(s.draft.copy(language = l.code))
        }
    }
}

@Composable
private fun DoneStep(s: OnboardingUiState, a: OnboardingActions) {
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(ikSoft()), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CheckCircle, null, tint = IkGreen, modifier = Modifier.size(54.dp))
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("JazakAllahu khairan, ${s.draft.fullName.trim().substringBefore(' ').ifBlank { "friend" }}!", color = stitchText(), fontFamily = LibreCaslon,
        fontWeight = FontWeight.Bold, fontSize = 26.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Text("Your profile is ready.", color = stitchMutedText(), fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(20.dp)).padding(16.dp).testTag("onb_summary")) {
        SummaryRow("Name", s.draft.fullName.trim())
        SummaryRow("Username", "@${s.draft.username}")
        SummaryRow("Profile", s.draft.profileType?.label.orEmpty())
        SummaryRow("Location", listOf(s.draft.city, s.draft.country).filter { it.isNotBlank() }.joinToString(", ") + if (s.draft.showLocation) "" else " (private)")
        SummaryRow("Language", com.noorpro.app.data.onboarding.primaryLanguage(s.draft.language)?.label.orEmpty(), last = true)
    }
    if (s.draft.profileType == ProfileType.SCHOLAR) {
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ikSoft()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WorkspacePremium, null, tint = IkGold, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text("Apply as a scholar", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text("Share your ijazah so you can answer questions in Ask a Scholar.", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            IkButton("Start application", Modifier.fillMaxWidth(), tag = "onb_apply_scholar", onClick = a.onApplyScholar)
        }
    }
}

@Composable
private fun SummaryRow(k: String, v: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(k, color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.width(96.dp))
        Text(v, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(stitchLine()))
}
