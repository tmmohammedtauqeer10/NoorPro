package com.noorpro.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

/** 1.0.78: WhatsApp-style realtime chat signals - presence, typing, delivered/read receipts,
 *  and "delete for me". Everything lives in Firestore under additive, owner-scoped rules. */
data class ChatMeta(
    val typingAt: Map<String, Long> = emptyMap(),
    val readAt: Map<String, Long> = emptyMap(),
    val deliveredAt: Map<String, Long> = emptyMap()
) {
    fun isTyping(uid: String, now: Long = System.currentTimeMillis()): Boolean =
        (typingAt[uid] ?: 0L).let { it > 0 && now - it < TYPING_TTL_MS }

    /** 1 = sent, 2 = delivered, 3 = read for a message I sent at [createdAt]. */
    fun tickFor(otherUid: String, createdAt: Long): Int = when {
        createdAt <= 0L -> 0
        (readAt[otherUid] ?: 0L) >= createdAt -> 3
        (deliveredAt[otherUid] ?: 0L) >= createdAt -> 2
        else -> 1
    }

    companion object {
        const val TYPING_TTL_MS = 6_000L
        fun fromFields(data: Map<String, Any?>): ChatMeta {
            fun collect(prefix: String): Map<String, Long> = data.entries
                .filter { it.key.startsWith(prefix) }
                .mapNotNull { (k, v) ->
                    val ms = when (v) {
                        is com.google.firebase.Timestamp -> v.toDate().time
                        is Number -> v.toLong()
                        else -> null
                    } ?: return@mapNotNull null
                    k.removePrefix(prefix) to ms
                }.toMap()
            return ChatMeta(collect("typing_"), collect("readAt_"), collect("deliveredAt_"))
        }
    }
}

data class Presence(val online: Boolean, val lastSeen: Long) {
    /** Treat a stale "online" (app killed without going offline) as offline after 2 minutes. */
    fun effectiveOnline(now: Long = System.currentTimeMillis()) = online && now - lastSeen < 120_000L
}

object ChatRealtime {
    private val db get() = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    fun setOnline(online: Boolean) {
        val me = uid ?: return
        db.collection("ummah_presence").document(me)
            .set(mapOf("online" to online, "lastSeen" to FieldValue.serverTimestamp()))
    }

    fun observePresence(otherUid: String, onResult: (Presence?) -> Unit): ListenerRegistration? {
        if (otherUid.isBlank() || uid == null) return null
        return db.collection("ummah_presence").document(otherUid).addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) { onResult(null); return@addSnapshotListener }
            onResult(Presence(snap.getBoolean("online") == true, snap.getTimestamp("lastSeen")?.toDate()?.time ?: 0L))
        }
    }

    fun observeChatMeta(chatId: String, onResult: (ChatMeta) -> Unit): ListenerRegistration? {
        if (chatId.isBlank() || uid == null) return null
        return db.collection("ummah_chats").document(chatId).addSnapshotListener { snap, _ ->
            onResult(ChatMeta.fromFields(snap?.getData(com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).orEmpty()))
        }
    }

    private var lastTypingPing = 0L
    /** Throttled: at most one write every 3 s while typing; a 0 clears it. */
    fun setTyping(chatId: String, typing: Boolean) {
        val me = uid ?: return
        if (chatId.isBlank()) return
        val now = System.currentTimeMillis()
        if (typing && now - lastTypingPing < 3_000L) return
        lastTypingPing = if (typing) now else 0L
        db.collection("ummah_chats").document(chatId)
            .set(mapOf("typing_$me" to if (typing) now else 0L), SetOptions.merge())
    }

    fun markRead(chatId: String) {
        val me = uid ?: return
        if (chatId.isBlank()) return
        db.collection("ummah_chats").document(chatId).update(
            mapOf("unread_$me" to 0L, "readAt_$me" to FieldValue.serverTimestamp(), "deliveredAt_$me" to FieldValue.serverTimestamp())
        )
    }

    fun markDelivered(chatId: String) {
        val me = uid ?: return
        if (chatId.isBlank()) return
        db.collection("ummah_chats").document(chatId).update("deliveredAt_$me", FieldValue.serverTimestamp())
    }

    fun hideForMe(chatId: String, messageId: String, onResult: (Boolean) -> Unit = {}) {
        val me = uid ?: return onResult(false)
        if (chatId.isBlank() || messageId.isBlank()) return onResult(false)
        db.collection("ummah_chats").document(chatId).collection("messages").document(messageId)
            .update("hiddenFor", FieldValue.arrayUnion(me))
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    /** Store this device's FCM token so incoming calls can ring it. */
    fun registerPushToken() {
        val me = uid ?: return
        runCatching {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (token.isNullOrBlank()) return@addOnSuccessListener
                db.collection("ummah_device_tokens").document(me).collection("tokens").document(token)
                    .set(mapOf("token" to token, "platform" to "android", "updatedAt" to FieldValue.serverTimestamp()))
            }
        }
    }

    fun lastSeenLabel(p: Presence?, now: Long = System.currentTimeMillis()): String {
        if (p == null) return ""
        if (p.effectiveOnline(now)) return "online"
        if (p.lastSeen <= 0) return ""
        val diff = now - p.lastSeen
        val time = android.text.format.DateFormat.format("h:mm a", p.lastSeen).toString()
        return when {
            diff < 60_000L -> "last seen just now"
            android.text.format.DateUtils.isToday(p.lastSeen) -> "last seen today at $time"
            android.text.format.DateUtils.isToday(p.lastSeen + 86_400_000L) -> "last seen yesterday at $time"
            else -> "last seen " + android.text.format.DateFormat.format("d MMM", p.lastSeen)
        }
    }
}
