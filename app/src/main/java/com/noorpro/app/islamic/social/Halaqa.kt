package com.noorpro.app.islamic.social

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.noorpro.app.data.FirebaseGate
import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.community.CommunityException
import com.noorpro.app.islamic.community.CommunityText
import com.noorpro.app.islamic.community.LocalReminder
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

/*
 * 1.1.6 Halaqa rooms (halaqa_rooms_enabled): live audio circles for Quran, dars and naat.
 * Room state (roles, raised hands, mute) lives in Firestore; audio is a small WebRTC mesh that
 * reuses the 1:1 call stack (CallConfig ICE servers, offer/answer + candidate docs). No TURN relay
 * is configured yet, so peers on strict/symmetric NATs can't connect — the UI says so calmly.
 */

enum class HalaqaTopic(val label: String, val blurb: String) {
    QURAN("Quran", "Recitation & tajweed"), DARS("Dars", "A short lesson"), NAAT("Naat & nasheed", "Voices only"),
    DHIKR("Dhikr", "Remembrance together"), QA("Q&A", "Questions & answers");
    companion object { fun of(s: String?) = entries.firstOrNull { it.name == s } ?: DARS }
}

enum class HalaqaRole { HOST, SPEAKER, LISTENER;
    val onStage get() = this != LISTENER
    companion object { fun of(s: String?) = when (s) { "host" -> HOST; "speaker" -> SPEAKER; else -> LISTENER } }
    val key get() = name.lowercase()
}

enum class HalaqaStatus { SCHEDULED, LIVE, ENDED; companion object { fun of(s: String?) = when (s) { "live" -> LIVE; "ended" -> ENDED; else -> SCHEDULED } } }

data class HalaqaRoom(
    val id: String,
    val title: String,
    val topic: HalaqaTopic = HalaqaTopic.DARS,
    val description: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val status: HalaqaStatus = HalaqaStatus.SCHEDULED,
    val scheduledAt: Long = 0,
)

data class HalaqaMember(val uid: String, val name: String, val role: HalaqaRole = HalaqaRole.LISTENER, val hand: Boolean = false, val muted: Boolean = true, val joinedAt: Long = 0)

/** Connection health of the whole room (worst link wins). */
enum class HalaqaLink { IDLE, CONNECTING, CONNECTED, NEEDS_RELAY, FAILED }

object HalaqaLogic {
    const val MAX_MEMBERS = 30
    const val MAX_SPEAKERS = 6
    const val REMIND_BEFORE_MIN = 10

    /** Pairs that need an audio link: anyone ↔ anyone on stage (listeners never link to listeners). */
    fun neededPeers(me: HalaqaMember, members: List<HalaqaMember>): List<String> =
        members.filter { it.uid != me.uid && (it.role.onStage || me.role.onStage) }.map { it.uid }.sorted()

    /** The lower uid always makes the offer, so both sides agree without extra signalling. */
    fun isOfferer(me: String, other: String) = me < other
    fun linkId(a: String, b: String) = if (a < b) "${a}_$b" else "${b}_$a"

    /** My microphone is live only on stage and unmuted. */
    fun micLive(m: HalaqaMember?) = m != null && m.role.onStage && !m.muted

    fun canJoin(members: List<HalaqaMember>, uid: String) = members.any { it.uid == uid } || members.size < MAX_MEMBERS
    fun canInvite(members: List<HalaqaMember>) = members.count { it.role.onStage } < MAX_SPEAKERS

    fun stage(members: List<HalaqaMember>) = members.filter { it.role.onStage }.sortedWith(compareBy({ it.role != HalaqaRole.HOST }, { it.joinedAt }))
    fun audience(members: List<HalaqaMember>) = members.filter { !it.role.onStage }.sortedWith(compareBy({ !it.hand }, { it.joinedAt }))
    fun hands(members: List<HalaqaMember>) = members.filter { !it.role.onStage && it.hand }.sortedBy { it.joinedAt }

    /** Overall link health; with no TURN relay a failed link means "needs relay" rather than a generic error. */
    fun health(states: Collection<HalaqaLink>, hasTurn: Boolean): HalaqaLink = when {
        states.isEmpty() -> HalaqaLink.IDLE
        states.any { it == HalaqaLink.FAILED || it == HalaqaLink.NEEDS_RELAY } -> if (hasTurn) HalaqaLink.FAILED else HalaqaLink.NEEDS_RELAY
        states.any { it == HalaqaLink.CONNECTING } -> HalaqaLink.CONNECTING
        else -> HalaqaLink.CONNECTED
    }

