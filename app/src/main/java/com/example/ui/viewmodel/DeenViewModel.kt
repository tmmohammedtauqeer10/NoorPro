package com.example.ui.viewmodel

import com.example.utils.CrashReporter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.media.MediaPlayer
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

val android.content.Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class DeenScreen {
    LOGIN,
    DASHBOARD,
    QURAN,
    REELS,
    NOW_PLAYING,
    PRAYER_TIMES,
    QIBLA_MORE,
    SETTINGS,
    ADVANCED_SETTINGS,
    ACCOUNT_SWITCHER,
    AZKAR,
    TASBIH,
    EXPLORE,
    EDUCATION,
    TAFSIR,
    CALENDAR,
    BOOKMARKS,
    HADITH_LIBRARY,
    HADITH_CHAPTERS,
    HADITH_READING,
    DUA_HUB,
    DUA_DETAILS,
    QAZA_TRACKER,
    LIBRARY_DASHBOARD,
    INDOPAK_QURAN,
    PDF_READER,
    QUIZ_DASHBOARD,
    ACTIVE_QUIZ,
    PROFILE_DASHBOARD,
    QURAN_LEARNING_DASHBOARD,
    QURAN_LEARNING_READER,
    QAIDA_TUTOR,
    UMMAH,
    UMMAH_FULL,
    UMMAH_CREATE_POST,
    UMMAH_CREATE_REEL,
    UMMAH_PROFILE,
    UMMAH_PROFILE_EDIT,
    UMMAH_SEARCH,
    UMMAH_SAVED,
    UMMAH_ACTIVITY,
    UMMAH_ARCHIVE,
    UMMAH_NOTIFICATIONS,
    UMMAH_LIKES_COMMENTS,
    UMMAH_TIME_MANAGEMENT,
    UMMAH_PRIVACY,
    UMMAH_CLOSE_FRIENDS,
    UMMAH_BLOCKED,
    NOOR_PRO_PLUS,
    UMMAH_CHAT,
    UMMAH_MESSAGES,
    UMMAH_CONSTITUTION,
    SPIRITUAL_PROGRESS,
    ASMA_UL_HUSNA,
    ZAKAT,
    HEALTH_WELLNESS,
    AI_HUB,
    PDF_LIBRARY,
    HAJJ_UMRAH,
    CREATOR_STUDIO,
    UMMAH_FOLLOWERS,
    UMMAH_FOLLOWING,
    DEEN_POINTS,
    AUDIO_LIBRARY,
    AUDIO_PLAYLIST,
    DISCOVER_GROUPS
}

/** One track in a curated audio playlist queue — a full surah, or a single ayah (ayah != null). */
data class AudioQueueItem(val surahId: Int, val ayah: Int?, val title: String, val arabic: String)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class DeenViewModel(application: Application) : AndroidViewModel(application) {

