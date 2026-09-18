package com.noorpro.app.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

interface PilgrimageRepository {
    fun getJourneyGuide(journeyType: JourneyType, language: PilgrimageLanguage, madhhab: Madhhab): List<PilgrimageGuide>
    fun getRitualGuide(ritualId: String, language: PilgrimageLanguage, madhhab: Madhhab): PilgrimageGuide?
    fun getJourneyProgress(userId: String, journeyType: JourneyType): JourneyProgress
    fun updateJourneyProgress(userId: String, journeyType: JourneyType, progress: JourneyProgress)
    fun syncJourneyProgress(userId: String, journeyType: JourneyType, onMerged: (JourneyProgress) -> Unit)
    fun savePilgrimPlace(userId: String, place: SavedPilgrimPlace)
    fun getSavedPilgrimPlaces(userId: String): List<SavedPilgrimPlace>
    fun getOfficialServiceLinks(country: String): List<OfficialServiceLink>
    fun downloadJourneyForOfflineUse(journeyType: JourneyType, language: PilgrimageLanguage): Result<Int>
    fun getPlannerState(userId: String): PilgrimagePlannerState
    fun updatePlannerState(userId: String, state: PilgrimagePlannerState)
}

class NoorPilgrimageRepository(context: Context) : PilgrimageRepository {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("noor_pilgrimage", Context.MODE_PRIVATE)
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun getJourneyGuide(
        journeyType: JourneyType,
        language: PilgrimageLanguage,
        madhhab: Madhhab
    ): List<PilgrimageGuide> = bundledPilgrimageGuides.filter { it.journeyType == journeyType }.sortedBy { it.ritualOrder }

    override fun getRitualGuide(
        ritualId: String,
        language: PilgrimageLanguage,
        madhhab: Madhhab
    ): PilgrimageGuide? = bundledPilgrimageGuides.firstOrNull { it.id == ritualId }

    override fun getJourneyProgress(userId: String, journeyType: JourneyType): JourneyProgress {
        val prefix = progressPrefix(userId, journeyType)
        val guides = getJourneyGuide(journeyType, PilgrimageLanguage.ENGLISH, Madhhab.GENERAL)
        val completed = prefs.getStringSet("${prefix}_completed", emptySet()).orEmpty()
        val counters = decodeIntMap(prefs.getString("${prefix}_counters", "").orEmpty())
        val checklist = decodeBooleanMap(prefs.getString("${prefix}_checklist", "").orEmpty())
        val active = prefs.getString("${prefix}_active", null)
            ?.takeIf { value -> guides.any { it.id == value } }
            ?: nextActiveRitualId(guides, completed)
        return JourneyProgress(
            userId = userId,
            journeyType = journeyType,
            activeRitualId = active,
            completedRitualIds = completed,
            ritualCounters = counters,
            checklistState = checklist,
            lastSyncedAt = prefs.getLong("${prefix}_updated", 0L)
        )
    }

    override fun updateJourneyProgress(userId: String, journeyType: JourneyType, progress: JourneyProgress) {
        val guides = getJourneyGuide(journeyType, PilgrimageLanguage.ENGLISH, Madhhab.GENERAL)
        val normalized = progress.copy(
            userId = userId,
            journeyType = journeyType,
            activeRitualId = progress.activeRitualId.ifBlank { nextActiveRitualId(guides, progress.completedRitualIds) },
            ritualCounters = progress.ritualCounters.mapValues { (id, value) ->
                value.coerceIn(0, if (id.contains("tawaf")) 7 else 99)
            },
            lastSyncedAt = System.currentTimeMillis()
        )
        writeProgressLocal(normalized)
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        if (firebaseUser.uid != userId) return
        firestore.collection("users").document(userId)
            .collection("journeys").document(journeyType.name.lowercase())
            .set(normalized.toFirestoreMap(), SetOptions.merge())
    }

