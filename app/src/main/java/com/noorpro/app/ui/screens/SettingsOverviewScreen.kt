package com.noorpro.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.CloudBackupRepository
import com.noorpro.app.ui.components.pressScale
import com.noorpro.app.ui.components.staggeredEntrance
import com.noorpro.app.ui.theme.*
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

@Composable
fun SettingsScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val hijriAdjustment by viewModel.hijriAdjustment.collectAsState()
    val reminderType by viewModel.reminderType.collectAsState()
    val use12HourTime by viewModel.use12HourTime.collectAsState()
    val theme by viewModel.themeMode.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    var showPrivacy by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    val showAdPrivacy = com.noorpro.app.ads.ConsentManager.privacyOptionsRequired.value
    val signedIn = viewModel.isLoggedIn
    // 1.1.5 Halal Guard: moderator entry only for admins, and only when the panel flag is on.
    var isModerator by remember { mutableStateOf(false) }
    LaunchedEffect(signedIn) {
        if (signedIn && com.noorpro.app.data.halal.HalalFlags.moderatorPanel)
            runCatching { com.noorpro.app.data.halal.ModeratorRepository().checkModerator { isModerator = it } }
    }
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val cloudBackupRepository = remember { CloudBackupRepository() }
    var cloudBusy by remember { mutableStateOf(false) }
    var cloudStatus by remember { mutableStateOf<String?>(null) }
    val backgroundBrush = if (isLightTheme) {
        Brush.verticalGradient(listOf(Color(0xFFFAF6EF), Color(0xFFFDFBF7), Color.White))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF07110F), Color(0xFF0B1713), Color(0xFF101B18)))
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                    writer.write(viewModel.exportSettingsJson())
                }
            }.onSuccess { Toast.makeText(context, "Backup saved.", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Backup failed.", Toast.LENGTH_SHORT).show() }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val restored = runCatching {
                val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                json != null && viewModel.importSettingsJson(json)
            }.getOrDefault(false)
            Toast.makeText(context, if (restored) "Settings restored." else "Restore failed.", Toast.LENGTH_SHORT).show()
        }
    }
    val exportPrayerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                    writer.write(viewModel.exportPrayerTimesCsv())
                }
            }.onSuccess { Toast.makeText(context, "Prayer times exported.", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.PROFILE_DASHBOARD) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = settingsTextColor())
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Settings", color = settingsTextColor(), fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("Personalize Noor Pro and manage your data", color = settingsMutedColor(), fontSize = 13.sp)
                    }
                }
                SettingsHeroCard(
                    title = "Noor Pro control center",
                    subtitle = "Theme, prayer settings, cloud backup, notifications, AI help and account tools in one clean place.",
                    icon = Icons.Default.Settings
                )
            }
            // 1.0.73: grouped sections (Account, Prayer, Quran, Appearance, Notifications,
            // Privacy & Data, Al-Noor Audio, Backup, Support, About). Every pre-existing action is kept.
            item {
                SettingsGroup("Account", Icons.Default.AccountCircle, index = 1) {
                    if (signedIn) {
                        GroupRow("Profile", viewModel.userEmail.ifBlank { "Your Noor Pro profile" }, Icons.Default.Person) { viewModel.navigateTo(DeenScreen.PROFILE_DASHBOARD) }
                        GroupRow("Switch account", "Saved accounts on this device", Icons.Default.SwitchAccount) { viewModel.navigateTo(DeenScreen.ACCOUNT_SWITCHER) }
                        GroupRow("Sign out", "You can sign back in any time", Icons.Default.Logout) {
                            viewModel.handleLogout(); viewModel.navigateTo(DeenScreen.LOGIN)
                        }
                        GroupRow("Delete account", "Permanently delete your account and data", Icons.Default.DeleteForever, danger = true) { showDeleteAccount = true }
                    } else {
                        GroupRow("Sign in", "Sync progress and join the Ummah community", Icons.Default.Login) { viewModel.navigateTo(DeenScreen.LOGIN) }
                        GroupRow("Switch account", "Saved accounts on this device", Icons.Default.SwitchAccount) { viewModel.navigateTo(DeenScreen.ACCOUNT_SWITCHER) }
                    }
                }
            }
            item {
                SettingsGroup("Prayer", Icons.Default.AccessTime, index = 2) {
                    GroupRow("Calculation method & Asr madhab", "Method, madhab and high-latitude rules", Icons.Default.Calculate) { viewModel.navigateTo(DeenScreen.ADVANCED_SETTINGS) }
                    GroupRow("Adhan sound per prayer", "Choose the adhan for each prayer", Icons.Default.VolumeUp) { viewModel.navigateTo(DeenScreen.ADVANCED_SETTINGS) }
                    GroupRow("Prayer notifications & exact alarms", "Reminders, pre-alerts and alarm permission", Icons.Default.Alarm) { viewModel.navigateTo(DeenScreen.ADVANCED_SETTINGS) }
                    GroupRow("Location & city", "Refresh location, prayer times, Qibla and Hijri date", Icons.Default.LocationOn) { viewModel.navigateTo(DeenScreen.ADVANCED_SETTINGS) }
                    GroupRow("Qibla", "Compass calibration and settings", Icons.Default.Explore) { viewModel.navigateTo(DeenScreen.QIBLA_MORE) }
                    GroupStepper("Hijri adjustment", "$hijriAdjustment day", Icons.Default.CalendarMonth,
                        onMinus = { viewModel.updateHijriAdjustment(hijriAdjustment - 1) },
                        onPlus = { viewModel.updateHijriAdjustment(hijriAdjustment + 1) })
                    GroupChoice("Time format", Icons.Default.Schedule, listOf("12 Hour", "24 Hour"), if (use12HourTime) "12 Hour" else "24 Hour") {
                        viewModel.updateTimeFormat(it == "12 Hour")
                    }
                    GroupRow("Sync home widget", "Refresh prayer and Hijri information", Icons.Default.Widgets) {
                        viewModel.recalculatePrayers()
                        Toast.makeText(context, "Prayer and Hijri data refreshed.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            item {
                SettingsGroup("Quran", Icons.Default.MenuBook, index = 3) {
                    GroupRow("Script, translations & font size", "Arabic script, translation languages, text size", Icons.Default.Translate) { viewModel.navigateTo(DeenScreen.ADVANCED_SETTINGS) }
                    GroupRow("Reciter", "Current: ${selectedReciter.name}", Icons.Default.RecordVoiceOver) { viewModel.navigateTo(DeenScreen.NOW_PLAYING) }
                    GroupRow("Offline Quran download", "Download the full Arabic Quran", Icons.Default.Download) { viewModel.navigateTo(DeenScreen.ADVANCED_SETTINGS) }
                }
            }
            item {
                SettingsGroup("Appearance", Icons.Default.Palette, index = 4) {
                    GroupChoice("Theme", Icons.Default.DarkMode, listOf("System", "Light", "Dark"),
                        theme.name.lowercase().replaceFirstChar { it.uppercase() }) {
                        viewModel.updateThemeMode(com.noorpro.app.ui.viewmodel.ThemeMode.valueOf(it.uppercase()))
                    }
                    GroupRow("App language", "Current: ${language.name}", Icons.Default.Language) { viewModel.navigateTo(DeenScreen.ADVANCED_SETTINGS) }
                    GroupRow("Text size", "Reading font size", Icons.Default.FormatSize) { viewModel.navigateTo(DeenScreen.ADVANCED_SETTINGS) }
                }
            }
            item {
                SettingsGroup("Notifications", Icons.Default.NotificationsActive, index = 5) {
                    GroupRow("Notification settings", "Prayer alerts, alarm sound and reminder schedules", Icons.Default.Notifications) { viewModel.navigateTo(DeenScreen.ADVANCED_SETTINGS) }
                    GroupRow("Community notifications", "Likes, comments and messages", Icons.Default.Forum) { viewModel.navigateTo(DeenScreen.UMMAH_NOTIFICATIONS) }
                }
            }
            item {
                SettingsGroup("Privacy & Data", Icons.Default.Shield, index = 6) {
                    GroupRow("Profile visibility", "Who can see your Ummah profile", Icons.Default.Visibility) { viewModel.navigateTo(DeenScreen.UMMAH_PRIVACY) }
                    GroupRow("Blocked accounts", "Manage people you blocked", Icons.Default.Block) { viewModel.navigateTo(DeenScreen.UMMAH_BLOCKED) }
                    if (signedIn && com.noorpro.app.data.halal.HalalFlags.safety)
                        GroupRow("Safety", "Message requests, comment filter, blur flagged media", Icons.Default.HealthAndSafety) { viewModel.navigateTo(DeenScreen.SAFETY_SETTINGS) }
                    GroupRow("Community guidelines", "Halal rules for posting, commenting and chat", Icons.Default.Gavel) { viewModel.navigateTo(DeenScreen.HALAL_GUIDELINES) }
                    if (com.noorpro.app.data.halal.ModeratorAccess.showPanel(signedIn, isModerator, com.noorpro.app.data.halal.HalalFlags.moderatorPanel))
                        GroupRow("Moderation", "Review queue, reports and audit log", Icons.Default.AdminPanelSettings) { viewModel.navigateTo(DeenScreen.MODERATOR_PANEL) }
                    GroupRow("Data safety", "What Noor Pro stores and why", Icons.Default.PrivacyTip) { showPrivacy = true }
                    if (showAdPrivacy) GroupRow("Ad consent", "Review or change your ad privacy choices", Icons.Default.AdsClick) {
                        (context as? android.app.Activity)?.let { com.noorpro.app.ads.ConsentManager.showPrivacyOptions(it) }
                    }
                }
            }
            item {
                SettingsGroup("Al-Noor Audio", Icons.Default.Headphones, index = 7) {
                    GroupRow("Audio library", "Recitations, nasheeds, podcasts, duas, tarjuma and tafsir", Icons.Default.LibraryMusic) { viewModel.navigateTo(DeenScreen.AUDIO_LIBRARY) }
                    GroupRow("Now playing", "Player, reciter and queue", Icons.Default.PlayCircle) { viewModel.navigateTo(DeenScreen.NOW_PLAYING) }
                }
            }
            item {
                SettingsGroup("Backup & Tools", Icons.Default.CloudSync, index = 8) {
                    SettingsCloudBackupCard(
                        busy = cloudBusy,
                        status = cloudStatus,
                        onBackup = {
                            cloudBusy = true
                            cloudStatus = "Saving your settings to Firebase..."
                            cloudBackupRepository.backupSettings(
                                settingsJson = viewModel.exportSettingsJson(),
                                appVersion = com.noorpro.app.BuildConfig.VERSION_NAME
                            ) { ok, message ->
                                cloudBusy = false
                                cloudStatus = message
                                Toast.makeText(context, message ?: if (ok) "Cloud backup saved." else "Cloud backup failed.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRestore = {
                            cloudBusy = true
                            cloudStatus = "Checking your cloud backup..."
                            cloudBackupRepository.restoreSettings { snapshot, message ->
                                cloudBusy = false
                                if (snapshot == null) {
                                    val status = message ?: "No cloud backup found."
                                    cloudStatus = status
                                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                } else {
                                    val restored = viewModel.importSettingsJson(snapshot.settingsJson)
                                    val status = if (restored) "Cloud backup restored." else "Cloud backup could not be applied."
                                    cloudStatus = status
                                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    GroupRow("Backup settings file", "Save preferences to a JSON file on your phone", Icons.Default.Backup) { backupLauncher.launch("noor-pro-settings.json") }
                    GroupRow("Restore settings file", "Restore preferences from a local backup", Icons.Default.Restore) { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }
                    GroupRow("Export prayer times", "Create a shareable CSV file", Icons.Default.FileDownload) { exportPrayerLauncher.launch("noor-pro-prayer-times.csv") }
                }
            }
            item {
                SettingsGroup("Support", Icons.Default.SupportAgent, index = 9) {
                    GroupRow("Ask Noor AI", "App help and Islamic guidance", Icons.Default.AutoAwesome) { viewModel.navigateTo(DeenScreen.AI_HUB) }
                    GroupRow("Rate Noor Pro", "Leave a review on Google Play", Icons.Default.StarRate) {
                        val pkg = context.packageName
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }
                            .onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))) } }
                    }
                    GroupRow("Share Noor Pro", "Invite family and friends", Icons.Default.Share) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Noor Pro — Quran, prayer times, Qibla and more: https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                        runCatching { context.startActivity(Intent.createChooser(send, "Share Noor Pro")) }
                    }
                    GroupRow("Contact us", "noorpro.official@gmail.com", Icons.Default.Email) {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:noorpro.official@gmail.com")).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Noor Pro feedback")
                            putExtra(Intent.EXTRA_TEXT, "App version: ${com.noorpro.app.BuildConfig.VERSION_NAME}\n\n")
                        }
                        runCatching { context.startActivity(intent) }
                            .onFailure { Toast.makeText(context, "No email app is available.", Toast.LENGTH_SHORT).show() }
                    }
                    GroupRow("Privacy policy", "How Noor Pro handles your information", Icons.Default.Policy) { UgcTerms.openUrl(context, UgcTerms.PRIVACY_URL) }
                    GroupRow("Terms of service", "Community rules, reporting and moderation", Icons.Default.Gavel) { UgcTerms.openUrl(context, UgcTerms.TERMS_URL) }
                }
            }
            item {
                SettingsGroup("About", Icons.Default.Info, index = 10) {
                    GroupRow("About Noor Pro", "Version ${com.noorpro.app.BuildConfig.VERSION_NAME} (${com.noorpro.app.BuildConfig.VERSION_CODE})", Icons.Default.Info) { showAbout = true }
                }
            }
        }
    }

    if (showPrivacy) {
        SettingsInfoDialog(
            title = "Privacy Policy",
            body = "Noor Pro stores prayer progress, bookmarks, points, and preferences locally on your device. Firebase Authentication is used only when you choose to sign in. Location is used to calculate prayer times and Qibla direction. Noor Pro does not sell personal information.",
            onDismiss = { showPrivacy = false }
        )
    }
    if (showDeleteAccount) DeleteAccountDialog(viewModel) { showDeleteAccount = false }
    if (showAbout) {
        SettingsInfoDialog(
            title = "About Noor Pro",
            showLogo = true,
            body = "Noor Pro is an all-in-one Islamic companion for Quran, prayer times, Qibla, Azkar, duas, learning, and personal progress. Prayer and Hijri information is refreshed through the AlAdhan API with an offline calculation fallback.",
            onDismiss = { showAbout = false }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title.uppercase(),
        color = settingsAccentColor(),
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        letterSpacing = 2.5.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsHubRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = settingsCardColor()),
        border = BorderStroke(1.dp, settingsBorderColor()),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(settingsIconBackground(), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = settingsAccentColor(), modifier = Modifier.size(23.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = settingsTextColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = settingsMutedColor(), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = settingsMutedColor())
        }
    }
}

