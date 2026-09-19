package com.example.data

enum class JourneyType { UMRAH, HAJJ }

enum class PilgrimageLanguage { ENGLISH, URDU }

enum class Madhhab { GENERAL, HANAFI, SHAFII, MALIKI, HANBALI }

enum class ScholarReviewStatus { SOURCE_REVIEWED, SCHOLAR_REVIEWED }

data class PilgrimageDua(
    val id: String,
    val title: String,
    val arabic: String,
    val english: String,
    val urdu: String,
    val source: String
)

data class MadhhabNote(
    val madhhab: Madhhab,
    val note: String,
    val reviewed: Boolean
)

data class ScholarReview(
    val status: ScholarReviewStatus,
    val reviewer: String,
    val reviewDate: String,
    val note: String
)

data class PilgrimageGuide(
    val id: String,
    val journeyType: JourneyType,
    val ritualOrder: Int,
    val title: String,
    val titleUrdu: String,
    val summary: String,
    val summaryUrdu: String,
    val instructions: List<String>,
    val prerequisites: List<String>,
    val duas: List<PilgrimageDua>,
    val references: List<String>,
    val madhhabNotes: List<MadhhabNote>,
    val accessibilityAdvice: List<String>,
    val mistakes: List<String>,
    val fiqhClassification: String,
    val scholarReview: ScholarReview,
    val updatedAt: String,
    val version: Int
)

data class JourneyProgress(
    val userId: String,
    val journeyType: JourneyType,
    val activeRitualId: String,
    val completedRitualIds: Set<String> = emptySet(),
    val ritualCounters: Map<String, Int> = emptyMap(),
    val checklistState: Map<String, Boolean> = emptyMap(),
    val lastSyncedAt: Long = 0L
)

data class SavedPilgrimPlace(
    val id: String,
    val userId: String,
    val type: String,
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val addressEnglish: String = "",
    val addressArabic: String = "",
    val isOfflineAvailable: Boolean = true
)

data class OfficialServiceLink(
    val id: String,
    val country: String,
    val serviceType: String,
    val title: String,
    val url: String,
    val active: Boolean,
    val verifiedAt: String
)

data class PilgrimagePlannerState(
    val passportChecked: Boolean = false,
    val visaChecked: Boolean = false,
    val insuranceChecked: Boolean = false,
    val medicinesChecked: Boolean = false,
    val ihramChecked: Boolean = false,
    val chargerChecked: Boolean = false,
    val flightNumber: String = "",
    val flightDate: String = "",
    val hotelName: String = "",
    val hotelAddressEnglish: String = "",
    val hotelAddressArabic: String = "",
    val transportNotes: String = "",
    val groupLeader: String = "",
    val groupPhone: String = "",
    val familyMeetingPoint: String = "",
    val updatedAt: Long = 0L
)

fun progressPercentage(progress: JourneyProgress, guideSize: Int): Int =
    if (guideSize <= 0) 0 else ((progress.completedRitualIds.size.coerceAtMost(guideSize) * 100f) / guideSize).toInt()

fun nextActiveRitualId(guides: List<PilgrimageGuide>, completedIds: Set<String>): String =
    guides.sortedBy { it.ritualOrder }.firstOrNull { it.id !in completedIds }?.id.orEmpty()

fun updateRitualCounter(progress: JourneyProgress, ritualId: String, delta: Int, maximum: Int): JourneyProgress {
    val next = ((progress.ritualCounters[ritualId] ?: 0) + delta).coerceIn(0, maximum)
    return progress.copy(
        ritualCounters = progress.ritualCounters + (ritualId to next),
        lastSyncedAt = System.currentTimeMillis()
    )
}

