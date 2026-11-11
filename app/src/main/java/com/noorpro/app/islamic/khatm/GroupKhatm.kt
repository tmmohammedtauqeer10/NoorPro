package com.noorpro.app.islamic.khatm

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.noorpro.app.data.FirebaseGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/*
 * 1.1.6 Group Khatm (Remote Config group_khatm_enabled, default OFF).
 * Firestore: group_khatms/{CODE} + group_khatms/{CODE}/paras/{1..30}.
 * Rules live in firestore-group-khatm.rules (additive, NOT deployed yet) — until they are,
 * every call fails with PERMISSION_DENIED and the UI shows a calm "not available yet" notice.
 */

data class KhatmGroup(
    val code: String,
    val title: String,
    val dedication: String,
    val creatorUid: String,
    val creatorName: String,
    val memberUids: List<String>,
    val createdAt: Long = 0,
)

enum class ParaStatus { CLAIMED, DONE }

data class ParaClaim(val para: Int, val uid: String, val name: String, val status: ParaStatus, val at: Long = 0)

/** How one para looks to the current reader. */
enum class ParaView { OPEN, MINE, MINE_DONE, TAKEN, DONE }

data class KhatmProgress(val done: Int, val claimed: Int, val open: Int) {
    val fraction get() = done / 30f
    val complete get() = done >= 30
}

/** The 30 paras (ajza) with their customary opening words and where they start. */
object Paras {
    val names = listOf(
        "Alif Lam Mim", "Sayaqul", "Tilkar Rusul", "Lan Tanalu", "Wal Muhsanat", "La Yuhibbullah", "Wa Idha Sami'u",
        "Wa Law Annana", "Qalal Mala'u", "Wa'lamu", "Ya'tadhirun", "Wa Ma Min Dabbah", "Wa Ma Ubarri'u", "Rubama",
        "Subhanalladhi", "Qal Alam", "Iqtaraba", "Qad Aflaha", "Wa Qalalladhina", "Amman Khalaq", "Utlu Ma Uhiya",
        "Wa Man Yaqnut", "Wa Mali", "Faman Azlamu", "Ilayhi Yuraddu", "Ha Mim", "Qala Fama Khatbukum",
        "Qad Sami'allah", "Tabarakalladhi", "'Amma",
    )
    /** Surah id + ayah where each para begins (standard Madani division). */
    val starts = listOf(
        1 to 1, 2 to 142, 2 to 253, 3 to 93, 4 to 24, 4 to 148, 5 to 82, 6 to 111, 7 to 88, 8 to 41,
        9 to 93, 11 to 6, 12 to 53, 15 to 1, 17 to 1, 18 to 75, 21 to 1, 23 to 1, 25 to 21, 27 to 56,
        29 to 46, 33 to 31, 36 to 28, 39 to 32, 41 to 47, 46 to 1, 51 to 31, 58 to 1, 67 to 1, 78 to 1,
    )
    fun name(n: Int) = names.getOrElse(n - 1) { "Para $n" }
}

object KhatmLogic {
    /** No 0/O/1/I/L so a code can be read aloud or typed from a WhatsApp message. */
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val CODE_LEN = 6
    const val MAX_MEMBERS = 100
    const val LINK_BASE = "https://noor-pro-d87e3.web.app/k?c="
    private val CODE_RE = Regex("^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}$")

    fun newCode(rnd: Random = Random.Default) = (1..CODE_LEN).map { ALPHABET[rnd.nextInt(ALPHABET.length)] }.joinToString("")
    fun isValidCode(code: String) = CODE_RE.matches(code)
    fun inviteLink(code: String) = LINK_BASE + code

    /** Accepts a bare code ("ab3 k9z"), or any text containing the invite link. */
    fun parseInvite(text: String): String? {
        val t = text.trim()
        Regex("[?&]c=([A-Za-z0-9]{6})").find(t)?.let { m -> return m.groupValues[1].uppercase().takeIf(::isValidCode) }
        val bare = t.replace(" ", "").replace("-", "").uppercase()
        return bare.takeIf(::isValidCode)
    }

    fun shareText(g: KhatmGroup): String = buildString {
        append("Join our Group Khatm on NoorPro: \"${g.title}\"")
        if (g.dedication.isNotBlank()) append(" — ${g.dedication}")
        append(". Pick a para and let's complete the Qur'an together.\n")
        append(inviteLink(g.code)).append("\nCode: ").append(g.code)
    }