@Composable
private fun SettingsStepperRow(title: String, subtitle: String, icon: ImageVector, onMinus: () -> Unit, onPlus: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = settingsCardColor()), border = BorderStroke(1.dp, settingsBorderColor()), shape = RoundedCornerShape(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(settingsIconBackground(), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = settingsAccentColor(), modifier = Modifier.size(23.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = settingsTextColor(), fontWeight = FontWeight.Bold)
                Text(subtitle, color = settingsMutedColor(), fontSize = 12.sp)
            }
            IconButton(onClick = onMinus) { Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = settingsAccentColor()) }
            IconButton(onClick = onPlus) { Icon(Icons.Default.Add, contentDescription = "Increase", tint = settingsAccentColor()) }
        }
    }
}

@Composable
private fun SettingsChoiceRow(title: String, subtitle: String, icon: ImageVector, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = settingsCardColor()), border = BorderStroke(1.dp, settingsBorderColor()), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).background(settingsIconBackground(), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = settingsAccentColor(), modifier = Modifier.size(23.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(title, color = settingsTextColor(), fontWeight = FontWeight.Bold)
                    Text(subtitle, color = settingsMutedColor(), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(selected = selected == option, onClick = { onSelected(option) }, label = { Text(option) })
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoDialog(title: String, body: String, showLogo: Boolean = false, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // 1.0.97: About shows the NoorPro logo.
        icon = if (showLogo) ({ NoorBrandMark(56.dp) }) else null,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun SettingsHeroCard(title: String, subtitle: String, icon: ImageVector) {
    Spacer(modifier = Modifier.height(18.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = settingsCardColor()),
        border = BorderStroke(1.dp, settingsBorderColor()),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Brush.verticalGradient(listOf(settingsAccentColor(), Color(0xFFD4AF37))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = settingsTextColor(), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(subtitle, color = settingsMutedColor(), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun SettingsCloudBackupCard(
    busy: Boolean,
    status: String?,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = settingsCardColor()),
        border = BorderStroke(1.dp, settingsBorderColor()),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).background(settingsIconBackground(), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = settingsAccentColor(), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cloud Backup", color = settingsTextColor(), fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("Save and restore your preferences through Firebase", color = settingsMutedColor(), fontSize = 12.sp)
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = settingsAccentColor())
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onBackup,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = settingsAccentColor(), contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back up", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !busy,
                    border = BorderStroke(1.dp, settingsBorderColor()),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore", color = settingsAccentColor(), fontWeight = FontWeight.Bold)
                }
            }
            status?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = settingsMutedColor(), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun settingsIsLight(): Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f

@Composable
private fun settingsAccentColor(): Color = if (settingsIsLight()) Color(0xFF087C6B) else MatteGold

@Composable
private fun settingsTextColor(): Color = if (settingsIsLight()) Color(0xFF10211E) else Color(0xFFFAF4E8)

@Composable
private fun settingsMutedColor(): Color = if (settingsIsLight()) Color(0xFF64726F) else Color(0xFFC6BCAB)

@Composable
private fun settingsCardColor(): Color = if (settingsIsLight()) Color.White.copy(alpha = 0.96f) else Color(0xFF101B18).copy(alpha = 0.96f)

@Composable
private fun settingsBorderColor(): Color = if (settingsIsLight()) Color(0xFFE4E7E0) else Color(0xFF2B3C35)

@Composable
private fun settingsIconBackground(): Color = if (settingsIsLight()) Color(0xFFF2F4EE) else Color(0xFF17241F)


@Composable
private fun SettingsGroup(title: String, icon: ImageVector, index: Int = 0, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).staggeredEntrance(index, settingsSeen, index)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)) {
            Icon(icon, contentDescription = null, tint = settingsAccentColor(), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), color = settingsAccentColor(), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = settingsCardColor()),
            border = BorderStroke(1.dp, settingsBorderColor()),
            shape = RoundedCornerShape(22.dp)
        ) { Column(Modifier.padding(vertical = 4.dp), content = content) }
    }
}

