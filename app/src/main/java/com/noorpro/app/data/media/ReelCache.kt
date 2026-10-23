package com.noorpro.app.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Shared on-disk reel cache (LRU, 256 MB) used by the reels player through [CacheDataSource], plus
 * a small prefetcher that warms the first bytes of the next reels so swiping starts instantly.
 */
@OptIn(UnstableApi::class)
object ReelCache {
    private const val MAX_BYTES = 256L * 1024 * 1024
    private const val PREFETCH_BYTES = 1_500_000L
    @Volatile private var cache: SimpleCache? = null
    private val prefetchExecutor = Executors.newFixedThreadPool(2) { r -> Thread(r, "reel-prefetch").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } }
    private val prefetched = ConcurrentHashMap.newKeySet<String>()
    private val geometry = ConcurrentHashMap<String, Float>()
    private val geometryInFlight = ConcurrentHashMap.newKeySet<String>()

    @Synchronized
    fun cache(context: Context): SimpleCache = cache ?: SimpleCache(
        File(context.applicationContext.cacheDir, "reel_media"),
        LeastRecentlyUsedCacheEvictor(MAX_BYTES),
        StandaloneDatabaseProvider(context.applicationContext)
    ).also { cache = it }

    fun dataSourceFactory(context: Context): CacheDataSource.Factory {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(12_000)
        return CacheDataSource.Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context.applicationContext, http))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Warm the first ~1.5 MB of each progressive (MP4) URL. HLS manifests are skipped. */
    fun prefetch(context: Context, urls: List<String>) {
        val app = context.applicationContext
        urls.filter { MediaUrls.isHttp(it) && !it.substringBefore('?').endsWith(".m3u8") }
            .forEach { url ->
                if (!prefetched.add(url)) return@forEach
                prefetchExecutor.execute {
                    runCatching {
                        val source = dataSourceFactory(app).createDataSource()
                        CacheWriter(source, DataSpec.Builder().setUri(Uri.parse(url)).setLength(PREFETCH_BYTES).build(), null, null).cache()
                    }.onFailure { prefetched.remove(url) }
                }
            }
    }

    /** Displayed aspect of the source MP4 (rotation applied), read from the container header only. */
    fun knownSourceAspect(url: String): Float? = geometry[url]

    fun probeSourceAspect(url: String, onResult: (Float) -> Unit) {
        geometry[url]?.let { onResult(it); return }
        if (!MediaUrls.isHttp(url) || !geometryInFlight.add(url)) return
        prefetchExecutor.execute {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(url, HashMap())
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val r = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (w > 0 && h > 0) {
                    val aspect = if (r % 180 != 0) h.toFloat() / w else w.toFloat() / h
                    geometry[url] = aspect
                    onResult(aspect)
                }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
                geometryInFlight.remove(url)
            }
        }
    }
}

/** One view per member per reel, remembered across launches (not just per session). */
object ReelViewLedger {
    // v2 ignores receipts written by older builds before the Firestore write had succeeded.
    private const val PREFS = "reel_views_by_user_v2"
    private const val MAX = 5_000

    fun hasViewed(context: Context, uid: String, postId: String): Boolean {
        if (postId.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return postId in prefs.getStringSet(key(uid), emptySet()).orEmpty()
    }

    fun markViewed(context: Context, uid: String, postId: String): Boolean {
        if (postId.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = key(uid)
        val seen = prefs.getStringSet(key, emptySet()).orEmpty()
        if (postId in seen) return false
        val next = (seen + postId).let { if (it.size > MAX) it.drop(it.size - MAX).toSet() else it }
        prefs.edit().putStringSet(key, next).apply()
        return true
    }

    private fun key(uid: String) = "v_" + uid.ifBlank { "guest" }
}
