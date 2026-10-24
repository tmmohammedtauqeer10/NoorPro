package com.noorpro.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noorpro.app.R
import com.noorpro.app.prayer.PrayerScheduler
import com.noorpro.app.ui.theme.AppLanguage
import com.noorpro.app.ui.theme.isAppDark
import com.noorpro.app.ui.viewmodel.CalculationMethod
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.noorpro.app.ui.viewmodel.Madhab
import com.noorpro.app.ui.viewmodel.ThemeMode
import kotlinx.coroutines.launch

/* ---------------------------------------------------------------- shared brand pieces */

/** Deep emerald used behind the brand mark (both modes; darker at night). */
internal val brandGreen: Color @Composable get() = if (isAppDark) Color(0xFF0B2A21) else Color(0xFF064E3B)
internal val brandGreenDeep: Color @Composable get() = if (isAppDark) Color(0xFF07160F) else Color(0xFF033327)
internal val brandGold = Color(0xFFD4AF37)

/** Subtle repeating eight-point star (khatam) lattice. */
@Composable
fun IslamicPattern(color: Color, modifier: Modifier = Modifier, cell: Dp = 56.dp) {
    Canvas(modifier) {
        val c = cell.toPx()
        val r = c * 0.34f
        val stroke = Stroke(width = 1.1.dp.toPx())
        var y = 0f
        var row = 0
        while (y < size.height + c) {
            var x = if (row % 2 == 0) 0f else c / 2
            while (x < size.width + c) {
                val p = Path()
                for (i in 0 until 16) {
                    val a = Math.toRadians(i * 22.5 - 90.0)
                    val rr = if (i % 2 == 0) r else r * 0.62f
                    val px = x + (kotlin.math.cos(a) * rr).toFloat()
                    val py = y + (kotlin.math.sin(a) * rr).toFloat()
                    if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                }
                p.close()
                drawPath(p, color, style = stroke)
                drawCircle(color, r * 0.18f, Offset(x, y), style = stroke)
                x += c
            }
            y += c * 0.86f; row++
        }
    }
}

/** 1.0.97: NoorPro brand green / cream (the N-crescent logo). */
internal val noorLogoGreen = Color(0xFF0B6243)
internal val noorLogoCream = Color(0xFFFDF9F0)

/**
 * 1.0.97: the NoorPro logo - cream N-crescent on a green rounded tile. On green surfaces
 * ([onGreen] = true) the bare cream N is drawn instead so it never sits as a tile-on-green.
 */
@Composable
fun NoorBrandMark(size: Dp = 84.dp, onGreen: Boolean = false) {
    if (onGreen) {
        Image(
            painterResource(R.drawable.brand_n_glyph), contentDescription = "NoorPro",
            modifier = Modifier.size(size * 0.99f, size)
        )
    } else {
        Image(painterResource(R.drawable.brand_n_tile), contentDescription = "NoorPro", modifier = Modifier.size(size))
    }
}

/** 1.0.97: "NoorPro" wordmark (vector, Plus Jakarta Sans ExtraBold) tinted to [color]. */
@Composable
fun NoorWordmark(height: Dp = 28.dp, color: Color = noorLogoGreen) {
    Image(
        painterResource(R.drawable.brand_wordmark), contentDescription = "NoorPro",
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color),
        modifier = Modifier.height(height)
    )
}

/** In-app splash that continues the system splash (same green + mark) and fades into the app. */
@Composable
fun BrandSplash(modifier: Modifier = Modifier) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    // 1.0.97: same flat green + cream N as the system splash, with the NoorPro wordmark under it.
    Box(modifier.fillMaxSize().background(noorLogoGreen), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsAlpha(appear.value).offset(y = ((1f - appear.value) * 16).dp)
        ) {
            NoorBrandMark(104.dp, onGreen = true)
            Spacer(Modifier.height(26.dp))
            NoorWordmark(34.dp, noorLogoCream)
        }
        // 1.0.99: publisher credit ("from Al-Hikmah Digital") replaces the bismillah line, matching the system splash.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 34.dp)
                .graphicsAlpha(appear.value).testTag("splash_publisher")
        ) {
            Text("from", color = noorLogoCream.copy(alpha = 0.6f), fontSize = 12.sp, letterSpacing = 0.4.sp,
                fontFamily = com.noorpro.app.ui.theme.PlusJakarta)
            Spacer(Modifier.height(2.dp))
            Text("Al-Hikmah Digital", color = noorLogoCream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp, fontFamily = com.noorpro.app.ui.theme.PlusJakarta)
        }
    }
}

