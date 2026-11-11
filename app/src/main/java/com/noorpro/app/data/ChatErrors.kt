package com.noorpro.app.data

/** 1.1.9: shared helpers for the 1:1 chat PERMISSION_DENIED fix (pure, unit-tested). */
object ChatErrors {
    const val MAX_RETRIES = 30

    fun isPermissionDenied(e: Throwable?): Boolean {
        if (e == null) return false
        val code = runCatching { (e as? com.google.firebase.firestore.FirebaseFirestoreException)?.code?.name }.getOrNull()
        return code == "PERMISSION_DENIED" || e.message.orEmpty().contains("PERMISSION_DENIED")
    }

    /** 2 s, 3 s, 4 s … capped at 10 s. */
    fun retryDelayMs(attempt: Int): Long = (2_000L + attempt * 1_000L).coerceAtMost(10_000L)

    /** Never show raw Firestore codes in the chat; keep other messages. */
    fun friendly(message: String?, fallback: String?): String? = when {
        message == null -> fallback
        message.contains("PERMISSION_DENIED") || message.contains("insufficient permissions", ignoreCase = true) ->
            "This chat isn't available right now. Check your connection and try again."
        else -> message
    }

    /** Chat document fields for a send; the member list is left out on the retry path. */
    fun chatMeta(withMembers: Boolean, members: List<String>, names: Map<String, String>, rest: Map<String, Any>): Map<String, Any> =
        if (withMembers) mapOf("participantUids" to members, "participantNames" to names) + rest
        else rest
}
