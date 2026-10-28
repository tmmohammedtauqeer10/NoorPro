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
 * 1.1.6 Ask a Scholar (ask_scholar_enabled). Questions stay private (asker + verified scholars)
 * until answered; answered Q&A become a searchable public library. Scholars are verified by
 * admins only: scholars/{uid} is written from the console / admin tooling, never by the app.
 * Like dua requests, the public doc never carries the asker's uid — ownership lives in
 * scholar_question_owners/{id}.
 */

enum class ScholarCategory(val label: String) {
    AQEEDAH("Aqeedah"), SALAH("Salah & purity"), FASTING("Fasting"), ZAKAT("Zakat & charity"), FAMILY("Family & marriage"),
    FINANCE("Halal finance"), QURAN("Quran & tafsir"), MANNERS("Character"), OTHER("Other");
    companion object { fun of(s: String?) = entries.firstOrNull { it.name == s } ?: OTHER }
}

data class Scholar(val uid: String, val name: String, val title: String = "", val verified: Boolean = true)

data class ScholarQuestion(
    val id: String,
    val text: String,
    val category: ScholarCategory = ScholarCategory.OTHER,
    val anonymous: Boolean = true,
    val name: String = "",
    val answered: Boolean = false,
    val answer: String = "",
    val scholarUid: String = "",
    val scholarName: String = "",
    val scholarTitle: String = "",
    val createdAt: Long = 0,
    val answeredAt: Long = 0,
) {
    val askedBy get() = if (anonymous || name.isBlank()) "Asked anonymously" else "Asked by $name"
}

object ScholarLogic {
    const val MIN_Q = 20
    const val MAX_Q = 1000
    const val MIN_A = 40
    const val MAX_A = 4000
    const val DISCLAIMER = "Answers are general guidance from scholars verified by NoorPro, not a personal fatwa. " +
        "For divorce, inheritance, contracts or anything specific to your situation, please speak to a qualified local scholar who can ask you more."

    fun validateQuestion(t: String): String? = when {
        t.trim().length < MIN_Q -> "Please give a little more detail so the scholar can help."
        t.length > MAX_Q -> "Please keep your question under $MAX_Q characters."
        else -> null
    }
    fun validateAnswer(t: String): String? = when {
        t.trim().length < MIN_A -> "Please write a fuller answer (at least $MIN_A characters)."
        t.length > MAX_A -> "Please keep the answer under $MAX_A characters."
        else -> null
    }

    fun newDoc(text: String, category: ScholarCategory, anonymous: Boolean, name: String): Map<String, Any> = mapOf(
        "text" to text, "category" to category.name, "anonymous" to anonymous, "name" to if (anonymous) "" else name.trim().take(40), "status" to "open",
    )

    fun fromFirestore(id: String, d: Map<String, Any?>) = ScholarQuestion(
        id, d["text"] as? String ?: "", ScholarCategory.of(d["category"] as? String), d["anonymous"] as? Boolean ?: true, d["name"] as? String ?: "",
        d["status"] == "answered", d["answer"] as? String ?: "", d["scholarUid"] as? String ?: "", d["scholarName"] as? String ?: "",
        d["scholarTitle"] as? String ?: "",
        (d["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0, (d["answeredAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
    )

    private fun norm(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), " ")

    /** Every word of the query must appear in the question, answer, category or scholar name. */
    fun search(list: List<ScholarQuestion>, query: String, category: ScholarCategory? = null): List<ScholarQuestion> {
        val words = norm(query).split(' ').filter { it.length > 1 }
        return list.filter { q ->
            (category == null || q.category == category) && q.answered &&
                norm("${q.text} ${q.answer} ${q.category.label} ${q.scholarName}").let { hay -> words.all { it in hay } }
        }.sortedByDescending { it.answeredAt }
    }

    /** Answers the asker hasn't seen yet (for the calm local notification). */
    fun newlyAnswered(mine: List<ScholarQuestion>, seen: Set<String>) = mine.filter { it.answered && it.id !in seen }
}

/** Saved Q&A are kept on the device only. */
class ScholarSaves(context: Context) {
    private val p = context.getSharedPreferences("ask_scholar_saved", Context.MODE_PRIVATE)
    fun ids(): Set<String> = p.getStringSet("ids", emptySet()).orEmpty()
    fun toggle(id: String): Set<String> {
        val next = ids().toMutableSet().apply { if (!add(id)) remove(id) }
        p.edit().putStringSet("ids", next).apply()
        return next
    }
}

interface ScholarBackend {
    val available: Boolean
    fun uid(): String?
    suspend fun library(): List<ScholarQuestion>
    suspend fun ask(text: String, category: ScholarCategory, anonymous: Boolean, name: String): ScholarQuestion
    suspend fun mine(): List<ScholarQuestion>
    suspend fun withdraw(id: String)
    suspend fun me(): Scholar?
    suspend fun inbox(): List<ScholarQuestion>
    suspend fun answer(id: String, text: String, scholar: Scholar)
}

class FirestoreScholarBackend : ScholarBackend {
    private val db by lazy { FirebaseFirestore.getInstance() }
    override val available get() = FirebaseGate.ready
    override fun uid() = FirebaseGate.currentUser?.uid
    private fun need() = uid() ?: throw CommunityException(CommunityError.SIGN_IN)
    private fun col() = db.collection("scholar_questions")
    private fun owners() = db.collection("scholar_question_owners")