private fun Modifier.graphicsAlpha(a: Float) = this.alpha(a)

/* ---------------------------------------------------------------- exact-alarm one-time ask */

/** Exact alarm special access is requested only on the onboarding permissions page or the first time
 *  the user turns on an adhan reminder — never over Login, and only once. */
object ExactAlarmAsk {
    var pending by mutableStateOf(false)
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    fun asked(ctx: Context) = prefs(ctx).getBoolean("exact_alarm_asked", false)
    fun markAsked(ctx: Context) { prefs(ctx).edit().putBoolean("exact_alarm_asked", true).apply(); pending = false }
    /** Call when an adhan reminder was just switched on. */
    fun onAdhanEnabled(ctx: Context) {
        if (!asked(ctx) && !PrayerScheduler.canScheduleExact(ctx)) pending = true
    }
}

/* ---------------------------------------------------------------- onboarding */

data class EntrancePermState(
    val location: Boolean, val notifications: Boolean, val exactAlarm: Boolean, val exactNeeded: Boolean,
    val locationDenied: Boolean = false, val manualCity: String? = null,
    /** null = battery card hidden. */
    val batteryOk: Boolean? = null,
) { val alarms: Boolean get() = notifications && (exactAlarm || !exactNeeded) }

/** Cities offered when location is denied (lat, lng). */
val ManualCities = listOf(
    Triple("Bengaluru, IN", 12.9716, 77.5946), Triple("Hyderabad, IN", 17.3850, 78.4867), Triple("Mumbai, IN", 19.0760, 72.8777),
    Triple("Delhi, IN", 28.6139, 77.2090), Triple("Chennai, IN", 13.0827, 80.2707), Triple("Kolkata, IN", 22.5726, 88.3639),
    Triple("Lucknow, IN", 26.8467, 80.9462), Triple("Srinagar, IN", 34.0837, 74.7973), Triple("Karachi, PK", 24.8607, 67.0011),
    Triple("Lahore, PK", 31.5204, 74.3587), Triple("Dhaka, BD", 23.8103, 90.4125), Triple("Dubai, AE", 25.2048, 55.2708),
    Triple("Riyadh, SA", 24.7136, 46.6753), Triple("Makkah, SA", 21.4225, 39.8262), Triple("London, UK", 51.5072, -0.1276),
    Triple("New York, US", 40.7128, -74.0060), Triple("Jakarta, ID", -6.2088, 106.8456), Triple("Istanbul, TR", 41.0082, 28.9784),
)

/** Shared permission actions (onboarding page + Home nudge banner). */
class PermissionActions(val state: EntrancePermState, val onLocation: () -> Unit, val onAlarms: () -> Unit, val onBattery: () -> Unit, val onCity: (Triple<String, Double, Double>) -> Unit)

