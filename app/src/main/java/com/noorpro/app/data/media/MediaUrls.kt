package com.noorpro.app.data.media

/**
 * Pure helpers that normalise the many shapes a post's video reference has taken over the
 * app's history (Firebase Storage download URLs, `gs://` URIs, bare storage paths, CloudFront
 * MP4/HLS, legacy field names). Kept free of Android/Firebase types so it is unit-testable.
 */
object MediaUrls {
    /** Field names older builds / tools wrote the playable media URL under, in priority order. */
    val LEGACY_MEDIA_FIELDS = listOf("mediaUrl", "videoUrl", "videoURL", "mediaURL", "downloadUrl", "url", "fileUrl")
    val LEGACY_THUMB_FIELDS = listOf("thumbnailUrl", "thumbUrl", "thumbnail", "posterUrl", "coverUrl")

    private val HTTPS_UPGRADE_HOSTS = listOf(
        "firebasestorage.googleapis.com", "storage.googleapis.com", ".cloudfront.net", ".amazonaws.com"
    )

    /** First non-blank value among [fields] read through [get]. */
    fun firstNonBlank(fields: List<String>, get: (String) -> String?): String =
        fields.asSequence().mapNotNull { get(it)?.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()

    fun isGsUri(url: String): Boolean = url.startsWith("gs://", ignoreCase = true)

    fun isHttp(url: String): Boolean =
        url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)

    /**
     * The storage object path that must be resolved through Firebase Storage `getDownloadUrl`,
     * or null when [url] is already directly playable. A blank URL with a [mediaPath] or a
     * `gs://` URL both need resolving.
     */
    fun storagePathToResolve(url: String, mediaPath: String): String? = when {
        isGsUri(url) -> url
        url.isBlank() && mediaPath.isNotBlank() -> mediaPath.trim().trimStart('/')
        else -> null
    }

    /**
     * Cleartext is blocked on Android 9+, so legacy `http://` links to hosts that also serve
     * HTTPS (Firebase Storage, GCS, CloudFront, S3) are upgraded. Other values pass through.
     */
    fun normalize(url: String): String {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true)) return trimmed
        val host = trimmed.substring(7).substringBefore('/').substringBefore(':').lowercase()
        val upgrade = HTTPS_UPGRADE_HOSTS.any { if (it.startsWith(".")) host.endsWith(it) else host == it }
        return if (upgrade) "https://" + trimmed.substring(7) else trimmed
    }

    /** URL safe to write to logs: drops the query (download tokens / signatures). */
    fun redact(url: String): String {
        val q = url.indexOf('?')
        return if (q < 0) url else url.substring(0, q) + "?<redacted>"
    }

    fun looksLikeVideo(type: String, url: String): Boolean {
        val t = type.lowercase()
        if (t == "reel" || t == "video" || t == "short") return true
        val path = url.substringBefore('?').lowercase()
        return path.endsWith(".mp4") || path.endsWith(".m3u8") || path.endsWith(".mov") ||
            path.endsWith(".webm") || path.contains("/videos/") || path.contains("%2fvideos%2f")
    }

    enum class Reachability { OK, DEAD, UNKNOWN }

    /**
     * Classify a probe's HTTP status. 401/402/403/404/410 mean the object can never play for
     * this client (missing file, revoked token, or Firebase Storage disabled for billing);
     * anything else (5xx, timeouts) is treated as transient.
     */
    fun classifyHttp(code: Int): Reachability = when (code) {
        in 200..399 -> Reachability.OK
        401, 402, 403, 404, 410 -> Reachability.DEAD
        else -> Reachability.UNKNOWN
    }
}