    /** Rooms worth listing: live ones from the last 12h and scheduled ones that haven't long passed. */
    fun listed(rooms: List<HalaqaRoom>, now: Long): Pair<List<HalaqaRoom>, List<HalaqaRoom>> {
        val live = rooms.filter { it.status == HalaqaStatus.LIVE && now - it.scheduledAt < 12 * 3_600_000L }.sortedByDescending { it.scheduledAt }
        val soon = rooms.filter { it.status == HalaqaStatus.SCHEDULED && it.scheduledAt > now - 3_600_000L }.sortedBy { it.scheduledAt }
        return live to soon
    }

    fun remindAt(r: HalaqaRoom) = r.scheduledAt - REMIND_BEFORE_MIN * 60_000L

    fun validateTitle(t: String): String? = when {
        t.trim().length < 4 -> "Give your halaqa a short title."
        t.length > 80 -> "Please keep the title under 80 characters."
        else -> null
    }

    /** Next occurrence of [hour]:[minute] on day offset [dayOffset] (0 = today) in [tz]. */
    fun slot(now: Long, dayOffset: Int, hour: Int, minute: Int, tz: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(tz).apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, dayOffset); set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

    fun whenLine(r: HalaqaRoom, now: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()): String {
        if (r.status == HalaqaStatus.LIVE) return "Live now"
        if (r.status == HalaqaStatus.ENDED) return "Ended"
        val f = java.text.SimpleDateFormat("h:mm a", java.util.Locale.ENGLISH).apply { timeZone = tz }
        val day = { t: Long -> Calendar.getInstance(tz).apply { timeInMillis = t }.let { it.get(Calendar.YEAR) * 400 + it.get(Calendar.DAY_OF_YEAR) } }
        val d = day(r.scheduledAt) - day(now)
        return when (d) {
            0 -> "Today, ${f.format(r.scheduledAt)}"
            1 -> "Tomorrow, ${f.format(r.scheduledAt)}"
            else -> java.text.SimpleDateFormat("EEE d MMM, h:mm a", java.util.Locale.ENGLISH).apply { timeZone = tz }.format(r.scheduledAt)
        }
    }

