package com.noorpro.app.calls

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/** One ICE server entry, independent of org.webrtc so it can be unit-tested on the JVM. */
data class IceSpec(val urls: List<String>, val username: String? = null, val credential: String? = null) {
    val isTurn get() = urls.any { it.startsWith("turn:") || it.startsWith("turns:") }
}

/** Short-lived TURN credentials returned by the credential endpoint (e.g. a Cloudflare Worker). */
data class TurnCreds(val servers: List<IceSpec>, val expiresAtMs: Long) {
    fun valid(nowMs: Long) = servers.isNotEmpty() && nowMs < expiresAtMs - IceConfig.REFRESH_MARGIN_MS
}

/**
 * Remote Config driven ICE setup. Keys:
 *  - halaqa_ice_servers: JSON array or {"iceServers":[...]} of {urls, username?, credential?}; long-lived
 *    TURN secrets must NOT be put here, only STUN (or static public entries).
 *  - halaqa_turn_credentials_url: optional HTTPS endpoint that returns short-lived TURN creds in the
 *    standard {"iceServers":[...]} shape (Cloudflare generate-ice-servers format) given a Firebase ID token.
 */
object IceConfig {
    const val KEY_ICE_SERVERS = "halaqa_ice_servers"
    const val KEY_TURN_URL = "halaqa_turn_credentials_url"
    const val DEFAULT_ICE_SERVERS_JSON =
        """[{"urls":["stun:stun.l.google.com:19302","stun:stun1.l.google.com:19302"]}]"""
    val DEFAULT: List<IceSpec> = listOf(IceSpec(listOf("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302")))
    const val REFRESH_MARGIN_MS = 5 * 60_000L
    const val DEFAULT_TTL_MS = 60 * 60_000L

    private val adapter = Moshi.Builder().build().adapter<Any>(Any::class.java)

    private fun validUrl(u: String) = u.startsWith("stun:") || u.startsWith("turn:") || u.startsWith("turns:")

    /** Parses a list of ICE servers; returns null when the JSON is malformed or yields nothing usable. */
    fun parse(json: String?): List<IceSpec>? {
        if (json.isNullOrBlank()) return null
        val root = runCatching { adapter.fromJson(json) }.getOrNull() ?: return null
        val arr = when (root) {
            is List<*> -> root
            is Map<*, *> -> root["iceServers"] as? List<*> ?: return null
            else -> return null
        }
        val out = arr.mapNotNull { e ->
            val m = e as? Map<*, *> ?: return@mapNotNull null
            val urls = when (val u = m["urls"] ?: m["url"]) {
                is String -> listOf(u)
                is List<*> -> u.filterIsInstance<String>()
                else -> emptyList()
            }.map { it.trim() }.filter(::validUrl)
            if (urls.isEmpty()) return@mapNotNull null
            val spec = IceSpec(urls, m["username"] as? String, m["credential"] as? String)
            // A TURN entry without credentials can never authenticate; drop it.
            if (spec.isTurn && (spec.username.isNullOrEmpty() || spec.credential.isNullOrEmpty())) null else spec
        }
        return out.ifEmpty { null }
    }

    /** Parses a credential-endpoint response; ttl (seconds) honoured when present, else [DEFAULT_TTL_MS]. */
    fun parseCreds(json: String?, nowMs: Long): TurnCreds? {
        val servers = parse(json)?.filter { it.isTurn } ?: return null
        if (servers.isEmpty()) return null
        val ttl = runCatching { ((adapter.fromJson(json!!) as? Map<*, *>)?.get("ttl") as? Number)?.toLong() }.getOrNull()
        return TurnCreds(servers, nowMs + (ttl?.times(1000) ?: DEFAULT_TTL_MS))
    }

    /** Final list: configured servers (or STUN default) plus any still-valid short-lived TURN creds. */
    fun merge(remoteJson: String?, creds: TurnCreds?, nowMs: Long): List<IceSpec> {
        val base = parse(remoteJson) ?: DEFAULT
        return if (creds != null && creds.valid(nowMs)) base + creds.servers else base
    }
}
