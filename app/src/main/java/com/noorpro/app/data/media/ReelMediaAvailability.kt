package com.noorpro.app.data.media

import android.content.Context
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Probes each video post's media once (a 2-byte ranged GET) so posts whose file is
 * permanently unreachable on the server are hidden instead of rendering a black / gradient
 * card or "Couldn't play this reel". Also resolves `gs://` and bare storage paths through
 * Firebase Storage. Dead results are remembered for [DEAD_TTL_MS] so they stay hidden on the
 * next launch without a flash, then are re-checked (e.g. once Storage billing is restored).
 * Nothing on the server is modified.
 */
object ReelMediaAvailability {
    private const val TAG = "NoorReels"
    private const val PREFS = "reel_media_availability"
    private const val DEAD_TTL_MS = 6 * 60 * 60 * 1000L
    private const val OK_TTL_MS = 30 * 60 * 1000L

    private data class Entry(val state: MediaUrls.Reachability, val at: Long)

    private val results = ConcurrentHashMap<String, Entry>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val resolved = ConcurrentHashMap<String, String>()
    private val executor = Executors.newFixedThreadPool(3) { r -> Thread(r, "reel-probe").apply { isDaemon = true } }
    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        prefs.all.forEach { (key, value) ->
            val at = value as? Long ?: return@forEach
            if (now - at < DEAD_TTL_MS) results[key] = Entry(MediaUrls.Reachability.DEAD, at)
        }
    }

    /** A gs:// / storage-path reference resolved to its https download URL, if known. */
    fun resolvedUrl(ref: String): String? = resolved[ref]

    /** True when every playable URL of the post is known to be permanently unreachable. */
    fun isDead(urls: List<String>): Boolean {
        val candidates = urls.filter { it.isNotBlank() }
        if (candidates.isEmpty()) return false
        return candidates.all { results[it]?.state == MediaUrls.Reachability.DEAD }
    }

    /**
     * Start probes for any URL not checked recently. [onChanged] runs (on a background thread)
     * when a result changes what should be shown.
     */
    fun ensureChecked(urls: List<String>, onChanged: () -> Unit) {
        val now = System.currentTimeMillis()
        urls.filter { it.isNotBlank() }.forEach { url ->
            val known = results[url]
            val fresh = known != null && now - known.at < (if (known.state == MediaUrls.Reachability.DEAD) DEAD_TTL_MS else OK_TTL_MS)
            if (fresh || !inFlight.add(url)) return@forEach
            executor.execute {
                try {
                    val state = probe(url)
                    val before = results[url]?.state
                    results[url] = Entry(state, System.currentTimeMillis())
                    persist(url, state)
                    if (before != state) onChanged()
                } finally {
                    inFlight.remove(url)
                }
            }
        }
    }

    /** Resolve a gs:// URI or storage path; [onResolved] fires once with the https URL. */
    fun resolveStorageRef(ref: String, onResolved: () -> Unit) {
        if (resolved.containsKey(ref) || !inFlight.add("resolve:$ref")) return
        runCatching {
            val storage = FirebaseStorage.getInstance()
            val sRef = if (MediaUrls.isGsUri(ref)) storage.getReferenceFromUrl(ref) else storage.reference.child(ref)
            sRef.downloadUrl
                .addOnSuccessListener { uri ->
                    resolved[ref] = uri.toString()
                    inFlight.remove("resolve:$ref")
                    onResolved()
                }
                .addOnFailureListener { e ->
                    inFlight.remove("resolve:$ref")
                    Log.w(TAG, "getDownloadUrl failed ref=$ref: ${e.javaClass.simpleName}: ${e.message}")
                }
        }.onFailure {
            inFlight.remove("resolve:$ref")
            Log.w(TAG, "storage ref invalid ref=$ref: ${it.message}")
        }
    }

    private fun probe(url: String): MediaUrls.Reachability {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                instanceFollowRedirects = true
                setRequestProperty("Range", "bytes=0-1")
            }
            val code = conn.responseCode
            val state = MediaUrls.classifyHttp(code)
            if (state == MediaUrls.Reachability.DEAD) {
                Log.w(TAG, "media unavailable http=$code url=${MediaUrls.redact(url)}; hiding post")
            }
            state
        } catch (e: Exception) {
            Log.i(TAG, "probe inconclusive url=${MediaUrls.redact(url)}: ${e.javaClass.simpleName}")
            MediaUrls.Reachability.UNKNOWN
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun persist(url: String, state: MediaUrls.Reachability) {
        val prefs = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        if (state == MediaUrls.Reachability.DEAD) {
            prefs.edit().putLong(url, System.currentTimeMillis()).apply()
        } else if (state == MediaUrls.Reachability.OK && prefs.contains(url)) {
            prefs.edit().remove(url).apply()
        }
    }
}
