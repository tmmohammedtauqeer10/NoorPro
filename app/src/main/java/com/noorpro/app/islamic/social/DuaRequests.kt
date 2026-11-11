package com.noorpro.app.islamic.social

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.noorpro.app.data.FirebaseGate
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.community.CommunityException
import com.noorpro.app.islamic.community.CommunityText
import com.noorpro.app.islamic.community.LocalNotice
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/*
 * 1.1.6 Dua requests (dua_requests_enabled). Builds on the Ummah: Firestore + Halal Guard text
 * filter + ummah_reports. Anonymity: the public doc never carries the uid of an anonymous asker;
 * ownership lives in dua_owners/{id} (readable by the owner and moderators only).
 */

enum class DuaCategory(val label: String) {
    HEALTH("Health & shifa"), FAMILY("Family"), RIZQ("Rizq & work"), STUDIES("Studies & exams"),
    MARRIAGE("Marriage"), FORGIVENESS("Forgiveness"), DEPARTED("For the departed"), HARDSHIP("Ease in hardship"), OTHER("Other");
    companion object { fun of(s: String?) = entries.firstOrNull { it.name == s } ?: OTHER }
}

enum class DuaResponse(val key: String, val label: String, val past: String) {
    AMEEN("ameen", "Ameen", "said Ameen"), PRAYED("prayed", "I prayed for you", "prayed for you");
}

data class DuaRequest(
    val id: String,
    val text: String,
    val category: DuaCategory = DuaCategory.OTHER,
    val anonymous: Boolean = false,
    val name: String = "",
    val authorUid: String = "",
    val ameenCount: Int = 0,
    val prayedCount: Int = 0,
    val answered: Boolean = false,
    val createdAt: Long = 0,
) {
    val total get() = ameenCount + prayedCount
    val shownName get() = if (anonymous || name.isBlank()) CommunityText.ANON_NAME else name
}

object DuaLogic {
    const val MAX_TEXT = 400
    const val MIN_TEXT = 8
    const val EXPIRY_DAYS = 30

    fun validate(text: String): String? = when {
        text.trim().length < MIN_TEXT -> "Write a little more so others know what to pray for."
        text.length > MAX_TEXT -> "Please keep it under $MAX_TEXT characters."
        else -> null
    }

    /** Public fields for a new request. Anonymous requests carry no uid and no name. */
    fun newDoc(text: String, category: DuaCategory, anonymous: Boolean, name: String, uid: String): Map<String, Any> = mapOf(
        "text" to text, "category" to category.name, "anonymous" to anonymous,
        "name" to if (anonymous) "" else name.trim().take(40), "authorUid" to if (anonymous) "" else uid,
        "ameenCount" to 0, "prayedCount" to 0, "answered" to false,
    )

