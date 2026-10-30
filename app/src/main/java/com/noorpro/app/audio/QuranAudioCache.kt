package com.noorpro.app.audio

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Optional per-surah download cache for streamed ayah audio. Audio is never bundled in the APK;
 * the user can save a surah for the selected reciter and playback then uses the local files.
 */
object QuranAudioCache {
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    }

    /** Folder holding one reciter's saved ayahs (filesDir/quran_audio/<folder>). */
    fun reciterDir(context: Context, prefixUrl: String): File {
        val folder = prefixUrl.trimEnd('/').substringAfterLast('/').ifBlank { "reciter" }
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return File(File(context.filesDir, "quran_audio"), folder)
    }

    private fun name(surah: Int, ayah: Int) = String.format(java.util.Locale.US, "%03d%03d.mp3", surah, ayah)

    fun cachedFile(context: Context, prefixUrl: String, surah: Int, ayah: Int): File? =
        File(reciterDir(context, prefixUrl), name(surah, ayah)).takeIf { it.isFile && it.length() > 0 }

    fun isSurahCached(context: Context, prefixUrl: String, surah: Int, ayahCount: Int): Boolean =
        ayahCount > 0 && (1..ayahCount).all { cachedFile(context, prefixUrl, surah, it) != null }

    /** Downloads every ayah of [surah]; returns true when the whole surah is available offline. */
    fun downloadSurah(context: Context, prefixUrl: String, surah: Int, ayahCount: Int, onProgress: (Float) -> Unit): Boolean {
        val dir = reciterDir(context, prefixUrl).apply { mkdirs() }
        var ok = true
        for (ayah in 1..ayahCount) {
            val target = File(dir, name(surah, ayah))
            if (!(target.isFile && target.length() > 0)) {
                val tmp = File(dir, name(surah, ayah) + ".part")
                val success = runCatching {
                    client.newCall(Request.Builder().url(prefixUrl + name(surah, ayah)).build()).execute().use { r ->
                        if (!r.isSuccessful) return@use false
                        val body = r.body ?: return@use false
                        tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                        tmp.length() > 0 && tmp.renameTo(target)
                    }
                }.getOrDefault(false)
                if (!success) { tmp.delete(); ok = false }
            }
            onProgress(ayah.toFloat() / ayahCount)
        }
        return ok
    }

    fun deleteSurah(context: Context, prefixUrl: String, surah: Int) {
        reciterDir(context, prefixUrl).listFiles()?.filter { it.name.startsWith(String.format(java.util.Locale.US, "%03d", surah)) }
            ?.forEach { it.delete() }
    }

    /** 1.1.6: removes every saved ayah of one reciter. */
    fun deleteReciter(context: Context, prefixUrl: String) {
        reciterDir(context, prefixUrl).deleteRecursively()
    }
}
