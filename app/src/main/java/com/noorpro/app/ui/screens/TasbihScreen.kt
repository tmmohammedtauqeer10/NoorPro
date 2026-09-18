package com.noorpro.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

data class TasbihPreset(
    val title: String,
    val arabic: String,
    val meaning: String,
    val target: Int
)

private val tasbihPresets = listOf(
    TasbihPreset("SubhanAllah", "سُبْحَانَ اللَّهِ", "Glory be to Allah", 33),
    TasbihPreset("Alhamdulillah", "الْحَمْدُ لِلَّهِ", "All praise is for Allah", 33),
    TasbihPreset("Allahu Akbar", "اللَّهُ أَكْبَرُ", "Allah is the Greatest", 34),
    TasbihPreset("Astaghfirullah", "أَسْتَغْفِرُ اللَّهَ", "I seek Allah's forgiveness", 100),
    TasbihPreset("La ilaha illallah", "لَا إِلٰهَ إِلَّا اللَّهُ", "There is no deity except Allah", 100),
    TasbihPreset("SubhanAllahi wa bihamdihi", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "Glory and praise be to Allah", 100),
    TasbihPreset("La hawla wala quwwata", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "There is no power except through Allah", 100),
    TasbihPreset("Salawat", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", "O Allah, send blessings upon Muhammad", 100),
    TasbihPreset("Hasbunallahu", "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "Allah is sufficient for us", 33),
    TasbihPreset("Dua of Yunus", "لَا إِلٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ", "There is no deity but You; glory be to You", 33),
    TasbihPreset("SubhanAllahil Azim", "سُبْحَانَ اللَّهِ الْعَظِيمِ", "Glory be to Allah the Magnificent", 100),
    TasbihPreset("Tahleel", "لَا إِلٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "None worthy of worship but Allah, alone, without partner", 100),
    TasbihPreset("Ya Hayyu Ya Qayyum", "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ", "O Ever-Living, O Sustainer, by Your mercy I seek help", 100),
    TasbihPreset("Rabbi Zidni Ilma", "رَبِّ زِدْنِي عِلْمًا", "My Lord, increase me in knowledge", 33),
    TasbihPreset("Allahumma Ajirni", "اللَّهُمَّ أَجِرْنِي مِنَ النَّارِ", "O Allah, save me from the Fire", 7),
    TasbihPreset("Durood Ibrahim", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ", "O Allah, send blessings upon Muhammad and his family", 100),
    TasbihPreset("Ar-Rahman", "يَا رَحْمَٰنُ", "O Most Gracious", 100),
    TasbihPreset("Al-Lateef", "يَا لَطِيفُ", "O Most Subtle and Kind", 129),
    TasbihPreset("SubhanAllahi wa bihamdihi (Azim)", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ سُبْحَانَ اللَّهِ الْعَظِيمِ", "Glory and praise to Allah, Glory to Allah the Magnificent", 100),
    TasbihPreset("La ilaha illallah (Power)", "لَا إِلٰهَ إِلَّا اللَّهُ الْمَلِكُ الْحَقُّ الْمُبِينُ", "There is no deity but Allah, the Sovereign, the Manifest Truth", 100)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(viewModel: DeenViewModel) {
    var selectedPreset by remember { mutableStateOf(tasbihPresets.first()) }
    val date = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    val persistedCount by viewModel.userPreferencesRepo
        .getDhikrCountFlow(date, selectedPreset.title)
        .collectAsState(initial = 0)
    var count by remember(selectedPreset.title, persistedCount) { mutableIntStateOf(persistedCount) }
    val progress = (count % selectedPreset.target).toFloat() / selectedPreset.target
    val completedRounds = count / selectedPreset.target

    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    fun saveCount(newCount: Int) {
        count = newCount.coerceAtLeast(0)
        scope.launch {
            viewModel.userPreferencesRepo.updateDhikrCount(date, selectedPreset.title, count)
        }
    }

    fun vibrate(isRoundComplete: Boolean) {
        try {
            val duration = if (isRoundComplete) 120L else 25L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (_: Exception) {
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Tasbih") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { saveCount(0) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("Choose Dhikr", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tasbihPresets) { preset ->
                            FilterChip(
                                selected = preset == selectedPreset,
                                onClick = { selectedPreset = preset },
                                label = { Text(preset.title, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(selectedPreset.arabic, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(selectedPreset.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(selectedPreset.meaning, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }

            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Round ${completedRounds + 1}", fontWeight = FontWeight.SemiBold)
                        Text("${count % selectedPreset.target} / ${selectedPreset.target}", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                    )
                    if (completedRounds > 0) {
                        Text("$completedRounds completed round${if (completedRounds == 1) "" else "s"}", modifier = Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .size(245.dp)
                        .scale(scale.value)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            val newCount = count + 1
                            saveCount(newCount)
                            vibrate(newCount % selectedPreset.target == 0)
                            scope.launch {
                                scale.animateTo(0.94f, tween(70))
                                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(count.toString(), fontSize = 76.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        Text("TAP TO COUNT", fontSize = 12.sp, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { saveCount(count - 1) },
                        modifier = Modifier.weight(1f),
                        enabled = count > 0
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Undo")
                    }
                    Button(onClick = { saveCount(0) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Reset")
                    }
                }
            }

            item {
                Text(
                    "Your count is saved automatically for each dhikr every day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
