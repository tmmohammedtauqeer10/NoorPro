package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.LibreCaslon
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

private val PilgrimageGreen = Color(0xFF075E4B)
private val PilgrimageLightGreen = Color(0xFFEEF6F3)
private val PilgrimageGold = Color(0xFFC6A33A)
private val PilgrimageCream = Color(0xFFFBF7EA)
private val PilgrimageError = Color(0xFFC62828)

private enum class PilgrimageSection(val label: String, val urdu: String, val icon: ImageVector) {
    JOURNEY("Journey", "سفر", Icons.Default.Explore),
    MAP("Map", "نقشہ", Icons.Default.Map),
    DUAS("Duas", "دعائیں", Icons.Default.MenuBook),
    PLAN("Plan", "منصوبہ", Icons.Default.Checklist),
    HELP("Help", "مدد", Icons.Default.Warning)
}

private enum class GuideTab { GUIDE, DUAS, SOURCES }
private enum class DuaTextLanguage { ARABIC, ENGLISH, URDU }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HajjUmrahScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val repository = remember { NoorPilgrimageRepository(context) }
    val mapProvider = remember { GoogleMapsIntentProvider() }
    val userId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "guest_local" }
    var journeyTypeName by rememberSaveable { mutableStateOf(JourneyType.UMRAH.name) }
    var languageName by rememberSaveable { mutableStateOf(PilgrimageLanguage.ENGLISH.name) }
    var madhhabName by rememberSaveable { mutableStateOf(Madhhab.GENERAL.name) }
    var sectionName by rememberSaveable { mutableStateOf(PilgrimageSection.JOURNEY.name) }
    var selectedRitualId by rememberSaveable { mutableStateOf<String?>(null) }
    val journeyType = JourneyType.valueOf(journeyTypeName)
    val language = PilgrimageLanguage.valueOf(languageName)
    val madhhab = Madhhab.valueOf(madhhabName)
    val section = PilgrimageSection.valueOf(sectionName)
    val guides = remember(journeyType, language, madhhab) { repository.getJourneyGuide(journeyType, language, madhhab) }
    var progress by remember(journeyType, userId) { mutableStateOf(repository.getJourneyProgress(userId, journeyType)) }
    var planner by remember(userId) { mutableStateOf(repository.getPlannerState(userId)) }
    val isOnline by rememberNetworkStatus()
    val speaker = remember { DuaSpeaker(context) }

    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    LaunchedEffect(journeyType, userId) {
        progress = repository.getJourneyProgress(userId, journeyType)
        repository.syncJourneyProgress(userId, journeyType) { progress = it }
    }

    fun saveProgress(candidate: JourneyProgress) {
        val next = candidate.copy(activeRitualId = candidate.activeRitualId.ifBlank {
            nextActiveRitualId(guides, candidate.completedRitualIds)
        })
        progress = next
        repository.updateJourneyProgress(userId, journeyType, next)
    }
    fun savePlanner(candidate: PilgrimagePlannerState) {
        planner = candidate
        repository.updatePlannerState(userId, candidate)
    }
    fun closeOrBack() {
        when {
            selectedRitualId != null -> selectedRitualId = null
            section != PilgrimageSection.JOURNEY -> sectionName = PilgrimageSection.JOURNEY.name
            !viewModel.goBack() -> viewModel.navigateTo(DeenScreen.DASHBOARD)
        }
    }
    BackHandler { closeOrBack() }

    CompositionLocalProvider(LocalLayoutDirection provides if (language == PilgrimageLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Scaffold(
            containerColor = stitchBackground(),
            topBar = {
                TopAppBar(
                    title = { Text(tr("Hajj & Umrah", "حج اور عمرہ", language), color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = ::closeOrBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back", "واپس", language), tint = PilgrimageGold)
                        }
                    },
                    actions = { LanguageMenu(language) { languageName = it.name } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = stitchBackground())
                )
            },
            bottomBar = {
                NavigationBar(modifier = Modifier.navigationBarsPadding(), containerColor = stitchSurface(), tonalElevation = 2.dp) {
                    PilgrimageSection.entries.forEach { item ->
                        NavigationBarItem(
                            selected = selectedRitualId == null && section == item,
                            onClick = { selectedRitualId = null; sectionName = item.name },
                            icon = { Icon(item.icon, null, modifier = Modifier.size(22.dp)) },
                            label = { Text(if (language == PilgrimageLanguage.URDU) item.urdu else item.label, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                val selectedGuide = selectedRitualId?.let { id -> guides.firstOrNull { it.id == id } }
                when {
                    selectedGuide != null -> RitualGuideScreen(
                        selectedGuide, guides, progress, language, madhhab,
                        onMadhhabChanged = { madhhabName = it.name },
                        onProgressChanged = ::saveProgress,
                        onSpeak = speaker::speak
                    )
                    section == PilgrimageSection.JOURNEY -> JourneyDashboard(
                        journeyType, guides, progress, language, isOnline,
                        onTypeChanged = { journeyTypeName = it.name; selectedRitualId = null },
                        onSection = { sectionName = it.name },
                        onGuide = { selectedRitualId = it.id },
                        onSpeak = { speaker.speak(it.arabic, Locale("ar")) },
                        onDownload = { repository.downloadJourneyForOfflineUse(journeyType, language) }
                    )
                    section == PilgrimageSection.MAP -> PilgrimMapScreen(language, planner, repository.getSavedPilgrimPlaces(userId), mapProvider) { sectionName = PilgrimageSection.HELP.name }
                    section == PilgrimageSection.DUAS -> PilgrimageDuasScreen(language, guides.flatMap { it.duas }.distinctBy { it.id }, speaker::speak)
                    section == PilgrimageSection.PLAN -> TravelPlannerScreen(language, planner, ::savePlanner, repository.getOfficialServiceLinks("Saudi Arabia"), isOnline)
                    else -> PilgrimageHelpScreen(language, planner, repository.getOfficialServiceLinks("Saudi Arabia"))
                }
            }
        }
    }
}

@Composable
private fun LanguageMenu(language: PilgrimageLanguage, onSelected: (PilgrimageLanguage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.height(48.dp)) {
            Icon(Icons.Default.Language, null, tint = stitchPrimary(), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (language == PilgrimageLanguage.ENGLISH) "EN" else "اردو", color = stitchText(), fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem({ Text("English") }, { expanded = false; onSelected(PilgrimageLanguage.ENGLISH) })
            DropdownMenuItem({ Text("اردو") }, { expanded = false; onSelected(PilgrimageLanguage.URDU) })
        }
    }
}

@Composable
private fun JourneyDashboard(
    type: JourneyType,
    guides: List<PilgrimageGuide>,
    progress: JourneyProgress,
    language: PilgrimageLanguage,
    isOnline: Boolean,
    onTypeChanged: (JourneyType) -> Unit,
    onSection: (PilgrimageSection) -> Unit,
    onGuide: (PilgrimageGuide) -> Unit,
    onSpeak: (PilgrimageDua) -> Unit,
    onDownload: () -> Result<Int>
) {
    val current = guides.firstOrNull { it.id == progress.activeRitualId }
        ?: guides.firstOrNull { it.id !in progress.completedRitualIds }
    val percentage = progressPercentage(progress, guides.size)
    var offlineMessage by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { OfflineStatus(isOnline, language) }
        item { SegmentedJourneySwitch(type, language, onTypeChanged) }
        item {
            FeatureCard(background = lightOrDark(PilgrimageLightGreen)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            tr(if (type == JourneyType.UMRAH) "Your Umrah Journey" else "Your Hajj Journey", if (type == JourneyType.UMRAH) "آپ کا عمرہ سفر" else "آپ کا حج سفر", language),
                            color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = LibreCaslon
                        )
                        Text(tr("${progress.completedRitualIds.size} of ${guides.size} steps completed", "${guides.size} میں سے ${progress.completedRitualIds.size} مراحل مکمل", language), color = stitchMutedText(), fontSize = 14.sp)
                    }
                    Text("$percentage%", color = stitchPrimary(), fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator({ percentage / 100f }, Modifier.fillMaxWidth().height(9.dp).clip(CircleShape), color = PilgrimageGreen, trackColor = stitchSurface())
            }
        }
        item {
            Text(tr("Quick actions", "فوری سہولیات", language), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                QuickAction(tr("Map", "نقشہ", language), Icons.Default.Map, Modifier.weight(1f)) { onSection(PilgrimageSection.MAP) }
                QuickAction(tr("Duas", "دعائیں", language), Icons.Default.MenuBook, Modifier.weight(1f)) { onSection(PilgrimageSection.DUAS) }
                QuickAction(tr("Checklist", "فہرست", language), Icons.Default.Checklist, Modifier.weight(1f)) { onSection(PilgrimageSection.PLAN) }
                QuickAction(tr("Emergency", "ہنگامی", language), Icons.Default.Warning, Modifier.weight(1f), true) { onSection(PilgrimageSection.HELP) }
            }
        }
        current?.let { next ->
            item {
                FeatureCard(background = lightOrDark(PilgrimageCream), border = PilgrimageGold.copy(.5f)) {
                    Text(tr("NEXT STEP", "اگلا مرحلہ", language), color = PilgrimageGold, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp)
                    Text(next.localizedTitle(language), color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = LibreCaslon)
                    Text(next.localizedSummary(language), color = stitchMutedText(), fontSize = 15.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(13.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ onGuide(next) }, Modifier.height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = PilgrimageGreen)) { Text(tr("Start guide", "رہنمائی شروع", language)) }
                        next.duas.firstOrNull()?.let { dua ->
                            OutlinedButton({ onSpeak(dua) }, Modifier.height(48.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text(tr("Play dua", "دعا سنیں", language)) }
                        }
                    }
                }
            }
        }
        item { Text(tr("Ritual timeline", "مناسک کی ترتیب", language), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(guides, key = { it.id }) { guide -> RitualTimelineRow(guide, guide.id in progress.completedRitualIds, guide.id == current?.id, language) { onGuide(guide) } }
        item {
            FeatureCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, null, tint = stitchPrimary())
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Available offline", "آف لائن دستیاب", language), color = stitchText(), fontWeight = FontWeight.Bold)
                        Text(tr("Save this journey guide and its duas.", "اس سفر کی رہنمائی اور دعائیں محفوظ کریں۔", language), color = stitchMutedText(), fontSize = 13.sp)
                    }
                }
                OutlinedButton({
                    offlineMessage = onDownload().fold(
                        { tr("Saved $it guides for offline use.", "$it رہنما آف لائن محفوظ ہوئے۔", language) },
                        { tr("Could not save offline content.", "آف لائن مواد محفوظ نہیں ہو سکا۔", language) }
                    )
                }, Modifier.fillMaxWidth().height(52.dp).padding(top = 6.dp)) { Text(tr("Download journey", "سفر ڈاؤن لوڈ کریں", language)) }
                offlineMessage?.let { Text(it, color = stitchPrimary(), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp)) }
            }
        }
        item { SourceSafetyNote(language) }
    }
}

