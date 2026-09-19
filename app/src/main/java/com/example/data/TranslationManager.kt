package com.example.data

import com.example.utils.CrashReporter

import android.content.Context
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class TranslationOption(
    val key: String,
    val name: String,
    val translator: String,
    val description: String,
    val language: String,
    val url: String
)

class TranslationManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("prayer_settings_prefs", Context.MODE_PRIVATE)
    private val _selectedTranslation = MutableStateFlow(prefs.getString("selected_translation", "en.ahmedraza") ?: "en.ahmedraza")
    val selectedTranslation: StateFlow<String> = _selectedTranslation.asStateFlow()

    private val translationsDir = File(context.filesDir, "translations").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    val availableTranslations = listOf(
        TranslationOption(
            key = "default",
            name = "Default",
            translator = "Standard English",
            description = "Built-in standard English translation for key chapters.",
            language = "English",
            url = ""
        ),
        TranslationOption(
            key = "en.sahih",
            name = "Sahih International",
            translator = "Umm Muhammad",
            description = "The most popular contemporary modern English translation.",
            language = "English",
            url = "https://api.alquran.cloud/v1/quran/en.sahih"
        ),
        TranslationOption(
            key = "en.yusufali",
            name = "Yusuf Ali",
            translator = "Abdullah Yusuf Ali",
            description = "Classic traditional translation with rich detailed phrasing.",
            language = "English",
            url = "https://api.alquran.cloud/v1/quran/en.yusufali"
        ),
        TranslationOption(
            key = "en.maududi",
            name = "Maududi (Sayyid Abul A'la)",
            translator = "Sayyid Abul A'la Maududi",
            description = "Comprehensive literal translation providing strong context.",
            language = "English",
            url = "https://api.alquran.cloud/v1/quran/en.maududi"
        ),
        TranslationOption(
            key = "en.ahmedraza",
            name = "Kanzul Iman",
            translator = "Ahmed Raza Khan",
            description = "English translation of Kanzul Iman.",
            language = "English",
            url = "https://api.alquran.cloud/v1/quran/en.ahmedraza"
        ),
        TranslationOption(
            key = "en.itani",
            name = "Clear Qur'an",
            translator = "Talal Itani",
            description = "Clear Qur'an by Talal Itani.",
            language = "English",
            url = "https://api.alquran.cloud/v1/quran/en.itani"
        ),
        TranslationOption(
            key = "ur.kanzuliman",
            name = "Kanzul Iman",
            translator = "Ahmed Raza Khan",
            description = "Urdu - A highly regarded traditional Urdu translation.",
            language = "Urdu",
            url = "https://api.alquran.cloud/v1/quran/ur.kanzuliman"
        ),
        TranslationOption(
            key = "bn.bengali",
            name = "Muhiuddin Khan",
            translator = "Muhiuddin Khan",
            description = "Bengali translation by Muhiuddin Khan.",
            language = "Bengali",
            url = "https://api.alquran.cloud/v1/quran/bn.bengali"
        ),
        TranslationOption(
            key = "ta.tamil",
            name = "Jan Trust",
            translator = "Jan Trust Foundation",
            description = "Tamil translation of the Quran.",
            language = "Tamil",
            url = "https://api.alquran.cloud/v1/quran/ta.tamil"
        ),
        TranslationOption(
            key = "ml.abdulhameed",
            name = "Abdul Hameed",
            translator = "Cheriyamundam Abdul Hameed",
            description = "Malayalam translation.",
            language = "Malayalam",
            url = "https://api.alquran.cloud/v1/quran/ml.abdulhameed"
        ),
        TranslationOption(
            key = "ur.jalandhry",
            name = "Fateh Muhammad Jalandhari",
            translator = "Fateh Muhammad Jalandhari",
            description = "Urdu - A widely accepted classic Urdu translation.",
            language = "Urdu",
            url = "https://api.alquran.cloud/v1/quran/ur.jalandhry"
        ),
        TranslationOption(
            key = "ur.maududi",
            name = "Tafheem ul Quran",
            translator = "Abul A'ala Maududi",
            description = "Urdu translation by Maududi.",
            language = "Urdu",
            url = "https://api.alquran.cloud/v1/quran/ur.maududi"
        ),
        TranslationOption(
            key = "tr.diyanet",
            name = "Diyanet İşleri",
            translator = "Diyanet İşleri",
            description = "Turkish translation by Diyanet İşleri.",
            language = "Turkish",
            url = "https://api.alquran.cloud/v1/quran/tr.diyanet"
        )
    )

    fun isDownloaded(key: String): Boolean {
        if (key == "default") return true
        val file = File(translationsDir, "$key.json")
        return file.exists() && file.length() > 1000
    }

    fun selectTranslation(key: String) {
        prefs.edit().putString("selected_translation", key).apply()
        _selectedTranslation.value = key
    }

    private var cachedJson: JSONObject? = null
    private var cachedKey: String? = null
    private val mutex = kotlinx.coroutines.sync.Mutex()

    suspend fun getTranslationText(key: String, surahId: Int, ayahId: Int): String? = withContext(Dispatchers.IO) {
        if (key == "default") return@withContext null
        try {
            mutex.lock()
            try {
                if (cachedKey != key || cachedJson == null) {
                    val file = File(translationsDir, "$key.json")
                    if (!file.exists()) return@withContext null
                    val content = file.readText()
                    cachedJson = JSONObject(content)
                    cachedKey = key
                }
            } finally {
                mutex.unlock()
            }
            
            val dataObj = cachedJson?.optJSONObject("data")
            if (dataObj != null) {
                val surahsArr = dataObj.optJSONArray("surahs")
                val surahObj = surahsArr?.optJSONObject(surahId - 1)
                val ayahsArr = surahObj?.optJSONArray("ayahs")
                val ayahObj = ayahsArr?.optJSONObject(ayahId - 1)
                return@withContext ayahObj?.optString("text")
            } else {
                val surahObj = cachedJson?.optJSONObject(surahId.toString()) ?: return@withContext null
                val ayahsObj = surahObj.optJSONObject("Ayahs") ?: return@withContext null
                val ayahObj = ayahsObj.optJSONObject(ayahId.toString()) ?: return@withContext null
                val keys = ayahObj.keys()
                if (keys.hasNext()) {
                    val transKey = keys.next()
                    return@withContext ayahObj.optString(transKey)
                }
            }
        } catch (e: Exception) {
            CrashReporter.report(e)
        }
        null
    }

    suspend fun downloadTranslation(
        key: String,
        onProgress: (Float) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val option = availableTranslations.find { it.key == key }
        if (option == null || option.url.isEmpty()) {
            withContext(Dispatchers.Main) {
                onError("Invalid translation option selection.")
            }
            return@withContext
        }

        val client = OkHttpClient()
        val request = Request.Builder().url(option.url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onError("Download failed: HTTP ${response.code}")
                    }
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    withContext(Dispatchers.Main) {
                        onError("Download failed: empty response body")
                    }
                    return@withContext
                }

                val totalBytes = body.contentLength()
                val tempFile = File(translationsDir, "$key.tmp")
                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                var lastNotifiedPercent = -1
                
                FileOutputStream(tempFile).use { output ->
                    body.byteStream().use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (totalBytes > 0) {
                                val progressVal = bytesRead.toFloat() / totalBytes.toFloat()
                                val percent = (progressVal * 100).toInt()
                                if (percent != lastNotifiedPercent) {
                                    lastNotifiedPercent = percent
                                    withContext(Dispatchers.Main) {
                                        onProgress(progressVal)
                                    }
                                }
                            }
                        }
                    }
                }

                val targetFile = File(translationsDir, "$key.json")
                if (tempFile.renameTo(targetFile)) {
                    // Pre-warm the cache
                    val content = targetFile.readText()
                    val newObj = JSONObject(content)
                    mutex.lock()
                    try {
                        cachedJson = newObj
                        cachedKey = key
                    } finally {
                        mutex.unlock()
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Failed to finalize downloaded file.")
                    }
                }
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                onError("Network error: ${e.message ?: "unknown error"}")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Processing error: ${e.message ?: "unknown error"}")
            }
        }
    }
}
