package com.noorpro.app.islamic.social

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.noorpro.app.data.FirebaseGate
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.community.CommunityException
import com.noorpro.app.islamic.community.CommunityText
import com.noorpro.app.islamic.community.LocalNotice
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.concurrent.TimeUnit

/*
 * 1.1.6 Good-deed challenges (challenges_enabled). Small daily deeds done together with friends.
 * Gentle by design: no ranking, no shaming, one "grace day" a week keeps a streak alive, and the group
 * sees a shared total rather than a leaderboard. Joining is by an 8-character invite code (= doc id).
 */

enum class ChallengeKind(val label: String, val goal: String, val defaultTitle: String) {
    QURAN("Quran", "Read at least one page of Quran", "A page a day"),
    ADHKAR("Adhkar", "Morning and evening adhkar", "Morning & evening adhkar"),
    SALAWAT("Salawat", "Send 100 salawat on the Prophet ﷺ", "100 salawat daily"),
    SADAQAH("Sadaqah", "Give something, even a smile", "Daily sadaqah"),
    KINDNESS("Kindness", "One kind word or deed for someone", "One kind deed"),
    NIGHT("Night prayer", "Two rak'ahs before sleeping or at night", "Two rak'ahs at night"),
    CUSTOM("Your own", "", "");
    companion object { fun of(s: String?) = entries.firstOrNull { it.name == s } ?: CUSTOM }
}

data class Challenge(
    val code: String,
    val title: String,
    val kind: ChallengeKind,
    val goal: String,
    val days: Int,
    val startDay: String,
    val createdBy: String,
    val creatorName: String,
    val members: List<String>,
)

data class ChallengeProgress(val uid: String, val name: String, val done: Set<String>)

enum class ChallengePhase { UPCOMING, ACTIVE, FINISHED }

data class Streak(val days: Int, val graceUsed: Boolean)

object ChallengeLogic {
    const val MAX_MEMBERS = 30
    const val CODE_LEN = 8
    val LENGTHS = listOf(7, 14, 21, 30, 40)
    /** No 0/O/1/I so codes are easy to read aloud. */
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun newCode(rnd: kotlin.random.Random = kotlin.random.Random.Default) = (1..CODE_LEN).map { ALPHABET[rnd.nextInt(ALPHABET.length)] }.joinToString("")
    fun normalizeCode(s: String) = s.uppercase().filter { it in ALPHABET }.take(CODE_LEN)
    fun validCode(s: String) = s.length == CODE_LEN && s.all { it in ALPHABET }

    fun validateTitle(t: String): String? = when {
        t.trim().length < 3 -> "Give your challenge a short name."
        t.length > 60 -> "Please keep the name under 60 characters."
        else -> null
    }

