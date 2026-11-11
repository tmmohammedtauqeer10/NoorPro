package com.noorpro.app.islamic.masjid

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.noorpro.app.data.FirebaseGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/*
 * 1.1.6 Masjid pages (Remote Config masjid_pages_enabled, default OFF).
 * Firestore: masjids/{id} (+ /announcements/{aid}), masjid_claims/{id}, users/{uid}/followed_masjids/{id}.
 * Rules: firestore-masjid-pages.rules (additive, NOT deployed). Until deployed, reads fail with
 * PERMISSION_DENIED and the UI shows a calm notice; the followed masjid is cached locally so its
 * jamaat times still show on the prayer screen offline.
 * NoorPro has no mosque finder yet, so directions reuse PilgrimMapProvider (Google Maps intent, no key).
 */

val JAMAAT_PRAYERS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

data class Announcement(val id: String, val text: String, val at: Long, val by: String = "")

data class Masjid(
    val id: String,
    val name: String,
    val address: String = "",
    val city: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    /** Prayer name (JAMAAT_PRAYERS) → "HH:mm" (24h). Missing = not published. */
    val jamaat: Map<String, String> = emptyMap(),
    val jummahTime: String = "",
    val jummahBayan: String = "",
    val adminUids: List<String> = emptyList(),
    val updatedAt: Long = 0,
)

enum class ClaimStatus { PENDING, APPROVED, REJECTED }

data class MasjidClaim(
    val id: String,
    val masjidId: String,
    val masjidName: String,
    val uid: String,
    val name: String,
    val role: String,
    val contact: String,
    val note: String,
    val status: ClaimStatus = ClaimStatus.PENDING,
    val at: Long = 0,
)

object MasjidLogic {
    val ROLES = listOf("Imam", "Committee member", "Volunteer admin")
    private val TIME = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")

    /** Accepts "5:30", "05:30", "17:30", "5.30", "1:15 pm" → "HH:mm"; null when unreadable. */
    fun normalizeTime(raw: String): String? {
        val s = raw.trim().lowercase().replace('.', ':').replace(" ", "")
        if (s.isEmpty()) return null
        val pm = s.endsWith("pm"); val am = s.endsWith("am")
        val core = s.removeSuffix("pm").removeSuffix("am")
        val parts = core.split(':')
        if (parts.size !in 1..2) return null
        var h = parts[0].toIntOrNull() ?: return null
        val m = if (parts.size == 2) parts[1].toIntOrNull() ?: return null else 0
        if (parts.size == 2 && parts[1].length != 2) return null
        if (am || pm) { if (h !in 1..12) return null; if (pm && h != 12) h += 12; if (am && h == 12) h = 0 }
        if (h !in 0..23 || m !in 0..59) return null
        return "%02d:%02d".format(h, m)
    }

    fun isValidTime(t: String) = TIME.matches(t)
    fun minutes(t: String): Int? = TIME.matchEntire(t)?.let { it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt() }

    /** 24h "HH:mm" → "h:mm AM" (or keep 24h when the user prefers it). */
    fun display(t: String, use24h: Boolean = false): String {
        val m = minutes(t) ?: return "—"
        if (use24h) return t
        val h = m / 60; val mm = m % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(h12, mm, if (h < 12) "AM" else "PM")
    }

    /** Index into JAMAAT_PRAYERS of the next jamaat after [nowMinutes], or null when all have passed. */
    fun nextJamaat(m: Masjid, nowMinutes: Int): String? =
        JAMAAT_PRAYERS.firstOrNull { p -> m.jamaat[p]?.let(::minutes)?.let { it > nowMinutes } == true }

    fun cleanName(s: String) = s.trim().replace(Regex("\\s+"), " ").take(80)
    fun cleanText(s: String, max: Int) = s.trim().take(max)