@Composable
private fun GroupRowShell(icon: ImageVector, danger: Boolean = false, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val tint = if (danger) Color(0xFFE57373) else settingsAccentColor()
    Row(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(settingsIconBackground(), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        content()
    }
}

@Composable
private fun GroupRow(title: String, subtitle: String, icon: ImageVector, danger: Boolean = false, onClick: () -> Unit) {
    GroupRowShell(icon, danger, Modifier.pressScale().clickable(onClick = onClick)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) Color(0xFFE57373) else settingsTextColor(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = settingsMutedColor(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = settingsMutedColor())
    }
}

@Composable
private fun GroupStepper(title: String, subtitle: String, icon: ImageVector, onMinus: () -> Unit, onPlus: () -> Unit) {
    GroupRowShell(icon) {
        Column(Modifier.weight(1f)) {
            Text(title, color = settingsTextColor(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = settingsMutedColor(), fontSize = 12.sp)
        }
        IconButton(onClick = onMinus) { Icon(Icons.Default.Remove, "Decrease", tint = settingsAccentColor()) }
        IconButton(onClick = onPlus) { Icon(Icons.Default.Add, "Increase", tint = settingsAccentColor()) }
    }
}

@Composable
private fun GroupChoice(title: String, icon: ImageVector, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    GroupRowShell(icon) {
        Text(title, color = settingsTextColor(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Row(Modifier.background(settingsIconBackground(), RoundedCornerShape(50)).padding(3.dp)) {
            options.forEach { o ->
                val sel = o == selected
                val bg by androidx.compose.animation.animateColorAsState(if (sel) settingsAccentColor() else Color.Transparent, label = "seg")
                Text(o, color = if (sel) (if (settingsIsLight()) Color.White else Color(0xFF10211E)) else settingsMutedColor(),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(bg, RoundedCornerShape(50)).clickable { onSelected(o) }.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}


/** Groups animate in once per process, not on every scroll back. */
private val settingsSeen = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