    fun progress(claims: Collection<ParaClaim>): KhatmProgress {
        val byPara = claims.filter { it.para in 1..30 }.associateBy { it.para }
        val done = byPara.values.count { it.status == ParaStatus.DONE }
        val claimed = byPara.values.count { it.status == ParaStatus.CLAIMED }
        return KhatmProgress(done, claimed, 30 - done - claimed)
    }

    fun view(para: Int, claims: Map<Int, ParaClaim>, myUid: String?): ParaView {
        val c = claims[para] ?: return ParaView.OPEN
        val mine = myUid != null && c.uid == myUid
        return when {
            mine && c.status == ParaStatus.DONE -> ParaView.MINE_DONE
            mine -> ParaView.MINE
            c.status == ParaStatus.DONE -> ParaView.DONE
            else -> ParaView.TAKEN
        }
    }

    /** Next open para — what the "Take next para" button claims. */
    fun nextOpen(claims: Map<Int, ParaClaim>): Int? = (1..30).firstOrNull { it !in claims }

    fun cleanTitle(s: String) = s.trim().replace(Regex("\\s+"), " ").take(80)
    fun cleanDedication(s: String) = s.trim().replace(Regex("\\s+"), " ").take(200)
    fun cleanName(s: String) = s.trim().ifBlank { "A reader" }.take(60)
}

/** What went wrong, in words a reader understands. */
enum class KhatmError(val title: String, val body: String) {
    OFFLINE_BACKEND("Group Khatm isn't available yet", "This feature is still being switched on for everyone. Your Qur'an reading and personal Khatm keep working as usual."),
    SIGN_IN("Sign in to join a Group Khatm", "Group Khatm keeps track of who is reading which para, so it needs your NoorPro account."),
    NOT_FOUND("We couldn't find that Khatm", "Check the code with the person who invited you — codes are 6 letters and numbers."),
    TAKEN("Someone just took that para", "Pick another open para — the list updates live."),
    FULL("This Khatm is full", "A Group Khatm can have up to 100 readers. Ask the organiser to start another one."),
    NETWORK("You seem to be offline", "Check your connection and try again."),
    UNKNOWN("Something went wrong", "Please try again in a moment.");

    companion object {
        fun from(t: Throwable): KhatmError = when {
            generateSequence(t) { it.cause }.any { it is KhatmException } ->
                (generateSequence(t) { it.cause }.first { it is KhatmException } as KhatmException).error
            t is FirebaseFirestoreException && t.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> OFFLINE_BACKEND
            t is FirebaseFirestoreException && t.code == FirebaseFirestoreException.Code.UNAVAILABLE -> NETWORK
            t is FirebaseFirestoreException && t.code == FirebaseFirestoreException.Code.NOT_FOUND -> NOT_FOUND
            else -> UNKNOWN
        }
    }
}

class KhatmException(val error: KhatmError) : Exception(error.title)

interface KhatmBackend {
    /** False when Firebase isn't configured (e.g. a debug build without google-services.json). */
    val available: Boolean
    fun uid(): String?
    suspend fun create(title: String, dedication: String, name: String): KhatmGroup
    suspend fun join(code: String): KhatmGroup
    suspend fun mine(): List<KhatmGroup>
    /** Live updates; returns a function that stops listening. */
    fun observe(code: String, onGroup: (KhatmGroup?) -> Unit, onParas: (List<ParaClaim>) -> Unit, onError: (Throwable) -> Unit): () -> Unit
    suspend fun claim(code: String, para: Int, name: String)
    suspend fun setDone(code: String, para: Int, done: Boolean)
    suspend fun release(code: String, para: Int)
    suspend fun leave(code: String)
}

class FirestoreKhatmBackend : KhatmBackend {
    private val db by lazy { FirebaseFirestore.getInstance() }
    override val available get() = FirebaseGate.ready
    override fun uid() = FirebaseGate.currentUser?.uid
    private fun me() = uid() ?: throw KhatmException(KhatmError.SIGN_IN)
    private fun ref(code: String) = db.collection("group_khatms").document(code)