    val userPreferencesRepo = com.example.data.UserPreferencesRepository(application)
    private val repository = QuranRepository(application)
    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    var currentPdfUrl: String = ""
    var currentPdfTitle: String = ""

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    // Theme Mode is now backed by DataStore flow
    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            application.dataStore.data
                .map { preferences ->
                    val themeName = preferences[THEME_MODE_KEY] ?: ThemeMode.LIGHT.name
                    try {
                        ThemeMode.valueOf(themeName)
                    } catch (e: Exception) {
                        ThemeMode.LIGHT
                    }
                }
                .collect { mode ->
                    _themeMode.value = mode
                }
        }
    }
    var currentBookId: String = ""

    fun openBook(book: com.example.data.BookItem) {
        currentBookId = book.id
        currentPdfUrl = book.pdfUrl
        currentPdfTitle = book.title
        navigateTo(DeenScreen.PDF_READER)
    }

    val prayerSettingsController = PrayerSettingsController(application)
    val translationManager = TranslationManager(application)

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _libraryCategories = MutableStateFlow<List<com.example.data.LibraryCategory>>(emptyList())
    val libraryCategories: StateFlow<List<com.example.data.LibraryCategory>> = _libraryCategories.asStateFlow()

    private val _isLibraryLoading = MutableStateFlow(false)
    val isLibraryLoading: StateFlow<Boolean> = _isLibraryLoading.asStateFlow()

    private val _libraryError = MutableStateFlow<String?>(null)
    val libraryError: StateFlow<String?> = _libraryError.asStateFlow()

    private val _audioCategories = MutableStateFlow<List<com.example.data.AudioCategory>>(emptyList())
    val audioCategories: StateFlow<List<com.example.data.AudioCategory>> = _audioCategories.asStateFlow()

    private val _isAudioCatalogLoading = MutableStateFlow(false)
    val isAudioCatalogLoading: StateFlow<Boolean> = _isAudioCatalogLoading.asStateFlow()

    private val _audioCatalogError = MutableStateFlow<String?>(null)
    val audioCatalogError: StateFlow<String?> = _audioCatalogError.asStateFlow()

    private val _currentAudioTrack = MutableStateFlow<com.example.data.AudioItem?>(null)
    val currentAudioTrack: StateFlow<com.example.data.AudioItem?> = _currentAudioTrack.asStateFlow()

    fun loadLibraryCatalog() {
        viewModelScope.launch {
            _isLibraryLoading.value = true
            _libraryError.value = null
            try {
                _libraryCategories.value = com.example.data.GithubApiService.fetchLibraryCatalogSafely()
            } catch (e: Exception) {
                CrashReporter.report(e)
                _libraryError.value = e.message ?: "Failed to load library catalog."
            } finally {
                _isLibraryLoading.value = false
            }
        }
    }

    fun loadAudioCatalog() {
        viewModelScope.launch {
            _isAudioCatalogLoading.value = true
            _audioCatalogError.value = null
            try {
                val catalog = com.example.data.GithubApiService.fetchAudioCatalogSafely()
                _audioCategories.value = catalog
                if (catalog.isEmpty()) {
                    _audioCatalogError.value = "Audio catalog unavailable offline. Check your connection."
                }
            } catch (e: Exception) {
                CrashReporter.report(e)
                _audioCatalogError.value = e.message ?: "Failed to load audio catalog."
            } finally {
                _isAudioCatalogLoading.value = false
            }
        }
    }

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOfflineMode.value = false
            loadLibraryCatalog()
            loadAudioCatalog()
        }
        override fun onLost(network: Network) {
            _isOfflineMode.value = true
        }
    }

    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            CrashReporter.report(e, "Failed to register network callback")
        }
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        _isOfflineMode.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true
        loadLibraryCatalog()
        loadAudioCatalog()
    }

    private val prefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    private val secureAccountStore = SecureAccountStore(application)

    private val _totalPoints = MutableStateFlow(
        prefs.getInt("total_points", prefs.getInt("quiz_points", 0))
    )
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()
    val quizPoints: Int get() = _totalPoints.value

    private val _hijriAdjustment = MutableStateFlow(prefs.getInt("hijri_adjustment", 0))
    val hijriAdjustment: StateFlow<Int> = _hijriAdjustment.asStateFlow()

    private val _reminderType = MutableStateFlow(prefs.getString("reminder_type", "Notification") ?: "Notification")
    val reminderType: StateFlow<String> = _reminderType.asStateFlow()

    private val _use12HourTime = MutableStateFlow(prefs.getBoolean("use_12_hour_time", true))
    val use12HourTime: StateFlow<Boolean> = _use12HourTime.asStateFlow()

    fun updateTimeFormat(use12Hour: Boolean) {
        _use12HourTime.value = use12Hour
        prefs.edit().putBoolean("use_12_hour_time", use12Hour).apply()
    }

    fun displayPrayerTime(time: String): String {
        if (!_use12HourTime.value) return time
        val parts = time.take(5).split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return time
        val minute = parts.getOrNull(1) ?: return time
        val suffix = if (hour >= 12) "PM" else "AM"
        val displayHour = when (hour % 12) {
            0 -> 12
            else -> hour % 12
        }
        return "$displayHour:$minute $suffix"
    }

    fun updateHijriAdjustment(days: Int) {
        _hijriAdjustment.value = days.coerceIn(-2, 2)
        prefs.edit().putInt("hijri_adjustment", _hijriAdjustment.value).apply()
        recalculatePrayers()
    }

    fun updateReminderType(type: String) {
        _reminderType.value = type
        prefs.edit().putString("reminder_type", type).apply()
    }

    fun exportSettingsJson(): String {
        return org.json.JSONObject().apply {
            put("version", 1)
            put("theme", _themeMode.value.name)
            put("language", _appLanguage.value.name)
            put("hijriAdjustment", _hijriAdjustment.value)
            put("reminderType", _reminderType.value)
            put("use12HourTime", _use12HourTime.value)
            put("totalPoints", _totalPoints.value)
            put("completedSurahs", org.json.JSONArray(_completedSurahs.value.toList()))
            put("calculationMethod", prayerSettingsController.selectedMethod.value.name)
            put("madhab", prayerSettingsController.selectedMadhab.value.name)
        }.toString(2)
    }

    fun importSettingsJson(json: String): Boolean {
        return try {
            val data = org.json.JSONObject(json)
            data.optString("theme").takeIf { it.isNotBlank() }?.let { updateThemeMode(ThemeMode.valueOf(it)) }
            data.optString("language").takeIf { it.isNotBlank() }?.let { updateLanguage(com.example.ui.theme.AppLanguage.valueOf(it)) }
            updateHijriAdjustment(data.optInt("hijriAdjustment", 0))
            updateReminderType(data.optString("reminderType", "Notification"))
            updateTimeFormat(data.optBoolean("use12HourTime", true))
            data.optString("calculationMethod").takeIf { it.isNotBlank() }?.let {
                prayerSettingsController.updateCalculationMethod(CalculationMethod.valueOf(it))
            }
            data.optString("madhab").takeIf { it.isNotBlank() }?.let {
                prayerSettingsController.updateMadhab(Madhab.valueOf(it))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun exportPrayerTimesCsv(): String {
        return buildString {
            appendLine("Prayer,Time,Notification Enabled")
            _prayers.value.forEach { appendLine("${it.name},${it.time},${it.isNotificationEnabled}") }
        }
    }

    private fun addPoints(points: Int) {
        if (points <= 0) return
        _totalPoints.value += points
        prefs.edit().putInt("total_points", _totalPoints.value).apply()
    }

    private fun awardPointsOnce(key: String, points: Int) {
        val awardKey = "points_awarded_$key"
        if (!prefs.getBoolean(awardKey, false)) {
            addPoints(points)
            prefs.edit().putBoolean(awardKey, true).apply()
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                preferences[THEME_MODE_KEY] = mode.name
            }
        }
    }

    private val bookmarkDao by lazy { com.example.data.BookmarkDatabase.getDatabase(application).bookmarkDao() }
    private val prayerTrackerDao by lazy { com.example.data.PrayerDatabase.getDatabase(application).prayerTrackerDao() }
    val libraryDao by lazy { com.example.data.LibraryDatabase.getDatabase(application).bookDownloadDao() }

    private val _bookmarks = MutableStateFlow<List<com.example.data.BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<com.example.data.BookmarkEntity>> = _bookmarks.asStateFlow()

    private val _todayPrayerLog = MutableStateFlow<PrayerLogEntity?>(null)
    val todayPrayerLog: StateFlow<PrayerLogEntity?> = _todayPrayerLog.asStateFlow()

    private val _weeklyPrayerLogs = MutableStateFlow<List<PrayerLogEntity>>(emptyList())
    val weeklyPrayerLogs: StateFlow<List<PrayerLogEntity>> = _weeklyPrayerLogs.asStateFlow()

    private val _currentMonthLogs = MutableStateFlow<List<PrayerLogEntity>>(emptyList())
    val currentMonthLogs: StateFlow<List<PrayerLogEntity>> = _currentMonthLogs.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _qazaCounts = MutableStateFlow<List<QazaCountEntity>>(emptyList())
    val qazaCounts: StateFlow<List<QazaCountEntity>> = _qazaCounts.asStateFlow()

    // Date formatting for Daily Tracker
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _selectedDuaCategory = MutableStateFlow<DuaCategory?>(null)
    val selectedDuaCategory: StateFlow<DuaCategory?> = _selectedDuaCategory.asStateFlow()

    private val _selectedHadithBookCode = MutableStateFlow<String?>(null)
    val selectedHadithBookCode: StateFlow<String?> = _selectedHadithBookCode.asStateFlow()

    private val _selectedHadithChapterId = MutableStateFlow<Int?>(null)
    val selectedHadithChapterId: StateFlow<Int?> = _selectedHadithChapterId.asStateFlow()

    private val _selectedHadithLanguage = MutableStateFlow("eng")
    val selectedHadithLanguage: StateFlow<String> = _selectedHadithLanguage.asStateFlow()

    private val _hadithChapters = MutableStateFlow<List<com.example.data.HadithChapter>>(emptyList())
    val hadithChapters: StateFlow<List<com.example.data.HadithChapter>> = _hadithChapters.asStateFlow()

    private val _hadithList = MutableStateFlow<List<com.example.data.HadithItem>>(emptyList())
    val hadithList: StateFlow<List<com.example.data.HadithItem>> = _hadithList.asStateFlow()

    private val _isLoadingHadith = MutableStateFlow(false)
    val isLoadingHadith: StateFlow<Boolean> = _isLoadingHadith.asStateFlow()


    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    private val _completedSurahs = MutableStateFlow<Set<Int>>(
        prefs.getStringSet("completed_surahs", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    )
    val completedSurahs: StateFlow<Set<Int>> = _completedSurahs.asStateFlow()

    fun toggleSurahCompletion(surahId: Int) {
        val current = _completedSurahs.value.toMutableSet()
        if (current.contains(surahId)) {
            current.remove(surahId)
        } else {
            current.add(surahId)
            awardPointsOnce("surah_complete_$surahId", 25)
        }
        _completedSurahs.value = current
        prefs.edit().putStringSet("completed_surahs", current.map { it.toString() }.toSet()).apply()
    }

    private val restoredFirebaseUser = try {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }

    var isLoggedIn by mutableStateOf(restoredFirebaseUser != null)
    var userDisplayName by mutableStateOf(restoredFirebaseUser?.displayName.orEmpty())
    var userEmail by mutableStateOf(restoredFirebaseUser?.email.orEmpty())
    // Custom profile photo (avatar). Restored from FirebaseAuth or local prefs so it survives
    // process restarts before the auth profile reload completes.
    var userPhotoUrl by mutableStateOf(
        restoredFirebaseUser?.photoUrl?.toString().orEmpty().ifBlank { prefs.getString("user_photo_url", "").orEmpty() }
    )
    var ummahUsername by mutableStateOf(
        prefs.getString("ummah_username", restoredFirebaseUser?.email?.substringBefore("@").orEmpty()).orEmpty()
    )
    var userBio by mutableStateOf(
        prefs.getString("user_bio_${restoredFirebaseUser?.uid.orEmpty()}", "").orEmpty()
            .ifBlank { prefs.getString("user_bio", "").orEmpty() }
    )

    var savedAuthAccounts by mutableStateOf(secureAccountStore.loadAccounts())
        private set

    // When set, the Ummah profile screen shows this creator's public profile (their posts/
    // reels) instead of the signed-in user's own. Tapping a username in reels/feed sets it.
    var viewedCreatorUid by mutableStateOf("")
    var viewedCreatorName by mutableStateOf("")
    var viewedCreatorHandle by mutableStateOf("")

    /** Open another community member's profile. */
    fun openCreatorProfile(uid: String, name: String, handle: String) {
        if (uid.isBlank()) return
        viewedCreatorUid = uid
        viewedCreatorName = name
        viewedCreatorHandle = handle
        navigateTo(DeenScreen.UMMAH_PROFILE)
    }

    // When set, the Reels screen jumps to (and plays) this specific reel on open, instead of
    // starting at the top of the feed. Tapping a saved/liked/profile reel sets it so the actual
    // reel opens and plays — no static thumbnail or intermediate page. Cleared once focused.
    var reelFocusId by mutableStateOf("")

    /** Open the Reels screen focused on a specific reel and play it. */
    fun openReel(postId: String) {
        reelFocusId = postId
        navigateTo(DeenScreen.REELS)
    }

    // Group invite target from a QR/deep link. The messages screen consumes this after
    // the live group list arrives.
    var groupFocusId by mutableStateOf("")

    fun openGroupInvite(groupId: String) {
        if (groupId.isBlank()) return
        groupFocusId = groupId
        navigateTo(DeenScreen.UMMAH_MESSAGES)
    }

    fun consumeGroupInviteFocus() {
        groupFocusId = ""
    }

    /** Open the signed-in user's own profile (clears any viewed creator). */
    fun openMyProfile() {
        viewedCreatorUid = ""
        viewedCreatorName = ""
        viewedCreatorHandle = ""
        navigateTo(DeenScreen.UMMAH_PROFILE)
    }

    // Direct-message target for the Instagram-style chat thread.
    var chatTargetUid by mutableStateOf("")
    var chatTargetName by mutableStateOf("")

    /** Open a 1:1 chat thread with another member. */
    fun openCreatorChat(uid: String, name: String) {
        if (uid.isBlank()) return
        chatTargetUid = uid
        chatTargetName = name.ifBlank { "Member" }
        navigateTo(DeenScreen.UMMAH_CHAT)
    }

    fun handleFirebaseUser(
        user: com.google.firebase.auth.FirebaseUser?,
        fallbackEmail: String = "",
        fallbackDisplayName: String = ""
    ) {
        if (user == null) {
            handleLogin(fallbackEmail, fallbackDisplayName)
            return
        }
        isLoggedIn = true
        userDisplayName = user.displayName.orEmpty().ifBlank { fallbackDisplayName.ifBlank { user.email?.substringBefore("@").orEmpty() } }
        userEmail = user.email.orEmpty().ifBlank { fallbackEmail }
        userPhotoUrl = user.photoUrl?.toString().orEmpty()
        ummahUsername = prefs.getString("ummah_username_${user.uid}", "").orEmpty()
            .ifBlank { userEmail.substringBefore("@") }
        userBio = prefs.getString("user_bio_${user.uid}", "").orEmpty()
            .ifBlank { prefs.getString("user_bio", "").orEmpty() }
        savedAuthAccounts = secureAccountStore.saveFirebaseUser(user)
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_display_name", userDisplayName)
            .putString("user_email", userEmail)
            .putString("user_photo_url", userPhotoUrl)
            .putString("ummah_username", ummahUsername)
            .putString("ummah_username_${user.uid}", ummahUsername)
            .putString("user_bio", userBio)
            .putString("user_bio_${user.uid}", userBio)
            .apply()
        UmmahRepository().upsertMyProfile(userDisplayName, ummahUsername, userPhotoUrl, userBio)
    }

    fun handleLogin(email: String, displayName: String) {
        val currentUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (currentUser != null) {
            handleFirebaseUser(currentUser, email, displayName)
            return
        }
        isLoggedIn = true
        userDisplayName = displayName
        userEmail = email
        userPhotoUrl = ""
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_display_name", displayName)
            .putString("user_email", email)
            .putString("user_photo_url", "")
            .apply()
    }

    fun rememberCurrentFirebaseAccount() {
        val currentUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (currentUser != null) savedAuthAccounts = secureAccountStore.saveFirebaseUser(currentUser)
    }

    fun reloadSavedAccounts() {
        savedAuthAccounts = secureAccountStore.loadAccounts()
    }

    fun removeSavedAccountFromDevice(uidOrEmail: String) {
        savedAuthAccounts = secureAccountStore.removeAccount(uidOrEmail)
    }

    fun handleLogout(rememberAccount: Boolean = true) {
        if (rememberAccount) rememberCurrentFirebaseAccount()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {
            // Firebase may be intentionally unavailable in guest-only builds.
        }
        isLoggedIn = false
        userDisplayName = ""
        userEmail = ""
        userPhotoUrl = ""
        userBio = ""
        viewedCreatorUid = ""
        viewedCreatorName = ""
        viewedCreatorHandle = ""
        chatTargetUid = ""
        chatTargetName = ""
        reelFocusId = ""
        groupFocusId = ""
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("user_display_name", "")
            .putString("user_email", "")
            .putString("user_photo_url", "")
            .putString("user_bio", "")
            .apply()
    }

    fun updateUmmahProfile(displayName: String, username: String, bio: String = userBio, onResult: (Boolean, String?) -> Unit) {
        val cleanName = displayName.trim()
        val cleanUsername = username.trim().removePrefix("@")
        val cleanBio = bio.trim().take(160)
        if (cleanName.isBlank()) {
            onResult(false, "Name cannot be empty.")
            return
        }
        if (cleanUsername.length < 3) {
            onResult(false, "Username must be at least 3 characters.")
            return
        }
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        fun persistLocal() {
            userDisplayName = cleanName
            ummahUsername = cleanUsername
            userBio = cleanBio
            prefs.edit()
                .putString("user_display_name", cleanName)
                .putString("ummah_username", cleanUsername)
                .putString("ummah_username_${user?.uid.orEmpty()}", cleanUsername)
                .putString("user_bio", cleanBio)
                .putString("user_bio_${user?.uid.orEmpty()}", cleanBio)
                .apply()
            // Broadcast the new identity so every existing reel/post updates live.
            com.example.data.UmmahRepository().upsertMyProfile(cleanName, cleanUsername, userPhotoUrl, cleanBio)
        }
        if (user == null) {
            persistLocal()
            onResult(true, null)
            return
        }
        val profile = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(cleanName)
            .build()
        user.updateProfile(profile)
            .addOnSuccessListener {
                persistLocal()
                onResult(true, null)
            }
            .addOnFailureListener { error ->
                onResult(false, error.localizedMessage ?: "Unable to update profile.")
            }
    }

    /** Upload a new profile photo to Firebase Storage and set it as the account avatar. */
    fun updateProfilePhoto(context: android.content.Context, uri: android.net.Uri, onResult: (Boolean, String?) -> Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, "Please sign in to change your photo.")
            return
        }
        val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ref = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            .child("ummah_avatars/${user.uid}/${System.currentTimeMillis()}")
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(contentType)
            .build()
        ref.putFile(uri, metadata)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val profile = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setPhotoUri(downloadUri)
                            .build()
                        user.updateProfile(profile)
                            .addOnSuccessListener {
                                userPhotoUrl = downloadUri.toString()
                                prefs.edit().putString("user_photo_url", downloadUri.toString()).apply()
                                // Broadcast the new photo so reels/posts avatars update live.
                                com.example.data.UmmahRepository().upsertMyProfile(
                                    userDisplayName, ummahUsername, downloadUri.toString(), userBio
                                )
                                onResult(true, null)
                            }
                            .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Unable to save photo.") }
                    }
                    .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Could not read photo URL.") }
            }
            .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Photo upload failed.") }
    }

    fun sendPasswordReset(onResult: (Boolean, String?) -> Unit) {
        val email = userEmail.trim()
        if (email.isBlank()) {
            onResult(false, "Email is missing.")
            return
        }
        com.google.firebase.auth.FirebaseAuth.getInstance()
            .sendPasswordResetEmail(email)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Unable to send password reset.") }
    }

    private val _appLanguage = MutableStateFlow(
        try {
            com.example.ui.theme.AppLanguage.valueOf(prefs.getString("app_language", "EN") ?: "EN")
        } catch (e: Exception) {
            com.example.ui.theme.AppLanguage.EN
        }
    )
    val appLanguage: StateFlow<com.example.ui.theme.AppLanguage> = _appLanguage.asStateFlow()

    fun updateLanguage(lang: com.example.ui.theme.AppLanguage) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang.name).apply()
    }

    private val _currentScreen = MutableStateFlow(
        if (restoredFirebaseUser != null) DeenScreen.DASHBOARD else DeenScreen.LOGIN
    )
    val currentScreen: StateFlow<DeenScreen> = _currentScreen.asStateFlow()

    private val _isUmmahReelsImmersive = MutableStateFlow(false)
    val isUmmahReelsImmersive: StateFlow<Boolean> = _isUmmahReelsImmersive.asStateFlow()

    fun setUmmahReelsImmersive(enabled: Boolean) {
        _isUmmahReelsImmersive.value = enabled
    }

    // --- Ummah privacy settings (persisted locally + published to the public profile) ---
    fun ummahPrivateAccount(): Boolean = prefs.getBoolean("ummah_private", false)
    fun ummahHideCounts(): Boolean = prefs.getBoolean("ummah_hide_counts", false)

    /** Save the member's privacy choices locally for instant UI, and publish them to their public
     *  profile so other members' apps hide the corresponding information. */
    fun setUmmahPrivacy(isPrivate: Boolean, hideCounts: Boolean) {
        prefs.edit()
            .putBoolean("ummah_private", isPrivate)
            .putBoolean("ummah_hide_counts", hideCounts)
            .apply()
        com.example.data.UmmahRepository().updateMyPrivacy(isPrivate, hideCounts)
    }

    // True while a full-screen modal overlay (e.g. the comment sheet) is open. The app's
    // floating bottom navigation bar hides while this is set so it never overlaps the
    // overlay's own input row.
    private val _isModalOverlayActive = MutableStateFlow(false)
    val isModalOverlayActive: StateFlow<Boolean> = _isModalOverlayActive.asStateFlow()

    fun setModalOverlayActive(active: Boolean) {
        _isModalOverlayActive.value = active
    }

    // Navigation back-stack so the system back button returns to the previous
    // screen instead of exiting the app. DASHBOARD and LOGIN are treated as roots.
    private val backStack = mutableListOf<DeenScreen>()

    private val _selectedSurah = MutableStateFlow<Surah?>(null)
    val selectedSurah: StateFlow<Surah?> = _selectedSurah.asStateFlow()

    private val _playingSurah = MutableStateFlow<Surah?>(null)
    val playingSurah: StateFlow<Surah?> = _playingSurah.asStateFlow()

    private val _selectedTafsirAyah = MutableStateFlow(1)
    val selectedTafsirAyah: StateFlow<Int> = _selectedTafsirAyah.asStateFlow()

    data class HijriDay(
        val gregorianDate: String,
        val hijriDate: String,
        val hijriDay: String,
        val hijriMonthEn: String,
        val hijriMonthAr: String,
        val hijriYear: String,
        val hijriWeekdayEn: String,
        val hijriWeekdayAr: String,
        val holidays: List<String>
    )

    private val _hijriCalendarDays = MutableStateFlow<List<HijriDay>>(emptyList())
    val hijriCalendarDays: StateFlow<List<HijriDay>> = _hijriCalendarDays.asStateFlow()

    private val _isLoadingCalendar = MutableStateFlow(false)
    val isLoadingCalendar: StateFlow<Boolean> = _isLoadingCalendar.asStateFlow()

    fun selectHadithBook(bookCode: String) {
        _selectedHadithBookCode.value = bookCode
        fetchBookChapters(bookCode, "eng")
    }

    fun selectHadithChapter(chapterId: Int) {
        _selectedHadithChapterId.value = chapterId
        val bookCode = _selectedHadithBookCode.value ?: return
        fetchHadithChapter(bookCode, chapterId)
    }

    fun setHadithLanguage(langCode: String) {
        _selectedHadithLanguage.value = langCode
        val bookCode = _selectedHadithBookCode.value
        val chapterId = _selectedHadithChapterId.value
        if (bookCode != null && chapterId != null) {
            fetchHadithChapter(bookCode, chapterId)
        }
    }

    private fun fetchBookChapters(bookCode: String, lang: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoadingHadith.value = true
            try {
                val url = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/info.json"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val stream = response.body?.byteStream()
                    if (stream != null) {
                        val reader = android.util.JsonReader(java.io.InputStreamReader(stream, "UTF-8"))
                        var chaptersFound = false
                        val chapters = mutableListOf<com.example.data.HadithChapter>()
                        
                        reader.beginObject() // opening JSON
                        while (reader.hasNext()) {
                            val name = reader.nextName()
                            if (name == bookCode) {
                                reader.beginObject() // bookCode object
                                while (reader.hasNext()) {
                                    val key1 = reader.nextName()
                                    if (key1 == "metadata") {
                                        reader.beginObject() // metadata object
                                        while (reader.hasNext()) {
                                            val key2 = reader.nextName()
                                            if (key2 == "sections") {
                                                reader.beginObject() // sections object
                                                while (reader.hasNext()) {
                                                    val idStr = reader.nextName()
                                                    val title = reader.nextString()
                                                    val id = idStr.toIntOrNull()
                                                    if (id != null && title.isNotBlank() && idStr != "0") {
                                                        chapters.add(com.example.data.HadithChapter(bookCode, id, title))
                                                    }
                                                }
                                                reader.endObject() // end sections
                                                chaptersFound = true
                                                break // found sections, break out of metadata loop
                                            } else {
                                                reader.skipValue()
                                            }
                                        }
                                        while (reader.hasNext()) {
                                            reader.nextName()
                                            reader.skipValue()
                                        }
                                        reader.endObject() // end metadata
                                        break // found metadata, break out of bookCode loop
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                while (reader.hasNext()) {
                                    reader.nextName()
                                    reader.skipValue()
                                }
                                reader.endObject()
                                break // found bookCode, break out of root loop
                            } else {
                                reader.skipValue()
                            }
                        }
                        
                        // We safely close the stream (reader.close() might throw if not fully consumed, so we suppress it or just use stream.close())
                        try { stream.close() } catch (e: Exception) {}
                        
                        if (chaptersFound) {
                            chapters.sortBy { it.chapterId }
                            _hadithChapters.value = chapters
                        }
                    }
                }
            } catch (e: Exception) {
                CrashReporter.report(e)
            } finally {
                _isLoadingHadith.value = false
            }
            navigateTo(DeenScreen.HADITH_CHAPTERS)
        }
    }

    private fun fetchHadithChapter(bookCode: String, chapterId: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoadingHadith.value = true
            try {
                val langCode = _selectedHadithLanguage.value
                val arUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-$bookCode/sections/$chapterId.json"
                val trUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/$langCode-$bookCode/sections/$chapterId.json"

                val arRequest = okhttp3.Request.Builder().url(arUrl).build()
                val arResponse = httpClient.newCall(arRequest).execute()
                
                val trRequest = okhttp3.Request.Builder().url(trUrl).build()
                var trResponse = httpClient.newCall(trRequest).execute()
                var isFallback = false

                if (!trResponse.isSuccessful && langCode != "eng") {
                    trResponse.close()
                    val fbUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-$bookCode/sections/$chapterId.json"
                    val fbRequest = okhttp3.Request.Builder().url(fbUrl).build()
                    trResponse = httpClient.newCall(fbRequest).execute()
                    isFallback = true
                }

                if (arResponse.isSuccessful) {
                    val arBody = arResponse.body?.string() ?: "{}"
                    val trBody = if (trResponse.isSuccessful) trResponse.body?.string() ?: "{}" else "{}"
                    
                    val arJson = org.json.JSONObject(arBody)
                    val trJson = if (trBody.isNotBlank() && trBody != "404: Not Found") org.json.JSONObject(trBody) else org.json.JSONObject()

                    val arHadiths = arJson.optJSONArray("hadiths") ?: org.json.JSONArray()
                    val trHadiths = trJson.optJSONArray("hadiths")

                    val hadiths = mutableListOf<com.example.data.HadithItem>()
                    for (i in 0 until arHadiths.length()) {
                        val arItem = arHadiths.getJSONObject(i)
                        val trItem = if (trHadiths != null && i < trHadiths.length()) trHadiths.getJSONObject(i) else null
                        
                        val hadithNo = arItem.optInt("hadithnumber", i + 1)
                        val arText = arItem.optString("text", "")
                        var trText = trItem?.optString("text", "Translation not available") ?: "Translation not available"
                        
                        if (isFallback) {
                            trText = "(English Fallback) " + trText
                        }

                        if (arText.isNotBlank()) {
                            hadiths.add(com.example.data.HadithItem(hadithNo, arText, trText, chapterId))
                        }
                    }
                    _hadithList.value = hadiths
                } else {
                    _hadithList.value = emptyList()
                }
            } catch (e: Exception) {
                CrashReporter.report(e)
                _hadithList.value = emptyList()
            } finally {
                _isLoadingHadith.value = false
            }
            navigateTo(DeenScreen.HADITH_READING)
        }
    }

    fun fetchHijriCalendar(month: Int, year: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoadingCalendar.value = true
            try {
                val url = "https://api.aladhan.com/v1/gToHCalendar/$month/$year"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = org.json.JSONObject(response.body?.string() ?: "{}")
                    val data = json.optJSONArray("data")
                    val parsedDays = mutableListOf<HijriDay>()
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            val hijri = item.optJSONObject("hijri")
                            val gregorian = item.optJSONObject("gregorian")
                            
                            val holidaysArr = hijri?.optJSONArray("holidays")
                            val holidays = mutableListOf<String>()
                            if (holidaysArr != null) {
                                for(j in 0 until holidaysArr.length()) {
                                    holidays.add(holidaysArr.getString(j))
                                }
                            }
                            
                            parsedDays.add(
                                HijriDay(
                                    gregorianDate = gregorian?.optString("date", "") ?: "",
                                    hijriDate = hijri?.optString("date", "") ?: "",
                                    hijriDay = hijri?.optString("day", "") ?: "",
                                    hijriMonthEn = hijri?.optJSONObject("month")?.optString("en", "") ?: "",
                                    hijriMonthAr = hijri?.optJSONObject("month")?.optString("ar", "") ?: "",
                                    hijriYear = hijri?.optString("year", "") ?: "",
                                    hijriWeekdayEn = hijri?.optJSONObject("weekday")?.optString("en", "") ?: "",
                                    hijriWeekdayAr = hijri?.optJSONObject("weekday")?.optString("ar", "") ?: "",
                                    holidays = holidays
                                )
                            )
                        }
                    }
                    _hijriCalendarDays.value = parsedDays
                }
            } catch (e: Exception) {
                CrashReporter.report(e)
            } finally {
                _isLoadingCalendar.value = false
            }
        }
    }

    data class QuranWord(
        val id: Int,
        val arabic: String,
        val translation: String
    )
    private val _ibnKathirText = MutableStateFlow<String?>(null)
    val ibnKathirText: StateFlow<String?> = _ibnKathirText.asStateFlow()

    private val _ayahWords = MutableStateFlow<List<QuranWord>>(emptyList())
    val ayahWords: StateFlow<List<QuranWord>> = _ayahWords.asStateFlow()

    private val _isLoadingDynamicTafsir = MutableStateFlow(false)
    val isLoadingDynamicTafsir: StateFlow<Boolean> = _isLoadingDynamicTafsir.asStateFlow()

    fun fetchDynamicTafsirAndWords(surahId: Int, ayahNo: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoadingDynamicTafsir.value = true
            _ibnKathirText.value = null
            _ayahWords.value = emptyList()
            try {
                // Fetch Ibn Kathir (en-tafisr-ibn-kathir = 169)
                val realSoraId = if (surahId < 0) 1 else surahId // Just safe default if Juz
                val lang = translationManager.selectedTranslation.value
                val tafsirId = when {
                    lang.startsWith("ur.") -> 160 // Urdu
                    lang.startsWith("bn.") -> 164 // Bengali
                    lang.startsWith("ar.") -> 14  // Arabic
                    else -> 169 // English
                }
                val tafsirUrl = "https://api.quran.com/api/v4/tafsirs/$tafsirId/by_ayah/$realSoraId:$ayahNo"
                val requestTafsir = okhttp3.Request.Builder().url(tafsirUrl).build()
                val responseTafsir = httpClient.newCall(requestTafsir).execute()
                if (responseTafsir.isSuccessful) {
                    val json = org.json.JSONObject(responseTafsir.body?.string() ?: "{}")
                    var fullText = ""
                    val tafsirObj = json.optJSONObject("tafsir")
                    if (tafsirObj != null) {
                        fullText = tafsirObj.optString("text", "")
                    } else {
                        val tafsirs = json.optJSONArray("tafsirs")
                        if (tafsirs != null) {
                            for (i in 0 until tafsirs.length()) {
                                fullText += tafsirs.getJSONObject(i).optString("text", "") + "\n\n"
                            }
                        }
                    }
                    fullText = androidx.core.text.HtmlCompat.fromHtml(fullText, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
                    _ibnKathirText.value = fullText.ifEmpty { "Detailed Tafsir not available currently." }
                }

                // Fetch word by word
                val wordsUrl = "https://api.quran.com/api/v4/verses/by_key/$realSoraId:$ayahNo?words=true&word_fields=text_uthmani,translation"
                val requestWords = okhttp3.Request.Builder().url(wordsUrl).build()
                val responseWords = httpClient.newCall(requestWords).execute()
                if (responseWords.isSuccessful) {
                    val json = org.json.JSONObject(responseWords.body?.string() ?: "{}")
                    val verseInfo = json.optJSONObject("verse")
                    val wordsArr = verseInfo?.optJSONArray("words")
                    val parsedWords = mutableListOf<QuranWord>()
                    if (wordsArr != null) {
                        for (i in 0 until wordsArr.length()) {
                            val w = wordsArr.getJSONObject(i)
                            val wordText = w.optString("text_uthmani", "")
                            val transObj = w.optJSONObject("translation")
                            val trans = transObj?.optString("text", "") ?: ""
                            if (wordText.isNotEmpty() && trans.isNotEmpty()) {
                                parsedWords.add(QuranWord(i, wordText, trans))
                            }
                        }
                    }
                    _ayahWords.value = parsedWords
                }
            } catch (e: Exception) {
                CrashReporter.report(e)
                _ibnKathirText.value = "Failed to load Ibn Kathir Tafsir."
            } finally {
                _isLoadingDynamicTafsir.value = false
            }
        }
    }

    fun navigateToTafsir(surah: Surah, ayahNumber: Int) {
        _selectedSurah.value = surah
        _selectedTafsirAyah.value = ayahNumber
        navigateTo(DeenScreen.TAFSIR)
        fetchDynamicTafsirAndWords(surah.id, ayahNumber)
    }

    private val _activeDbVerses = MutableStateFlow<List<QuranVerse>>(emptyList())
    val activeDbVerses: StateFlow<List<QuranVerse>> = _activeDbVerses.asStateFlow()

    private val defaultLastReadSurah = IslamicData.surahs.firstOrNull { it.id == 18 }
        ?: IslamicData.surahs.first()

    private val _lastReadSurah = MutableStateFlow(defaultLastReadSurah)
    val lastReadSurah: StateFlow<Surah> = _lastReadSurah.asStateFlow()

    private val _lastReadAyah = MutableStateFlow(1)
    val lastReadAyah: StateFlow<Int> = _lastReadAyah.asStateFlow()

    private val _recentQuranReads = MutableStateFlow(
        listOf(
            defaultLastReadSurah to 10,
            IslamicData.surahs.first() to 1,
            Surah(
                id = -30,
                nameEnglish = "Juz 30",
                nameArabic = "Juz 30",
                englishTranslation = "Amma",
                versesCount = IslamicData.juzAyahsCount[30] ?: 564,
                type = "Juz",
                verses = emptyList()
            ) to 1
        )
    )
    val recentQuranReads: StateFlow<List<Pair<Surah, Int>>> = _recentQuranReads.asStateFlow()

    private val _prayers = MutableStateFlow(IslamicData.defaultPrayers)
    val prayers: StateFlow<List<PrayerTime>> = _prayers.asStateFlow()

    private val _nextPrayerName = MutableStateFlow("Maghrib")
    val nextPrayerName: StateFlow<String> = _nextPrayerName.asStateFlow()

    private val _nextPrayerCountdown = MutableStateFlow("1h 06m")
    val nextPrayerCountdown: StateFlow<String> = _nextPrayerCountdown.asStateFlow()

    private val _todayHijri = MutableStateFlow("...")
    val todayHijri: StateFlow<String> = _todayHijri.asStateFlow()

    private val _todayGregorian = MutableStateFlow("...")
    val todayGregorian: StateFlow<String> = _todayGregorian.asStateFlow()

    private val _qiblaBearing = MutableStateFlow(0f)
    val qiblaBearing: StateFlow<Float> = _qiblaBearing.asStateFlow()

    private val _qiblaDistanceKm = MutableStateFlow(0f)
    val qiblaDistanceKm: StateFlow<Float> = _qiblaDistanceKm.asStateFlow()

    private val _currentLocationName = MutableStateFlow("Locating…")
    val currentLocationName: StateFlow<String> = _currentLocationName.asStateFlow()

    private val _hasCurrentLocation = MutableStateFlow(false)
    val hasCurrentLocation: StateFlow<Boolean> = _hasCurrentLocation.asStateFlow()

    private val _dynamicHijriEvents = MutableStateFlow(IslamicData.hijriEvents)
    val dynamicHijriEvents: StateFlow<List<com.example.data.HijriEvent>> = _dynamicHijriEvents.asStateFlow()

    private val _activePrayerIndex = MutableStateFlow(3) // Default Asr
    val activePrayerIndex: StateFlow<Int> = _activePrayerIndex.asStateFlow()

    private val _dailyQuote = MutableStateFlow(IslamicData.quranQuotes[0])
    val dailyQuote: StateFlow<Pair<String, String>> = _dailyQuote.asStateFlow()

    private val _dailyHadith = MutableStateFlow<com.example.data.Hadith?>(null)
    val dailyHadith: StateFlow<com.example.data.Hadith?> = _dailyHadith.asStateFlow()

    private val _isDailyHadithLoading = MutableStateFlow(false)
    val isDailyHadithLoading: StateFlow<Boolean> = _isDailyHadithLoading.asStateFlow()

    // Quran Audio Playback State Simulation
    private val _selectedReciter = MutableStateFlow(IslamicData.reciters[0])
    val selectedReciter = _selectedReciter.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying = _isAudioPlaying.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled = _isRepeatEnabled.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    private val _audioPlaybackSpeed = MutableStateFlow(1.0f)
    val audioPlaybackSpeed = _audioPlaybackSpeed.asStateFlow()

    private val _audioProgress = MutableStateFlow(0.0f)
    val audioProgress = _audioProgress.asStateFlow()

    private val _playingAyahIndex = MutableStateFlow(0)
    val playingAyahIndex = _playingAyahIndex.asStateFlow()

    private val _audioPosition = MutableStateFlow("0:00")
    val audioPosition = _audioPosition.asStateFlow()

    private val _audioDuration = MutableStateFlow("2:45")
    val audioDuration = _audioDuration.asStateFlow()
    
    private var mediaPlayer: MediaPlayer? = null
    private var samplePlayer: android.media.MediaPlayer? = null
    private val _playingSampleReciterId = MutableStateFlow<Int?>(null)
    val playingSampleReciterId = _playingSampleReciterId.asStateFlow()

    // Azkar counting progress
    private val _azkarCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val azkarCounts = _azkarCounts.asStateFlow()

    // Search query for Quran screening
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Quran content loading state for dynamic online fetching/caching
    private val _isQuranLoading = MutableStateFlow(false)
    val isQuranLoading: StateFlow<Boolean> = _isQuranLoading.asStateFlow()

    private val _quranError = MutableStateFlow<String?>(null)
    val quranError: StateFlow<String?> = _quranError.asStateFlow()

    private val _moreActiveTab = MutableStateFlow(0)
    val moreActiveTab = _moreActiveTab.asStateFlow()

    private val _sensorAzimuth = MutableStateFlow(0f)
    val sensorAzimuth = _sensorAzimuth.asStateFlow()

    private val _hasCompassReading = MutableStateFlow(false)
    val hasCompassReading = _hasCompassReading.asStateFlow()

    private val _compassAccuracy = MutableStateFlow(android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE)
    val compassAccuracy = _compassAccuracy.asStateFlow()

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
    private val rotationVectorSensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
    private val magneticFieldSensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD)
    private val _hasCompassSensor = MutableStateFlow(
        rotationVectorSensor != null || (accelerometerSensor != null && magneticFieldSensor != null)
    )
    val hasCompassSensor = _hasCompassSensor.asStateFlow()

    @Volatile
    private var magneticDeclinationDegrees = 0f
    private var smoothedTrueAzimuth: Float? = null
    private var gravityReading: FloatArray? = null
    private var magneticReading: FloatArray? = null

    private fun displayRotation(): Int {
        val displayManager = getApplication<Application>()
            .getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
        return displayManager?.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.rotation
            ?: android.view.Surface.ROTATION_0
    }

    private fun publishCompassHeading(rotationMatrix: FloatArray, accuracy: Int) {
        val adjusted = FloatArray(9)
        val remapped = when (displayRotation()) {
            android.view.Surface.ROTATION_90 -> android.hardware.SensorManager.remapCoordinateSystem(
                rotationMatrix,
                android.hardware.SensorManager.AXIS_Y,
                android.hardware.SensorManager.AXIS_MINUS_X,
                adjusted
            )
            android.view.Surface.ROTATION_180 -> android.hardware.SensorManager.remapCoordinateSystem(
                rotationMatrix,
                android.hardware.SensorManager.AXIS_MINUS_X,
                android.hardware.SensorManager.AXIS_MINUS_Y,
                adjusted
            )
            android.view.Surface.ROTATION_270 -> android.hardware.SensorManager.remapCoordinateSystem(
                rotationMatrix,
                android.hardware.SensorManager.AXIS_MINUS_Y,
                android.hardware.SensorManager.AXIS_X,
                adjusted
            )
            else -> false
        }
        val screenMatrix = if (remapped) adjusted else rotationMatrix
        val orientation = FloatArray(3)
        android.hardware.SensorManager.getOrientation(screenMatrix, orientation)
        val magneticAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val trueAzimuth = magneticToTrueHeading(magneticAzimuth, magneticDeclinationDegrees)
        if (!trueAzimuth.isFinite()) return
        smoothedTrueAzimuth = smoothCompassHeading(smoothedTrueAzimuth, trueAzimuth)
        _sensorAzimuth.value = smoothedTrueAzimuth ?: trueAzimuth
        _compassAccuracy.value = accuracy
        _hasCompassReading.value = true
    }
    
    private val sensorEventListener = object : android.hardware.SensorEventListener {
        override fun onSensorChanged(event: android.hardware.SensorEvent?) {
            try {
                if (event == null) return
                when (event.sensor.type) {
                    android.hardware.Sensor.TYPE_ROTATION_VECTOR,
                    android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                        val rotationMatrix = FloatArray(9)
                        android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        publishCompassHeading(rotationMatrix, event.accuracy)
                    }
                    android.hardware.Sensor.TYPE_ACCELEROMETER -> {
                        gravityReading = event.values.copyOf(3)
                        val gravity = gravityReading
                        val magnetic = magneticReading
                        if (gravity != null && magnetic != null) {
                            val rotationMatrix = FloatArray(9)
                            if (android.hardware.SensorManager.getRotationMatrix(rotationMatrix, null, gravity, magnetic)) {
                                publishCompassHeading(rotationMatrix, _compassAccuracy.value)
                            }
                        }
                    }
                    android.hardware.Sensor.TYPE_MAGNETIC_FIELD -> {
                        magneticReading = event.values.copyOf(3)
                        _compassAccuracy.value = event.accuracy
                        val gravity = gravityReading
                        val magnetic = magneticReading
                        if (gravity != null && magnetic != null) {
                            val rotationMatrix = FloatArray(9)
                            if (android.hardware.SensorManager.getRotationMatrix(rotationMatrix, null, gravity, magnetic)) {
                                publishCompassHeading(rotationMatrix, event.accuracy)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                CrashReporter.report(e, "qiblaSensorChanged")
            }
        }
        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {
            if (sensor?.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR ||
                sensor?.type == android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR ||
                sensor?.type == android.hardware.Sensor.TYPE_MAGNETIC_FIELD
            ) {
                _compassAccuracy.value = accuracy
            }
        }
    }

    init {
        viewModelScope.launch {
            bookmarkDao.getAllBookmarks().collect { list ->
                _bookmarks.value = list
            }
        }
        viewModelScope.launch {
            translationManager.selectedTranslation.collect { key ->
                reloadSurahList(key)
            }
        }
        viewModelScope.launch {
            val key = translationManager.selectedTranslation.value
            if (key != "default" && !translationManager.isDownloaded(key)) {
                try {
                    translationManager.downloadTranslation(
                        key = key,
                        onProgress = {},
                        onSuccess = { reloadSurahList(key) },
                        onError = {}
                    )
                } catch (e: Exception) {
                    CrashReporter.report(e)
                }
            }
        }
        viewModelScope.launch {
            prayerSettingsController.selectedMadhab.collect {
                recalculatePrayers()
            }
        }
        viewModelScope.launch {
            prayerSettingsController.selectedMethod.collect {
                recalculatePrayers()
            }
        }
        startPrayerTimer()
        rotateDailyQuote()
        fetchDailyHadith()
        observeAudioPlayback()
        
        val calendar = java.util.Calendar.getInstance()
        fetchHijriCalendar(calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.YEAR))

        viewModelScope.launch {
            val todayStr = dateFormat.format(Date())
            val existingLog = prayerTrackerDao.getLogForDateSync(todayStr)
            if (existingLog == null) {
                val initial = PrayerLogEntity(todayStr)
                prayerTrackerDao.upsertPrayerLog(initial)
            }
            prayerTrackerDao.getLogForDate(todayStr).collect { log ->
                _todayPrayerLog.value = log
            }
        }
        viewModelScope.launch {
            val c = java.util.Calendar.getInstance()
            c.firstDayOfWeek = java.util.Calendar.MONDAY
            c.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            val startOfWeek = dateFormat.format(c.time)
            c.add(java.util.Calendar.DAY_OF_YEAR, 6)
            val endOfWeek = dateFormat.format(c.time)
            
            prayerTrackerDao.getLogsBetweenDates(startOfWeek, endOfWeek).collect { logs ->
                _weeklyPrayerLogs.value = logs
            }
        }
        viewModelScope.launch {
            val c = java.util.Calendar.getInstance()
            c.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = dateFormat.format(c.time)
            c.set(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            val endOfMonth = dateFormat.format(c.time)
            prayerTrackerDao.getLogsBetweenDates(startOfMonth, endOfMonth).collect { logs ->
                _currentMonthLogs.value = logs
            }
        }
        viewModelScope.launch {
            val currentCounts = prayerTrackerDao.getAllQazaCountsSync()
            if (currentCounts.isEmpty()) {
                val defaultCounts = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Witr").map {
                    QazaCountEntity(it, 0)
                }
                prayerTrackerDao.insertDefaultQazaCounts(defaultCounts)
            }
            prayerTrackerDao.getAllQazaCounts().collect { counts ->
                _qazaCounts.value = counts
            }
        }
    }

    fun toggleDailyPrayer(prayer: String) {
        viewModelScope.launch {
            val todayStr = dateFormat.format(Date())
            val log = prayerTrackerDao.getLogForDateSync(todayStr) ?: PrayerLogEntity(todayStr)
            val wasCompleted = when (prayer) {
                "Fajr" -> log.fajr
                "Dhuhr" -> log.dhuhr
                "Asr" -> log.asr
                "Maghrib" -> log.maghrib
                "Isha" -> log.isha
                else -> false
            }
            val updated = when (prayer) {
                "Fajr" -> log.copy(fajr = !log.fajr)
                "Dhuhr" -> log.copy(dhuhr = !log.dhuhr)
                "Asr" -> log.copy(asr = !log.asr)
                "Maghrib" -> log.copy(maghrib = !log.maghrib)
                "Isha" -> log.copy(isha = !log.isha)
                else -> log
            }
            prayerTrackerDao.upsertPrayerLog(updated)
            if (!wasCompleted) {
                awardPointsOnce("prayer_${todayStr}_$prayer", 5)
            }
        }
    }

    fun incrementQaza(prayer: String) {
        viewModelScope.launch {
            val current = _qazaCounts.value.find { it.prayerName == prayer } ?: return@launch
            prayerTrackerDao.updateQazaCount(prayer, current.totalRemaining + 1, current.totalCompleted)
        }
    }

    fun decrementQaza(prayer: String) {
        viewModelScope.launch {
            val current = _qazaCounts.value.find { it.prayerName == prayer } ?: return@launch
            if (current.totalRemaining > 0) {
                prayerTrackerDao.updateQazaCount(prayer, current.totalRemaining - 1, current.totalCompleted + 1)
            }
        }
    }

    fun setQazaCount(prayer: String, remaining: Int) {
        viewModelScope.launch {
            val current = _qazaCounts.value.find { it.prayerName == prayer }
            prayerTrackerDao.updateQazaCount(prayer, remaining, current?.totalCompleted ?: 0)
        }
    }

    fun reloadSurahList() {
        val key = translationManager.selectedTranslation.value
        reloadSurahList(key)
    }

    private fun reloadSurahList(translationKey: String) {
        viewModelScope.launch {
            try {
                val dbSurahs = repository.getSurahList(translationKey, translationManager)
                if (dbSurahs.isNotEmpty()) {
                    _surahs.value = dbSurahs
                    // Also refresh active structures for dynamic screen redraws
                    _selectedSurah.value?.let { current ->
                        dbSurahs.find { it.id == current.id }?.let { updated ->
                            val dbVerses = repository.getVersesForSurah(current.id)
                            val mappedVerses = displayVersesFor(current, dbVerses)
                            _selectedSurah.value = updated.copy(verses = mappedVerses)
                        }
                    }
                    _lastReadSurah.value?.let { current ->
                        dbSurahs.find { it.id == current.id }?.let { updated ->
                            _lastReadSurah.value = updated
                        }
                    }
                }
            } catch (e: Exception) {
                CrashReporter.report(e)
            }
        }
    }

    private var isLocationRefreshInFlight = false

    fun checkLocationAndRefresh(onResult: (Boolean) -> Unit) {
        if (isLocationRefreshInFlight) {
            onResult(false)
            return
        }
        val app = getApplication<Application>()
        if (!com.example.data.DeviceLocationProvider.hasPermission(app)) {
            _currentLocationName.value = "Location permission needed"
            onResult(false)
            return
        }
        isLocationRefreshInFlight = true
        val finish: (Boolean) -> Unit = { success ->
            isLocationRefreshInFlight = false
            onResult(success)
        }
        _currentLocationName.value = "Locating…"
        com.example.data.DeviceLocationProvider.getCurrentLocation(app) { result ->
            result.fold(
                onSuccess = { location -> processLocation(location, finish) },
                onFailure = { error ->
                    _currentLocationName.value = error.message ?: "Unable to get location"
                    finish(false)
                }
            )
        }
    }

    fun refreshLocationIfNeeded() {
        val app = getApplication<Application>()
        if (!com.example.data.DeviceLocationProvider.hasPermission(app)) {
            _currentLocationName.value = "Location permission needed"
            return
        }

        val lastRefresh = prefs.getLong("last_location_refresh_ms", 0L)
        val refreshIntervalMs = 30L * 60L * 1000L
        if (System.currentTimeMillis() - lastRefresh >= refreshIntervalMs) {
            checkLocationAndRefresh { }
        }
    }

    private fun processLocation(location: android.location.Location, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val currentLat = location.latitude
            val currentLng = location.longitude
            if (!currentLat.isFinite() || currentLat !in -90.0..90.0 ||
                !currentLng.isFinite() || currentLng !in -180.0..180.0
            ) {
                _currentLocationName.value = "Unable to get location"
                onResult(false)
                return@launch
            }
            updateMagneticDeclination(
                currentLat,
                currentLng,
                if (location.hasAltitude()) location.altitude.toFloat() else 0f
            )
            _qiblaBearing.value = qiblaTrueBearing(currentLat, currentLng)
            val qiblaDistance = FloatArray(1)
            android.location.Location.distanceBetween(
                currentLat,
                currentLng,
                21.422487,
                39.826206,
                qiblaDistance
            )
            _qiblaDistanceKm.value = qiblaDistance[0] / 1000f
            userPreferencesRepo.updateLocation(currentLat, currentLng)
            _hasCurrentLocation.value = true
            _currentLocationName.value = String.format(Locale.US, "%.4f, %.4f", currentLat, currentLng)
            prefs.edit().putLong("last_location_refresh_ms", System.currentTimeMillis()).apply()
            recalculatePrayers()
            onResult(true)
        }
    }

    private fun updateMagneticDeclination(lat: Double, lng: Double, altitudeMeters: Float = 0f) {
        magneticDeclinationDegrees = runCatching {
            android.hardware.GeomagneticField(
                lat.toFloat(),
                lng.toFloat(),
                altitudeMeters,
                System.currentTimeMillis()
            ).declination
        }.getOrDefault(0f)
        // Do not blend headings calculated against two different north references.
        smoothedTrueAzimuth = null
    }

    fun recalculatePrayers() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val app = getApplication<Application>()
            val contextWithAttribution = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                app.createAttributionContext("play-services-location")
            } else {
                app
            }
            val locationState = userPreferencesRepo.locationFlow.first()
            if (!locationState.isAvailable) {
                _hasCurrentLocation.value = false
                _currentLocationName.value = "Location needed"
                _prayers.value = IslamicData.defaultPrayers.map { it.copy(time = "--:--") }
                return@launch
            }
            val lat = locationState.latitude
            val lng = locationState.longitude

            _hasCurrentLocation.value = true
            _currentLocationName.value = String.format(Locale.US, "%.4f, %.4f", lat, lng)

            updateMagneticDeclination(lat, lng)
            _qiblaBearing.value = qiblaTrueBearing(lat, lng)
            val qiblaDistance = FloatArray(1)
            android.location.Location.distanceBetween(lat, lng, 21.422487, 39.826206, qiblaDistance)
            _qiblaDistanceKm.value = qiblaDistance[0] / 1000f

            try {
                val geocoder = android.location.Geocoder(contextWithAttribution, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: address.subAdminArea ?: address.adminArea.orEmpty()
                    val areaAndCity = listOf(address.subLocality.orEmpty(), city)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(", ")
                    val countryCode = address.countryCode ?: ""
                    if (areaAndCity.isNotEmpty()) {
                        _currentLocationName.value = if (countryCode.isNotEmpty()) {
                            "$areaAndCity, $countryCode"
                        } else {
                            areaAndCity
                        }
                    }
                }
            } catch (e: Exception) {
                // Geocoder fails without internet on some devices
            }

            val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val todayStr = dateFormatter.format(Date())

            try {
                
                val isHanafi = prayerSettingsController.selectedMadhab.value == Madhab.HANAFI
                val schoolStr = if (isHanafi) "1" else "0" 
                val method = when (prayerSettingsController.selectedMethod.value) {
                    CalculationMethod.KARACHI -> 1
                    CalculationMethod.MWL -> 3
                }
                // Let AlAdhan resolve the timezone from the current coordinates. A fixed
                // timezone (for example UTC) produces incorrect prayer times for most users.
                val url = okhttp3.HttpUrl.Builder()
                    .scheme("https")
                    .host("api.aladhan.com")
                    .addPathSegments("v1/timings")
                    .addPathSegment(todayStr)
                    .addQueryParameter("latitude", lat.toString())
                    .addQueryParameter("longitude", lng.toString())
                    .addQueryParameter("method", method.toString())
                    .addQueryParameter("shafaq", "general")
                    .addQueryParameter("school", schoolStr)
                    .addQueryParameter("midnightMode", "0")
                    .addQueryParameter("latitudeAdjustmentMethod", "1")
                    .addQueryParameter("calendarMethod", "UAQ")
                    .addQueryParameter("adjustment", _hijriAdjustment.value.toString())
                    .addQueryParameter("iso8601", "false")
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("Accept-Encoding", "")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = org.json.JSONObject(body)
                            val data = json.getJSONObject("data")
                            val timings = data.getJSONObject("timings")
                            
                            val dateObj = data.getJSONObject("date")
                            val hijriObj = dateObj.getJSONObject("hijri")
                            val gregorianObj = dateObj.getJSONObject("gregorian")

                            // Save to cache
                            prefs.edit().putString("cached_prayer_data_$todayStr", body).apply()
                            
                            val hDay = hijriObj.getString("day")
                            val hMonth = hijriObj.getJSONObject("month").getString("en")
                            val hYear = hijriObj.getString("year")
                            _todayHijri.value = "$hDay $hMonth $hYear AH"

                            val gDay = gregorianObj.getString("day")
                            val gMonth = gregorianObj.getJSONObject("month").getString("en")
                            val gYear = gregorianObj.getString("year")
                            val gWeekday = gregorianObj.getJSONObject("weekday").getString("en")
                            _todayGregorian.value = "$gWeekday, $gDay $gMonth $gYear"
                            
                            // Generate upcoming events dynamically based on current Hijri Year
                            val nextYear = (hYear.toIntOrNull() ?: 1445) + 1
                            val dynamicEvents = listOf(
                                com.example.data.HijriEvent("Day of Arafah", "9 Dhul-Hijjah $hYear", 0, com.example.ui.theme.MatteGold),
                                com.example.data.HijriEvent("Eid al-Adha", "10 Dhul-Hijjah $hYear", 1, com.example.ui.theme.FajrColor),
                                com.example.data.HijriEvent("Islamic New Year", "1 Muharram $nextYear", 20, com.example.ui.theme.SunriseColor),
                                com.example.data.HijriEvent("Ashura", "10 Muharram $nextYear", 29, com.example.ui.theme.MaghribColor),
                                com.example.data.HijriEvent("Mawlid al-Nabi", "12 Rabi' al-Awwal $nextYear", 90, com.example.ui.theme.IshaColor),
                                com.example.data.HijriEvent("Ramadan Begins", "1 Ramadan $nextYear", 260, com.example.ui.theme.GlowGold),
                                com.example.data.HijriEvent("Eid al-Fitr", "1 Shawwal $nextYear", 290, com.example.ui.theme.MatteGold)
                            )
                            _dynamicHijriEvents.value = dynamicEvents
                            
                            val parsedPrayers = listOf(
                                com.example.data.PrayerTime("Fajr", timings.getString("Fajr").take(5), com.example.ui.theme.FajrColor, prayerSettingsController.isAlarmEnabled("Fajr")),
                                PrayerTime("Sunrise", timings.getString("Sunrise").take(5), com.example.ui.theme.SunriseColor, prayerSettingsController.isAlarmEnabled("Sunrise")),
                                PrayerTime("Dhuhr", timings.getString("Dhuhr").take(5), com.example.ui.theme.DhuhrColor, prayerSettingsController.isAlarmEnabled("Dhuhr")),
                                PrayerTime("Asr", timings.getString("Asr").take(5), com.example.ui.theme.AsrColor, prayerSettingsController.isAlarmEnabled("Asr")),
                                PrayerTime("Maghrib", timings.getString("Maghrib").take(5), com.example.ui.theme.MaghribColor, prayerSettingsController.isAlarmEnabled("Maghrib")),
                                PrayerTime("Isha", timings.getString("Isha").take(5), com.example.ui.theme.IshaColor, prayerSettingsController.isAlarmEnabled("Isha"))
                            )
                            _prayers.value = parsedPrayers
                            prayerSettingsController.scheduleAlarms(parsedPrayers)
                            calculateNextPrayer()
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                CrashReporter.report(e)
            }
            
            // Fallback to local cache or calculation
            val cachedBody = prefs.getString("cached_prayer_data_$todayStr", null)
            if (cachedBody != null) {
                try {
                    val json = org.json.JSONObject(cachedBody)
                    val data = json.getJSONObject("data")
                    val timings = data.getJSONObject("timings")
                    
                    val dateObj = data.getJSONObject("date")
                    val hijriObj = dateObj.getJSONObject("hijri")
                    val gregorianObj = dateObj.getJSONObject("gregorian")

                    val hDay = hijriObj.getString("day")
                    val hMonth = hijriObj.getJSONObject("month").getString("en")
                    val hYear = hijriObj.getString("year")
                    _todayHijri.value = "$hDay $hMonth $hYear AH (Cached)"

                    val gDay = gregorianObj.getString("day")
                    val gMonth = gregorianObj.getJSONObject("month").getString("en")
                    val gYear = gregorianObj.getString("year")
                    val gWeekday = gregorianObj.getJSONObject("weekday").getString("en")
                    _todayGregorian.value = "$gWeekday, $gDay $gMonth $gYear"
                    
                    val parsedPrayers = listOf(
                        com.example.data.PrayerTime("Fajr", timings.getString("Fajr").take(5), com.example.ui.theme.FajrColor, prayerSettingsController.isAlarmEnabled("Fajr")),
                        com.example.data.PrayerTime("Sunrise", timings.getString("Sunrise").take(5), com.example.ui.theme.SunriseColor, prayerSettingsController.isAlarmEnabled("Sunrise")),
                        com.example.data.PrayerTime("Dhuhr", timings.getString("Dhuhr").take(5), com.example.ui.theme.DhuhrColor, prayerSettingsController.isAlarmEnabled("Dhuhr")),
                        com.example.data.PrayerTime("Asr", timings.getString("Asr").take(5), com.example.ui.theme.AsrColor, prayerSettingsController.isAlarmEnabled("Asr")),
                        com.example.data.PrayerTime("Maghrib", timings.getString("Maghrib").take(5), com.example.ui.theme.MaghribColor, prayerSettingsController.isAlarmEnabled("Maghrib")),
                        com.example.data.PrayerTime("Isha", timings.getString("Isha").take(5), com.example.ui.theme.IshaColor, prayerSettingsController.isAlarmEnabled("Isha"))
                    )
                    _prayers.value = parsedPrayers
                    prayerSettingsController.scheduleAlarms(parsedPrayers)
                    calculateNextPrayer()
                    return@launch
                } catch (e: Exception) {
                    CrashReporter.report(e)
                }
            }

            val calculated = prayerSettingsController.calculatePrayers(latitude = lat, longitude = lng)
            _prayers.value = calculated
            calculateNextPrayer()
        }
    }

    fun setMoreActiveTab(index: Int) {
        _moreActiveTab.value = index
    }

    fun selectReciter(reciter: Reciter) {
        _selectedReciter.value = reciter
        _audioProgress.value = 0f
        _audioPosition.value = "0:00"
        _currentAudioTrack.value = null
        
        // If currently playing, restart playback with new reciter
        if (_isAudioPlaying.value) {
            val surah = _playingSurah.value ?: _selectedSurah.value
            if (surah != null && surah.id > 0) {
                playAyahAudioFromUrl(surah.id, _playingAyahIndex.value)
            }
        }
    }

    fun playAudioTrack(track: com.example.data.AudioItem) {
        _currentAudioTrack.value = track
        _audioProgress.value = 0f
        _audioPosition.value = "0:00"
        _audioDuration.value = track.duration ?: "0:00"
        _playingAyahIndex.value = 0
        if (track.audioUrl.isBlank()) {
            _isAudioPlaying.value = false
            return
        }

        try {
            val oldPlayer = mediaPlayer
            mediaPlayer = null
            oldPlayer?.setOnPreparedListener(null)
            oldPlayer?.setOnCompletionListener(null)
            oldPlayer?.setOnErrorListener(null)
            oldPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(track.audioUrl)
                setOnErrorListener { _, _, _ ->
                    _isAudioPlaying.value = false
                    true
                }
                setOnPreparedListener {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            it.playbackParams = it.playbackParams.setSpeed(_audioPlaybackSpeed.value)
                        }
                    } catch (e: Exception) {
                        CrashReporter.report(e)
                    }
                    it.start()
                    _isAudioPlaying.value = true
                    val durationMs = it.duration
                    if (durationMs > 0) {
                        val mins = durationMs / 1000 / 60
                        val secs = (durationMs / 1000) % 60
                        _audioDuration.value = String.format("%d:%02d", mins, secs)
                    }
                }
                setOnCompletionListener {
                    _isAudioPlaying.value = false
                    _audioProgress.value = 0f
                    _audioPosition.value = "0:00"
                    if (_isRepeatEnabled.value) {
                        playAudioTrack(track)
                    }
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            CrashReporter.report(e)
            _isAudioPlaying.value = false
        }
    }

    fun toggleAudioPlayback() {
        try {
            if (_isAudioPlaying.value) {
                mediaPlayer?.pause()
                _isAudioPlaying.value = false
            } else {
                if (_playingAyahIndex.value == 0 && _audioProgress.value == 0f) {
                    selectAyahToPlay(0)
                } else {
                    mediaPlayer?.start()
                    _isAudioPlaying.value = true
                }
            }
        } catch (e: Exception) {
            CrashReporter.report(e)
            _isAudioPlaying.value = false
        }
    }

    fun dismissMiniAudioPlayer() {
        try {
            mediaPlayer?.setOnPreparedListener(null)
            mediaPlayer?.setOnCompletionListener(null)
            mediaPlayer?.setOnErrorListener(null)
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            CrashReporter.report(e)
        } finally {
            mediaPlayer = null
            _isAudioPlaying.value = false
            _playingSurah.value = null
            _currentAudioTrack.value = null
            _audioProgress.value = 0f
            _audioPosition.value = "0:00"
            _audioDuration.value = "0:00"
            _playingAyahIndex.value = 0
        }
    }

    private fun playAyahAudioFromUrl(surahId: Int, ayahIndex: Int) {
        val ayahNumberInSurah = ayahIndex + 1
        val surahStr = String.format("%03d", surahId)
        val ayahStr = String.format("%03d", ayahNumberInSurah)
        val prefix = _selectedReciter.value.prefixUrl
        val url = "${prefix}${surahStr}${ayahStr}.mp3"

        try {
            val oldPlayer = mediaPlayer
            mediaPlayer = null
            oldPlayer?.setOnPreparedListener(null)
            oldPlayer?.setOnCompletionListener(null)
            oldPlayer?.setOnErrorListener(null)
            oldPlayer?.release()
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnErrorListener { _, _, _ ->
                    _isAudioPlaying.value = false
                    true // Return true to indicate error was handled and prevent onCompletion from being called
                }
                setOnPreparedListener { 
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            it.playbackParams = it.playbackParams.setSpeed(_audioPlaybackSpeed.value)
                        }
                    } catch (e: Exception) {
                        CrashReporter.report(e)
                    }
                    it.start()
                    _isAudioPlaying.value = true
                    val durationMs = it.duration
                    val mins = durationMs / 1000 / 60
                    val secs = (durationMs / 1000) % 60
                    _audioDuration.value = String.format("%d:%02d", mins, secs)
                }
                setOnCompletionListener {
                    val surah = _playingSurah.value ?: _selectedSurah.value
                    if (surah != null) {
                        if (_isRepeatEnabled.value) {
                            playAyahAudioFromUrl(surah.id, ayahIndex)
                        } else if (_isShuffleEnabled.value && surah.verses.isNotEmpty()) {
                            val randomIndex = (0 until surah.verses.size).random()
                            selectAyahToPlay(randomIndex)
                        } else if (ayahIndex + 1 < surah.verses.size) {
                            selectAyahToPlay(ayahIndex + 1)
                        } else if (audioQueueActive()) {
                            // In a playlist: advance to the next track, or stop after the last one.
                            if (!advanceAudioQueue()) {
                                _isAudioPlaying.value = false
                                _audioProgress.value = 0f
                                _playingAyahIndex.value = 0
                            }
                        } else if (surah.id < 114) {
                            playNextSurah()
                        } else {
                            _isAudioPlaying.value = false
                            _audioProgress.value = 0f
                            _playingAyahIndex.value = 0
                        }
                    } else {
                        _isAudioPlaying.value = false
                        _audioProgress.value = 0f
                        _playingAyahIndex.value = 0
                    }
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            CrashReporter.report(e)
        }
    }

    /** Which curated audio playlist the playlist-detail screen should show (e.g. "morning"). */
    var audioPlaylistKey: String = ""
        private set

    fun openAudioPlaylist(key: String) {
        audioPlaylistKey = key
        navigateTo(DeenScreen.AUDIO_PLAYLIST)
    }

    // ---- Playlist queue: "Play all" (and tapping a track) autoplays the whole playlist in order,
    //      advancing to the next track automatically when the current one finishes. ----
    private var audioQueue: List<AudioQueueItem> = emptyList()
    private var audioQueueIndex: Int = -1

    private fun audioQueueActive(): Boolean = audioQueue.isNotEmpty()

    fun clearAudioQueue() {
        audioQueue = emptyList()
        audioQueueIndex = -1
    }

    /** Start a playlist from [startIndex] and keep playing track-by-track. */
    fun playQueue(items: List<AudioQueueItem>, startIndex: Int) {
        if (items.isEmpty()) return
        audioQueue = items
        audioQueueIndex = startIndex.coerceIn(0, items.size - 1)
        playCurrentQueueItem()
    }

    private fun playCurrentQueueItem() {
        val item = audioQueue.getOrNull(audioQueueIndex) ?: return
        if (item.ayah != null) playAyahTrackInternal(item.surahId, item.ayah, item.title, item.arabic)
        else playSurahNow(playbackSurah(item.surahId), triggerPlayback = true)
    }

    /** Advance to the next track; clears the queue and returns false when it was the last one. */
    private fun advanceAudioQueue(): Boolean {
        if (audioQueue.isEmpty()) return false
        if (audioQueueIndex + 1 < audioQueue.size) {
            audioQueueIndex++
            playCurrentQueueItem()
            return true
        }
        clearAudioQueue()
        return false
    }

    /**
     * Play one specific ayah (e.g. Ayatul Kursi = 2:255) through the main player and show it with a
     * friendly title in Now Playing. Stops cleanly at the end (no auto-advance through the surah),
     * so a single "track" in an adhkar playlist behaves like one Spotify track.
     */
    fun playAyahTrack(surahId: Int, ayahNumber: Int, englishName: String, arabicName: String) {
        clearAudioQueue()
        playAyahTrackInternal(surahId, ayahNumber, englishName, arabicName)
    }

    private fun playAyahTrackInternal(surahId: Int, ayahNumber: Int, englishName: String, arabicName: String) {
        val synthetic = Surah(
            id = surahId,
            nameEnglish = englishName,
            nameArabic = arabicName,
            englishTranslation = englishName,
            versesCount = 1,
            type = com.example.data.QuranMetaData.surahTypes.getOrElse(surahId - 1) { "Medinan" },
            verses = emptyList()
        )
        _selectedSurah.value = synthetic
        _playingSurah.value = synthetic
        _currentAudioTrack.value = null
        _audioProgress.value = 0f
        _audioPosition.value = "0:00"
        _playingAyahIndex.value = 0

        val url = "${_selectedReciter.value.prefixUrl}${String.format("%03d", surahId)}${String.format("%03d", ayahNumber)}.mp3"
        try {
            val old = mediaPlayer
            mediaPlayer = null
            old?.setOnPreparedListener(null)
            old?.setOnCompletionListener(null)
            old?.setOnErrorListener(null)
            old?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnErrorListener { _, _, _ -> _isAudioPlaying.value = false; true }
                setOnPreparedListener { mp ->
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            mp.playbackParams = mp.playbackParams.setSpeed(_audioPlaybackSpeed.value)
                        }
                    } catch (_: Exception) {}
                    mp.start()
                    _isAudioPlaying.value = true
                    val d = mp.duration
                    _audioDuration.value = String.format("%d:%02d", d / 1000 / 60, (d / 1000) % 60)
                }
                setOnCompletionListener {
                    // Continue the playlist if one is active; otherwise just stop.
                    if (!(audioQueueActive() && advanceAudioQueue())) {
                        _isAudioPlaying.value = false
                        _audioProgress.value = 1f
                    }
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            CrashReporter.report(e)
        }
    }

    fun selectAyahToPlay(index: Int) {
        // An explicit ayah tap belongs to the open reader. When the reader is
        // closed, controls continue using the independently retained playback Surah.
        val surah = _selectedSurah.value ?: _playingSurah.value ?: return
        _playingSurah.value = surah
        if (index in surah.verses.indices) {
            _playingAyahIndex.value = index
            val progressFactor = (index.toFloat() / surah.verses.size.toFloat()).coerceIn(0f, 1f)
            _audioProgress.value = progressFactor
            if (surah.id > 0) {
                playAyahAudioFromUrl(surah.id, index)
            }
        }
    }

    fun seekAudio(newProgress: Float) {
        _audioProgress.value = newProgress.coerceIn(0f, 1f)
        val surah = _playingSurah.value ?: _selectedSurah.value
        if (surah != null && surah.verses.isNotEmpty()) {
            val approxAyah = (newProgress * surah.verses.size).toInt().coerceIn(0, surah.verses.size - 1)
            if (approxAyah != _playingAyahIndex.value) {
                selectAyahToPlay(approxAyah)
            }
        }
    }

    fun seekAudioByMs(offsetMs: Int) {
        try {
            val player = mediaPlayer ?: return
            val currentPos = player.currentPosition
            val newPos = (currentPos + offsetMs).coerceIn(0, player.duration)
            player.seekTo(newPos)
        } catch (e: Exception) {
            CrashReporter.report(e)
        }
    }

    fun playNextSurah() {
        if (audioQueueActive()) { advanceAudioQueue(); return }
        val currentId = (_playingSurah.value ?: _selectedSurah.value)?.id ?: 1
        val nextId = if (_isShuffleEnabled.value) {
            (1..114).filter { it != currentId }.random()
        } else {
            if (currentId >= 114) 1 else currentId + 1
        }
        selectSurahForPlayback(playbackSurah(nextId), triggerPlayback = true)
    }

    fun playPreviousSurah() {
        if (audioQueueActive()) {
            if (audioQueueIndex > 0) { audioQueueIndex--; playCurrentQueueItem() }
            return
        }
        val currentId = (_playingSurah.value ?: _selectedSurah.value)?.id ?: 1
        val previousId = if (currentId <= 1) 114 else currentId - 1
        selectSurahForPlayback(playbackSurah(previousId), triggerPlayback = true)
    }

    private fun playbackSurah(id: Int): Surah {
        return IslamicData.surahs.find { it.id == id } ?: Surah(
            id = id,
            nameEnglish = com.example.data.QuranMetaData.surahNamesEn[id - 1],
            nameArabic = com.example.data.QuranMetaData.surahNamesAr[id - 1],
            englishTranslation = com.example.data.QuranMetaData.surahTranslations[id - 1],
            versesCount = IslamicData.surahAyahsCount[id] ?: 0,
            type = com.example.data.QuranMetaData.surahTypes[id - 1],
            verses = emptyList()
        )
    }

    fun setAudioPlaybackSpeed(speed: Float) {
        _audioPlaybackSpeed.value = speed
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = player.playbackParams.setSpeed(speed)
                    }
                }
            }
        } catch (e: Exception) {
            CrashReporter.report(e)
        }
    }

    fun toggleRepeatMode() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
    }

    fun toggleShuffleMode() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    private fun observeAudioPlayback() {
        viewModelScope.launch {
            while (true) {
                try {
                    if (_isAudioPlaying.value && mediaPlayer?.isPlaying == true) {
                        val currentPos = mediaPlayer?.currentPosition ?: 0
                        val totalDur = mediaPlayer?.duration ?: 1
                        
                        val surah = _playingSurah.value ?: _selectedSurah.value
                        if (_currentAudioTrack.value != null && totalDur > 0) {
                            _audioProgress.value = (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)
                            val currentSeconds = currentPos / 1000
                            _audioPosition.value = String.format("%d:%02d", currentSeconds / 60, currentSeconds % 60)
                            val totalSeconds = totalDur / 1000
                            _audioDuration.value = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)
                        } else if (surah != null && surah.verses.isNotEmpty() && totalDur > 0) {
                            val currentAyahIndex = _playingAyahIndex.value
                            val intraAyahProgress = if (totalDur > 0) {
                            (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                            val totalProgress = (currentAyahIndex + intraAyahProgress) / surah.verses.size
                            _audioProgress.value = totalProgress.coerceIn(0f, 1f)
                            
                            // Fake total duration for display: total ayahs * 10 seconds approx
                            val totalEstimatedSeconds = surah.verses.size * 10
                            val currentSeconds = (totalProgress * totalEstimatedSeconds).toInt()
                            val mins = currentSeconds / 60
                            val secs = currentSeconds % 60
                            _audioPosition.value = String.format("%d:%02d", mins, secs)
                            
                            val durMins = totalEstimatedSeconds / 60
                            val durSecs = totalEstimatedSeconds % 60
                            _audioDuration.value = String.format("%d:%02d", durMins, durSecs)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore IllegalStateException if media player is not ready
                }
                delay(500)
            }
        }
    }

    fun incrementAzkarCount(azkarId: Int) {
        val currentMap = _azkarCounts.value.toMutableMap()
        val target = IslamicData.azkarList.find { it.id == azkarId }?.targetCount ?: 1
        val currentVal = currentMap[azkarId] ?: 0
        if (currentVal < target) {
            currentMap[azkarId] = currentVal + 1
            if (currentVal + 1 == target) {
                awardPointsOnce("azkar_complete_$azkarId", 10)
            }
        } else {
            // Cycle back to 0 if tapped after completing
            currentMap[azkarId] = 0
        }
        _azkarCounts.value = currentMap
    }

    fun resetAzkarCount(azkarId: Int) {
        val currentMap = _azkarCounts.value.toMutableMap()
        currentMap[azkarId] = 0
        _azkarCounts.value = currentMap
    }

    fun selectDuaCategory(category: DuaCategory) {
        _selectedDuaCategory.value = category
        navigateTo(DeenScreen.DUA_DETAILS)
    }

    var currentQuizCategory: com.example.data.QuizCategory? by mutableStateOf(null)
    
    fun addQuizPoints(points: Int) {
        addPoints(points)
    }

    // Gamification & Learning Module
    val learningGamificationPoints: StateFlow<Int> = totalPoints

    private val _learningSurahs = MutableStateFlow<List<QuranLearningSurah>>(emptyList())
    val learningSurahs: StateFlow<List<QuranLearningSurah>> = _learningSurahs.asStateFlow()

    private val _isLearningSurahsLoading = MutableStateFlow(false)
    val isLearningSurahsLoading: StateFlow<Boolean> = _isLearningSurahsLoading.asStateFlow()

    private val _learningSelectedSurah = MutableStateFlow<QuranLearningSurah?>(null)
    val learningSelectedSurah: StateFlow<QuranLearningSurah?> = _learningSelectedSurah.asStateFlow()

    private val _learningAyahs = MutableStateFlow<List<QuranLearningAyah>>(emptyList())
    val learningAyahs: StateFlow<List<QuranLearningAyah>> = _learningAyahs.asStateFlow()

    private val _isLearningAyahsLoading = MutableStateFlow(false)
    val isLearningAyahsLoading: StateFlow<Boolean> = _isLearningAyahsLoading.asStateFlow()

    fun loadLearningSurahs() {
        if (_learningSurahs.value.isNotEmpty()) return
        viewModelScope.launch {
            _isLearningSurahsLoading.value = true
            _learningSurahs.value = QuranLearningApiService.fetchSurahList()
            _isLearningSurahsLoading.value = false
        }
    }

    fun openLearningSurah(surah: QuranLearningSurah) {
        _learningSelectedSurah.value = surah
        _learningAyahs.value = emptyList()
        navigateTo(DeenScreen.QURAN_LEARNING_READER)
        
        viewModelScope.launch {
            _isLearningAyahsLoading.value = true
            _learningAyahs.value = QuranLearningApiService.fetchSurahDetails(surah.number)
            _isLearningAyahsLoading.value = false
        }
    }
    
    fun addGamificationPoints(points: Int) {
        addPoints(points)
    }
    
    // Recreate on each play so release() in onCleared does not leave a dead instance.
    private var learningMediaPlayer: android.media.MediaPlayer? = null

    fun playAyahAudio(url: String?) {
        if (url == null) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val player = learningMediaPlayer ?: android.media.MediaPlayer().also { learningMediaPlayer = it }
                player.reset()
                player.setDataSource(url)
                player.prepare()
                player.start()
            } catch (e: Exception) {
                CrashReporter.report(e)
            }
        }
    }
    
    fun releaseLearningMediaPlayer() {
        try {
            learningMediaPlayer?.release()
        } catch (_: Exception) { }
        learningMediaPlayer = null
    }

    fun startQuiz(category: com.example.data.QuizCategory) {
        currentQuizCategory = category
        navigateTo(DeenScreen.ACTIVE_QUIZ)
    }

    fun navigateTo(screen: DeenScreen) {
        val current = _currentScreen.value
        if (screen == current) return
        if (screen == DeenScreen.DASHBOARD || screen == DeenScreen.LOGIN) {
            backStack.clear()            // roots — nothing to return to
        } else {
            backStack.add(current)
        }
        _currentScreen.value = screen
    }

    /**
     * Pop to the previous screen. Returns true if a back navigation happened,
     * false if the stack was empty (caller should let the system handle it).
     */
    fun goBack(): Boolean {
        if (backStack.isEmpty()) return false
        _currentScreen.value = backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun downloadAllQuranData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                var allDownloaded = true
                for (juzId in 1..30) {
                    val expectedCount = com.example.data.IslamicData.juzAyahsCount[juzId] ?: 0
                    var dbVerses = repository.getVersesForJuz(juzId)
                    if (dbVerses.size < expectedCount) {
                        try {
                           if (!fetchAndCacheJuzFromApi(juzId)) {
                               allDownloaded = false
                           }
                        } catch (e: Exception) {
                            allDownloaded = false
                        }
                        dbVerses = repository.getVersesForJuz(juzId)
                        if (dbVerses.size < expectedCount) allDownloaded = false
                    }
                }
                onResult(allDownloaded)
            } catch (e: Exception) {
                CrashReporter.report(e)
                onResult(false)
            }
        }
    }

    private suspend fun fetchAndCacheSurahFromApi(surahId: Int): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val url = "https://api.alquran.cloud/v1/surah/$surahId/editions/quran-uthmani,ur.kanzuliman,en.ahmedraza"
        val request = okhttp3.Request.Builder().url(url).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body?.string() ?: return@withContext false
                val json = org.json.JSONObject(body)
                val dataArray = json.getJSONArray("data")
                
                val arabicEdition = dataArray.getJSONObject(0)
                val urduEdition = dataArray.getJSONObject(1)
                val englishEdition = dataArray.getJSONObject(2)
                
                val arabicAyahs = arabicEdition.getJSONArray("ayahs")
                val urduAyahs = urduEdition.getJSONArray("ayahs")
                val englishAyahs = englishEdition.getJSONArray("ayahs")
                
                val soraNameEn = arabicEdition.getString("englishName")
                val soraNameAr = arabicEdition.getString("name")
                
                val versesList = mutableListOf<QuranVerse>()
                val count = minOf(arabicAyahs.length(), urduAyahs.length(), englishAyahs.length())
                
                for (i in 0 until count) {
                    val arAyah = arabicAyahs.getJSONObject(i)
                    val urAyah = urduAyahs.getJSONObject(i)
                    val enAyah = englishAyahs.getJSONObject(i)
                    
                    val ayaNo = arAyah.getInt("numberInSurah")
                    val jozz = arAyah.getInt("juz")
                    val page = arAyah.getInt("page")
                    
                    val arabicText = cleanAyaArabicText(arAyah.getString("text"), surahId, ayaNo)
                    // Combine Urdu and English or put Urdu in tafseerMoysar, wait where does English go?
                    val englishTranslation = unescapeHtml(enAyah.getString("text"))
                    val urduTranslation = unescapeHtml(urAyah.getString("text"))
                    
                    val verseId = (surahId * 1000) + ayaNo
                    
                    val verse = QuranVerse(
                        id = verseId,
                        jozz = jozz,
                        sora = surahId,
                        soraNameEn = soraNameEn,
                        soraNameAr = soraNameAr,
                        page = page,
                        lineStart = 1,
                        lineEnd = 1,
                        ayaNo = ayaNo,
                        ayaText = arabicText,
                        ayaTextEmlaey = arabicText,
                        maanyAya = "معاني الكلمات",
                        earabQuran = "إعراب الآية",
                        reasonsOfVerses = "سبب النزول",
                        tafseerSaadi = urduTranslation,
                        tafseerMoysar = englishTranslation,
                        tafseerBughiu = "تفسير البغوي",
                        ayaTextTashkil = arabicText
                    )
                    versesList.add(verse)
                }
                
                if (versesList.isNotEmpty()) {
                    repository.deleteVersesForSurah(surahId)
                    repository.insertVerses(versesList)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            CrashReporter.report(e)
        }
        false
    }

    private suspend fun fetchAndCacheJuzFromApi(juzId: Int): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val arUrl = "https://api.alquran.cloud/v1/juz/$juzId/quran-uthmani"
        val urUrl = "https://api.alquran.cloud/v1/juz/$juzId/ur.kanzuliman"
        val enUrl = "https://api.alquran.cloud/v1/juz/$juzId/en.ahmedraza"
        
        var arBody: String? = null
        var urBody: String? = null
        var enBody: String? = null
        
        try {
            httpClient.newCall(okhttp3.Request.Builder().url(arUrl).build()).execute().use { response ->
                if (response.isSuccessful) arBody = response.body?.string()
            }
            httpClient.newCall(okhttp3.Request.Builder().url(urUrl).build()).execute().use { response ->
                if (response.isSuccessful) urBody = response.body?.string()
            }
            httpClient.newCall(okhttp3.Request.Builder().url(enUrl).build()).execute().use { response ->
                if (response.isSuccessful) enBody = response.body?.string()
            }
            
            if (arBody != null && urBody != null && enBody != null) {
                val arJson = org.json.JSONObject(arBody)
                val urJson = org.json.JSONObject(urBody)
                val enJson = org.json.JSONObject(enBody)
                
                val arData = arJson.getJSONObject("data")
                val urData = urJson.getJSONObject("data")
                val enData = enJson.getJSONObject("data")
                
                val arabicAyahs = arData.getJSONArray("ayahs")
                val urduAyahs = urData.getJSONArray("ayahs")
                val englishAyahs = enData.getJSONArray("ayahs")
                
                val versesList = mutableListOf<QuranVerse>()
                val count = minOf(arabicAyahs.length(), urduAyahs.length(), englishAyahs.length())
                
                for (i in 0 until count) {
                    val arAyah = arabicAyahs.getJSONObject(i)
                    val urAyah = urduAyahs.getJSONObject(i)
                    val enAyah = englishAyahs.getJSONObject(i)
                    
                    val ayaNo = arAyah.getInt("numberInSurah")
                    val surahId = arAyah.getJSONObject("surah").getInt("number")
                    val soraNameEn = arAyah.getJSONObject("surah").getString("englishName")
                    val soraNameAr = arAyah.getJSONObject("surah").getString("name")
                    val page = arAyah.getInt("page")
                    
                    val arabicText = cleanAyaArabicText(arAyah.getString("text"), surahId, ayaNo)
                    val urduTranslation = unescapeHtml(urAyah.getString("text"))
                    val englishTranslation = unescapeHtml(enAyah.getString("text"))
                    
                    val verseId = (surahId * 1000) + ayaNo
                    
                    val verse = QuranVerse(
                        id = verseId,
                        jozz = juzId,
                        sora = surahId,
                        soraNameEn = soraNameEn,
                        soraNameAr = soraNameAr,
                        page = page,
                        lineStart = 1,
                        lineEnd = 1,
                        ayaNo = ayaNo,
                        ayaText = arabicText,
                        ayaTextEmlaey = arabicText,
                        maanyAya = "معاني الكلمات",
                        earabQuran = "إعراب الآية",
                        reasonsOfVerses = "سبب النزول",
                        tafseerSaadi = urduTranslation,
                        tafseerMoysar = englishTranslation,
                        tafseerBughiu = "تفسير البغوي",
                        ayaTextTashkil = arabicText
                    )
                    versesList.add(verse)
                }
                
                if (versesList.isNotEmpty()) {
                    repository.deleteVersesForJuz(juzId)
                    repository.insertVerses(versesList)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            CrashReporter.report(e)
        }
        false
    }

    private fun fallbackVerseEntitiesForSurah(surah: Surah): List<QuranVerse> {
        if (surah.id !in 1..114) return emptyList()
        val fallbackSurah = IslamicData.surahs.find { it.id == surah.id } ?: return emptyList()
        if (fallbackSurah.verses.isEmpty()) return emptyList()
        val englishName = QuranMetaData.surahNamesEn.getOrNull(surah.id - 1) ?: surah.nameEnglish
        val arabicName = QuranMetaData.surahNamesAr.getOrNull(surah.id - 1) ?: surah.nameArabic
        return fallbackSurah.verses.mapIndexed { index, pair ->
            val ayahNo = index + 1
            val arabic = repairMojibake(pair.first)
            val english = repairMojibake(pair.second)
            QuranVerse(
                id = (surah.id * 1000) + ayahNo,
                jozz = 0,
                sora = surah.id,
                soraNameEn = englishName,
                soraNameAr = repairMojibake(arabicName),
                page = 0,
                lineStart = 1,
                lineEnd = 1,
                ayaNo = ayahNo,
                ayaText = arabic,
                ayaTextEmlaey = arabic,
                maanyAya = "",
                earabQuran = "",
                reasonsOfVerses = "",
                tafseerSaadi = "",
                tafseerMoysar = english,
                tafseerBughiu = "",
                ayaTextTashkil = arabic
            )
        }
    }

    private suspend fun displayVersesFor(
        surah: Surah,
        dbVerses: List<QuranVerse>
    ): List<Pair<String, String>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val translationKey = translationManager.selectedTranslation.value
        val existingSurah = IslamicData.surahs.find { it.id == surah.id }
        val indoPakAyahs = repository.getIndoPakAyahsForSurah(surah.id)
        dbVerses.mapIndexed { index, verse ->
            val arabic = indoPakAyahs[verse.ayaNo]
                ?: repairMojibake(cleanAyaArabicText(verse.ayaTextTashkil, verse.sora, verse.ayaNo))
            val rawEnglish = if (translationKey == "default") {
                existingSurah?.verses?.getOrNull(index)?.second ?: verse.tafseerMoysar
            } else {
                translationManager.getTranslationText(translationKey, verse.sora, verse.ayaNo)
                    ?: (existingSurah?.verses?.getOrNull(index)?.second ?: verse.tafseerMoysar)
            }
            arabic to repairMojibake(unescapeHtml(rawEnglish))
        }
    }

    private suspend fun displayVersesForJuz(
        dbVerses: List<QuranVerse>
    ): List<Pair<String, String>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val translationKey = translationManager.selectedTranslation.value
        val indoPakAyahs = repository.getIndoPakAyahsForVerses(dbVerses)
        dbVerses.map { verse ->
            val arabic = indoPakAyahs[verse.sora to verse.ayaNo]
                ?: repairMojibake(cleanAyaArabicText(verse.ayaTextTashkil, verse.sora, verse.ayaNo))
            val rawEnglish = if (translationKey == "default") {
                verse.tafseerMoysar
            } else {
                translationManager.getTranslationText(translationKey, verse.sora, verse.ayaNo)
                    ?: verse.tafseerMoysar
            }
            arabic to repairMojibake(unescapeHtml(rawEnglish))
        }
    }

    fun selectSurahForPlayback(surah: Surah, triggerPlayback: Boolean = true) {
        // A direct surah/reciter/featured tap is not a playlist — drop any active queue.
        clearAudioQueue()
        playSurahNow(surah, triggerPlayback)
    }

    private fun playSurahNow(surah: Surah, triggerPlayback: Boolean = true) {
        _selectedSurah.value = surah
        _playingSurah.value = surah
        _currentAudioTrack.value = null
        _lastReadSurah.value = surah
        _lastReadAyah.value = 1
        pushRecentQuranRead(surah, 1)
        viewModelScope.launch {
            _isQuranLoading.value = true
            _quranError.value = null
            try {
                val expectedCount = com.example.data.IslamicData.surahAyahsCount[surah.id] ?: surah.versesCount
                var dbVerses = repository.getVersesForSurah(surah.id)
                if (dbVerses.size < expectedCount) {
                    try {
                        fetchAndCacheSurahFromApi(surah.id)
                        dbVerses = repository.getVersesForSurah(surah.id)
                    } catch (e: Exception) {
                        CrashReporter.report(e)
                    }
                }
                if (dbVerses.isEmpty()) {
                    dbVerses = fallbackVerseEntitiesForSurah(surah)
                }
                
                _activeDbVerses.value = dbVerses
                
                val mappedVerses = displayVersesFor(surah, dbVerses)
                val updatedSurah = surah.copy(
                    versesCount = dbVerses.size.takeIf { it > 0 } ?: expectedCount,
                    verses = mappedVerses
                )
                _selectedSurah.value = updatedSurah
                _playingSurah.value = updatedSurah
                _audioProgress.value = 0f
                _audioPosition.value = "0:00"
                _playingAyahIndex.value = 0
                if (triggerPlayback) {
                    playAyahAudioFromUrl(updatedSurah.id, 0)
                }
            } catch (e: Exception) {
                _activeDbVerses.value = emptyList()
                _quranError.value = "Unable to load this chapter. Please try again."
            } finally {
                _isQuranLoading.value = false
            }
        }
    }

    fun selectSurah(surah: Surah) {
        _selectedSurah.value = surah
        _lastReadSurah.value = surah
        _lastReadAyah.value = 1
        pushRecentQuranRead(surah, 1)
        navigateTo(DeenScreen.QURAN)
        viewModelScope.launch {
            _isQuranLoading.value = true
            _quranError.value = null
            try {
                val expectedCount = com.example.data.IslamicData.surahAyahsCount[surah.id] ?: surah.versesCount
                var dbVerses = repository.getVersesForSurah(surah.id)
                if (dbVerses.size < expectedCount) {
                    try {
                        fetchAndCacheSurahFromApi(surah.id)
                        dbVerses = repository.getVersesForSurah(surah.id)
                    } catch (e: Exception) {
                        CrashReporter.report(e)
                    }
                }
                if (dbVerses.isEmpty()) {
                    dbVerses = fallbackVerseEntitiesForSurah(surah)
                }
                if (dbVerses.isEmpty()) {
                    throw java.io.IOException("No verses are available for this chapter.")
                }
                
                _activeDbVerses.value = dbVerses
                
                val mappedVerses = displayVersesFor(surah, dbVerses)
                _selectedSurah.value = surah.copy(
                    versesCount = dbVerses.size.takeIf { it > 0 } ?: expectedCount,
                    verses = mappedVerses
                )
            } catch (e: Exception) {
                _activeDbVerses.value = emptyList()
                _quranError.value = if (_isOfflineMode.value) {
                    "This chapter is not cached yet. Connect to the internet and try again."
                } else {
                    "Unable to load this chapter. Please try again."
                }
            } finally {
                _isQuranLoading.value = false
            }
        }
    }

    fun closeSelectedSurah() {
        _selectedSurah.value = null
        _activeDbVerses.value = emptyList()
        _quranError.value = null
        _currentScreen.value = DeenScreen.QURAN
    }

    fun retryQuranContent() {
        val selected = _selectedSurah.value ?: return
        if (selected.id > 0) {
            selectSurah(selected)
        } else {
            selectJuz(-selected.id)
        }
    }

    fun selectJuz(juzId: Int) {
        navigateTo(DeenScreen.QURAN)
        viewModelScope.launch {
            _isQuranLoading.value = true
            _quranError.value = null
            try {
                var dbVerses = repository.getVersesForJuz(juzId)
                
                // Juz has minimum ~110 verses, fetch if size doesn't match
                val expectedCount = com.example.data.IslamicData.juzAyahsCount[juzId] ?: 0
                if (dbVerses.size < expectedCount) { 
                    try {
                        fetchAndCacheJuzFromApi(juzId)
                        dbVerses = repository.getVersesForJuz(juzId)
                    } catch (e: Exception) {
                        CrashReporter.report(e)
                    }
                }
                if (dbVerses.isEmpty()) {
                    throw java.io.IOException("No verses are available for this Juz.")
                }
                
                val dummySurah = Surah(
                    id = -juzId,
                    nameEnglish = "Juz $juzId",
                    nameArabic = "الجزء $juzId",
                    englishTranslation = "Part $juzId",
                    versesCount = dbVerses.size,
                    type = "Juz",
                    verses = emptyList()
                )
                _selectedSurah.value = dummySurah
                _lastReadSurah.value = dummySurah
                _lastReadAyah.value = 1
                pushRecentQuranRead(dummySurah, 1)
                
                _activeDbVerses.value = dbVerses
                
                val mappedVerses = displayVersesForJuz(dbVerses)
                _selectedSurah.value = dummySurah.copy(
                    versesCount = dbVerses.size,
                    verses = mappedVerses
                )
            } catch (e: Exception) {
                _activeDbVerses.value = emptyList()
                _quranError.value = if (_isOfflineMode.value) {
                    "This Juz is not cached yet. Connect to the internet and try again."
                } else {
                    "Unable to load this Juz. Please try again."
                }
            } finally {
                _isQuranLoading.value = false
            }
        }
    }

    fun resumeReading(surah: Surah, ayah: Int) {
        if (surah.id < 0) {
            selectJuz(-surah.id)
            _lastReadAyah.value = ayah
            pushRecentQuranRead(surah, ayah)
        } else {
            selectSurah(surah)
            _lastReadAyah.value = ayah
            pushRecentQuranRead(surah, ayah)
        }
    }

    fun updateLastRead(surah: Surah, ayah: Int) {
        _lastReadSurah.value = surah
        _lastReadAyah.value = ayah
        pushRecentQuranRead(surah, ayah)
    }

    private fun pushRecentQuranRead(surah: Surah, ayah: Int) {
        val cleanSurah = surah.copy(verses = emptyList())
        val cleanAyah = ayah.coerceAtLeast(1)
        _recentQuranReads.value = (listOf(cleanSurah to cleanAyah) +
            _recentQuranReads.value.filterNot { it.first.id == cleanSurah.id })
            .take(3)
    }

    fun toggleBookmark(surah: Surah, ayahNumber: Int, ayahText: String) {
        viewModelScope.launch {
            val existing = bookmarkDao.getBookmark(surah.id, ayahNumber)
            if (existing != null) {
                bookmarkDao.deleteBookmark(surah.id, ayahNumber)
            } else {
                bookmarkDao.insertBookmark(
                    com.example.data.BookmarkEntity(
                        surahId = surah.id,
                        surahNameEng = surah.nameEnglish,
                        surahNameAr = surah.nameArabic,
                        ayahNumber = ayahNumber,
                        ayahText = ayahText
                    )
                )
                awardPointsOnce("bookmark_${surah.id}_$ayahNumber", 2)
            }
        }
    }

    fun toggleNotification(prayerName: String) {
        val current = _prayers.value.find { it.name == prayerName }
        if (current != null) {
            val newEnabled = !current.isNotificationEnabled
            prayerSettingsController.setAlarmEnabled(prayerName, newEnabled)
            recalculatePrayers()
        }
    }

    private fun fetchDailyHadith() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val todayStr = dateFormat.format(Date())
            
            // Try to load cached hadith first
            val cachedJson = prefs.getString("daily_hadith_cached_$todayStr", null)
            if (cachedJson != null) {
                try {
                    val jObj = org.json.JSONObject(cachedJson)
                    val hadith = com.example.data.Hadith(
                        id = jObj.getInt("id"),
                        text = jObj.getString("text"),
                        chapterNumber = jObj.getInt("chapterNumber"),
                        chapterTitle = jObj.getString("chapterTitle"),
                        bookCode = jObj.getString("bookCode")
                    )
                    _dailyHadith.value = hadith
                    return@launch
                } catch(e: Exception) { CrashReporter.report(e) }
            }

            _isDailyHadithLoading.value = true
            try {
                // Fetch english bukhari first chapter just to get a valid authentic hadith. 
                // There are 97 sections in bukhari. We pseudo-randomly pick one.
                val sectionIds = listOf(2, 3, 4, 8, 9, 10, 11, 24, 31, 38, 43, 54, 55, 56, 73, 76, 78, 81) 
                val sectionId = sectionIds[dayOfYear % sectionIds.size]
                
                val url = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari/sections/$sectionId.json"
                val request = okhttp3.Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = org.json.JSONObject(body)
                        val hadithsArray = json.getJSONArray("hadiths")
                        
                        if (hadithsArray.length() > 0) {
                            val hadithIndex = dayOfYear % hadithsArray.length()
                            val hObj = hadithsArray.getJSONObject(hadithIndex)
                            
                            val text = hObj.getString("text")
                            val hadithNo = hObj.getInt("hadithnumber")
                            // the title might not be directly inside the hadith object, but available in metadata if we fetch metadata. 
                            // Since it's from Bukhari, we just state it is.
                            
                            val chapterNameObj = json.optJSONObject("metadata")?.optJSONObject("section")?.optString(sectionId.toString()) ?: "Sahih Bukhari"
                            
                            val hadith = com.example.data.Hadith(
                                id = hadithNo,
                                text = text,
                                chapterNumber = sectionId,
                                chapterTitle = chapterNameObj,
                                bookCode = "bukhari"
                            )
                            _dailyHadith.value = hadith
                            
                            val cacheObj = org.json.JSONObject()
                            cacheObj.put("id", hadith.id)
                            cacheObj.put("text", hadith.text)
                            cacheObj.put("chapterNumber", hadith.chapterNumber)
                            cacheObj.put("chapterTitle", hadith.chapterTitle)
                            cacheObj.put("bookCode", hadith.bookCode)
                            prefs.edit().putString("daily_hadith_cached_$todayStr", cacheObj.toString()).apply()
                        }
                    }
                }
            } catch (e: Exception) {
                CrashReporter.report(e)
            } finally {
                _isDailyHadithLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun rotateDailyQuote() {
        val calendar = Calendar.getInstance()
        val index = calendar.get(Calendar.DAY_OF_YEAR) % IslamicData.quranQuotes.size
        _dailyQuote.value = IslamicData.quranQuotes[index]
    }

    private fun startPrayerTimer() {
        viewModelScope.launch {
            while (true) {
                calculateNextPrayer()
                delay(1000) // Update every second
            }
        }
    }

    fun startCompassListening() {
        try {
            _hasCompassReading.value = false
            smoothedTrueAzimuth = null
            val manager = sensorManager
            val registered = when {
                manager == null -> false
                rotationVectorSensor != null -> manager.registerListener(
                    sensorEventListener,
                    rotationVectorSensor,
                    android.hardware.SensorManager.SENSOR_DELAY_UI
                )
                accelerometerSensor != null && magneticFieldSensor != null -> {
                    val accelerometerRegistered = manager.registerListener(
                        sensorEventListener,
                        accelerometerSensor,
                        android.hardware.SensorManager.SENSOR_DELAY_UI
                    )
                    val magneticRegistered = manager.registerListener(
                        sensorEventListener,
                        magneticFieldSensor,
                        android.hardware.SensorManager.SENSOR_DELAY_UI
                    )
                    accelerometerRegistered && magneticRegistered
                }
                else -> false
            }
            _hasCompassSensor.value = registered
        } catch (e: Exception) {
            _hasCompassSensor.value = false
            CrashReporter.report(e, "startCompassListening")
        }
    }

    fun stopCompassListening() {
        try {
            sensorManager?.unregisterListener(sensorEventListener)
            gravityReading = null
            magneticReading = null
            smoothedTrueAzimuth = null
            _hasCompassReading.value = false
        } catch (e: Exception) {
            CrashReporter.report(e, "stopCompassListening")
        }
    }

    private fun calculateNextPrayer() {
        val prayerList = _prayers.value
        if (prayerList.isEmpty()) {
            _activePrayerIndex.value = -1
            _nextPrayerName.value = "Fajr"
            _nextPrayerCountdown.value = "--"
            return
        }

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMin = now.get(Calendar.MINUTE)
        val currentSec = now.get(Calendar.SECOND)
        val currentTotalSeconds = currentHour * 3600 + currentMin * 60 + currentSec

        // Only the five daily salah — never countdown to Sunrise.
        val salahEntries = prayerList.mapIndexedNotNull { index, prayer ->
            if (prayer.name.equals("Sunrise", ignoreCase = true)) return@mapIndexedNotNull null
            try {
                val parts = prayer.time.split(":")
                if (parts.size >= 2) {
                    val hour = parts[0].toIntOrNull() ?: return@mapIndexedNotNull null
                    val min = parts[1].toIntOrNull() ?: return@mapIndexedNotNull null
                    Triple(index, prayer, hour * 3600 + min * 60)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

        if (salahEntries.isEmpty()) {
            _activePrayerIndex.value = -1
            _nextPrayerName.value = "Fajr"
            _nextPrayerCountdown.value = "--:--:--"
            return
        }

        var minDiff = Int.MAX_VALUE
        var nextEntry = salahEntries.first()
        var foundUpcoming = false

        for (entry in salahEntries) {
            val diff = entry.third - currentTotalSeconds
            if (diff > 0 && diff < minDiff) {
                minDiff = diff
                nextEntry = entry
                foundUpcoming = true
            }
        }

        if (!foundUpcoming) {
            // All salah passed — next is tomorrow's Fajr (first salah entry).
            nextEntry = salahEntries.first()
            minDiff = (24 * 3600 - currentTotalSeconds) + nextEntry.third
            _activePrayerIndex.value = salahEntries.last().first
        } else {
            val nextSalahPos = salahEntries.indexOfFirst { it.first == nextEntry.first }
            _activePrayerIndex.value = if (nextSalahPos <= 0) {
                salahEntries.last().first
            } else {
                salahEntries[nextSalahPos - 1].first
            }
        }

        _nextPrayerName.value = nextEntry.second.name

        val hours = minDiff / 3600
        val rem = minDiff % 3600
        val mins = rem / 60
        val secs = rem % 60
        _nextPrayerCountdown.value = String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    }

    private fun cleanAyaArabicText(text: String, surahId: Int, ayahNo: Int): String {
        val withoutBom = text.replace("\uFEFF", "").trim()
        if (surahId == 1 || surahId == 9 || ayahNo != 1) {
            return withoutBom
        }
        
        // Build a normalized version checking if it starts with Bismillah
        val bismillahNormalized = "بسماللهالرحمنالرحيم"
        
        val indexMapping = mutableListOf<Int>()
        val normalizedBuilder = StringBuilder()
        
        for (i in withoutBom.indices) {
            val char = withoutBom[i]
            val normChar = normalizeChar(char)
            if (normChar != null) {
                normalizedBuilder.append(normChar)
                indexMapping.add(i)
            }
        }
        
        val normText = normalizedBuilder.toString()
        if (normText.startsWith(bismillahNormalized)) {
            val matchedCharCount = bismillahNormalized.length
            if (matchedCharCount < indexMapping.size) {
                val originalStripIndex = indexMapping[matchedCharCount]
                return withoutBom.substring(originalStripIndex).trim()
            } else {
                return ""
            }
        }
        
        return withoutBom
    }

    private fun normalizeChar(char: Char): Char? {
        if (char.isWhitespace()) return null
        val code = char.code
        if (code in 0x064B..0x065F) return null // tashkeel / vowels
        if (code == 0x0670) return null // superscript alef
        if (code in 0x0610..0x061A) return null // special markers
        if (code in 0x0656..0x065E) return null // other diacritics
        
        return when (char) {
            'ٱ', 'أ', 'إ', 'آ' -> 'ا'
            else -> char
        }
    }

    private fun repairMojibake(value: String): String {
        return try {
            if (value.any { it.code in 0x0080..0x00FF }) {
                String(value.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
            } else {
                value
            }
        } catch (_: Exception) {
            value
        }
    }

    private fun unescapeHtml(text: String): String {
        return androidx.core.text.HtmlCompat.fromHtml(text, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {}
        try {
            sensorManager?.unregisterListener(sensorEventListener)
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        try {
            samplePlayer?.release()
            samplePlayer = null
        } catch (_: Exception) {}
        try {
            releaseLearningMediaPlayer()
        } catch (_: Exception) {}
    }
}
