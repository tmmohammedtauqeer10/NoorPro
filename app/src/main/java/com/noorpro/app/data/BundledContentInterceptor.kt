package com.noorpro.app.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.GZIPInputStream

/**
 * Offline-first: answers static content requests (hadith, tafsir, word-by-word) from compressed
 * assets bundled in the app. Anything not bundled falls through to the network (and its disk cache).
 *
 * Asset layout (gzip JSON, ".jgz" so AGP leaves it alone):
 *  hadith/info.jgz                     trimmed hadith-api info.json (sections per book)
 *  hadith/{edition}/{section}.jgz      {"hadiths":[{"hadithnumber","text"}]} e.g. ara-bukhari, eng-muslim, urd-nasai
 *  tafsir/{tafsirId}/{surah}.jgz       ["text for ayah 1", ...] (grouped ayahs carry the group's text)
 *  wbw/{surah}.jgz                     {"ayah": [[text_uthmani, translation], ...]}
 */
class BundledContentInterceptor(context: Context) : Interceptor {
    private val app = context.applicationContext
    private val json = "application/json; charset=utf-8".toMediaType()

    private val hadithSection = Regex("^/gh/fawazahmed0/hadith-api@1/editions/([a-z]+-[a-z0-9]+)/sections/(\\d+)(?:\\.min)?\\.json$")
    private val tafsirAyah = Regex("^/api/v4/tafsirs/(\\d+)/by_ayah/(\\d+):(\\d+)$")
    private val wordsAyah = Regex("^/api/v4/verses/by_key/(\\d+):(\\d+)$")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method == "GET") {
            val body = try { bundledBody(request.url.host, request.url.encodedPath, request.url.queryParameter("words")) } catch (_: Exception) { null }
            if (body != null) {
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK (bundled)")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(body.toResponseBody(json))
                    .build()
            }
        }
        return chain.proceed(request)
    }

    private fun read(path: String): String? = try {
        GZIPInputStream(app.assets.open(path), 32 * 1024).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (_: java.io.IOException) {
        null
    }

    private fun bundledBody(host: String, path: String, words: String?): String? {
        when (host) {
            "cdn.jsdelivr.net" -> {
                if (path == "/gh/fawazahmed0/hadith-api@1/info.json" || path == "/gh/fawazahmed0/hadith-api@1/info.min.json") {
                    return read("hadith/info.jgz")
                }
                val m = hadithSection.matchEntire(path) ?: return null
                return read("hadith/${m.groupValues[1]}/${m.groupValues[2]}.jgz")
            }
            "api.quran.com" -> {
                tafsirAyah.matchEntire(path)?.let { m ->
                    val arr = read("tafsir/${m.groupValues[1]}/${m.groupValues[2]}.jgz")?.let { JSONArray(it) } ?: return null
                    val ayah = m.groupValues[3].toInt()
                    if (ayah < 1 || ayah > arr.length()) return null
                    return JSONObject().put("tafsir", JSONObject().put("text", arr.optString(ayah - 1))).toString()
                }
                wordsAyah.matchEntire(path)?.let { m ->
                    if (words != "true") return null
                    val surah = read("wbw/${m.groupValues[1]}.jgz")?.let { JSONObject(it) } ?: return null
                    val rows = surah.optJSONArray(m.groupValues[2]) ?: return null
                    val out = JSONArray()
                    for (i in 0 until rows.length()) {
                        val r = rows.getJSONArray(i)
                        out.put(JSONObject().put("text_uthmani", r.optString(0)).put("translation", JSONObject().put("text", r.optString(1))))
                    }
                    return JSONObject().put("verse", JSONObject().put("verse_key", "${m.groupValues[1]}:${m.groupValues[2]}").put("words", out)).toString()
                }
            }
        }
        return null
    }
}