@Composable
private fun SegmentedJourneySwitch(type: JourneyType, language: PilgrimageLanguage, onChanged: (JourneyType) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSoftSurface()).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        JourneyType.entries.forEach { option ->
            val selected = type == option
            Surface(Modifier.weight(1f).height(48.dp).clickable { onChanged(option) }, RoundedCornerShape(13.dp), color = if (selected) PilgrimageGreen else Color.Transparent) {
                Box(contentAlignment = Alignment.Center) { Text(tr(if (option == JourneyType.UMRAH) "Umrah" else "Hajj", if (option == JourneyType.UMRAH) "عمرہ" else "حج", language), color = if (selected) Color.White else stitchText(), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val tint = if (danger) PilgrimageError else stitchPrimary()
    Column(modifier.height(88.dp).clip(RoundedCornerShape(16.dp)).background(stitchSurface()).border(1.dp, tint.copy(.18f), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(25.dp)); Spacer(Modifier.height(6.dp)); Text(label, color = stitchText(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RitualTimelineRow(guide: PilgrimageGuide, completed: Boolean, active: Boolean, language: PilgrimageLanguage, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(stitchSurface()).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (completed) PilgrimageGreen else if (active) PilgrimageGold else stitchSoftSurface()), contentAlignment = Alignment.Center) {
            if (completed) Icon(Icons.Default.Check, tr("Completed", "مکمل", language), tint = Color.White, modifier = Modifier.size(21.dp))
            else if (active) Text(guide.ritualOrder.toString(), color = Color.White, fontWeight = FontWeight.Black)
            else Icon(Icons.Default.RadioButtonUnchecked, tr("Upcoming", "آئندہ", language), tint = stitchMutedText(), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(guide.localizedTitle(language), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(guide.localizedSummary(language), color = stitchMutedText(), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        if (active) Text(tr("NOW", "اب", language), color = PilgrimageGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RitualGuideScreen(
    guide: PilgrimageGuide,
    guides: List<PilgrimageGuide>,
    progress: JourneyProgress,
    language: PilgrimageLanguage,
    madhhab: Madhhab,
    onMadhhabChanged: (Madhhab) -> Unit,
    onProgressChanged: (JourneyProgress) -> Unit,
    onSpeak: (String, Locale) -> Unit
) {
    var tabName by rememberSaveable(guide.id) { mutableStateOf(GuideTab.GUIDE.name) }
    val tab = GuideTab.valueOf(tabName)
    val counter = progress.ritualCounters[guide.id] ?: 0
    val isTawaf = guide.id.contains("tawaf") || guide.id.contains("ifadah")
    val vibrationEnabled = progress.checklistState["vibration_enabled"] ?: true
    val context = LocalContext.current
    val position = guides.indexOfFirst { it.id == guide.id } + 1
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(tr("Step $position of ${guides.size}", "مرحلہ $position از ${guides.size}", language), color = PilgrimageGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(guide.localizedTitle(language), color = stitchText(), fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black, fontFamily = LibreCaslon)
            Text(guide.localizedSummary(language), color = stitchMutedText(), fontSize = 15.sp, lineHeight = 22.sp)
        }
        item {
            FeatureCard {
                Text(tr("Before you begin", "شروع کرنے سے پہلے", language), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                guide.prerequisites.forEachIndexed { index, item ->
                    val key = "${guide.id}_pre_$index"
                    ChecklistRow(item, progress.checklistState[key] == true) { onProgressChanged(progress.copy(checklistState = progress.checklistState + (key to it))) }
                }
            }
        }
        if (isTawaf) item {
            FeatureCard(background = lightOrDark(PilgrimageLightGreen), border = PilgrimageGreen.copy(.35f)) {
                Text(tr("Tawaf round counter", "طواف چکر کاؤنٹر", language), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("$counter / 7", color = stitchPrimary(), fontSize = 46.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                LinearProgressIndicator({ counter / 7f }, Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = PilgrimageGreen, trackColor = stitchSurface())
                Spacer(Modifier.height(13.dp))
                Button(
                    onClick = { onProgressChanged(updateRitualCounter(progress, guide.id, 1, 7)); if (vibrationEnabled) vibrateRound(context) },
                    enabled = counter < 7,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PilgrimageGreen)
                ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(7.dp)); Text(if (counter < 7) tr("Complete round ${counter + 1}", "چکر ${counter + 1} مکمل", language) else tr("Seven rounds complete", "سات چکر مکمل", language), fontWeight = FontWeight.Bold) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onProgressChanged(updateRitualCounter(progress, guide.id, -1, 7)) }, enabled = counter > 0, modifier = Modifier.height(48.dp)) { Icon(Icons.Default.Undo, null, Modifier.size(19.dp)); Spacer(Modifier.width(5.dp)); Text(tr("Undo last", "آخری واپس", language)) }
                    Spacer(Modifier.weight(1f)); Text(tr("Vibrate", "وائبریشن", language), color = stitchMutedText(), fontSize = 13.sp)
                    Switch(vibrationEnabled, { onProgressChanged(progress.copy(checklistState = progress.checklistState + ("vibration_enabled" to it))) })
                }
                Text(tr("The count saves immediately and never resets when the app closes.", "گنتی فوراً محفوظ ہوتی ہے اور ایپ بند ہونے پر ری سیٹ نہیں ہوتی۔", language), color = stitchMutedText(), fontSize = 12.sp)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GuideTab.entries.forEach { option -> ChoicePill(tr(option.name.lowercase().replaceFirstChar { it.uppercase() }, when (option) { GuideTab.GUIDE -> "رہنمائی"; GuideTab.DUAS -> "دعائیں"; GuideTab.SOURCES -> "حوالہ جات" }, language), tab == option) { tabName = option.name } }
            }
        }
        when (tab) {
            GuideTab.GUIDE -> {
                item { GuideContentCard(tr("Step-by-step", "مرحلہ وار", language), Icons.Default.Checklist, guide.instructions) }
                item { GuideContentCard(tr("Common mistakes", "عام غلطیاں", language), Icons.Default.Warning, guide.mistakes, true) }
                item { GuideContentCard(tr("Accessibility and crowd safety", "رسائی اور ہجوم کی حفاظت", language), Icons.Default.Accessible, guide.accessibilityAdvice) }
                item { FeatureCard { Text(tr("Fiqh classification", "فقہی درجہ بندی", language), color = stitchText(), fontWeight = FontWeight.Bold); Text(guide.fiqhClassification, color = stitchMutedText(), fontSize = 14.sp); Text(tr("See Sources for review status and madhhab notes.", "جائزے اور فقہی نوٹس کے لیے حوالہ جات دیکھیں۔", language), color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) } }
            }
            GuideTab.DUAS -> item { DuaPanel(guide.duas, language, onSpeak) }
            GuideTab.SOURCES -> {
                item {
                    Text(tr("Guidance preference", "فقہی ترجیح", language), color = stitchText(), fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { Madhhab.entries.forEach { ChoicePill(madhhabLabel(it), it == madhhab) { onMadhhabChanged(it) } } }
                }
                item {
                    val note = guide.madhhabNotes.firstOrNull { it.madhhab == madhhab }
                    FeatureCard {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Verified, null, tint = if (guide.scholarReview.status == ScholarReviewStatus.SCHOLAR_REVIEWED) PilgrimageGreen else PilgrimageGold); Spacer(Modifier.width(8.dp)); Column { Text(guide.scholarReview.status.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, color = stitchText(), fontWeight = FontWeight.Bold); Text("${guide.scholarReview.reviewer} · ${guide.scholarReview.reviewDate}", color = stitchMutedText(), fontSize = 12.sp) } }
                        Text(guide.scholarReview.note, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp)); note?.let { Text(it.note, color = stitchText(), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }
                    }
                }
                item { GuideContentCard(tr("Quran and Hadith references", "قرآن و حدیث کے حوالے", language), Icons.Default.MenuBook, guide.references) }
            }
        }
        item {
            val completed = guide.id in progress.completedRitualIds
            Button(
                onClick = { val done = progress.completedRitualIds + guide.id; onProgressChanged(progress.copy(completedRitualIds = done, activeRitualId = nextActiveRitualId(guides, done))) },
                enabled = !completed && (!isTawaf || counter == 7),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PilgrimageGreen)
            ) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text(if (completed) tr("Ritual completed", "منسک مکمل", language) else if (isTawaf && counter < 7) tr("Complete all 7 rounds first", "پہلے ساتوں چکر مکمل کریں", language) else tr("Mark ritual complete", "منسک مکمل نشان زد کریں", language), fontWeight = FontWeight.Bold) }
        }
        item { SourceSafetyNote(language) }
    }
}

@Composable
private fun GuideContentCard(title: String, icon: ImageVector, points: List<String>, warning: Boolean = false) {
    FeatureCard(border = if (warning) PilgrimageGold.copy(.35f) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = if (warning) PilgrimageGold else stitchPrimary(), modifier = Modifier.size(23.dp)); Spacer(Modifier.width(9.dp)); Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        Spacer(Modifier.height(7.dp))
        points.forEachIndexed { index, point -> Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) { Text("${index + 1}", color = if (warning) PilgrimageGold else stitchPrimary(), fontWeight = FontWeight.Black, modifier = Modifier.width(25.dp)); Text(point, color = stitchMutedText(), fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f)) } }
    }
}

@Composable
private fun DuaPanel(duas: List<PilgrimageDua>, language: PilgrimageLanguage, onSpeak: (String, Locale) -> Unit) {
    var textLanguageName by rememberSaveable { mutableStateOf(DuaTextLanguage.ARABIC.name) }
    val selected = DuaTextLanguage.valueOf(textLanguageName)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { DuaTextLanguage.entries.forEach { ChoicePill(it.name.lowercase().replaceFirstChar { c -> c.uppercase() }, it == selected) { textLanguageName = it.name } } }
        if (duas.isEmpty()) FeatureCard { Text(tr("No ritual-specific dua is required here. You may make sincere dua in any language.", "یہاں کوئی مخصوص دعا لازم نہیں۔ آپ کسی بھی زبان میں دعا کر سکتے ہیں۔", language), color = stitchMutedText(), fontSize = 15.sp, lineHeight = 22.sp) }
        else duas.forEach { dua ->
            val text = when (selected) { DuaTextLanguage.ARABIC -> dua.arabic; DuaTextLanguage.ENGLISH -> dua.english; DuaTextLanguage.URDU -> dua.urdu }
            val locale = when (selected) { DuaTextLanguage.ARABIC -> Locale("ar"); DuaTextLanguage.ENGLISH -> Locale.ENGLISH; DuaTextLanguage.URDU -> Locale("ur") }
            FeatureCard {
                Text(dua.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text, color = stitchText(), fontSize = if (selected == DuaTextLanguage.ARABIC) 23.sp else 15.sp, lineHeight = if (selected == DuaTextLanguage.ARABIC) 38.sp else 23.sp, textAlign = if (selected == DuaTextLanguage.ARABIC) TextAlign.End else TextAlign.Start, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                Text(dua.source, color = stitchMutedText(), fontSize = 12.sp)
                OutlinedButton({ onSpeak(text, locale) }, Modifier.fillMaxWidth().height(52.dp).padding(top = 5.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(tr("Play with device voice", "آلے کی آواز میں سنیں", language)) }
            }
        }
    }
}

@Composable
private fun PilgrimageDuasScreen(language: PilgrimageLanguage, duas: List<PilgrimageDua>, onSpeak: (String, Locale) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { ScreenIntro(tr("Pilgrimage duas", "سفر کی دعائیں", language), tr("Reviewed duas saved with the journey and available offline.", "جائزہ شدہ دعائیں سفر کے ساتھ آف لائن دستیاب ہیں۔", language), Icons.Default.MenuBook) }
        items(duas, key = { it.id }) { dua ->
            FeatureCard {
                Text(dua.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(dua.arabic, color = stitchText(), fontSize = 22.sp, lineHeight = 36.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Text(if (language == PilgrimageLanguage.URDU) dua.urdu else dua.english, color = stitchMutedText(), fontSize = 14.sp, lineHeight = 21.sp)
                Text(dua.source, color = PilgrimageGold, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                OutlinedButton({ onSpeak(dua.arabic, Locale("ar")) }, Modifier.fillMaxWidth().height(52.dp).padding(top = 5.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(tr("Play dua", "دعا سنیں", language)) }
            }
        }
    }
}

private data class MapPlaceUi(
    val title: String,
    val subtitle: String,
    val query: String,
    val icon: ImageVector,
    val approximate: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Composable
private fun PilgrimMapScreen(
    language: PilgrimageLanguage,
    planner: PilgrimagePlannerState,
    savedPlaces: List<SavedPilgrimPlace>,
    mapProvider: GoogleMapsIntentProvider,
    onEmergency: () -> Unit
) {
    val context = LocalContext.current
    var location by remember { mutableStateOf<android.location.Location?>(null) }
    var locationStatus by remember(language) { mutableStateOf(tr("Location not requested", "مقام کی درخواست نہیں کی گئی", language)) }
    fun getLocation() {
        locationStatus = tr("Finding your location…", "آپ کا مقام تلاش کیا جا رہا ہے…", language)
        DeviceLocationProvider.getCurrentLocation(context) { result ->
            result.onSuccess { location = it; locationStatus = tr("Location ready · accuracy ±${it.accuracy.toInt()} m", "مقام تیار · درستگی ±${it.accuracy.toInt()} میٹر", language) }
                .onFailure { locationStatus = it.message ?: tr("Location unavailable", "مقام دستیاب نہیں", language) }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) getLocation() else locationStatus = tr("Location permission was not granted", "مقام کی اجازت نہیں دی گئی", language)
    }
    val places = buildList {
        add(MapPlaceUi(tr("Masjid al-Haram", "مسجد الحرام", language), tr("Primary pilgrimage landmark", "مرکزی زیارتی مقام", language), "Masjid al-Haram", Icons.Default.LocationOn, false, 21.4225, 39.8262))
        add(MapPlaceUi(tr("Important gates", "اہم دروازے", language), tr("Check current access and gate signs", "موجودہ رسائی اور دروازوں کے نشانات دیکھیں", language), "Masjid al-Haram gates", Icons.Default.DoorFront))
        add(MapPlaceUi(tr("Nearby toilets", "قریب بیت الخلا", language), tr("Live map search; follow on-site signs", "براہ راست نقشہ تلاش؛ موقع کے نشانات دیکھیں", language), "public toilets near Masjid al-Haram", Icons.Default.Wc))
        add(MapPlaceUi(tr("Medical centres", "طبی مراکز", language), tr("Live nearby search", "قریب تلاش کریں", language), "medical center near Masjid al-Haram", Icons.Default.LocalHospital))
        add(MapPlaceUi(tr("Transport points", "ٹرانسپورٹ مقامات", language), tr("Stations and authorised pickup points", "اسٹیشن اور مجاز پک اپ مقامات", language), "public transport near Masjid al-Haram", Icons.Default.DirectionsBus))
        if (planner.hotelName.isNotBlank() || planner.hotelAddressEnglish.isNotBlank()) add(MapPlaceUi(planner.hotelName.ifBlank { tr("Saved hotel", "محفوظ ہوٹل", language) }, planner.hotelAddressEnglish.ifBlank { planner.hotelAddressArabic }, listOf(planner.hotelName, planner.hotelAddressEnglish, planner.hotelAddressArabic).filter { it.isNotBlank() }.joinToString(" "), Icons.Default.Hotel, false))
        if (planner.familyMeetingPoint.isNotBlank()) add(MapPlaceUi(tr("Family meeting point", "خاندان کا ملنے کا مقام", language), planner.familyMeetingPoint, planner.familyMeetingPoint, Icons.Default.Groups, false))
        savedPlaces.filter { it.id != "hotel" }.forEach { add(MapPlaceUi(it.name, it.addressEnglish.ifBlank { it.addressArabic }, listOf(it.name, it.addressEnglish, it.addressArabic).joinToString(" "), Icons.Default.EditLocationAlt, it.latitude == null, it.latitude, it.longitude)) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenIntro(tr("Pilgrim map", "زائر نقشہ", language), tr("Current location, essential places and safe directions.", "موجودہ مقام، ضروری جگہیں اور محفوظ راستے۔", language), Icons.Default.Map) }
        item {
            FeatureCard(background = lightOrDark(PilgrimageLightGreen)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MyLocation, null, tint = stitchPrimary()); Spacer(Modifier.width(9.dp)); Text(locationStatus, color = stitchText(), fontSize = 14.sp, modifier = Modifier.weight(1f)) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ if (DeviceLocationProvider.hasPermission(context)) getLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = PilgrimageGreen)) { Text(tr("Locate me", "میرا مقام", language)) }
                    OutlinedButton({ location?.let { mapProvider.shareLocation(context, it.latitude, it.longitude, "My pilgrimage location") } }, Modifier.weight(1f).height(48.dp), enabled = location != null) { Icon(Icons.Default.Share, null, Modifier.size(19.dp)); Spacer(Modifier.width(5.dp)); Text(tr("Share", "شیئر", language)) }
                }
            }
        }
        item { PilgrimageGoogleMap(context, location, places, language) }
        items(places) { place ->
            FeatureCard {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(place.icon, null, tint = stitchPrimary(), modifier = Modifier.size(28.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(place.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(place.subtitle, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp)
                        if (place.approximate) Text(tr("Map result may be approximate or community-provided.", "نقشے کا نتیجہ تخمینی یا کمیونٹی فراہم کردہ ہو سکتا ہے۔", language), color = PilgrimageGold, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ mapProvider.openPlace(context, place.query, place.latitude, place.longitude) }, Modifier.weight(1f).height(48.dp)) { Icon(Icons.Default.Map, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(tr("View", "دیکھیں", language)) }
                    Button({ mapProvider.openDirections(context, place.query, place.latitude, place.longitude) }, Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = PilgrimageGreen)) { Icon(Icons.Default.Directions, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(tr("Directions", "راستہ", language)) }
                }
            }
        }
        item { Button(onEmergency, Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = PilgrimageError)) { Icon(Icons.Default.Warning, null); Spacer(Modifier.width(8.dp)); Text(tr("Open emergency help", "ہنگامی مدد کھولیں", language), fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun PilgrimageGoogleMap(context: Context, location: android.location.Location?, places: List<MapPlaceUi>, language: PilgrimageLanguage) {
    if (!hasConfiguredMapsKey(context)) {
        FeatureCard(background = lightOrDark(PilgrimageCream), border = PilgrimageGold.copy(.35f)) {
            Row(verticalAlignment = Alignment.Top) { Icon(Icons.Default.Map, null, tint = PilgrimageGold); Spacer(Modifier.width(9.dp)); Column { Text(tr("Google Maps key needed", "گوگل نقشہ کلید درکار", language), color = stitchText(), fontWeight = FontWeight.Bold); Text(tr("Add the restricted MAPS_API_KEY to local.properties to enable the embedded map. Place cards and external directions already work.", "اندرونی نقشہ فعال کرنے کے لیے محدود MAPS_API_KEY کو local.properties میں شامل کریں۔ جگہوں کے کارڈ اور بیرونی راستے کام کرتے ہیں۔", language), color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp) } }
        }
        return
    }
    val makkah = com.google.android.gms.maps.model.LatLng(21.4225, 39.8262)
    val camera = com.google.maps.android.compose.rememberCameraPositionState { position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(makkah, 14f) }
    Card(Modifier.fillMaxWidth().height(245.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, PilgrimageGreen.copy(.25f))) {
        com.google.maps.android.compose.GoogleMap(
            modifier = Modifier.fillMaxSize(), cameraPositionState = camera,
            uiSettings = com.google.maps.android.compose.MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = true),
            properties = com.google.maps.android.compose.MapProperties(isMyLocationEnabled = location != null && DeviceLocationProvider.hasPermission(context))
        ) {
            places.filter { it.latitude != null && it.longitude != null }.forEach { place ->
                com.google.maps.android.compose.Marker(
                    state = com.google.maps.android.compose.rememberUpdatedMarkerState(position = com.google.android.gms.maps.model.LatLng(place.latitude!!, place.longitude!!)),
                    title = place.title, snippet = place.subtitle
                )
            }
        }
    }
}

@Composable
private fun TravelPlannerScreen(language: PilgrimageLanguage, planner: PilgrimagePlannerState, onChanged: (PilgrimagePlannerState) -> Unit, links: List<OfficialServiceLink>, isOnline: Boolean) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenIntro(tr("Travel planner", "سفر کا منصوبہ", language), tr("Important details save locally and remain available offline.", "اہم معلومات مقامی طور پر محفوظ اور آف لائن دستیاب رہتی ہیں۔", language), Icons.Default.Luggage) }
        item { OfflineStatus(isOnline, language) }
        item { ExpandableModule(tr("Documents", "دستاویزات", language), Icons.Default.Badge, true) { ChecklistRow(tr("Passport copy", "پاسپورٹ کاپی", language), planner.passportChecked) { onChanged(planner.copy(passportChecked = it)) }; ChecklistRow(tr("Visa or permit confirmation", "ویزا یا اجازت نامہ", language), planner.visaChecked) { onChanged(planner.copy(visaChecked = it)) }; ChecklistRow(tr("Travel insurance", "سفری بیمہ", language), planner.insuranceChecked) { onChanged(planner.copy(insuranceChecked = it)) } } }
        item { ExpandableModule(tr("Packing checklist", "سامان کی فہرست", language), Icons.Default.Luggage) { ChecklistRow(tr("Medicines and prescriptions", "ادویات اور نسخے", language), planner.medicinesChecked) { onChanged(planner.copy(medicinesChecked = it)) }; ChecklistRow(tr("Ihram and unscented essentials", "احرام اور بغیر خوشبو ضروریات", language), planner.ihramChecked) { onChanged(planner.copy(ihramChecked = it)) }; ChecklistRow(tr("Charger and power bank", "چارجر اور پاور بینک", language), planner.chargerChecked) { onChanged(planner.copy(chargerChecked = it)) } } }
        item { ExpandableModule(tr("Flight information", "پرواز کی معلومات", language), Icons.Default.FlightTakeoff) { PlannerField(tr("Flight number", "پرواز نمبر", language), planner.flightNumber) { onChanged(planner.copy(flightNumber = it)) }; PlannerField(tr("Flight date and time", "پرواز تاریخ اور وقت", language), planner.flightDate) { onChanged(planner.copy(flightDate = it)) } } }
        item { ExpandableModule(tr("Hotel information", "ہوٹل کی معلومات", language), Icons.Default.Hotel) { PlannerField(tr("Hotel name", "ہوٹل نام", language), planner.hotelName) { onChanged(planner.copy(hotelName = it)) }; PlannerField(tr("Address in English", "پتہ انگریزی میں", language), planner.hotelAddressEnglish) { onChanged(planner.copy(hotelAddressEnglish = it)) }; PlannerField(tr("Address in Arabic", "پتہ عربی میں", language), planner.hotelAddressArabic) { onChanged(planner.copy(hotelAddressArabic = it)) } } }
        item { ExpandableModule(tr("Transport", "ٹرانسپورٹ", language), Icons.Default.DirectionsBus) { PlannerField(tr("Pickup, bus or train notes", "پک اپ، بس یا ٹرین نوٹس", language), planner.transportNotes, false) { onChanged(planner.copy(transportNotes = it)) } } }
        item { ExpandableModule(tr("Family and group", "خاندان اور گروپ", language), Icons.Default.Groups) { PlannerField(tr("Group leader", "گروپ لیڈر", language), planner.groupLeader) { onChanged(planner.copy(groupLeader = it)) }; PlannerField(tr("Group phone", "گروپ فون", language), planner.groupPhone) { onChanged(planner.copy(groupPhone = it)) }; PlannerField(tr("Family meeting point", "خاندان کا ملنے کا مقام", language), planner.familyMeetingPoint) { onChanged(planner.copy(familyMeetingPoint = it)) } } }
        item { ExpandableModule(tr("Official services", "سرکاری خدمات", language), Icons.Default.Link) { Text(tr("Permits and bookings are completed only on official platforms. NoorPro does not issue permits or sell packages.", "اجازت نامے اور بکنگ صرف سرکاری پلیٹ فارم پر ہوتے ہیں۔ NoorPro اجازت نامہ یا پیکیج فروخت نہیں کرتا۔", language), color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp); links.forEach { link -> OutlinedButton({ openOfficialUrl(context, link.url) }, Modifier.fillMaxWidth().height(52.dp).padding(top = 5.dp)) { Text(link.title, maxLines = 1, overflow = TextOverflow.Ellipsis); Spacer(Modifier.weight(1f)); Icon(Icons.Default.Link, null, Modifier.size(18.dp)) } } } }
    }
}

@Composable
private fun PilgrimageHelpScreen(language: PilgrimageLanguage, planner: PilgrimagePlannerState, links: List<OfficialServiceLink>) {
    val context = LocalContext.current
    val contacts = listOf(Triple("911", tr("Unified emergency", "متحدہ ہنگامی نمبر", language), tr("Saudi Arabia", "سعودی عرب", language)), Triple("997", tr("Ambulance", "ایمبولینس", language), tr("Saudi Arabia", "سعودی عرب", language)), Triple("999", tr("Police", "پولیس", language), tr("Saudi Arabia", "سعودی عرب", language)), Triple("937", tr("Ministry of Health", "وزارت صحت", language), tr("Saudi Arabia", "سعودی عرب", language)), Triple("1966", tr("Pilgrim Care Center", "زائر نگہداشت مرکز", language), tr("24/7 · 11 languages", "24/7 · 11 زبانیں", language)))
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenIntro(tr("Help and emergency", "مدد اور ہنگامی", language), tr("Large one-hand actions for urgent use in Saudi Arabia.", "سعودی عرب میں فوری استعمال کے لیے بڑے بٹن۔", language), Icons.Default.Warning, true) }
        item { Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PilgrimageError)) { Column(Modifier.padding(16.dp)) { Text(tr("If anyone is in immediate danger", "اگر کسی کو فوری خطرہ ہو", language), color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp); Text(tr("Call the appropriate Saudi emergency service. The phone dialler opens first.", "متعلقہ سعودی ہنگامی سروس کو کال کریں۔ پہلے فون ڈائلر کھلے گا۔", language), color = Color.White.copy(.9f), fontSize = 14.sp, lineHeight = 20.sp) } } }
        items(contacts) { (number, title, subtitle) -> FeatureCard(border = PilgrimageError.copy(.22f)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("$number · $subtitle", color = stitchMutedText(), fontSize = 13.sp) }; Button({ openDialer(context, number) }, Modifier.height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = PilgrimageError)) { Icon(Icons.Default.Phone, null); Spacer(Modifier.width(5.dp)); Text(tr("Call", "کال", language)) } } } }
        if (planner.hotelName.isNotBlank() || planner.groupPhone.isNotBlank() || planner.familyMeetingPoint.isNotBlank()) item { FeatureCard(background = lightOrDark(PilgrimageCream)) { Text(tr("Your offline emergency card", "آپ کا آف لائن ہنگامی کارڈ", language), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp); if (planner.hotelName.isNotBlank()) InfoLine(tr("Hotel", "ہوٹل", language), planner.hotelName); if (planner.groupLeader.isNotBlank()) InfoLine(tr("Group leader", "گروپ لیڈر", language), planner.groupLeader); if (planner.groupPhone.isNotBlank()) InfoLine(tr("Phone", "فون", language), planner.groupPhone); if (planner.familyMeetingPoint.isNotBlank()) InfoLine(tr("Meeting point", "ملنے کا مقام", language), planner.familyMeetingPoint) } }
        item { FeatureCard { Text(tr("Crowd safety", "ہجوم کی حفاظت", language), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp); listOf(tr("Follow official signs and crowd-control staff.", "سرکاری نشانات اور عملے کی ہدایات پر عمل کریں۔", language), tr("Do not push or stop in moving lanes.", "چلتی قطار میں دھکا یا توقف نہ کریں۔", language), tr("If separated, go to your saved meeting point.", "بچھڑنے پر محفوظ ملنے کے مقام پر جائیں۔", language), tr("Seek medical help early for heat symptoms.", "گرمی کی علامات پر جلد طبی مدد لیں۔", language)).forEach { BulletLine(it) } } }
        item { links.firstOrNull { it.serviceType == "emergency" }?.let { OutlinedButton({ openOfficialUrl(context, it.url) }, Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Default.Verified, null); Spacer(Modifier.width(6.dp)); Text(tr("Verify on Saudi government website", "سعودی سرکاری ویب سائٹ پر تصدیق", language)) } }; Text(tr("Emergency numbers verified 12 September 2026 from Saudi government sources.", "ہنگامی نمبرز 12 ستمبر 2026 کو سعودی سرکاری ذرائع سے تصدیق شدہ ہیں۔", language), color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp)) }
    }
}