    fun search(list: List<Masjid>, q: String): List<Masjid> {
        val t = q.trim().lowercase()
        if (t.isEmpty()) return list.sortedBy { it.name.lowercase() }
        return list.filter { "${it.name} ${it.city} ${it.address}".lowercase().contains(t) }
            .sortedWith(compareBy({ !it.name.lowercase().startsWith(t) }, { it.name.lowercase() }))
    }

    fun claimProblem(name: String, contact: String, role: String): String? = when {
        cleanName(name).length < 2 -> "Please enter your full name."
        role !in ROLES -> "Choose your role at the masjid."
        contact.trim().length < 5 -> "Add a phone number or email so the NoorPro team can verify you."
        else -> null
    }

    fun canManage(m: Masjid, uid: String?) = uid != null && uid in m.adminUids

    fun directionsQuery(m: Masjid) = listOf(m.name, m.address, m.city).filter { it.isNotBlank() }.joinToString(", ")

    fun shareText(m: Masjid): String = buildString {
        append(m.name)
        if (m.address.isNotBlank()) append("\n").append(m.address)
        val j = JAMAAT_PRAYERS.mapNotNull { p -> m.jamaat[p]?.let { "$p ${display(it)}" } }
        if (j.isNotEmpty()) append("\nJamaat: ").append(j.joinToString(" · "))
        if (m.jummahTime.isNotBlank()) append("\nJummah: ").append(display(m.jummahTime))
        if (m.jummahBayan.isNotBlank()) append(" — ").append(m.jummahBayan)
    }

    fun toJson(m: Masjid): JSONObject = JSONObject().apply {
        put("id", m.id); put("name", m.name); put("address", m.address); put("city", m.city)
        m.lat?.let { put("lat", it) }; m.lng?.let { put("lng", it) }
        put("jamaat", JSONObject(m.jamaat as Map<*, *>)); put("jummahTime", m.jummahTime); put("jummahBayan", m.jummahBayan)
        put("adminUids", JSONArray(m.adminUids)); put("updatedAt", m.updatedAt)
    }