    fun fromFirestore(id: String, d: Map<String, Any?>) = DuaRequest(
        id, d["text"] as? String ?: "", DuaCategory.of(d["category"] as? String), d["anonymous"] as? Boolean ?: false,
        d["name"] as? String ?: "", d["authorUid"] as? String ?: "",
        (d["ameenCount"] as? Number)?.toInt() ?: 0, (d["prayedCount"] as? Number)?.toInt() ?: 0,
        d["answered"] as? Boolean ?: false, (d["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
    )

    fun responseId(uid: String, r: DuaResponse) = "${uid}_${r.key}"

    /** Optimistic local count after toggling a response. */
    fun toggle(d: DuaRequest, r: DuaResponse, on: Boolean): DuaRequest {
        val delta = if (on) 1 else -1
        return when (r) {
            DuaResponse.AMEEN -> d.copy(ameenCount = (d.ameenCount + delta).coerceAtLeast(0))
            DuaResponse.PRAYED -> d.copy(prayedCount = (d.prayedCount + delta).coerceAtLeast(0))
        }
    }

    fun visible(all: List<DuaRequest>, category: DuaCategory?, now: Long = System.currentTimeMillis()): List<DuaRequest> =
        all.filter { (category == null || it.category == category) && (it.createdAt == 0L || now - it.createdAt < EXPIRY_DAYS * 86_400_000L) }

    fun countLine(d: DuaRequest): String = when {
        d.total == 0 -> "Be the first to make dua"
        d.prayedCount == 0 -> "${d.ameenCount} said Ameen"
        d.ameenCount == 0 -> "${d.prayedCount} prayed"
        else -> "${d.ameenCount} Ameen · ${d.prayedCount} prayed"
    }

    /**
     * Gentle requester notification: compares counts with what the requester last saw.
     * Returns null when nothing new, otherwise one calm summary line (never per tap).
     */
    fun newSupport(mine: List<DuaRequest>, lastSeen: Map<String, Int>): Pair<String, String>? {
        val fresh = mine.sumOf { (it.total - (lastSeen[it.id] ?: 0)).coerceAtLeast(0) }
        if (fresh <= 0) return null
        val title = if (fresh == 1) "Someone made dua for you" else "$fresh people made dua for you"
        return title to "May Allah accept every one of them. Tap to see your requests."
    }
}

interface DuaBackend {
    val available: Boolean
    fun uid(): String?
    suspend fun recent(): List<DuaRequest>
    suspend fun post(text: String, category: DuaCategory, anonymous: Boolean, name: String): DuaRequest
    suspend fun myRequestIds(): List<String>
    suspend fun get(ids: List<String>): List<DuaRequest>
    suspend fun myResponses(ids: List<String>): Set<String>
    suspend fun respond(id: String, r: DuaResponse, on: Boolean)
    suspend fun markAnswered(id: String)
    suspend fun delete(id: String)
}

class FirestoreDuaBackend : DuaBackend {
    private val db by lazy { FirebaseFirestore.getInstance() }
    override val available get() = FirebaseGate.ready
    override fun uid() = FirebaseGate.currentUser?.uid
    private fun me() = uid() ?: throw CommunityException(CommunityError.SIGN_IN)
    private fun col() = db.collection("dua_requests")

    override suspend fun recent() = col().orderBy("createdAt", Query.Direction.DESCENDING).limit(60).get().await()
        .documents.map { DuaLogic.fromFirestore(it.id, it.data.orEmpty()) }

    override suspend fun post(text: String, category: DuaCategory, anonymous: Boolean, name: String): DuaRequest {
        val uid = me()
        DuaLogic.validate(text)?.let { throw CommunityException(CommunityError.UNKNOWN, it) }
        val clean = CommunityText.prepare(text, DuaLogic.MAX_TEXT)
        if (clean.review) throw CommunityException(CommunityError.BLOCKED_TEXT, clean.message)
        val ref = col().document()
        val data = DuaLogic.newDoc(clean.text, category, anonymous, name, uid) + ("createdAt" to FieldValue.serverTimestamp())
        db.batch()
            .set(ref, data)
            .set(db.collection("dua_owners").document(ref.id), mapOf("uid" to uid, "at" to FieldValue.serverTimestamp()))
            .commit().await()
        return DuaLogic.fromFirestore(ref.id, data - "createdAt").copy(createdAt = System.currentTimeMillis())
    }

    override suspend fun myRequestIds() = db.collection("dua_owners").whereEqualTo("uid", me()).limit(30).get().await().documents.map { it.id }

    override suspend fun get(ids: List<String>) = ids.mapNotNull { id ->
        runCatching { col().document(id).get().await() }.getOrNull()?.takeIf { it.exists() }?.let { DuaLogic.fromFirestore(it.id, it.data.orEmpty()) }
    }

    override suspend fun myResponses(ids: List<String>): Set<String> {
        val uid = uid() ?: return emptySet()
        return ids.flatMap { id -> DuaResponse.entries.mapNotNull { r ->
            val s = runCatching { col().document(id).collection("responses").document(DuaLogic.responseId(uid, r)).get().await() }.getOrNull()
            if (s?.exists() == true) "$id/${r.key}" else null
        } }.toSet()
    }

    override suspend fun respond(id: String, r: DuaResponse, on: Boolean) {
        val uid = me()
        val field = if (r == DuaResponse.AMEEN) "ameenCount" else "prayedCount"
        val resp = col().document(id).collection("responses").document(DuaLogic.responseId(uid, r))
        val b = db.batch()
        if (on) b.set(resp, mapOf("uid" to uid, "kind" to r.key, "at" to FieldValue.serverTimestamp())) else b.delete(resp)
        b.update(col().document(id), field, FieldValue.increment(if (on) 1 else -1))
        b.commit().await()
    }

    override suspend fun markAnswered(id: String) { me(); col().document(id).update("answered", true).await() }

    override suspend fun delete(id: String) {
        me()
        db.batch().delete(col().document(id)).delete(db.collection("dua_owners").document(id)).commit().await()
    }
}

/** Remembers what the requester has already seen, so the notification is calm and only on news. */
class DuaSeenStore(context: Context) {
    private val p = context.getSharedPreferences("dua_requests_seen", Context.MODE_PRIVATE)
    fun seen(): Map<String, Int> = p.all.mapNotNull { (k, v) -> (v as? Int)?.let { k to it } }.toMap()
    fun markSeen(mine: List<DuaRequest>) { p.edit().apply { clear(); mine.forEach { putInt(it.id, it.total) } }.apply() }
}

/**
 * No Cloud Function is deployed for this, so the requester's own phone checks every few hours
 * (WorkManager, network required) and posts at most one gentle summary notification.
 */
object DuaNotifier {
    private const val WORK = "dua_requests_support"
    const val NOTIFICATION_ID = 74_501

    fun ensureScheduled(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<DuaSupportWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    suspend fun check(ctx: Context, backend: DuaBackend = FirestoreDuaBackend()) {
        if (!IslamicFlags.isOn(IslamicFlags.DUA_REQUESTS) || !backend.available || backend.uid() == null) return
        val mine = backend.get(backend.myRequestIds())
        val store = DuaSeenStore(ctx)
        DuaLogic.newSupport(mine, store.seen())?.let { (t, b) -> LocalNotice.post(ctx, NOTIFICATION_ID, t, b, "UMMAH_DUA_REQUESTS") }
        store.markSeen(mine)
    }
}

class DuaSupportWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        runCatching { DuaNotifier.check(applicationContext) }
        return Result.success()
    }
}
