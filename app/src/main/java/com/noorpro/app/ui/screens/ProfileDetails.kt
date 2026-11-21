package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.noorpro.app.data.onboarding.primaryLanguage
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText

/**
 * 1.2.0 (117): Profile › Edit profile shows and saves everything filled in at sign-up (onboarding): gender, profile
 * type, date of birth, city/state/country, show-location and primary language. Saved to users/{uid}/private/profile
 * (owner only) and the public card (name, handle, city/country if allowed) by OnboardingRepository.save.
 */
object ProfileEditLogic {
    private val DETAIL_STEPS = listOf(OnbStep.GENDER, OnbStep.TYPE, OnbStep.BIRTH, OnbStep.ADDRESS, OnbStep.LANGUAGE)

    /** The draft with the name/username typed in the edit form (username normalised). */
    fun merge(d: OnboardingDraft, name: String, username: String): OnboardingDraft =
        d.copy(fullName = name.trim(), username = normalizeUsername(username))

    /** First problem in the personal details, or null when they can be saved. */
    fun problem(d: OnboardingDraft, today: SimpleDate): String? =
        OnboardingRules.nameProblem(d.fullName) ?: DETAIL_STEPS.firstNotNullOfOrNull { s -> OnboardingRules.stepProblem(s, d, today)?.let { "${s.title}: $it" } }

    /** "City, Country" for the profile page (own profile shows it even if hidden from others). */
    fun place(d: OnboardingDraft): String = listOf(d.city.trim(), d.country.trim()).filter { it.isNotBlank() }.joinToString(", ")

    /** Short "about" line for the own profile, e.g. "Student of knowledge · English". */
    fun aboutLine(d: OnboardingDraft): String = listOfNotNull(
        d.profileType?.takeIf { it != ProfileType.REGULAR }?.label,
        primaryLanguage(d.language)?.label?.substringAfterLast("· ")?.trim(),
    ).joinToString(" · ")

    /** Parses day / month / year boxes into a date (null if incomplete or impossible). */
    fun date(day: String, month: String, year: String): SimpleDate? {
        val d = day.trim().toIntOrNull() ?: return null
        val m = month.trim().toIntOrNull() ?: return null
        val y = year.trim().toIntOrNull() ?: return null
        if (y < 1900 || m !in 1..12) return null
        val max = java.time.YearMonth.of(y, m).lengthOfMonth()
        return if (d in 1..max) SimpleDate(y, m, d) else null
    }

    /** True when the username changed from the one this account already holds. */
    fun usernameChanged(d: OnboardingDraft): Boolean = d.username.isNotBlank() && d.username != d.reservedUsername
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ProfileDetailsEditor(d: OnboardingDraft, today: SimpleDate, onChange: (OnboardingDraft) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag("profile_details_editor")) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)) {
            Text("About you", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Lock, null, tint = stitchMutedText(), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Private", color = stitchMutedText(), fontSize = 12.sp)
        }
        Text("Only you see these. Others see your name, @username and (if you allow it) your city.", color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp)

        DetailLabel("Gender")
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OnbGender.entries.forEach { g -> com.noorpro.app.ui.islamic.IkPill(g.label, d.gender == g, tag = "profile_gender_${g.name.lowercase()}") { onChange(d.copy(gender = g)) } }
        }

        DetailLabel("Profile type")
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileType.entries.forEach { t -> com.noorpro.app.ui.islamic.IkPill(t.label, d.profileType == t, tag = "profile_type_${t.code}") { onChange(d.copy(profileType = t)) } }
        }
        if (d.profileType == ProfileType.SCHOLAR) Text("The scholar badge shows only after your ijazah is verified (Profile › Ask a Scholar › Apply).",
            color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))

        DetailLabel("Date of birth")
        var day by remember(d.dob) { mutableStateOf(d.dob?.day?.toString().orEmpty()) }
        var month by remember(d.dob) { mutableStateOf(d.dob?.month?.toString().orEmpty()) }
        var year by remember(d.dob) { mutableStateOf(d.dob?.year?.toString().orEmpty()) }
        fun push(nd: String, nm: String, ny: String) { onChange(d.copy(dob = ProfileEditLogic.date(nd, nm, ny))) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailBox("DD", day, { day = it.filter(Char::isDigit).take(2); push(day, month, year) }, "profile_dob_day", Modifier.weight(1f), numeric = true)
            DetailBox("MM", month, { month = it.filter(Char::isDigit).take(2); push(day, month, year) }, "profile_dob_month", Modifier.weight(1f), numeric = true)
            DetailBox("YYYY", year, { year = it.filter(Char::isDigit).take(4); push(day, month, year) }, "profile_dob_year", Modifier.weight(1.4f), numeric = true)
        }
        d.dob?.let { if (AgeRules.plausible(it, today)) Text("Age ${AgeRules.age(it, today)}", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }

        DetailLabel("Where you live")
        DetailBox("City or town", d.city, { onChange(d.copy(city = it.take(60))) }, "profile_city")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailBox("State", d.state, { onChange(d.copy(state = it.take(60))) }, "profile_state", Modifier.weight(1f))
            DetailBox("Country", d.country, { onChange(d.copy(country = it.take(60))) }, "profile_country", Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text("Show my city on my public profile", color = stitchText(), fontSize = 13.sp, modifier = Modifier.weight(1f))
            Switch(checked = d.showLocation, onCheckedChange = { onChange(d.copy(showLocation = it)) }, modifier = Modifier.testTag("profile_show_location"))
        }

        DetailLabel("Primary language")
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PRIMARY_LANGUAGES.forEach { l -> com.noorpro.app.ui.islamic.IkPill(l.label, d.language == l.code, tag = "profile_lang_${l.code}") { onChange(d.copy(language = l.code)) } }
        }
    }
}

@Composable
private fun DetailLabel(text: String) {
    Text(text, color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp))
}

@Composable
private fun DetailBox(hint: String, value: String, onChange: (String) -> Unit, tag: String, modifier: Modifier = Modifier.fillMaxWidth(), numeric: Boolean = false) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(if (isStitchLight()) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF1C2A24))
        .border(1.dp, stitchLine(), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 12.dp)) {
        if (value.isEmpty()) Text(hint, color = stitchMutedText(), fontSize = 15.sp)
        BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(color = stitchText(), fontSize = 15.sp),
            cursorBrush = SolidColor(R2Green), keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth().testTag(tag))
    }
}