@Composable private fun ExpandableModule(title: String, icon: ImageVector, initiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit) { var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }; FeatureCard { Row(Modifier.fillMaxWidth().height(48.dp).clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = stitchPrimary(), modifier = Modifier.size(24.dp)); Spacer(Modifier.width(10.dp)); Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f)); Icon(Icons.Default.ExpandMore, null, tint = stitchMutedText()) }; if (expanded) { Spacer(Modifier.height(4.dp)); content() } } }
@Composable private fun PlannerField(label: String, value: String, singleLine: Boolean = true, onChanged: (String) -> Unit) { OutlinedTextField(value, onChanged, label = { Text(label) }, singleLine = singleLine, minLines = if (singleLine) 1 else 2, modifier = Modifier.fillMaxWidth().padding(top = 7.dp), shape = RoundedCornerShape(14.dp)) }
@Composable private fun ChecklistRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).clickable { onChanged(!checked) }, verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, onChanged); Text(label, color = stitchText(), fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f)) } }
@Composable private fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) { Surface(Modifier.height(44.dp).clickable(onClick = onClick), CircleShape, color = if (selected) PilgrimageGreen else stitchSurface(), border = BorderStroke(1.dp, if (selected) PilgrimageGreen else stitchMutedText().copy(.25f))) { Box(Modifier.padding(horizontal = 17.dp), contentAlignment = Alignment.Center) { Text(label, color = if (selected) Color.White else stitchText(), fontSize = 13.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun FeatureCard(background: Color = stitchSurface(), border: Color? = null, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = background), border = BorderStroke(1.dp, border ?: if (isStitchLight()) Color(0xFFDDE6E2) else stitchMutedText().copy(.18f)), elevation = CardDefaults.cardElevation(1.dp)) { Column(Modifier.padding(16.dp), content = content) } }
@Composable private fun ScreenIntro(title: String, subtitle: String, icon: ImageVector, danger: Boolean = false) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (danger) PilgrimageError.copy(.12f) else stitchSoftSurface()), contentAlignment = Alignment.Center) { Icon(icon, title, tint = if (danger) PilgrimageError else stitchPrimary(), modifier = Modifier.size(28.dp)) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = stitchText(), fontWeight = FontWeight.Black, fontFamily = LibreCaslon, fontSize = 23.sp); Text(subtitle, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 18.sp) } } }
@Composable private fun OfflineStatus(isOnline: Boolean, language: PilgrimageLanguage) { Surface(shape = CircleShape, color = if (isOnline) PilgrimageLightGreen else PilgrimageCream, border = BorderStroke(1.dp, if (isOnline) PilgrimageGreen.copy(.25f) else PilgrimageGold.copy(.35f))) { Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (isOnline) Icons.Default.CloudDone else Icons.Default.WifiOff, null, tint = if (isOnline) PilgrimageGreen else PilgrimageGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(if (isOnline) tr("Online · progress syncs when signed in", "آن لائن · سائن ان پر پیش رفت سنک", language) else tr("Offline · counters and checklists still save", "آف لائن · کاؤنٹر اور فہرست محفوظ", language), color = Color(0xFF14242D), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } } }
@Composable private fun SourceSafetyNote(language: PilgrimageLanguage) { FeatureCard(background = lightOrDark(PilgrimageCream), border = PilgrimageGold.copy(.32f)) { Row(verticalAlignment = Alignment.Top) { Icon(Icons.Default.Verified, null, tint = PilgrimageGold, modifier = Modifier.size(23.dp)); Spacer(Modifier.width(9.dp)); Text(tr("Source-reviewed general guidance. For personal rulings, health needs, crowd concessions and madhhab differences, follow a qualified scholar, your authorised group and current official instructions.", "یہ ماخذ سے جائزہ شدہ عمومی رہنمائی ہے۔ ذاتی مسائل، صحت، ہجوم کی رعایت اور فقہی اختلافات کے لیے مستند عالم، مجاز گروپ اور موجودہ سرکاری ہدایات پر عمل کریں۔", language), color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp) } } }
@Composable private fun InfoLine(label: String, value: String) { Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.Top) { Text("$label:", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.width(105.dp)); Text(value, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)) } }
@Composable private fun BulletLine(text: String) { Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.CheckCircle, null, tint = stitchPrimary(), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text(text, color = stitchMutedText(), fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f)) } }
@Composable private fun lightOrDark(light: Color): Color = if (isStitchLight()) light else stitchSoftSurface()