    fun roomFrom(id: String, d: Map<String, Any?>) = HalaqaRoom(
        id, d["title"] as? String ?: "", HalaqaTopic.of(d["topic"] as? String), d["description"] as? String ?: "",
        d["hostUid"] as? String ?: "", d["hostName"] as? String ?: "", HalaqaStatus.of(d["status"] as? String),
        (d["scheduledAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
    )

    fun memberFrom(uid: String, d: Map<String, Any?>) = HalaqaMember(
        uid, d["name"] as? String ?: "", HalaqaRole.of(d["role"] as? String), d["hand"] as? Boolean ?: false, d["muted"] as? Boolean ?: true,
        (d["joinedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
    )
}

/** Reminders for scheduled halaqas: local only (WorkManager), 10 minutes before. */
class HalaqaReminders(context: Context) {
    private val ctx = context.applicationContext
    private val p = ctx.getSharedPreferences("halaqa_reminders", Context.MODE_PRIVATE)
    fun ids(): Set<String> = p.getStringSet("ids", emptySet()).orEmpty()
    fun set(r: HalaqaRoom, on: Boolean): Set<String> {
        val next = ids().toMutableSet().apply { if (on) add(r.id) else remove(r.id) }
        p.edit().putStringSet("ids", next).apply()
        if (on) LocalReminder.schedule(ctx, "halaqa_${r.id}", HalaqaLogic.remindAt(r), "${r.title} starts in ${HalaqaLogic.REMIND_BEFORE_MIN} minutes",
            "${r.topic.label} halaqa with ${r.hostName.ifBlank { "the Ummah" }}. Tap to join.", "UMMAH_HALAQA")
        else LocalReminder.cancel(ctx, "halaqa_${r.id}")
        return next
    }
}

interface HalaqaBackend {
    val available: Boolean
    fun uid(): String?
    suspend fun rooms(): List<HalaqaRoom>
    suspend fun create(title: String, topic: HalaqaTopic, description: String, hostName: String, startAt: Long?): HalaqaRoom
    suspend fun join(room: HalaqaRoom, name: String)
    suspend fun leave(room: HalaqaRoom)
    suspend fun kick(room: HalaqaRoom, uid: String)
    suspend fun setHand(room: HalaqaRoom, up: Boolean)
    suspend fun setMuted(room: HalaqaRoom, uid: String, muted: Boolean)
    suspend fun setRole(room: HalaqaRoom, uid: String, role: HalaqaRole)
    suspend fun goLive(room: HalaqaRoom)
    suspend fun end(room: HalaqaRoom)
    fun watchMembers(room: HalaqaRoom, onChange: (List<HalaqaMember>) -> Unit): () -> Unit
    fun watchRoom(room: HalaqaRoom, onChange: (HalaqaRoom?) -> Unit): () -> Unit
}

class FirestoreHalaqaBackend : HalaqaBackend {
    private val db by lazy { FirebaseFirestore.getInstance() }
    override val available get() = FirebaseGate.ready
    override fun uid() = FirebaseGate.currentUser?.uid
    private fun me() = uid() ?: throw CommunityException(CommunityError.SIGN_IN)
    private fun rooms_() = db.collection("halaqa_rooms")
    private fun members(id: String) = rooms_().document(id).collection("members")

    override suspend fun rooms() = rooms_().orderBy("scheduledAt", Query.Direction.DESCENDING).limit(40).get().await()
        .documents.map { HalaqaLogic.roomFrom(it.id, it.data.orEmpty()) }.filter { it.status != HalaqaStatus.ENDED }

    override suspend fun create(title: String, topic: HalaqaTopic, description: String, hostName: String, startAt: Long?): HalaqaRoom {
        val uid = me()
        HalaqaLogic.validateTitle(title)?.let { throw CommunityException(CommunityError.UNKNOWN, it) }
        val t = CommunityText.prepare(title, 80); val d = CommunityText.prepare(description, 280)
        if (t.review || d.review) throw CommunityException(CommunityError.BLOCKED_TEXT, t.message.ifBlank { d.message })
        val ref = rooms_().document()
        val at = startAt ?: System.currentTimeMillis()
        ref.set(mapOf("title" to t.text, "topic" to topic.name, "description" to d.text, "hostUid" to uid, "hostName" to hostName.take(40),
            "status" to if (startAt == null) "live" else "scheduled", "scheduledAt" to com.google.firebase.Timestamp(java.util.Date(at)),
            "createdAt" to FieldValue.serverTimestamp())).await()
        return HalaqaRoom(ref.id, t.text, topic, d.text, uid, hostName, if (startAt == null) HalaqaStatus.LIVE else HalaqaStatus.SCHEDULED, at)
    }

    override suspend fun join(room: HalaqaRoom, name: String) {
        val uid = me()
        val now = members(room.id).get().await().documents.map { it.id }
        // Mesh audio doesn't scale; the cap is client-side (documented in SOCIAL_AUDIO_1.1.6.md).
        if (uid !in now && now.size >= HalaqaLogic.MAX_MEMBERS) throw CommunityException(CommunityError.FULL, "This halaqa is full (${HalaqaLogic.MAX_MEMBERS} people). Please try again later.")
        if (uid in now) return
        members(room.id).document(uid).set(mapOf("name" to name.take(40), "role" to if (room.hostUid == uid) "host" else "listener",
            "hand" to false, "muted" to true, "joinedAt" to FieldValue.serverTimestamp())).await()
    }
    override suspend fun leave(room: HalaqaRoom) { members(room.id).document(me()).delete().await() }
    override suspend fun kick(room: HalaqaRoom, uid: String) { me(); members(room.id).document(uid).delete().await() }
    override suspend fun setHand(room: HalaqaRoom, up: Boolean) { members(room.id).document(me()).update("hand", up).await() }
    override suspend fun setMuted(room: HalaqaRoom, uid: String, muted: Boolean) { me(); members(room.id).document(uid).update("muted", muted).await() }
    override suspend fun setRole(room: HalaqaRoom, uid: String, role: HalaqaRole) {
        me(); members(room.id).document(uid).update(mapOf("role" to role.key, "hand" to false, "muted" to true)).await()
    }
    override suspend fun goLive(room: HalaqaRoom) { me(); rooms_().document(room.id).update("status", "live").await() }
    override suspend fun end(room: HalaqaRoom) { me(); rooms_().document(room.id).update("status", "ended").await() }

    override fun watchMembers(room: HalaqaRoom, onChange: (List<HalaqaMember>) -> Unit): () -> Unit {
        val reg: ListenerRegistration = members(room.id).addSnapshotListener { s, _ ->
            onChange(s?.documents.orEmpty().map { HalaqaLogic.memberFrom(it.id, it.data.orEmpty()) })
        }
        return { reg.remove() }
    }
    override fun watchRoom(room: HalaqaRoom, onChange: (HalaqaRoom?) -> Unit): () -> Unit {
        val reg = rooms_().document(room.id).addSnapshotListener { s, _ -> onChange(s?.takeIf { it.exists() }?.let { HalaqaLogic.roomFrom(it.id, it.data.orEmpty()) }) }
        return { reg.remove() }
    }
}