    override fun syncJourneyProgress(userId: String, journeyType: JourneyType, onMerged: (JourneyProgress) -> Unit) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        if (firebaseUser.uid != userId) return
        val local = getJourneyProgress(userId, journeyType)
        firestore.collection("users").document(userId)
            .collection("journeys").document(journeyType.name.lowercase())
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    updateJourneyProgress(userId, journeyType, local)
                    return@addOnSuccessListener
                }
                val remoteUpdated = snapshot.getLong("lastSyncedAt") ?: 0L
                val remoteCompleted = (snapshot.get("completedRitualIds") as? List<*>)
                    .orEmpty().mapNotNull { it as? String }.toSet()
                @Suppress("UNCHECKED_CAST")
                val remoteCounters = (snapshot.get("ritualCounters") as? Map<String, Number>)
                    .orEmpty().mapValues { it.value.toInt() }
                @Suppress("UNCHECKED_CAST")
                val remoteChecklist = (snapshot.get("checklistState") as? Map<String, Boolean>).orEmpty()
                val merged = local.copy(
                    activeRitualId = if (remoteUpdated > local.lastSyncedAt) snapshot.getString("activeRitualId").orEmpty() else local.activeRitualId,
                    completedRitualIds = local.completedRitualIds + remoteCompleted,
                    ritualCounters = mergeCounters(local.ritualCounters, remoteCounters),
                    checklistState = if (remoteUpdated > local.lastSyncedAt) local.checklistState + remoteChecklist else remoteChecklist + local.checklistState,
                    lastSyncedAt = maxOf(local.lastSyncedAt, remoteUpdated)
                )
                writeProgressLocal(merged)
                onMerged(merged)
            }
    }

    override fun savePilgrimPlace(userId: String, place: SavedPilgrimPlace) {
        val normalized = place.copy(userId = userId)
        prefs.edit()
            .putString("place_${safeKey(userId)}_${safeKey(place.id)}", encodePlace(normalized))
            .putStringSet("places_${safeKey(userId)}", getSavedPlaceIds(userId) + place.id)
            .apply()
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        if (firebaseUser.uid != userId) return
        firestore.collection("users").document(userId)
            .collection("savedPilgrimPlaces").document(place.id)
            .set(normalized.toFirestoreMap(), SetOptions.merge())
    }

    override fun getSavedPilgrimPlaces(userId: String): List<SavedPilgrimPlace> =
        getSavedPlaceIds(userId).mapNotNull { id ->
            decodePlace(prefs.getString("place_${safeKey(userId)}_${safeKey(id)}", null))
        }

    override fun getOfficialServiceLinks(country: String): List<OfficialServiceLink> = officialServiceLinks
        .filter { it.country.equals(country, ignoreCase = true) && it.active && isSafeOfficialUrl(it.url) }

    override fun downloadJourneyForOfflineUse(
        journeyType: JourneyType,
        language: PilgrimageLanguage
    ): Result<Int> = runCatching {
        val guide = getJourneyGuide(journeyType, language, Madhhab.GENERAL)
        prefs.edit()
            .putInt("offline_${journeyType.name}_${language.name}", guide.maxOfOrNull { it.version } ?: 0)
            .putLong("offline_${journeyType.name}_${language.name}_at", System.currentTimeMillis())
            .apply()
        guide.size
    }

    override fun getPlannerState(userId: String): PilgrimagePlannerState {
        val p = "planner_${safeKey(userId)}"
        return PilgrimagePlannerState(
            passportChecked = prefs.getBoolean("${p}_passport", false),
            visaChecked = prefs.getBoolean("${p}_visa", false),
            insuranceChecked = prefs.getBoolean("${p}_insurance", false),
            medicinesChecked = prefs.getBoolean("${p}_medicines", false),
            ihramChecked = prefs.getBoolean("${p}_ihram", false),
            chargerChecked = prefs.getBoolean("${p}_charger", false),
            flightNumber = prefs.getString("${p}_flight_number", "").orEmpty(),
            flightDate = prefs.getString("${p}_flight_date", "").orEmpty(),
            hotelName = prefs.getString("${p}_hotel_name", "").orEmpty(),
            hotelAddressEnglish = prefs.getString("${p}_hotel_en", "").orEmpty(),
            hotelAddressArabic = prefs.getString("${p}_hotel_ar", "").orEmpty(),
            transportNotes = prefs.getString("${p}_transport", "").orEmpty(),
            groupLeader = prefs.getString("${p}_group_leader", "").orEmpty(),
            groupPhone = prefs.getString("${p}_group_phone", "").orEmpty(),
            familyMeetingPoint = prefs.getString("${p}_meeting", "").orEmpty(),
            updatedAt = prefs.getLong("${p}_updated", 0L)
        )
    }

    override fun updatePlannerState(userId: String, state: PilgrimagePlannerState) {
        val p = "planner_${safeKey(userId)}"
        val normalized = state.copy(updatedAt = System.currentTimeMillis())
        prefs.edit()
            .putBoolean("${p}_passport", normalized.passportChecked)
            .putBoolean("${p}_visa", normalized.visaChecked)
            .putBoolean("${p}_insurance", normalized.insuranceChecked)
            .putBoolean("${p}_medicines", normalized.medicinesChecked)
            .putBoolean("${p}_ihram", normalized.ihramChecked)
            .putBoolean("${p}_charger", normalized.chargerChecked)
            .putString("${p}_flight_number", normalized.flightNumber)
            .putString("${p}_flight_date", normalized.flightDate)
            .putString("${p}_hotel_name", normalized.hotelName)
            .putString("${p}_hotel_en", normalized.hotelAddressEnglish)
            .putString("${p}_hotel_ar", normalized.hotelAddressArabic)
            .putString("${p}_transport", normalized.transportNotes)
            .putString("${p}_group_leader", normalized.groupLeader)
            .putString("${p}_group_phone", normalized.groupPhone)
            .putString("${p}_meeting", normalized.familyMeetingPoint)
            .putLong("${p}_updated", normalized.updatedAt)
            .apply()
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        if (firebaseUser.uid != userId) return
        firestore.collection("users").document(userId)
            .collection("journeys").document("planner")
            .set(normalized.toFirestoreMap(), SetOptions.merge())
        if (normalized.hotelName.isNotBlank() || normalized.hotelAddressEnglish.isNotBlank() || normalized.hotelAddressArabic.isNotBlank()) {
            savePilgrimPlace(
                userId,
                SavedPilgrimPlace(
                    id = "hotel",
                    userId = userId,
                    type = "hotel",
                    name = normalized.hotelName.ifBlank { "Saved hotel" },
                    addressEnglish = normalized.hotelAddressEnglish,
                    addressArabic = normalized.hotelAddressArabic
                )
            )
        }
    }

    private fun writeProgressLocal(progress: JourneyProgress) {
        val prefix = progressPrefix(progress.userId, progress.journeyType)
        prefs.edit()
            .putString("${prefix}_active", progress.activeRitualId)
            .putStringSet("${prefix}_completed", progress.completedRitualIds)
            .putString("${prefix}_counters", encodeMap(progress.ritualCounters))
            .putString("${prefix}_checklist", encodeMap(progress.checklistState))
            .putLong("${prefix}_updated", progress.lastSyncedAt)
            .apply()
    }

    private fun progressPrefix(userId: String, type: JourneyType) = "progress_${safeKey(userId)}_${type.name}"
    private fun safeKey(value: String) = value.replace(Regex("[^A-Za-z0-9_-]"), "_")
    private fun getSavedPlaceIds(userId: String) = prefs.getStringSet("places_${safeKey(userId)}", emptySet()).orEmpty()
    private fun encodeMap(map: Map<String, *>) = map.entries.joinToString(";") { "${safeKey(it.key)}=${it.value}" }
    private fun decodeIntMap(value: String) = value.split(';').mapNotNull { token ->
        val parts = token.split('=', limit = 2)
        if (parts.size == 2) parts[1].toIntOrNull()?.let { parts[0] to it } else null
    }.toMap()
    private fun decodeBooleanMap(value: String) = value.split(';').mapNotNull { token ->
        val parts = token.split('=', limit = 2)
        if (parts.size == 2 && parts[1] in setOf("true", "false")) parts[0] to parts[1].toBoolean() else null
    }.toMap()
    private fun mergeCounters(local: Map<String, Int>, remote: Map<String, Int>) =
        (local.keys + remote.keys).associateWith { key -> maxOf(local[key] ?: 0, remote[key] ?: 0) }

    private fun encodePlace(place: SavedPilgrimPlace) = listOf(
        place.id, place.userId, place.type, place.name, place.latitude?.toString().orEmpty(),
        place.longitude?.toString().orEmpty(), place.addressEnglish, place.addressArabic,
        place.isOfflineAvailable.toString()
    ).joinToString("\u001F") { it.replace("\u001F", " ") }

    private fun decodePlace(value: String?): SavedPilgrimPlace? {
        val p = value?.split("\u001F") ?: return null
        if (p.size != 9) return null
        return SavedPilgrimPlace(p[0], p[1], p[2], p[3], p[4].toDoubleOrNull(), p[5].toDoubleOrNull(), p[6], p[7], p[8].toBoolean())
    }
}