private fun PilgrimageGuide.localizedTitle(language: PilgrimageLanguage) = if (language == PilgrimageLanguage.URDU) titleUrdu else title
private fun PilgrimageGuide.localizedSummary(language: PilgrimageLanguage) = if (language == PilgrimageLanguage.URDU) summaryUrdu else summary
private fun tr(english: String, urdu: String, language: PilgrimageLanguage) = if (language == PilgrimageLanguage.URDU) urdu else english
private fun madhhabLabel(value: Madhhab) = when (value) { Madhhab.GENERAL -> "General"; Madhhab.HANAFI -> "Hanafi"; Madhhab.SHAFII -> "Shafi‘i"; Madhhab.MALIKI -> "Maliki"; Madhhab.HANBALI -> "Hanbali" }

@Composable private fun rememberNetworkStatus(): State<Boolean> { val context = LocalContext.current; val manager = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }; val state = remember { mutableStateOf(manager.isCurrentlyOnline()) }; DisposableEffect(manager) { val callback = object : ConnectivityManager.NetworkCallback() { override fun onAvailable(network: Network) { state.value = manager.isCurrentlyOnline() }; override fun onLost(network: Network) { state.value = manager.isCurrentlyOnline() }; override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { state.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } }; runCatching { manager.registerDefaultNetworkCallback(callback) }; onDispose { runCatching { manager.unregisterNetworkCallback(callback) } } }; return state }
private fun ConnectivityManager.isCurrentlyOnline(): Boolean = activeNetwork?.let { getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } == true
private fun hasConfiguredMapsKey(context: Context): Boolean = runCatching { val metadata = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA).metaData; val key = metadata?.getString("com.google.android.geo.API_KEY").orEmpty(); key.isNotBlank() && key != "DEFAULT_API_KEY" && key != "NO_KEY_CONFIGURED" && !key.startsWith("${'$'}{") }.getOrDefault(false)
private fun vibrateRound(context: Context) { val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") vibrator?.vibrate(80) }
private fun openDialer(context: Context, number: String) { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) } }
private fun openOfficialUrl(context: Context, url: String) { if (isSafeOfficialUrl(url)) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }

private class DuaSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    private var pending: Pair<String, Locale>? = null
    private val tts = TextToSpeech(context.applicationContext, this)
    override fun onInit(status: Int) { ready = status == TextToSpeech.SUCCESS; if (ready) pending?.let { speak(it.first, it.second) }; pending = null }
    fun speak(text: String, locale: Locale) { if (!ready) { pending = text to locale; return }; tts.language = locale; tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pilgrimage_dua") }
    fun shutdown() { tts.stop(); tts.shutdown() }
}
