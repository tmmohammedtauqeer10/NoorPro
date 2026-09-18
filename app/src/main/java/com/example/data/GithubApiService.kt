package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.io.IOException

@JsonClass(generateAdapter = true)
data class BookItem(
    val id: String,
    val title: String,
    val author: String,
    val language: String? = null,
    val pages: Int? = null,
    @Json(name = "cover_url") val coverUrl: String,
    @Json(name = "pdf_url") val pdfUrl: String
)

@JsonClass(generateAdapter = true)
data class LibraryCategory(
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryTitle: String,
    val books: List<BookItem>
)

@JsonClass(generateAdapter = true)
data class LibraryResponse(
    val categories: List<LibraryCategory>
)

@JsonClass(generateAdapter = true)
data class AudioItem(
    val id: String,
    val title: String,
    val artist: String,
    val language: String? = null,
    val duration: String? = null,
    val description: String? = null,
    @Json(name = "audio_url") val audioUrl: String = "",
    @Json(name = "cover_url") val coverUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class AudioCategory(
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryTitle: String,
    val description: String? = null,
    val items: List<AudioItem>
)

@JsonClass(generateAdapter = true)
data class AudioResponse(
    val categories: List<AudioCategory>
)

interface GithubApi {
    @GET("library_api.json")
    suspend fun fetchLibraryCatalog(): LibraryResponse

    @GET("quiz_data.json")
    suspend fun fetchQuizData(): QuizResponse

    @GET("audio_api.json")
    suspend fun fetchAudioCatalog(): AudioResponse
}

object GithubApiService {
    private const val BASE_URL = "https://raw.githubusercontent.com/tmmohammedtauqeer10/Deenflow/main/"

    const val libraryApiUrl = "${BASE_URL}library_api.json"
    const val quizApiUrl = "${BASE_URL}quiz_data.json"
    const val audioApiUrl = "${BASE_URL}audio_api.json"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val api: GithubApi = retrofit.create(GithubApi::class.java)

    suspend fun fetchLibraryCatalogSafely(): List<LibraryCategory> {
        return withContext(Dispatchers.IO) {
            try {
                api.fetchLibraryCatalog().categories
            } catch (e: Exception) {
                throw IOException("Library error: ${e.message}", e)
            }
        }
    }

    suspend fun fetchQuizDataSafely(): List<QuizCategory> {
        return withContext(Dispatchers.IO) {
            try {
                api.fetchQuizData().categories
            } catch (e: Exception) {
                throw IOException("Quiz error: ${e.message}", e)
            }
        }
    }

    suspend fun fetchAudioCatalogSafely(): List<AudioCategory> {
        return withContext(Dispatchers.IO) {
            try {
                api.fetchAudioCatalog().categories
            } catch (e: Exception) {
                // Do not invent empty-URL tracks — UI should show an offline/error state.
                emptyList()
            }
        }
    }

    private fun fallbackAudioCatalog(): List<AudioCategory> = listOf(
        AudioCategory(
            categoryId = "nasheed",
            categoryTitle = "Nasheeds - All Languages",
            description = "Arabic, Urdu, English, Hindi, Turkish, and Malay vocal collections",
            items = listOf(
                AudioItem("nasheed_01", "Global Nasheed Mix", "Noor Audio", "Multi-language", "Playlist", "A peaceful multilingual nasheed selection."),
                AudioItem("nasheed_02", "Urdu Nasheed Essentials", "Noor Audio", "Urdu", "Playlist", "Soft Urdu vocals for daily listening."),
                AudioItem("nasheed_03", "Arabic Nasheed Classics", "Noor Audio", "Arabic", "Playlist", "Classic Arabic vocal reminders."),
                AudioItem("nasheed_04", "English Nasheed Picks", "Noor Audio", "English", "Playlist", "Family-friendly English nasheeds.")
            )
        ),
        AudioCategory(
            categoryId = "playlists",
            categoryTitle = "Featured Playlists",
            description = "Curated Islamic listening sets for every moment",
            items = listOf(
                AudioItem("playlist_01", "Morning Iman Boost", "Noor Curated", "English / Urdu", "Playlist", "Start the day with calm reminders."),
                AudioItem("playlist_02", "Before Sleep Peace", "Noor Curated", "Multi-language", "Playlist", "Gentle listening for night reflection."),
                AudioItem("playlist_03", "Kids Islamic Learning", "Noor Curated", "English", "Playlist", "Simple songs and manners for children."),
                AudioItem("playlist_04", "Ramadan Focus", "Noor Curated", "Arabic / Urdu", "Playlist", "Fasting, dua, and reflection audio.")
            )
        ),
        AudioCategory(
            categoryId = "naat",
            categoryTitle = "Naat",
            description = "Respectful praise and love for the Prophet",
            items = listOf(
                AudioItem("naat_01", "Selected Naat Collection", "Noor Audio", "Urdu", "Playlist", "Curated devotional listening."),
                AudioItem("naat_02", "Madani Naat Series", "Noor Audio", "Urdu", "Series", "A respectful naat series."),
                AudioItem("naat_03", "English Praise Collection", "Noor Audio", "English", "Playlist", "English vocal praise and reflection.")
            )
        ),
        AudioCategory(
            categoryId = "hamd",
            categoryTitle = "Hamd",
            description = "Praise of Allah with calm, spiritual audio",
            items = listOf(
                AudioItem("hamd_01", "Hamd Favorites", "Noor Audio", "Urdu", "Playlist", "Praise, gratitude, and reflection."),
                AudioItem("hamd_02", "Gratitude And Praise", "Noor Audio", "English / Urdu", "Series", "Short spiritual listening set."),
                AudioItem("hamd_03", "Names And Praise", "Noor Audio", "Arabic / Urdu", "Playlist", "Praise of Allah and His beautiful names.")
            )
        ),
        AudioCategory(
            categoryId = "islamic_songs",
            categoryTitle = "Islamic Songs",
            description = "Family-safe Islamic vocals and learning songs",
            items = listOf(
                AudioItem("song_01", "Kids Islamic Songs", "Noor Audio", "English", "Playlist", "Good for family learning content."),
                AudioItem("song_02", "Faith And Manners Songs", "Noor Audio", "English / Urdu", "Series", "Manners, values, and simple reminders."),
                AudioItem("song_03", "Alphabet And Adab", "Noor Audio", "English", "Learning", "Children's learning audio.")
            )
        ),
        AudioCategory(
            categoryId = "seerah",
            categoryTitle = "Prophet & Seerah Series",
            description = "Episodes about the Prophet, companions, and Islamic history",
            items = listOf(
                AudioItem("seerah_01", "Life Of The Prophet", "Noor Series", "English", "Episodes", "A structured seerah audio series."),
                AudioItem("seerah_02", "Stories Of The Prophets", "Noor Series", "Urdu", "Episodes", "Prophet stories for all ages."),
                AudioItem("seerah_03", "Companions Around The Prophet", "Noor Series", "English / Urdu", "Episodes", "Short companion stories.")
            )
        ),
        AudioCategory(
            categoryId = "tafsir",
            categoryTitle = "Tafsir & Quran Reflections",
            description = "Short tafsir lessons and Quran meaning episodes",
            items = listOf(
                AudioItem("tafsir_01", "Surah Al-Fatiha Reflection", "Noor Tafsir", "English", "Episode", "Short reflection series."),
                AudioItem("tafsir_02", "Juz Amma Reflections", "Noor Tafsir", "Urdu", "Series", "Easy Quran meaning lessons."),
                AudioItem("tafsir_03", "Daily Ayah Podcast", "Noor Tafsir", "English / Urdu", "Podcast", "Daily Quran reflection.")
            )
        ),
        AudioCategory(
            categoryId = "podcasts",
            categoryTitle = "Islamic Podcasts",
            description = "Faith, family, worship, manners, and daily-life talks",
            items = listOf(
                AudioItem("podcast_01", "Faith And Daily Life", "Noor Podcast", "English", "Podcast", "Short weekly faith conversations."),
                AudioItem("podcast_02", "Ask And Learn", "Noor Podcast", "Urdu", "Podcast", "Questions, answers, and reminders."),
                AudioItem("podcast_03", "Muslim Family Talks", "Noor Podcast", "English / Urdu", "Podcast", "Family, adab, and character.")
            )
        ),
        AudioCategory(
            categoryId = "reminders",
            categoryTitle = "Short Reminders",
            description = "Quick reminders for prayer, dhikr, akhlaq, and hope",
            items = listOf(
                AudioItem("reminder_01", "Two Minute Reminder", "Noor Reminders", "English", "Series", "Short daily reminder."),
                AudioItem("reminder_02", "Prayer Focus", "Noor Reminders", "Urdu", "Series", "Salah motivation and focus."),
                AudioItem("reminder_03", "Hope And Tawbah", "Noor Reminders", "English / Urdu", "Series", "Mercy, repentance, and hope.")
            )
        )
    )
}