private fun JourneyProgress.toFirestoreMap() = mapOf(
    "journeyType" to journeyType.name,
    "activeRitualId" to activeRitualId,
    "completedRitualIds" to completedRitualIds.toList(),
    "ritualCounters" to ritualCounters,
    "checklistState" to checklistState,
    "lastSyncedAt" to lastSyncedAt,
    "schemaVersion" to 1
)

private fun SavedPilgrimPlace.toFirestoreMap() = mapOf(
    "type" to type, "name" to name, "latitude" to latitude, "longitude" to longitude,
    "addressEnglish" to addressEnglish, "addressArabic" to addressArabic,
    "isOfflineAvailable" to isOfflineAvailable, "updatedAt" to System.currentTimeMillis()
)

private fun PilgrimagePlannerState.toFirestoreMap() = mapOf(
    "passportChecked" to passportChecked, "visaChecked" to visaChecked,
    "insuranceChecked" to insuranceChecked, "medicinesChecked" to medicinesChecked,
    "ihramChecked" to ihramChecked, "chargerChecked" to chargerChecked,
    "flightNumber" to flightNumber, "flightDate" to flightDate, "hotelName" to hotelName,
    "hotelAddressEnglish" to hotelAddressEnglish, "hotelAddressArabic" to hotelAddressArabic,
    "transportNotes" to transportNotes, "groupLeader" to groupLeader, "groupPhone" to groupPhone,
    "familyMeetingPoint" to familyMeetingPoint, "updatedAt" to updatedAt, "schemaVersion" to 1
)