    fun fromJson(o: JSONObject): Masjid? = runCatching {
        val j = o.optJSONObject("jamaat")
        Masjid(
            id = o.getString("id"), name = o.getString("name"), address = o.optString("address"), city = o.optString("city"),
            lat = if (o.has("lat")) o.getDouble("lat") else null, lng = if (o.has("lng")) o.getDouble("lng") else null,
            jamaat = JAMAAT_PRAYERS.mapNotNull { p -> j?.optString(p)?.takeIf { isValidTime(it) }?.let { p to it } }.toMap(),
            jummahTime = o.optString("jummahTime").takeIf { isValidTime(it) }.orEmpty(), jummahBayan = o.optString("jummahBayan"),
            adminUids = o.optJSONArray("adminUids")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty(),
            updatedAt = o.optLong("updatedAt"),
        )
    }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    fun fromFirestore(id: String, d: Map<String, Any?>): Masjid {
        val j = d["jamaat"] as? Map<String, Any?> ?: emptyMap()
        return Masjid(
            id = id, name = d["name"] as? String ?: "Masjid", address = d["address"] as? String ?: "", city = d["city"] as? String ?: "",
            lat = (d["lat"] as? Number)?.toDouble(), lng = (d["lng"] as? Number)?.toDouble(),
            jamaat = JAMAAT_PRAYERS.mapNotNull { p -> (j[p] as? String)?.takeIf { isValidTime(it) }?.let { p to it } }.toMap(),
            jummahTime = (d["jummahTime"] as? String)?.takeIf { isValidTime(it) }.orEmpty(), jummahBayan = d["jummahBayan"] as? String ?: "",
            adminUids = (d["adminUids"] as? List<String>).orEmpty(),
            updatedAt = (d["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
        )
    }
}

enum class MasjidError(val title: String, val body: String) {
    OFFLINE_BACKEND("Masjid pages are coming soon", "Masjid listings aren't switched on yet. Any masjid you follow stays saved on this phone and on your prayer screen."),
    SIGN_IN("Sign in to continue", "Following a masjid across devices and requesting to manage one need a NoorPro account."),
    NETWORK("You're offline", "We'll refresh the masjid list when you're back online."),
    NOT_FOUND("Masjid not found", "This listing may have been removed."),
    UNKNOWN("Something went wrong", "Please try again in a moment.");

    companion object {
        fun from(t: Throwable): MasjidError = when {
            generateSequence(t) { it.cause }.any { it is MasjidException } ->
                (generateSequence(t) { it.cause }.first { it is MasjidException } as MasjidException).error
            t is FirebaseFirestoreException && t.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> OFFLINE_BACKEND
            t is FirebaseFirestoreException && t.code == FirebaseFirestoreException.Code.UNAVAILABLE -> NETWORK
            t is FirebaseFirestoreException && t.code == FirebaseFirestoreException.Code.NOT_FOUND -> NOT_FOUND
            else -> UNKNOWN
        }
    }
}

class MasjidException(val error: MasjidError) : Exception(error.title)

interface MasjidBackend {
    val available: Boolean
    fun uid(): String?
    suspend fun list(): List<Masjid>
    suspend fun get(id: String): Masjid
    suspend fun announcements(id: String): List<Announcement>
    suspend fun follow(m: Masjid, on: Boolean)
    suspend fun requestClaim(m: Masjid, name: String, role: String, contact: String, note: String)
    suspend fun myClaims(): List<MasjidClaim>
    suspend fun saveTimes(id: String, jamaat: Map<String, String>, jummahTime: String, jummahBayan: String)
    suspend fun announce(id: String, text: String)
    suspend fun deleteAnnouncement(id: String, aid: String)
    /** True when the signed-in user has an admins/{uid} doc (NoorPro moderator). */
    suspend fun isModerator(): Boolean
    suspend fun pendingClaims(): List<MasjidClaim>
    suspend fun decide(claim: MasjidClaim, approve: Boolean)
}

class FirestoreMasjidBackend : MasjidBackend {
    private val db by lazy { FirebaseFirestore.getInstance() }
    override val available get() = FirebaseGate.ready
    override fun uid() = FirebaseGate.currentUser?.uid
    private fun me() = uid() ?: throw MasjidException(MasjidError.SIGN_IN)
    private fun ref(id: String) = db.collection("masjids").document(id)

    override suspend fun list() = db.collection("masjids").orderBy("name").limit(200).get().await()
        .documents.map { MasjidLogic.fromFirestore(it.id, it.data.orEmpty()) }

    override suspend fun get(id: String): Masjid {
        val s = ref(id).get().await()
        if (!s.exists()) throw MasjidException(MasjidError.NOT_FOUND)
        return MasjidLogic.fromFirestore(id, s.data.orEmpty())
    }

    override suspend fun announcements(id: String) = ref(id).collection("announcements")
        .orderBy("at", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).get().await()
        .documents.map { Announcement(it.id, it.getString("text").orEmpty(), it.getTimestamp("at")?.toDate()?.time ?: 0, it.getString("by").orEmpty()) }

    override suspend fun follow(m: Masjid, on: Boolean) {
        val r = db.collection("users").document(me()).collection("followed_masjids").document(m.id)
        if (on) r.set(mapOf("name" to m.name, "at" to FieldValue.serverTimestamp())).await() else r.delete().await()
    }

    override suspend fun requestClaim(m: Masjid, name: String, role: String, contact: String, note: String) {
        val uid = me()
        db.collection("masjid_claims").document("${m.id}_$uid").set(mapOf(
            "masjidId" to m.id, "masjidName" to m.name, "uid" to uid, "name" to MasjidLogic.cleanName(name), "role" to role,
            "contact" to MasjidLogic.cleanText(contact, 120), "note" to MasjidLogic.cleanText(note, 500),
            "status" to "pending", "at" to FieldValue.serverTimestamp(),
        )).await()
    }

    private fun claim(id: String, d: Map<String, Any?>) = MasjidClaim(
        id, d["masjidId"] as? String ?: "", d["masjidName"] as? String ?: "", d["uid"] as? String ?: "",
        d["name"] as? String ?: "", d["role"] as? String ?: "", d["contact"] as? String ?: "", d["note"] as? String ?: "",
        when (d["status"]) { "approved" -> ClaimStatus.APPROVED; "rejected" -> ClaimStatus.REJECTED; else -> ClaimStatus.PENDING },
        (d["at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
    )

    override suspend fun myClaims() = db.collection("masjid_claims").whereEqualTo("uid", me()).limit(20).get().await()
        .documents.map { claim(it.id, it.data.orEmpty()) }

    override suspend fun saveTimes(id: String, jamaat: Map<String, String>, jummahTime: String, jummahBayan: String) {
        me()
        ref(id).update(mapOf(
            "jamaat" to jamaat.filter { (k, v) -> k in JAMAAT_PRAYERS && MasjidLogic.isValidTime(v) },
            "jummahTime" to jummahTime.takeIf { MasjidLogic.isValidTime(it) }.orEmpty(),
            "jummahBayan" to MasjidLogic.cleanText(jummahBayan, 140), "updatedAt" to FieldValue.serverTimestamp(),
        )).await()
    }

    override suspend fun announce(id: String, text: String) {
        val uid = me()
        ref(id).collection("announcements").add(mapOf("text" to MasjidLogic.cleanText(text, 500), "by" to uid, "at" to FieldValue.serverTimestamp())).await()
    }

    override suspend fun deleteAnnouncement(id: String, aid: String) { me(); ref(id).collection("announcements").document(aid).delete().await() }

    override suspend fun isModerator(): Boolean {
        val uid = uid() ?: return false
        return runCatching { db.collection("admins").document(uid).get().await().exists() }.getOrDefault(false)
    }

    override suspend fun pendingClaims() = db.collection("masjid_claims").whereEqualTo("status", "pending").limit(50).get().await()
        .documents.map { claim(it.id, it.data.orEmpty()) }

    override suspend fun decide(claim: MasjidClaim, approve: Boolean) {
        val uid = me()
        val batch = db.batch()
        batch.update(db.collection("masjid_claims").document(claim.id), mapOf(
            "status" to if (approve) "approved" else "rejected", "decidedBy" to uid, "decidedAt" to FieldValue.serverTimestamp()))
        if (approve) batch.update(ref(claim.masjidId), "adminUids", FieldValue.arrayUnion(claim.uid))
        batch.commit().await()
    }
}

/** Device cache: the followed masjid (shown on the prayer screen, offline) and the last list. */
class MasjidLocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("masjid_pages_v1", Context.MODE_PRIVATE)

    fun followed(): Masjid? = prefs.getString("followed", null)?.let { runCatching { MasjidLogic.fromJson(JSONObject(it)) }.getOrNull() }
    fun setFollowed(m: Masjid?) {
        prefs.edit().apply { if (m == null) remove("followed") else putString("followed", MasjidLogic.toJson(m).toString()) }.apply()
        FollowedMasjid.state.value = m
    }
    fun cached(): List<Masjid> = prefs.getString("list", null)?.let { s ->
        runCatching { val a = JSONArray(s); (0 until a.length()).mapNotNull { MasjidLogic.fromJson(a.getJSONObject(it)) } }.getOrNull()
    }.orEmpty()
    fun cache(list: List<Masjid>) { prefs.edit().putString("list", JSONArray(list.take(200).map { MasjidLogic.toJson(it) }).toString()).apply() }
}

/** Live value so the prayer screen updates the moment you follow / unfollow. */
object FollowedMasjid {
    internal val state = MutableStateFlow<Masjid?>(null)
    private var loaded = false
    fun flow(context: Context): StateFlow<Masjid?> {
        if (!loaded) { loaded = true; state.value = MasjidLocalStore(context).followed() }
        return state
    }
}