    fun dayKey(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toString()
    private fun d(key: String) = LocalDate.parse(key)

    /** 1-based day of the challenge (may be <1 before it starts or > days after it ends). */
    fun dayNumber(c: Challenge, today: String) = ChronoUnit.DAYS.between(d(c.startDay), d(today)).toInt() + 1

    fun phase(c: Challenge, today: String) = dayNumber(c, today).let { n -> when { n < 1 -> ChallengePhase.UPCOMING; n > c.days -> ChallengePhase.FINISHED; else -> ChallengePhase.ACTIVE } }

    fun window(c: Challenge): List<String> = (0 until c.days).map { d(c.startDay).plusDays(it.toLong()).toString() }

    fun doneInWindow(c: Challenge, done: Set<String>) = window(c).count { it in done }

    fun fraction(c: Challenge, done: Set<String>) = if (c.days == 0) 0f else doneInWindow(c, done).toFloat() / c.days

    /**
     * Gentle streak: consecutive days done, counting back from today (today not done yet doesn't break it).
     * One missed day is forgiven when there's a done day on both sides and no other grace was used in the last 7 days.
     */
    fun streak(done: Set<String>, today: String): Streak {
        var day = d(today)
        if (today !in done) day = day.minusDays(1)
        var count = 0; var lastGraceAt = -100; var walked = 0; var graceUsed = false
        while (true) {
            val key = day.toString()
            if (key in done) { count++ }
            else {
                val before = day.minusDays(1).toString()
                if (count > 0 && before in done && walked - lastGraceAt > 7) { lastGraceAt = walked; graceUsed = true }
                else break
            }
            day = day.minusDays(1); walked++
            if (walked > 400) break
        }
        return Streak(count, graceUsed && count > 0)
    }

    fun streakLine(s: Streak) = when {
        s.days == 0 -> "Every streak starts with one small deed."
        s.graceUsed -> "${s.days}-day streak · a grace day kept it going"
        else -> "${s.days}-day streak"
    }

    fun together(progress: List<ChallengeProgress>, c: Challenge) = progress.sumOf { doneInWindow(c, it.done) }

    fun togetherLine(total: Int, people: Int) = when {
        total == 0 -> "Be the first to do today's deed."
        people <= 1 -> "$total deeds done so far, alhamdulillah"
        else -> "$total deeds done together by $people people, alhamdulillah"
    }

    fun newDoc(code: String, title: String, kind: ChallengeKind, goal: String, days: Int, startDay: String, uid: String, name: String): Map<String, Any> = mapOf(
        "title" to title.trim(), "kind" to kind.name, "goal" to goal.trim().take(120), "days" to days, "startDay" to startDay,
        "createdBy" to uid, "creatorName" to name.trim().take(40), "members" to listOf(uid),
    )

    @Suppress("UNCHECKED_CAST")
    fun fromFirestore(code: String, d: Map<String, Any?>) = Challenge(
        code, d["title"] as? String ?: "", ChallengeKind.of(d["kind"] as? String), d["goal"] as? String ?: "",
        (d["days"] as? Number)?.toInt() ?: 7, d["startDay"] as? String ?: "1970-01-01", d["createdBy"] as? String ?: "",
        d["creatorName"] as? String ?: "", (d["members"] as? List<String>).orEmpty(),
    )

    fun inviteText(c: Challenge) = "Join my good-deed challenge “${c.title}” on NoorPro (${c.days} days). " +
        "Open NoorPro → Ummah → Challenges → Join, and enter code ${c.code}."

    /** Done keys we keep: only the challenge window, so the doc stays small. */
    fun toggle(c: Challenge, done: Set<String>, day: String, on: Boolean): Set<String> =
        (if (on) done + day else done - day).filter { it in window(c) }.toSortedSet()
}

interface ChallengeBackend {
    val available: Boolean
    fun uid(): String?
    suspend fun mine(): List<Challenge>
    suspend fun get(code: String): Challenge?
    suspend fun create(title: String, kind: ChallengeKind, goal: String, days: Int, startDay: String, name: String): Challenge
    suspend fun join(code: String, name: String): Challenge
    suspend fun leave(c: Challenge)
    suspend fun delete(c: Challenge)
    suspend fun progress(c: Challenge): List<ChallengeProgress>
    suspend fun setDone(c: Challenge, name: String, done: Set<String>)
}

class FirestoreChallengeBackend : ChallengeBackend {
    private val db by lazy { FirebaseFirestore.getInstance() }
    override val available get() = FirebaseGate.ready
    override fun uid() = FirebaseGate.currentUser?.uid
    private fun me() = uid() ?: throw CommunityException(CommunityError.SIGN_IN)
    private fun col() = db.collection("challenges")

    override suspend fun mine() = col().whereArrayContains("members", me()).limit(30).get().await()
        .documents.map { ChallengeLogic.fromFirestore(it.id, it.data.orEmpty()) }

    override suspend fun get(code: String) = col().document(code).get().await().takeIf { it.exists() }?.let { ChallengeLogic.fromFirestore(it.id, it.data.orEmpty()) }

    override suspend fun create(title: String, kind: ChallengeKind, goal: String, days: Int, startDay: String, name: String): Challenge {
        val uid = me()
        ChallengeLogic.validateTitle(title)?.let { throw CommunityException(CommunityError.UNKNOWN, it) }
        val t = CommunityText.prepare(title, 60); val g = CommunityText.prepare(goal, 120)
        if (t.review || g.review) throw CommunityException(CommunityError.BLOCKED_TEXT, if (t.review) t.message else g.message)
        val code = ChallengeLogic.newCode()
        val data = ChallengeLogic.newDoc(code, t.text, kind, g.text, days, startDay, uid, name)
        col().document(code).set(data + ("createdAt" to FieldValue.serverTimestamp())).await()
        return ChallengeLogic.fromFirestore(code, data)
    }