fun isSafeOfficialUrl(url: String): Boolean = runCatching {
    val uri = java.net.URI(url)
    uri.scheme == "https" && (uri.host == "nusuk.sa" || uri.host.endsWith(".nusuk.sa") || uri.host == "haj.gov.sa" || uri.host.endsWith(".haj.gov.sa") || uri.host == "my.gov.sa" || uri.host.endsWith(".my.gov.sa"))
}.getOrDefault(false)

private val officialServiceLinks = listOf(
    OfficialServiceLink("nusuk_services", "Saudi Arabia", "permits", "Official Nusuk services", "https://services.nusuk.sa/", true, "2026-09-12"),
    OfficialServiceLink("nusuk_umrah", "Saudi Arabia", "umrah", "Official Nusuk Umrah", "https://umrah.nusuk.sa/", true, "2026-09-12"),
    OfficialServiceLink("nusuk_hajj", "Saudi Arabia", "hajj", "Official Nusuk Hajj", "https://hajj.nusuk.sa/", true, "2026-09-12"),
    OfficialServiceLink("emergency", "Saudi Arabia", "emergency", "Saudi emergency contacts", "https://my.gov.sa/en/emergency-contact", true, "2026-09-12")
)

private val sourceReview = ScholarReview(
    status = ScholarReviewStatus.SOURCE_REVIEWED,
    reviewer = "NoorPro source review",
    reviewDate = "2026-09-12",
    note = "Checked against Quran and cited authentic reports. Madhhab-specific scholar sign-off is still pending."
)

