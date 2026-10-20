package com.noorpro.app.data

import android.content.Context
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Disk cache for static, read-only content APIs (hadith, tafsir, word-by-word, Quran text fallback).
 * Anything opened once stays readable offline: on network failure the cached copy is served.
 */
object OfflineHttpCache {
    private val staticHosts = setOf("cdn.jsdelivr.net", "api.quran.com", "api.alquran.cloud", "raw.githubusercontent.com")

    fun install(context: Context, builder: OkHttpClient.Builder): OkHttpClient.Builder {
        val dir = File(context.applicationContext.cacheDir, "static_http")
        return builder
            .cache(Cache(dir, 30L * 1024 * 1024))
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val isStatic = request.method == "GET" && request.url.host in staticHosts
                try {
                    chain.proceed(request)
                } catch (e: IOException) {
                    if (!isStatic) throw e
                    val cached = chain.proceed(
                        request.newBuilder()
                            .cacheControl(CacheControl.Builder().onlyIfCached().maxStale(3650, TimeUnit.DAYS).build())
                            .build()
                    )
                    if (cached.code == 504) { cached.close(); throw e }
                    cached
                }
            })
            .addNetworkInterceptor(Interceptor { chain ->
                val response = chain.proceed(chain.request())
                if (chain.request().method == "GET" && chain.request().url.host in staticHosts && response.isSuccessful) {
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, max-age=3600")
                        .build()
                } else response
            })
    }
}