    @Suppress("UNCHECKED_CAST")
    private fun group(code: String, d: Map<String, Any?>) = KhatmGroup(
        code, d["title"] as? String ?: "Group Khatm", d["dedication"] as? String ?: "",
        d["creatorUid"] as? String ?: "", d["creatorName"] as? String ?: "",
        (d["memberUids"] as? List<String>).orEmpty(), (d["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
    )

    override suspend fun create(title: String, dedication: String, name: String): KhatmGroup {
        val uid = me()
        repeat(4) {
            val code = KhatmLogic.newCode()
            val r = ref(code)
            if (!r.get().await().exists()) {
                r.set(mapOf(
                    "title" to KhatmLogic.cleanTitle(title).ifBlank { "Group Khatm" },
                    "dedication" to KhatmLogic.cleanDedication(dedication),
                    "creatorUid" to uid, "creatorName" to KhatmLogic.cleanName(name),
                    "memberUids" to listOf(uid), "createdAt" to FieldValue.serverTimestamp(),
                )).await()
                return KhatmGroup(code, KhatmLogic.cleanTitle(title).ifBlank { "Group Khatm" }, KhatmLogic.cleanDedication(dedication), uid, name, listOf(uid))
            }
        }
        throw KhatmException(KhatmError.UNKNOWN)
    }

    override suspend fun join(code: String): KhatmGroup {
        val uid = me()
        val snap = ref(code).get().await()
        if (!snap.exists()) throw KhatmException(KhatmError.NOT_FOUND)
        val g = group(code, snap.data.orEmpty())
        if (uid in g.memberUids) return g
        if (g.memberUids.size >= KhatmLogic.MAX_MEMBERS) throw KhatmException(KhatmError.FULL)
        ref(code).update("memberUids", FieldValue.arrayUnion(uid)).await()
        return g.copy(memberUids = g.memberUids + uid)
    }

    override suspend fun mine(): List<KhatmGroup> {
        val uid = me()
        return db.collection("group_khatms").whereArrayContains("memberUids", uid).limit(30).get().await()
            .documents.map { group(it.id, it.data.orEmpty()) }.sortedByDescending { it.createdAt }
    }

    override fun observe(code: String, onGroup: (KhatmGroup?) -> Unit, onParas: (List<ParaClaim>) -> Unit, onError: (Throwable) -> Unit): () -> Unit {
        val a = ref(code).addSnapshotListener { s, e ->
            if (e != null) onError(e) else onGroup(s?.takeIf { it.exists() }?.let { group(code, it.data.orEmpty()) })
        }
        val b = ref(code).collection("paras").addSnapshotListener { s, e ->
            if (e != null) onError(e) else onParas(s?.documents.orEmpty().mapNotNull { d ->
                val n = d.id.toIntOrNull() ?: return@mapNotNull null
                ParaClaim(n, d.getString("uid").orEmpty(), d.getString("name").orEmpty(),
                    if (d.getString("status") == "done") ParaStatus.DONE else ParaStatus.CLAIMED, d.getTimestamp("at")?.toDate()?.time ?: 0)
            })
        }
        return { a.remove(); b.remove() }
    }

    override suspend fun claim(code: String, para: Int, name: String) {
        val uid = me()
        val r = ref(code).collection("paras").document(para.toString())
        db.runTransaction { tx ->
            if (tx.get(r).exists()) throw KhatmException(KhatmError.TAKEN)
            tx.set(r, mapOf("uid" to uid, "name" to KhatmLogic.cleanName(name), "status" to "claimed", "at" to FieldValue.serverTimestamp()))
        }.await()
    }

    override suspend fun setDone(code: String, para: Int, done: Boolean) {
        me()
        ref(code).collection("paras").document(para.toString())
            .update(mapOf("status" to if (done) "done" else "claimed", "at" to FieldValue.serverTimestamp())).await()
    }

    override suspend fun release(code: String, para: Int) { me(); ref(code).collection("paras").document(para.toString()).delete().await() }

    override suspend fun leave(code: String) { ref(code).update("memberUids", FieldValue.arrayRemove(me())).await() }
}

/** Codes I've created or joined, so the list works instantly (and offline) before Firestore answers. */
class KhatmLocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("group_khatm_v1", Context.MODE_PRIVATE)
    fun codes(): List<String> = prefs.getString("codes", "").orEmpty().split(',').filter { KhatmLogic.isValidCode(it) }
    fun title(code: String): String = prefs.getString("t_$code", "").orEmpty()
    fun remember(g: KhatmGroup) {
        val list = (listOf(g.code) + codes().filter { it != g.code }).take(30)
        prefs.edit().putString("codes", list.joinToString(",")).putString("t_${g.code}", g.title).apply()
    }
    fun forget(code: String) { prefs.edit().putString("codes", codes().filter { it != code }.joinToString(",")).remove("t_$code").apply() }
}

/** An invite link (https://noor-pro-d87e3.web.app/k?c=CODE) waiting to be opened by the Khatm screen. */
object KhatmNav {
    val pendingCode = MutableStateFlow<String?>(null)
}