@Composable
fun rememberPermissionActions(viewModel: DeenViewModel): PermissionActions {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var locDenied by rememberSaveable { mutableStateOf(false) }
    var city by rememberSaveable { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }
    val exactNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    fun openExactIfNeeded() {
        if (exactNeeded && !PrayerScheduler.canScheduleExact(ctx)) { ExactAlarmAsk.markAsked(ctx); PrayerPermissions.openExactAlarmSettings(ctx) }
    }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) { locDenied = false; viewModel.checkLocationAndRefresh { } } else locDenied = true
        tick++
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) openExactIfNeeded()
    }
    @Suppress("UNUSED_EXPRESSION") tick
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
    val state = EntrancePermState(
        location = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        notifications = PrayerPermissions.notificationsGranted(ctx),
        exactAlarm = PrayerScheduler.canScheduleExact(ctx),
        exactNeeded = exactNeeded,
        locationDenied = locDenied, manualCity = city,
        batteryOk = pm?.isIgnoringBatteryOptimizations(ctx.packageName),
    )
    return PermissionActions(
        state,
        onLocation = { locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
        onAlarms = {
            when {
                !state.notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                !state.notifications -> PrayerPermissions.openAppNotificationSettings(ctx)
                else -> openExactIfNeeded()
            }
        },
        onBattery = {
            runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
        onCity = { (n, la, ln) -> city = n; viewModel.setManualCity(n, la, ln) },
    )
}

/** Stateful onboarding: wires view-model settings and Android permission requests. */
@Composable
fun EntranceOnboarding(viewModel: DeenViewModel, onFinished: () -> Unit) {
    val ctx = LocalContext.current
    val theme by viewModel.themeMode.collectAsState()
    val method by viewModel.prayerSettingsController.selectedMethod.collectAsState()
    val madhab by viewModel.prayerSettingsController.selectedMadhab.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()
    val actions = rememberPermissionActions(viewModel)
    OnboardingPager(
        theme = theme, onTheme = viewModel::updateThemeMode,
        method = method, onMethod = viewModel.prayerSettingsController::updateCalculationMethod,
        madhab = madhab, onMadhab = viewModel.prayerSettingsController::updateMadhab,
        language = lang, onLanguage = viewModel::updateLanguage,
        perms = actions.state, actions = actions,
        onFinish = {
            ExactAlarmAsk.markAsked(ctx)
            PermissionNudge.setSkipped(ctx, !(actions.state.location || actions.state.manualCity != null) || !actions.state.alarms)
            onFinished()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPager(
    theme: ThemeMode, onTheme: (ThemeMode) -> Unit,
    method: CalculationMethod, onMethod: (CalculationMethod) -> Unit,
    madhab: Madhab, onMadhab: (Madhab) -> Unit,
    language: AppLanguage, onLanguage: (AppLanguage) -> Unit,
    perms: EntrancePermState,
    actions: PermissionActions? = null,
    onFinish: () -> Unit,
    initialPage: Int = 0,
) {
    val pages = 4
    val pager = rememberPagerState(initialPage) { pages }
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(scheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Step ${pager.currentPage + 1} of $pages", color = scheme.onSurfaceVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp).weight(1f)
                )
                AnimatedVisibility(pager.currentPage < pages - 1, enter = fadeIn(), exit = fadeOut()) {
                    TextButton(onClick = { scope.launch { pager.animateScrollToPage(pages - 1) } }) {
                        Text("Skip", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { p ->
                Box(Modifier.fillMaxSize().widthIn(max = 560.dp), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (p) {
                            0 -> WelcomePage()
                            1 -> FeaturesPage()
                            2 -> PersonalisePage(theme, onTheme, method, onMethod, madhab, onMadhab, language, onLanguage)
                            else -> PermissionsPage(perms, actions)
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(pages) { i ->
                        val w by animateDpAsState(if (i == pager.currentPage) 26.dp else 8.dp, label = "dot")
                        Box(Modifier.height(8.dp).width(w).clip(CircleShape)
                            .background(if (i == pager.currentPage) scheme.primary else scheme.outlineVariant))
                    }
                }
                Spacer(Modifier.height(18.dp))
                val last = pager.currentPage == pages - 1
                Button(
                    onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        when (pager.currentPage) { 0 -> "Get started"; pages - 1 -> "Continue"; else -> "Next" },
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp))
                }
                if (last) TextButton(onClick = onFinish, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Skip for now", color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PageTitle(eyebrow: String, title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    Text(eyebrow.uppercase(), color = scheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Spacer(Modifier.height(6.dp))
    Text(title, color = scheme.onBackground, fontSize = 28.sp, lineHeight = 34.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(body, color = scheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun WelcomePage() {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(brandGreen, brandGreenDeep))),
        contentAlignment = Alignment.Center
    ) {
        IslamicPattern(brandGold.copy(alpha = 0.10f), Modifier.fillMaxSize(), cell = 48.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NoorBrandMark(84.dp, onGreen = true)
            Spacer(Modifier.height(16.dp))
            Text("السَّلَامُ عَلَيْكُمْ", color = brandGold, fontSize = 30.sp)
            Text("Assalamu Alaikum", color = Color(0xFFFAF3EA), fontSize = 16.sp, letterSpacing = 1.sp)
        }
    }
    Spacer(Modifier.height(28.dp))
    PageTitle(
        "Welcome to Noor Pro", "Your daily companion\nfor faith",
        "Pray on time, read and learn the Quran, and keep your remembrance close — beautifully, and offline when you need it."
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MiniStat("Offline", "Quran & duas", Modifier.weight(1f))
        MiniStat("Accurate", "Prayer times", Modifier.weight(1f))
        MiniStat("Gentle", "Daily reminders", Modifier.weight(1f))
    }
}

@Composable
private fun MiniStat(title: String, sub: String, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(scheme.surface).border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp)).padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(sub, color = scheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FeaturesPage() {
    PageTitle("Everything in one place", "Built for your day", "The essentials of a Muslim's day, designed with care.")
    val items = listOf(
        Triple(Icons.AutoMirrored.Filled.MenuBook, "Quran offline", "Indo-Pak mushaf, translations & audio"),
        Triple(Icons.Default.AccessTime, "Prayer & adhan", "Accurate times with adhan alerts"),
        Triple(Icons.Default.Explore, "Qibla", "Find the direction of the Kaaba"),
        Triple(Icons.Default.FavoriteBorder, "Duas & Darood", "Daily duas, azkar and salawat"),
        Triple(Icons.Default.AutoAwesome, "Noor AI", "Ask questions, get sourced answers"),
        Triple(Icons.Default.Spa, "Tasawwuf", "Books and teachings of the path"),
    )
    items.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            row.forEach { (icon, t, s) -> FeatureCard(icon, t, s, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, sub: String, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.heightIn(min = 132.dp).clip(RoundedCornerShape(22.dp)).background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(22.dp)).padding(16.dp)
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(scheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = scheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(2.dp))
        Text(sub, color = scheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp))
}

@Composable
private fun ChoiceCard(label: String, sub: String?, icon: ImageVector?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(if (selected) scheme.primary.copy(alpha = 0.12f) else scheme.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(role = Role.RadioButton, onClick = onClick).padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) { Icon(icon, null, tint = if (selected) scheme.primary else scheme.onSurfaceVariant); Spacer(Modifier.height(6.dp)) }
        Text(label, color = if (selected) scheme.primary else scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (sub != null) Text(sub, color = scheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PersonalisePage(
    theme: ThemeMode, onTheme: (ThemeMode) -> Unit,
    method: CalculationMethod, onMethod: (CalculationMethod) -> Unit,
    madhab: Madhab, onMadhab: (Madhab) -> Unit,
    language: AppLanguage, onLanguage: (AppLanguage) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    PageTitle("Make it yours", "Personalise Noor Pro", "You can change any of these later in Settings.")
    SectionLabel("Appearance")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ChoiceCard("Light", null, Icons.Default.LightMode, theme == ThemeMode.LIGHT, Modifier.weight(1f)) { onTheme(ThemeMode.LIGHT) }
        ChoiceCard("Dark", null, Icons.Default.DarkMode, theme == ThemeMode.DARK, Modifier.weight(1f)) { onTheme(ThemeMode.DARK) }
        ChoiceCard("Auto", null, Icons.Default.BrightnessAuto, theme == ThemeMode.SYSTEM, Modifier.weight(1f)) { onTheme(ThemeMode.SYSTEM) }
    }
    Spacer(Modifier.height(14.dp))
    SectionLabel("Prayer calculation method")
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(scheme.surface)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp)).clickable { open = true }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Calculate, null, tint = scheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(method.label, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(method.region, color = scheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(Icons.Default.ExpandMore, null, tint = scheme.onSurfaceVariant)
        }
        DropdownMenu(open, { open = false }) {
            CalculationMethod.entries.forEach { m ->
                DropdownMenuItem(
                    text = { Column { Text(m.label, fontSize = 14.sp); Text(m.region, fontSize = 11.sp, color = scheme.onSurfaceVariant) } },
                    trailingIcon = { if (m == method) Icon(Icons.Default.Check, null, tint = scheme.primary) },
                    onClick = { onMethod(m); open = false }
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    SectionLabel("Asr time")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ChoiceCard("Hanafi", "Later Asr (2× shadow)", null, madhab == Madhab.HANAFI, Modifier.weight(1f)) { onMadhab(Madhab.HANAFI) }
        ChoiceCard("Shafi'i", "Shafi'i, Maliki, Hanbali", null, madhab == Madhab.STANDARD, Modifier.weight(1f)) { onMadhab(Madhab.STANDARD) }
    }
    Spacer(Modifier.height(14.dp))
    SectionLabel("Language")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ChoiceCard("English", null, null, language == AppLanguage.EN, Modifier.weight(1f)) { onLanguage(AppLanguage.EN) }
        ChoiceCard("العربية", null, null, language == AppLanguage.AR, Modifier.weight(1f)) { onLanguage(AppLanguage.AR) }
        ChoiceCard("اردو", null, null, language == AppLanguage.UR, Modifier.weight(1f)) { onLanguage(AppLanguage.UR) }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PermissionsPage(perms: EntrancePermState, actions: PermissionActions?) {
    PageTitle("Stay on time", "Set up prayer essentials", "Both are optional — tap Continue or Skip any time.")
    val locOk = perms.location || perms.manualCity != null
    PermissionCard(
        Icons.Default.LocationOn, "Enable location",
        when { perms.manualCity != null && !perms.location -> "Using ${perms.manualCity} for prayer times and Qibla."
               perms.locationDenied -> "Location was denied — choose your city instead."
               else -> "For accurate prayer times and Qibla direction." },
        locOk, "Enable", { actions?.onLocation?.invoke() },
        extra = if (perms.locationDenied && !perms.location) ({ CityPicker(perms.manualCity) { actions?.onCity?.invoke(it) } }) else null,
    )
    PermissionCard(
        Icons.Default.Alarm, "Allow adhan alarms",
        if (perms.exactNeeded) "Notifications + exact alarms so the adhan sounds on the minute." else "Notifications so the adhan reaches you on time.",
        perms.alarms, "Allow", { actions?.onAlarms?.invoke() },
    )
    perms.batteryOk?.let { ok ->
        PermissionCard(Icons.Default.BatteryChargingFull, "Battery saver (optional)", "Helps alarms ring reliably on some phones.", ok, "Open", { actions?.onBattery?.invoke() })
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("You stay in control — change permissions any time in Settings.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun CityPicker(current: String?, onPick: (Triple<String, Double, Double>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.LocationCity, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text(current ?: "Choose city")
        }
        DropdownMenu(open, { open = false }) {
            ManualCities.forEach { c -> DropdownMenuItem(text = { Text(c.first) }, onClick = { open = false; onPick(c) }) }
        }
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, body: String, granted: Boolean, action: String, onAllow: () -> Unit, extra: (@Composable () -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(22.dp)).background(scheme.surface)
            .border(1.dp, if (granted) scheme.primary.copy(alpha = 0.5f) else scheme.outlineVariant, RoundedCornerShape(22.dp)).padding(16.dp)
    ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(scheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = scheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(body, color = scheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        AnimatedContent(granted, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "perm") { ok ->
            if (ok) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, "Enabled", tint = Color(0xFF2E9A72), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(4.dp))
                Text("Enabled", color = Color(0xFF2E9A72), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            } else FilledTonalButton(onClick = onAllow, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                Text(action, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (extra != null) { Spacer(Modifier.height(10.dp)); Row(Modifier.padding(start = 60.dp)) { extra() } }
    }
}


object PermissionNudge {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    fun setSkipped(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("perm_skipped", v).apply()
    fun show(ctx: Context) = prefs(ctx).getBoolean("perm_skipped", false) && !prefs(ctx).getBoolean("perm_nudge_dismissed", false)
    fun dismiss(ctx: Context) = prefs(ctx).edit().putBoolean("perm_nudge_dismissed", true).apply()
}

/** Small dismissible Home banner shown only if onboarding permissions were skipped. */
@Composable
fun PermissionNudgeBanner(viewModel: DeenViewModel, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var visible by remember { mutableStateOf(PermissionNudge.show(ctx)) }
    if (!visible) return
    val a = rememberPermissionActions(viewModel)
    val st = a.state
    val needLoc = !st.location && st.manualCity == null
    val needAlarm = !st.alarms
    if (!needLoc && !needAlarm) return
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(scheme.primaryContainer.copy(alpha = 0.55f))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp)).padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Finish setup for on-time prayers", color = scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { PermissionNudge.dismiss(ctx); visible = false }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Dismiss", tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            if (needAlarm) FilledTonalButton(onClick = a.onAlarms, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Alarm, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Turn on adhan alarms", fontSize = 13.sp)
            }
            if (needLoc) OutlinedButton(onClick = a.onLocation, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Enable location", fontSize = 13.sp)
            }
        }
        if (st.locationDenied && needLoc) Row(Modifier.padding(top = 6.dp)) { CityPicker(null) { a.onCity(it) } }
    }
}