private fun guide(
    id: String,
    type: JourneyType,
    order: Int,
    title: String,
    titleUrdu: String,
    summary: String,
    summaryUrdu: String,
    prerequisites: List<String>,
    instructions: List<String>,
    mistakes: List<String>,
    accessibility: List<String>,
    references: List<String>,
    dua: PilgrimageDua? = null,
    classification: String = "General guidance"
) = PilgrimageGuide(
    id, type, order, title, titleUrdu, summary, summaryUrdu, instructions, prerequisites,
    listOfNotNull(dua), references,
    Madhhab.entries.map { MadhhabNote(it, if (it == Madhhab.GENERAL) "General source-reviewed guidance." else "No reviewed madhhab-specific difference is published in this version.", it == Madhhab.GENERAL) },
    accessibility, mistakes, classification, sourceReview, "2026-09-12", 1
)

private val talbiyah = PilgrimageDua(
    "talbiyah", "Talbiyah",
    "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ",
    "Here I am, O Allah, here I am. You have no partner. Praise, blessing and sovereignty belong to You.",
    "میں حاضر ہوں اے اللہ، میں حاضر ہوں۔ تیرا کوئی شریک نہیں۔ تمام تعریف، نعمت اور بادشاہی تیرے ہی لیے ہے۔",
    "Sahih al-Bukhari 1549; Sahih Muslim 1184"
)

private val rabbana = PilgrimageDua(
    "rabbana_atina", "A Quranic dua",
    "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
    "Our Lord, give us good in this world and good in the Hereafter, and protect us from the Fire.",
    "اے ہمارے رب، ہمیں دنیا میں بھلائی دے اور آخرت میں بھی بھلائی دے اور ہمیں آگ کے عذاب سے بچا۔",
    "Quran 2:201"
)

