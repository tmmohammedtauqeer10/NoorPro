package com.noorpro.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

// 2. The Data Models

@JsonClass(generateAdapter = true)
data class AlQuranSurahListResponse(
    val data: List<AlQuranSurahItem>
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahItem(
    val number: Int,
    val name: String,
    val englishName: String,
    val emptyNameTranslation: String? = null,
    val numberOfAyahs: Int,
    val revelationType: String? = null
)

@JsonClass(generateAdapter = true)
data class AlQuranAyahEdition(
    val number: Int,
    val text: String,
    val audio: String? = null
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahDetailsEdition(
    val englishName: String,
    val ayahs: List<AlQuranAyahEdition>
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahDetailsResponse(
    val data: List<AlQuranSurahDetailsEdition>
)

// Clean domain models
data class QuranLearningSurah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val ayahsCount: Int,
    val revelationType: String?
)

data class QuranLearningAyah(
    val number: Int,
    val arabicText: String,
    val transliteration: String,
    val englishTranslation: String,
    val audioUrl: String?
)

// 1. The API Service
interface AlQuranApi {
    @GET("surah")
    suspend fun getSurahList(): AlQuranSurahListResponse

    @GET("surah/{surahNumber}/editions/quran-uthmani,en.transliteration,en.asad,ar.alafasy")
    suspend fun getSurahDetails(@Path("surahNumber") surahNumber: Int): AlQuranSurahDetailsResponse
}

object QuranLearningApiService {
    private const val BASE_URL = "https://api.alquran.cloud/v1/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val api: AlQuranApi = retrofit.create(AlQuranApi::class.java)

    suspend fun fetchSurahList(): List<QuranLearningSurah> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSurahList()
            response.data.map {
                QuranLearningSurah(
                    number = it.number,
                    nameArabic = it.name,
                    nameEnglish = it.englishName,
                    ayahsCount = it.numberOfAyahs,
                    revelationType = it.revelationType
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchSurahDetails(surahNumber: Int): List<QuranLearningAyah> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSurahDetails(surahNumber)
            val data = response.data
            
            // data[0] -> quran-uthmani (Arabic text)
            // data[1] -> en.transliteration (Transliteration)
            // data[2] -> en.asad (English Translation)
            // data[3] -> ar.alafasy (Audio / Arabic text)

            val arabicEditions = data.getOrNull(0)?.ayahs ?: emptyList()
            val transliterationEditions = data.getOrNull(1)?.ayahs ?: emptyList()
            val englishEditions = data.getOrNull(2)?.ayahs ?: emptyList()
            val audioEditions = data.getOrNull(3)?.ayahs ?: emptyList()

            // Merge them into domain model
            val size = maxOf(arabicEditions.size, englishEditions.size)
            val result = mutableListOf<QuranLearningAyah>()

            for (i in 0 until size) {
                val arabic = arabicEditions.getOrNull(i)?.text ?: ""
                val transliteration = transliterationEditions.getOrNull(i)?.text ?: ""
                val english = englishEditions.getOrNull(i)?.text ?: ""
                val audioUrl = audioEditions.getOrNull(i)?.audio

                result.add(
                    QuranLearningAyah(
                        number = i + 1,
                        arabicText = arabic,
                        transliteration = transliteration,
                        englishTranslation = english,
                        audioUrl = audioUrl
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
