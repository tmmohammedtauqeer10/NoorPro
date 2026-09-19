package com.example.ui.screens

import com.example.utils.CrashReporter

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalculationMethod
import com.example.ui.viewmodel.DeenViewModel
import com.example.ui.viewmodel.Madhab
import kotlinx.coroutines.launch

// Recolored from gold → green: these file-local constants shadow the theme's gold tokens so
// both Settings screens match the app's green + cream design (same pattern as the other
// recolored screens).
private val MatteGold = Color(0xFF0E8C73)
private val GlowGold = Color(0xFF17B892)
private val GlassOverlay = Color(0x140E8C73)
private val GlassBorder = Color(0x450E8C73)

@Composable
fun AdvancedSettingsScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val controller = viewModel.prayerSettingsController
    val selectedMadhab by controller.selectedMadhab.collectAsState()
    val selectedMethod by controller.selectedMethod.collectAsState()
    val prayers by viewModel.prayers.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentAppLanguage by viewModel.appLanguage.collectAsState()
    val strings = LocalAppStrings.current

    var madhabExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 40.dp)
            .padding(bottom = 128.dp)
            .animateContentSize()
    ) {
        // Section Title: Theme Settings
        Text(
            text = "App Theme",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MatteGold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("theme_card")
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MatteGold.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme",
                            tint = MatteGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display Theme",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                val currentThemeMode by viewModel.themeMode.collectAsState()
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentThemeMode == com.example.ui.viewmodel.ThemeMode.SYSTEM) MatteGold.copy(alpha=0.3f) else Color.Transparent)
                            .clickable { viewModel.updateThemeMode(com.example.ui.viewmodel.ThemeMode.SYSTEM) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("System", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentThemeMode == com.example.ui.viewmodel.ThemeMode.LIGHT) MatteGold.copy(alpha=0.3f) else Color.Transparent)
                            .clickable { viewModel.updateThemeMode(com.example.ui.viewmodel.ThemeMode.LIGHT) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Light", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentThemeMode == com.example.ui.viewmodel.ThemeMode.DARK) MatteGold.copy(alpha=0.3f) else Color.Transparent)
                            .clickable { viewModel.updateThemeMode(com.example.ui.viewmodel.ThemeMode.DARK) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Dark", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Section Title: Language
        Text(
            text = strings.languageSettings,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MatteGold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("language_card")
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MatteGold.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Language",
                            tint = MatteGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Language",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentAppLanguage == AppLanguage.EN) MatteGold.copy(alpha=0.3f) else Color.Transparent)
                            .clickable { viewModel.updateLanguage(AppLanguage.EN) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.english, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentAppLanguage == AppLanguage.AR) MatteGold.copy(alpha=0.3f) else Color.Transparent)
                            .clickable { viewModel.updateLanguage(AppLanguage.AR) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.arabic, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentAppLanguage == AppLanguage.UR) MatteGold.copy(alpha=0.3f) else Color.Transparent)
                            .clickable { viewModel.updateLanguage(AppLanguage.UR) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.urdu, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Section Title: Calculation Settings
        Text(
            text = "Calculation Preference Settings",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MatteGold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 1. Asr Madhab Toggle / Dropdown Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("madhab_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MatteGold.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Madhab Icon",
                            tint = MatteGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Asr Juristic Method (Madhab)",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Determines shadow length calculations for the Asr prayer time",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Modern Segmented Option Bar for Madhab selection (each item >= 48dp height requirement)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val standardText = "Standard (Shafi'i, Maliki, Hanbali)"
                    val isStandard = selectedMadhab == Madhab.STANDARD

                    Button(
                        onClick = { controller.updateMadhab(Madhab.STANDARD) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStandard) MatteGold else GlassOverlay,
                            contentColor = if (isStandard) NightBackground else TextPrimary
                        ),
                        border = BorderStroke(1.dp, if (isStandard) MatteGold else GlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp) // Exceeds 48dp touch target
                            .testTag("madhab_standard_btn")
                    ) {
                        Text(
                            text = "Standard",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    val isHanafi = selectedMadhab == Madhab.HANAFI
                    Button(
                        onClick = { controller.updateMadhab(Madhab.HANAFI) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHanafi) MatteGold else GlassOverlay,
                            contentColor = if (isHanafi) NightBackground else TextPrimary
                        ),
                        border = BorderStroke(1.dp, if (isHanafi) MatteGold else GlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp) // Exceeds 48dp touch target
                            .testTag("madhab_hanafi_btn")
                    ) {
                        Text(
                            text = "Hanafi",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Dynamic detailed helper text based on selection
                Text(
                    text = if (selectedMadhab == Madhab.STANDARD) {
                        "✓ Active: Shadow of an object equals its height (Earlier Asr time)."
                    } else {
                        "✓ Active: Shadow of an object is twice its height (Later Asr time)."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MatteGold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        // 2. Calculation Convention dropdown select Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calc_convention_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MatteGold.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Convention Icon",
                            tint = MatteGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Calculation Authority",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Standard angles used for calculating Dawn (Fajr) and Night (Isha)",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Exposed custom simulated dropdown trigger (>= 48dp height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassOverlay)
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .clickable { methodExpanded = !methodExpanded }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val selectedLabel = if (selectedMethod == CalculationMethod.KARACHI) {
                            "Karachi (Islamic Sciences)"
                        } else {
                            "Muslim World League (MWL)"
                        }
                        Text(
                            text = selectedLabel,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Icon(
                            imageVector = if (methodExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            tint = MatteGold,
                            contentDescription = "Toggle dropdown"
                        )
                    }
                }

                // Dropdown Menu popup items
                if (methodExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        // Option 1: Karachi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp) // Touch target minimum
                                .clickable {
                                    controller.updateCalculationMethod(CalculationMethod.KARACHI)
                                    methodExpanded = false
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == CalculationMethod.KARACHI,
                                onClick = {
                                    controller.updateCalculationMethod(CalculationMethod.KARACHI)
                                    methodExpanded = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MatteGold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "University of Islamic Sciences, Karachi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Best precision for South Asian regions",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        HorizontalDivider(color = GlassBorder.copy(alpha = 0.3f), thickness = 1.dp)

                        // Option 2: MWL
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp) // Touch target minimum
                                .clickable {
                                    controller.updateCalculationMethod(CalculationMethod.MWL)
                                    methodExpanded = false
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == CalculationMethod.MWL,
                                onClick = {
                                    controller.updateCalculationMethod(CalculationMethod.MWL)
                                    methodExpanded = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MatteGold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Muslim World League (MWL)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Standard convention for Europe & Global",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live calculation results card to instantly demo high dynamic update responsiveness
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MatteGold.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Live update status",
                        tint = GlowGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Instant Calculation Preview",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlowGold
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Green.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Green
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val asrTime = prayers.find { it.name == "Asr" }?.time ?: "15:38"
                    val fajrTime = prayers.find { it.name == "Fajr" }?.time ?: "04:35"
                    val ishaTime = prayers.find { it.name == "Isha" }?.time ?: "19:48"

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Fajr", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = fajrTime,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Asr (Shadow time)", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = asrTime,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MatteGold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Isha", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = ishaTime,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // --- OFFLINE FUNCTIONALITY ---
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Offline & Data",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MatteGold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth().testTag("offline_data_card")
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                val isOffline by viewModel.isOfflineMode.collectAsState()
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MatteGold.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            contentDescription = "Offline Mode",
                            tint = MatteGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isOffline) "App is operating in Offline mode." else "App is Online.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Download resources to access them without internet.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                var isDownloadingQuran by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current

                Button(
                    onClick = {
                        if (!isDownloadingQuran) {
                            isDownloadingQuran = true
                            viewModel.downloadAllQuranData { success ->
                                isDownloadingQuran = false
                                if (success) {
                                    android.widget.Toast.makeText(context, "Full Quran downloaded!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to download enirely. Check network.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NightBackground,
                        contentColor = MatteGold
                    ),
                    border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (isDownloadingQuran) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MatteGold,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Downloading (May take a moment)...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Download Full Arabic Quran", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- SMART LOCATION SYNC ---
        RefreshLocationButton(onRefresh = { onComplete ->
            viewModel.checkLocationAndRefresh(onComplete)
        })

        // --- CUSTOMIZABLE PRAYER ALARMS CARD ---
        var soundDropdownExpanded by remember { mutableStateOf(false) }
        val currentSound by controller.alarmSound.collectAsState()
        val alarmSounds = listOf("Mecca Adhan", "Medina Adhan", "Spiritual Oud", "Ascite Echo", "System Sound")
        var configuringPrayerName by remember { mutableStateOf<String?>(null) }

        Text(
            text = "Customizable Prayer Alarms",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MatteGold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prayer_alarm_config_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 1. Master Sound Profile Selection Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MatteGold.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Sound Icon",
                            tint = MatteGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alarm Sound Profile",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Currently selected notification audio",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            fontSize = 11.sp
                        )
                    }
                    
                    Button(
                        onClick = { soundDropdownExpanded = !soundDropdownExpanded },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlassOverlay),
                        border = BorderStroke(1.dp, GlassBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp).testTag("select_sound_profile_btn")
                    ) {
                        Text(
                            text = currentSound,
                            color = MatteGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (soundDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            tint = MatteGold,
                            contentDescription = "sound drop",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                if (soundDropdownExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        alarmSounds.forEach { sound ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        controller.updateAlarmSound(sound)
                                        soundDropdownExpanded = false
                                    }
                                    .padding(horizontal = 12.dp)
                            ) {
                                RadioButton(
                                    selected = currentSound == sound,
                                    onClick = {
                                        controller.updateAlarmSound(sound)
                                        soundDropdownExpanded = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = MatteGold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = sound, color = TextPrimary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        controller.updateAlarmSound(sound)
                                        // Preview sound
                                        try {
                                            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                                            val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                                            ringtone.play()
                                        } catch (e: Exception) {
                                            CrashReporter.report(e)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Test Sound",
                                        tint = MatteGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GlassBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Manage Prayer Reminder Schedules",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 2. Alarms Configuration list for each of the calculated prayers
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    prayers.forEach { prayer ->
                        val isEnabled = prayer.isNotificationEnabled
                        val offsetMinutes = controller.getReminderOffset(prayer.name)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEnabled) GlassOverlay else Color.Transparent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isEnabled) MatteGold.copy(alpha = 0.2f) else GlassBorder.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Custom visual indicators for Alarms
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(prayer.color)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prayer.name,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isEnabled) {
                                                if (offsetMinutes == 0) "Alarm: exact time (${prayer.time})" else "Alert: $offsetMinutes mins before"
                                            } else {
                                                "Alarm disabled"
                                            },
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    // Offset selector expander
                                    if (isEnabled) {
                                        Button(
                                            onClick = {
                                                configuringPrayerName = if (configuringPrayerName == prayer.name) null else prayer.name
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GlassOverlay),
                                            border = BorderStroke(1.dp, GlassBorder),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp).testTag("alarm_offset_trigger_${prayer.name}")
                                        ) {
                                            Text(
                                                text = if (offsetMinutes == 0) "Exact" else "${offsetMinutes}m before",
                                                fontSize = 11.sp,
                                                color = MatteGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    // Alarm on/off switcher
                                    IconButton(
                                        onClick = { viewModel.toggleNotification(prayer.name) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isEnabled) MatteGold.copy(alpha = 0.15f) else GlassOverlay)
                                            .testTag("alarm_toggle_${prayer.name}")
                                    ) {
                                        Icon(
                                            imageVector = if (isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            contentDescription = "toggle alarm",
                                            tint = if (isEnabled) MatteGold else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Interactive offset selection Chips expanded block
                                if (isEnabled && configuringPrayerName == prayer.name) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Select customized alert timing:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    ) {
                                        val offsetOptions = listOf(-15, -10, -5, 0, 5, 10, 15, 30)
                                        offsetOptions.forEach { opt ->
                                            val isOptSelected = offsetMinutes == opt
                                            val label = when {
                                                opt == 0 -> "Exact time"
                                                opt > 0 -> "${opt}m before"
                                                else -> "${-opt}m after"
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isOptSelected) MatteGold else GlassOverlay)
                                                    .border(
                                                        1.dp,
                                                        if (isOptSelected) MatteGold else GlassBorder,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        controller.setReminderOffset(prayer.name, opt)
                                                        viewModel.recalculatePrayers() // Re-triggers full recalculation & reload cycle
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    .testTag("alarm_opt_${prayer.name}_$opt")
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOptSelected) NightBackground else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Quran Translations
        val translationManager = viewModel.translationManager
        val selectedTranslation by translationManager.selectedTranslation.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        var translationExpanded by remember { mutableStateOf(false) }
        var downloadingKey by remember { mutableStateOf<String?>(null) }
        var downloadProgress by remember { mutableStateOf(0f) }
        var downloadError by remember { mutableStateOf<String?>(null) }

        Text(
            text = "Quran Translation Dataset Selection",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MatteGold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("translation_settings_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MatteGold.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Translation Icon",
                            tint = MatteGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "English Translation Dictionary",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        val activeOption = translationManager.availableTranslations.find { it.key == selectedTranslation }
                        Text(
                            text = "Active: ${activeOption?.name ?: "Default (Study Tafseer)"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MatteGold),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = { translationExpanded = !translationExpanded },
                        modifier = Modifier.testTag("toggle_translation_expand_btn")
                    ) {
                        Icon(
                            imageVector = if (translationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Translation Options",
                            tint = TextSecondary
                        )
                    }
                }

                // If error, show it
                downloadError?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Error: $err",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }

                // Expanded Translation list options
                if (translationExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        translationManager.availableTranslations.forEach { option ->
                            val isSelected = selectedTranslation == option.key
                            val isDownloaded = translationManager.isDownloaded(option.key)
                            val isCurrentlyDownloading = downloadingKey == option.key

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MatteGold.copy(alpha = 0.05f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MatteGold.copy(alpha = 0.3f) else GlassBorder.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = isDownloaded && !isCurrentlyDownloading) {
                                        translationManager.selectTranslation(option.key)
                                        viewModel.reloadSurahList()
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            if (isDownloaded && !isCurrentlyDownloading) {
                                                translationManager.selectTranslation(option.key)
                                                viewModel.reloadSurahList()
                                            }
                                        },
                                        enabled = isDownloaded && !isCurrentlyDownloading,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MatteGold,
                                            unselectedColor = TextSecondary
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MatteGold else TextPrimary
                                            )
                                        )
                                        Text(
                                            text = option.translator,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = option.description,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary.copy(alpha = 0.8f),
                                                lineHeight = 14.sp
                                            ),
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Action button / status icon
                                    if (option.key == "default" || isDownloaded) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MatteGold,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Button(
                                                onClick = {
                                                    translationManager.selectTranslation(option.key)
                                                    viewModel.reloadSurahList()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.Transparent,
                                                    contentColor = TextPrimary
                                                ),
                                                border = BorderStroke(1.dp, GlassBorder),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .testTag("apply_${option.key}_btn")
                                            ) {
                                                Text("Use", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        if (isCurrentlyDownloading) {
                                            CircularProgressIndicator(
                                                color = MatteGold,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        downloadingKey = option.key
                                                        downloadProgress = 0f
                                                        downloadError = null
                                                        translationManager.downloadTranslation(
                                                            key = option.key,
                                                            onProgress = { progress -> downloadProgress = progress },
                                                            onSuccess = {
                                                                downloadingKey = null
                                                                translationManager.selectTranslation(option.key)
                                                                viewModel.reloadSurahList()
                                                            },
                                                            onError = { error ->
                                                                downloadingKey = null
                                                                downloadError = error
                                                            }
                                                        )
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(GlassBorder)
                                                    .testTag("download_${option.key}_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDownload,
                                                    contentDescription = "Download ${option.name}",
                                                    tint = MatteGold,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // If downloading, show progress bar indicator
                                if (isCurrentlyDownloading) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress },
                                            color = MatteGold,
                                            trackColor = GlassBorder,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${(downloadProgress * 100).toInt()}%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MatteGold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RefreshLocationButton(onRefresh: ((Boolean) -> Unit) -> Unit) {
    var isLoading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val locationPermissionRequest = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
        ) {
            onRefresh { success ->
                isLoading = false
                if (success) {
                    android.widget.Toast.makeText(context, "Location & prayer times updated successfully.", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to update location. Please check GPS.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } else {
            isLoading = false
            android.widget.Toast.makeText(context, "Location permission denied.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    androidx.compose.animation.AnimatedContent(targetState = isLoading, label = "refreshBtn") { loading ->
        Button(
            onClick = {
                if (!loading) {
                    isLoading = true
                    locationPermissionRequest.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = com.example.ui.theme.NightBackground,
                contentColor = com.example.ui.theme.MatteGold
            ),
            border = BorderStroke(1.dp, com.example.ui.theme.MatteGold.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = com.example.ui.theme.MatteGold,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sync Current Location",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