    override suspend fun join(code: String, name: String): Challenge {
        val uid = me()
        val c = get(code) ?: throw CommunityException(CommunityError.NOT_FOUND, "No challenge has that code. Check it with your friend.")
        if (uid in c.members) return c
        if (c.members.size >= ChallengeLogic.MAX_MEMBERS) throw CommunityException(CommunityError.FULL)
        col().document(code).update("members", FieldValue.arrayUnion(uid)).await()
        return c.copy(members = c.members + uid)
    }

    override suspend fun leave(c: Challenge) {
        val uid = me()
        runCatching { col().document(c.code).collection("progress").document(uid).delete().await() }
        col().document(c.code).update("members", FieldValue.arrayRemove(uid)).await()
    }

    override suspend fun delete(c: Challenge) { me(); col().document(c.code).delete().await() }

    @Suppress("UNCHECKED_CAST")
    override suspend fun progress(c: Challenge) = col().document(c.code).collection("progress").get().await().documents.map {
        ChallengeProgress(it.id, it.getString("name").orEmpty(), (it.get("done") as? List<String>).orEmpty().toSet())
    }

    override suspend fun setDone(c: Challenge, name: String, done: Set<String>) {
        val uid = me()
        col().document(c.code).collection("progress").document(uid)
            .set(mapOf("name" to name.trim().take(40), "done" to done.sorted(), "updatedAt" to FieldValue.serverTimestamp())).await()
    }
}

/** What this phone remembers: my done days per challenge (for the reminder) and whether reminders are on. */
class ChallengeLocal(context: Context) {
    private val p = context.getSharedPreferences("challenges_local", Context.MODE_PRIVATE)
    var reminders: Boolean
        get() = p.getBoolean("reminders", true)
        set(v) { p.edit().putBoolean("reminders", v).apply() }
    fun remember(list: List<Challenge>) { p.edit().putString("active", list.joinToString(";") { "${it.code},${it.startDay},${it.days}" }).apply() }
    fun active(): List<Triple<String, String, Int>> = p.getString("active", "").orEmpty().split(';').mapNotNull { s ->
        s.split(',').takeIf { it.size == 3 }?.let { Triple(it[0], it[1], it[2].toIntOrNull() ?: 0) }
    }
    fun setDone(code: String, done: Set<String>) { p.edit().putStringSet("done_$code", done).apply() }
    fun done(code: String): Set<String> = p.getStringSet("done_$code", emptySet()).orEmpty()
}

/**
 * One calm evening nudge (around 8 pm local), only when an active challenge isn't done today.
 * Runs on the phone with WorkManager; no server push is involved.
 */
object ChallengeReminder {
    private const val WORK = "challenges_evening"
    const val NOTIFICATION_ID = 74_503

    fun initialDelayMs(now: Long = System.currentTimeMillis(), hour: Int = 20): Long {
        val c = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis - now
    }

    fun ensureScheduled(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<ChallengeReminderWorker>(24, TimeUnit.HOURS).setInitialDelay(initialDelayMs(), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancel(ctx: Context) { WorkManager.getInstance(ctx).cancelUniqueWork(WORK) }

    /** Returns the notice text when something active is still open today, else null. */
    fun pending(active: List<Triple<String, String, Int>>, done: (String) -> Set<String>, today: String): Pair<String, String>? {
        val open = active.count { (code, start, days) ->
            val c = Challenge(code, "", ChallengeKind.CUSTOM, "", days, start, "", "", emptyList())
            ChallengeLogic.phase(c, today) == ChallengePhase.ACTIVE && today !in done(code)
        }
        if (open == 0) return null
        return "A small deed before the day ends?" to "The most beloved deeds to Allah are the consistent ones, even if small. Tap when you've done it."
    }

    fun check(ctx: Context) {
        if (!IslamicFlags.isOn(IslamicFlags.CHALLENGES)) return
        val local = ChallengeLocal(ctx)
        if (!local.reminders) return
        pending(local.active(), local::done, ChallengeLogic.dayKey(System.currentTimeMillis()))?.let { (t, b) ->
            LocalNotice.post(ctx, NOTIFICATION_ID, t, b, "UMMAH_CHALLENGES")
        }
    }
}

class ChallengeReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result { runCatching { ChallengeReminder.check(applicationContext) }; return Result.success() }
}
