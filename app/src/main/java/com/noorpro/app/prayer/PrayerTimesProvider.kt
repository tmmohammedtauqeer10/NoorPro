package com.noorpro.app.prayer

import android.content.Context
import com.noorpro.app.data.OfflinePrayerCalculator
import com.noorpro.app.ui.viewmodel.CalculationMethod
import com.noorpro.app.ui.viewmodel.Madhab
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Single source of prayer times for widgets, notifications and alarms so they all agree with the
 * Prayer screen: AlAdhan timings cached by the app for today, else AlAdhan fetched/cached here,
 * else the offline calculator.
 */
object PrayerTimesProvider {
    private val NAMES = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
    private const val CACHE_PREFS = "prayer_times_cache"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    fun timesFor(
        context: Context,
        date: Date,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        madhab: Madhab,
        utcOffsetHours: Double,
        allowNetwork: Boolean,
    ): Map<String, String> {
        val app = context.applicationContext
        if (isSameDay(date, Date())) {
            appScreenCache(app)?.let { return it }
        }
        val key = cacheKey(date, latitude, longitude, method, madhab)
        val cache = app.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
        cache.getString(key, null)?.let { parseTimings(it) }?.let { return it }
        if (allowNetwork) {
            fetch(date, latitude, longitude, method, madhab)?.let { body ->
                val parsed = parseTimings(body)
                if (parsed != null) {
                    // Keep the cache small: drop everything except the freshly fetched entry.
                    cache.edit().clear().putString(key, body).apply()
                    return parsed
                }
            }
        }
        return OfflinePrayerCalculator.calculate(date, latitude, longitude, utcOffsetHours, method, madhab)
    }

    /** Today's timings exactly as the Prayer screen cached them (same key format as DeenViewModel). */
    private fun appScreenCache(context: Context): Map<String, String>? = runCatching {
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val body = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("cached_prayer_data_$today", null) ?: return null
        parseTimings(body)
    }.getOrNull()

    internal fun parseTimings(body: String): Map<String, String>? = runCatching {
        val timings = JSONObject(body).getJSONObject("data").getJSONObject("timings")
        val out = LinkedHashMap<String, String>()
        for (name in NAMES) {
            val raw = timings.getString(name).trim().take(5)
            if (!Regex("\\d{1,2}:\\d{2}").matches(raw)) return null
            out[name] = raw
        }
        out as Map<String, String>
    }.getOrNull()

    private fun fetch(date: Date, lat: Double, lng: Double, method: CalculationMethod, madhab: Madhab): String? = runCatching {
        val url = HttpUrl.Builder()
            .scheme("https").host("api.aladhan.com")
            .addPathSegments("v1/timings")
            .addPathSegment(SimpleDateFormat("dd-MM-yyyy", Locale.US).format(date))
            .addQueryParameter("latitude", lat.toString())
            .addQueryParameter("longitude", lng.toString())
            .addQueryParameter("method", method.aladhanId.toString())
            .addQueryParameter("shafaq", "general")
            .addQueryParameter("school", if (madhab == Madhab.HANAFI) "1" else "0")
            .addQueryParameter("midnightMode", "0")
            .addQueryParameter("latitudeAdjustmentMethod", "1")
            .addQueryParameter("calendarMethod", "UAQ")
            .addQueryParameter("iso8601", "false")
            .build()
        client.newCall(Request.Builder().url(url).header("Accept-Encoding", "").build()).execute().use { r ->
            if (r.isSuccessful) r.body?.string() else null
        }
    }.getOrNull()

    private fun cacheKey(date: Date, lat: Double, lng: Double, method: CalculationMethod, madhab: Madhab): String =
        String.format(
            Locale.US, "%s|%s|%s|%.2f|%.2f",
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date), method.name, madhab.name, lat, lng
        )

    private fun isSameDay(a: Date, b: Date): Boolean {
        val ca = Calendar.getInstance().apply { time = a }
        val cb = Calendar.getInstance().apply { time = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}