private val bundledPilgrimageGuides = listOf(
    guide("umrah_ihram", JourneyType.UMRAH, 1, "Ihram & Niyyah", "احرام اور نیت", "Enter ihram before the miqat and begin the Talbiyah.", "میقات سے پہلے احرام باندھیں اور تلبیہ شروع کریں۔", listOf("Know your route and miqat", "Prepare unscented essentials"), listOf("Prepare before crossing the miqat.", "Make the intention in your heart and begin the Talbiyah.", "Avoid the restrictions of ihram after entering it."), listOf("Crossing the miqat before entering ihram", "Treating the intention as a required long formula"), listOf("Ask your group leader for help before the miqat", "Keep medicines and identification easy to reach"), listOf("Quran 2:196", "Sahih al-Bukhari 1549", "Sahih Muslim 1184"), talbiyah),
    guide("umrah_tawaf", JourneyType.UMRAH, 2, "Tawaf", "طواف", "Complete seven counter-clockwise rounds around the Kaaba.", "کعبہ کے گرد سات چکر گھڑی کی مخالف سمت میں مکمل کریں۔", listOf("Be ready before entering the mataf", "Choose a safe level for your mobility"), listOf("Align with the Black Stone line and begin without pushing.", "Count one round each time you return to the starting line.", "Make dhikr and dua in any language while keeping others safe."), listOf("Pushing to touch the Black Stone", "Believing every round has a compulsory special dua", "Losing count and restarting unnecessarily"), listOf("Wheelchair users should follow current access signs", "Use the counter and vibration rather than watching the phone continuously"), listOf("Quran 22:29", "Sahih Muslim 1218", "Sahih al-Bukhari 1613"), rabbana),
    guide("umrah_maqam_zamzam", JourneyType.UMRAH, 3, "Maqam Ibrahim & Zamzam", "مقام ابراہیم اور زمزم", "Pray two rak'ah where safe, then drink Zamzam.", "محفوظ جگہ پر دو رکعت پڑھیں، پھر زمزم پئیں۔", listOf("Finish seven Tawaf rounds"), listOf("Move away from the crowd before praying.", "If the area is crowded, pray elsewhere in the Haram.", "Drink Zamzam and make personal dua."), listOf("Blocking the walking flow", "Pushing for one exact prayer spot"), listOf("Use any safe prayer area", "Sit and ask for help if dizzy or unwell"), listOf("Quran 2:125", "Sahih Muslim 1218"), rabbana),
    guide("umrah_sai", JourneyType.UMRAH, 4, "Sa'i", "سعی", "Walk seven lengths between Safa and Marwah.", "صفا اور مروہ کے درمیان سات چکر مکمل کریں۔", listOf("Complete Tawaf", "Start at Safa"), listOf("Safa to Marwah is one length.", "Marwah back to Safa is the second length.", "Finish the seventh length at Marwah."), listOf("Counting a return trip as one", "Running where it is unsafe"), listOf("Wheelchairs and mobility assistance are available", "Rest when needed and keep access lanes clear"), listOf("Quran 2:158", "Sahih Muslim 1218"), rabbana),
    guide("umrah_halq_taqsir", JourneyType.UMRAH, 5, "Halq or Taqsir", "حلق یا تقصیر", "Shave or trim as applicable to complete Umrah.", "عمرہ مکمل کرنے کے لیے شرعی طریقے سے بال منڈوائیں یا کٹوائیں۔", listOf("Complete Sa'i"), listOf("Use a safe and hygienic barber or tool.", "Complete the applicable shaving or trimming.", "Your Umrah journey progress can then be marked complete."), listOf("Using unhygienic shared blades", "Trimming before Sa'i is complete"), listOf("Ask a companion for safe assistance", "Avoid crowded passages while stopping"), listOf("Quran 2:196", "Sahih al-Bukhari 1727", "Sahih Muslim 1301")),
    guide("hajj_mina_8", JourneyType.HAJJ, 1, "8 Dhul Hijjah · Mina", "8 ذوالحجہ · منیٰ", "Enter ihram for Hajj and proceed with your authorised group plan.", "حج کا احرام باندھیں اور اپنے مجاز گروپ کے منصوبے کے مطابق منیٰ جائیں۔", listOf("Confirm your Hajj type with a trusted scholar", "Keep permit and group identification ready"), listOf("Follow official transport and group timings.", "Use the day for prayer, Talbiyah and rest.", "Prepare for Arafah without separating from your group."), listOf("Leaving the group in heavy crowds", "Relying on unofficial transport instructions"), listOf("Wear visible identification", "Save your group leader and meeting point offline"), listOf("Quran 22:27", "Quran 2:197", "Sahih Muslim 1218"), talbiyah),
    guide("hajj_arafah_9", JourneyType.HAJJ, 2, "9 Dhul Hijjah · Arafah", "9 ذوالحجہ · عرفات", "Remain within Arafah during the prescribed time and make dua.", "مقررہ وقت میں حدود عرفات کے اندر رہیں اور دعا کریں۔", listOf("Verify your camp is within Arafah", "Carry water, medicine and identification"), listOf("Follow your official group to Arafah.", "Devote the time to worship, repentance and dua.", "Leave after sunset according to official movement instructions."), listOf("Standing outside the Arafah boundary", "Ignoring heat or medical symptoms"), listOf("Use shaded areas and hydrate", "Seek medical help early"), listOf("Quran 2:198", "Jami at-Tirmidhi 889", "Sahih Muslim 1218"), rabbana),
    guide("hajj_muzdalifah", JourneyType.HAJJ, 3, "Muzdalifah", "مزدلفہ", "Travel after sunset, pray and rest according to your Hajj plan.", "غروب آفتاب کے بعد روانہ ہوں، نماز اور آرام اپنے حج منصوبے کے مطابق کریں۔", listOf("Depart Arafah after sunset"), listOf("Move calmly with your authorised group.", "Follow the prayer and rest arrangement given by your trusted guide.", "Collect only what your plan requires without crowding."), listOf("Rushing transport queues", "Collecting oversized stones"), listOf("Keep warm clothing accessible", "Use concessions only after trusted guidance"), listOf("Quran 2:198", "Sahih Muslim 1218", "Sahih al-Bukhari 1673"), talbiyah),
    guide("hajj_jamarah_10", JourneyType.HAJJ, 4, "10 Dhul Hijjah · Jamarah", "10 ذوالحجہ · جمرہ", "Complete the scheduled rites using official routes and crowd controls.", "سرکاری راستوں اور ہجوم کے انتظام کے مطابق مقررہ مناسک مکمل کریں۔", listOf("Confirm timing and route with your group"), listOf("Use only the route and time assigned to your group.", "Keep a safe distance and do not push.", "Follow your verified Hajj-type plan for the remaining rites."), listOf("Going at an unassigned crowded time", "Throwing objects other than small pebbles"), listOf("Use accessible routes when assigned", "Do not continue if unwell"), listOf("Quran 22:36", "Sahih Muslim 1218", "Sahih al-Bukhari 1735")),
    guide("hajj_ifadah_sai", JourneyType.HAJJ, 5, "Tawaf al-Ifadah & Sa'i", "طواف افاضہ اور سعی", "Complete Tawaf al-Ifadah and Sa'i when required by your verified plan.", "اپنے تصدیق شدہ منصوبے کے مطابق طواف افاضہ اور ضروری سعی مکمل کریں۔", listOf("Confirm what your Hajj type requires"), listOf("Choose a safe time and level for Tawaf.", "Use the seven-round counter if helpful.", "Complete Sa'i if it applies to your Hajj plan."), listOf("Pushing in peak crowds", "Applying another pilgrim's Hajj plan to yourself"), listOf("Use mobility services and quieter assigned periods", "Keep your group contact available"), listOf("Quran 22:29", "Quran 2:158", "Sahih Muslim 1218"), rabbana),
    guide("hajj_mina_days", JourneyType.HAJJ, 6, "Days of Mina", "ایام منیٰ", "Complete the scheduled Jamarat rites and remain with your group.", "مقررہ رمی مکمل کریں اور اپنے گروپ کے ساتھ رہیں۔", listOf("Confirm your assigned Jamarat schedule"), listOf("Follow official crowd-control times.", "Complete each scheduled Jamarat safely.", "Keep your departure plan and meeting point offline."), listOf("Using a different group's time", "Stopping in moving crowd lanes"), listOf("Use authorised wheelchair routes", "Ask your group about valid concessions"), listOf("Quran 2:203", "Sahih Muslim 1218", "Sahih al-Bukhari 1751")),
    guide("hajj_farewell_tawaf", JourneyType.HAJJ, 7, "Farewell Tawaf", "طواف وداع", "Complete the farewell rite when it applies to you.", "اگر آپ پر لاگو ہو تو طواف وداع مکمل کریں۔", listOf("Confirm applicability with a trusted scholar or guide", "Plan close to departure"), listOf("Choose a safe period before leaving Makkah.", "Complete seven rounds without pushing.", "Follow your group departure time."), listOf("Missing transport while waiting for a crowded period", "Assuming the same ruling applies to every pilgrim"), listOf("Ask about exemptions and assistance", "Keep luggage and companions coordinated"), listOf("Sahih al-Bukhari 1755", "Sahih Muslim 1327", "Quran 22:29"), rabbana)
)