    override suspend fun library() = col().whereEqualTo("status", "answered").orderBy("answeredAt", Query.Direction.DESCENDING).limit(100).get().await()
        .documents.map { ScholarLogic.fromFirestore(it.id, it.data.orEmpty()) }

    override suspend fun ask(text: String, category: ScholarCategory, anonymous: Boolean, name: String): ScholarQuestion {
        val uid = need()
        ScholarLogic.validateQuestion(text)?.let { throw CommunityException(CommunityError.UNKNOWN, it) }
        val clean = CommunityText.prepare(text, ScholarLogic.MAX_Q)
        if (clean.review) throw CommunityException(CommunityError.BLOCKED_TEXT, clean.message)
        val ref = col().document()
        val data = ScholarLogic.newDoc(clean.text, category, anonymous, name) + ("createdAt" to FieldValue.serverTimestamp())
        db.batch().set(ref, data).set(owners().document(ref.id), mapOf("uid" to uid, "at" to FieldValue.serverTimestamp())).commit().await()
        return ScholarLogic.fromFirestore(ref.id, data - "createdAt").copy(createdAt = System.currentTimeMillis())
    }

    override suspend fun mine(): List<ScholarQuestion> {
        val ids = owners().whereEqualTo("uid", need()).limit(30).get().await().documents.map { it.id }
        return ids.mapNotNull { id -> runCatching { col().document(id).get().await() }.getOrNull()?.takeIf { it.exists() }?.let { ScholarLogic.fromFirestore(it.id, it.data.orEmpty()) } }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun withdraw(id: String) { need(); db.batch().delete(col().document(id)).delete(owners().document(id)).commit().await() }

    override suspend fun me(): Scholar? {
        val uid = uid() ?: return null
        val d = runCatching { db.collection("scholars").document(uid).get().await() }.getOrNull()?.takeIf { it.exists() } ?: return null
        if (d.getBoolean("verified") != true) return null
        return Scholar(uid, d.getString("name").orEmpty(), d.getString("title").orEmpty())
    }

    override suspend fun inbox() = col().whereEqualTo("status", "open").orderBy("createdAt", Query.Direction.ASCENDING).limit(50).get().await()
        .documents.map { ScholarLogic.fromFirestore(it.id, it.data.orEmpty()) }

    override suspend fun answer(id: String, text: String, scholar: Scholar) {
        ScholarLogic.validateAnswer(text)?.let { throw CommunityException(CommunityError.UNKNOWN, it) }
        col().document(id).update(mapOf("status" to "answered", "answer" to text.trim(), "scholarUid" to scholar.uid, "scholarName" to scholar.name.take(60),
            "scholarTitle" to scholar.title.take(60), "answeredAt" to FieldValue.serverTimestamp())).await()
    }
}

/** No Cloud Function: the asker's phone checks every 6h and posts one calm notice per newly answered question. */
object ScholarNotifier {
    private const val WORK = "ask_scholar_answers"
    const val NOTIFICATION_ID = 74_502
    private fun seen(ctx: Context) = ctx.getSharedPreferences("ask_scholar_seen", Context.MODE_PRIVATE)

    fun ensureScheduled(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<ScholarAnswerWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun markSeen(ctx: Context, mine: List<ScholarQuestion>) =
        seen(ctx).edit().putStringSet("ids", mine.filter { it.answered }.map { it.id }.toSet()).apply()

    suspend fun check(ctx: Context, backend: ScholarBackend = FirestoreScholarBackend()) {
        if (!IslamicFlags.isOn(IslamicFlags.ASK_SCHOLAR) || !backend.available || backend.uid() == null) return
        val mine = backend.mine()
        val fresh = ScholarLogic.newlyAnswered(mine, seen(ctx).getStringSet("ids", emptySet()).orEmpty())
        if (fresh.isNotEmpty()) LocalNotice.post(ctx, NOTIFICATION_ID, if (fresh.size == 1) "Your question has been answered" else "${fresh.size} of your questions were answered",
            fresh.first().text.take(80), "UMMAH_ASK_SCHOLAR")
        markSeen(ctx, mine)
    }
}

class ScholarAnswerWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result { runCatching { ScholarNotifier.check(applicationContext) }; return Result.success() }
}
