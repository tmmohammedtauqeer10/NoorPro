package com.noorpro.app.data.halal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.noorpro.app.data.messaging.MsgChat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 1.1.5 Halal Guard client flags (Remote Config). Client-only behaviour defaults ON; anything that
 * needs a rules/functions deploy defaults OFF.
 */
object HalalFlags {
    /** On-device text warnings + blocking severe words before send/post. Client-only. */
    const val KEY_TEXT_WARN = "halal_text_warnings_enabled"
    /** Moderator panel entry (needs the 1.1.5 rules + setModerationDecision deployed). */
    const val KEY_MOD_PANEL = "halal_moderator_panel_enabled"
    /** Safety settings screen (message requests, comment filter, blur). Client-only + private doc. */
    const val KEY_SAFETY = "halal_safety_settings_enabled"
    /** Sisters-only / Brothers-only circles (optional private gender). Client-enforced only; see docs. */
    const val KEY_CIRCLES = "halal_gender_circles_enabled"
    /** Server notices inbox (moderation_notices/{uid}/items; needs the 1.1.5 rules). Submission-status notices work without it. */
    const val KEY_NOTICES = "halal_moderation_notices_enabled"
    /** Blur media a moderator approved as sensitive (needs setModerationDecision; reading `sensitive` is harmless). */
    const val KEY_BLUR = "halal_blur_flagged_enabled"
    private val DEFAULTS = mapOf(KEY_TEXT_WARN to true, KEY_MOD_PANEL to false, KEY_SAFETY to true, KEY_CIRCLES to false,
        KEY_NOTICES to false, KEY_BLUR to true)
    val ALL_KEYS get() = DEFAULTS.keys
    fun default(key: String) = DEFAULTS[key] ?: false
    @Volatile var override: Map<String, Boolean>? = null
    @Volatile private var loaded: Map<String, Boolean> = emptyMap()
    @Volatile private var started = false
    fun isOn(key: String) = override?.get(key) ?: loaded[key] ?: DEFAULTS[key] ?: false
    val textWarnings get() = isOn(KEY_TEXT_WARN)
    val moderatorPanel get() = isOn(KEY_MOD_PANEL)
    val safety get() = isOn(KEY_SAFETY)
    val circles get() = isOn(KEY_CIRCLES)
    val notices get() = isOn(KEY_NOTICES)
    val blur get() = isOn(KEY_BLUR)
    fun load() {
        if (started) return
        started = true
        runCatching {
            val rc = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            fun apply() {
                loaded = DEFAULTS.keys.mapNotNull { k ->
                    val v = rc.getValue(k)
                    if (v.source == com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_STATIC) null else k to v.asBoolean()
                }.toMap()
            }
            apply()
            rc.fetchAndActivate().addOnCompleteListener { runCatching { apply() } }
        }
    }
}

enum class MessageRequestMode(val label: String, val blurb: String) {
    EVERYONE("Everyone", "Anyone can message you; all chats stay in your inbox."),
    FOLLOWING("Only people I follow", "Chats from people you don't follow wait in a Requests folder.")
}

enum class CircleGender(val label: String) { UNSET("Prefer not to say"), FEMALE("Sister"), MALE("Brother") }

enum class CircleAudience(val label: String, val blurb: String) {
    ALL("Everyone", "Open to all members"),
    SISTERS("Sisters only", "Only members who declared 'Sister'"),
    BROTHERS("Brothers only", "Only members who declared 'Brother'");
    companion object { fun of(v: String?) = entries.firstOrNull { it.name.equals(v, true) } ?: ALL }
}

data class SafetySettings(
    val messageRequests: MessageRequestMode = MessageRequestMode.EVERYONE,
    val hideNonFollowerComments: Boolean = false,
    val blurFlagged: Boolean = true,
    val gender: CircleGender = CircleGender.UNSET
)

object SafetyRules {
    /**
     * Splits the inbox into (inbox, requests). EVERYONE keeps every chat in the inbox; FOLLOWING moves
     * chats with people you don't follow into the Requests ("stranger") folder. Chats you started stay.
     */
    fun <T> splitRequests(items: List<T>, otherUid: (T) -> String, startedByMe: (T) -> Boolean,
                          following: Set<String>, mode: MessageRequestMode): Pair<List<T>, List<T>> {
        if (mode == MessageRequestMode.EVERYONE) return items to emptyList()
        return items.partition { val u = otherUid(it); u.isBlank() || u in following || startedByMe(it) }
    }

    fun splitInbox(chats: List<MsgChat>, following: Set<String>, mode: MessageRequestMode): Pair<List<MsgChat>, List<MsgChat>> =
        splitRequests(chats, { it.otherUid }, { false }, following, mode)

    /** Comments shown on a post whose owner chose "followers only": owner, the viewer and followers stay. */
    fun <T> visibleComments(items: List<T>, uidOf: (T) -> String, ownerUid: String, viewerUid: String,
                            ownerFollowers: Set<String>, ownerLimitsToFollowers: Boolean): List<T> {
        if (!ownerLimitsToFollowers) return items
        return items.filter { val u = uidOf(it); u == ownerUid || u == viewerUid || u in ownerFollowers }
    }

    fun canJoin(audience: CircleAudience, me: CircleGender) = when (audience) {
        CircleAudience.ALL -> true
        CircleAudience.SISTERS -> me == CircleGender.FEMALE
        CircleAudience.BROTHERS -> me == CircleGender.MALE
    }

    fun joinBlockedReason(audience: CircleAudience, me: CircleGender) = when {
        canJoin(audience, me) -> ""
        me == CircleGender.UNSET -> "This is a ${audience.label.lowercase()} circle. Set 'Sister' or 'Brother' privately in Safety settings to join."
        else -> "This circle is ${audience.label.lowercase()}."
    }
}

/**
 * Who sees the moderator entry points. Mirrors firestore.rules isModerator(): the `admin` custom claim OR
 * an admins/{uid} doc, and the client additionally needs the panel flag (rules + callable deployed).
 */
object ModeratorAccess {
    fun isModerator(adminClaim: Any?, hasAdminDoc: Boolean) = adminClaim == true || hasAdminDoc
    fun showPanel(signedIn: Boolean, isModerator: Boolean, flagOn: Boolean) = signedIn && isModerator && flagOn
}

/** A notice for the signed-in creator, from their own submission status or the server notices inbox. */
data class HalalNotice(val id: String, val queued: Boolean, val reason: String)

/**
 * Submission-status notices: the publisher writes status rejected (guardBlocked) / in_review with a
 * moderationReason on the creator's own ummah_submissions doc, which the app already listens to.
 * Each (id, status) pair shows once per install.
 */
object HalalNotices {
    val pending = androidx.compose.runtime.mutableStateListOf<HalalNotice>()
    private val seen = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private var prefs: android.content.SharedPreferences? = null
    private val main by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("halal_notices", Context.MODE_PRIVATE).also {
            seen.addAll(it.getStringSet("seen", emptySet()).orEmpty())
        }
    }

    /** Pure decision (tested): which submission updates become a notice. */
    fun noticeFor(id: String, status: String, reason: String, guardBlocked: Boolean): HalalNotice? = when {
        status == "rejected" && guardBlocked -> HalalNotice("$id:rejected", false, reason)
        status == "in_review" -> HalalNotice("$id:in_review", true, reason)
        else -> null
    }

    fun onSubmission(id: String, status: String, reason: String, guardBlocked: Boolean) {
        val n = noticeFor(id, status, reason, guardBlocked) ?: return
        if (!seen.add(n.id)) return
        prefs?.edit()?.putStringSet("seen", seen.toSet())?.apply()
        val post = { if (pending.none { it.id == n.id }) pending.add(n) }
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) post() else main.post { post() }
    }

    fun dismiss(id: String) { pending.removeAll { it.id == id } }
}

/** Live mirror of SafetySettings.blurFlagged for feed reads (default on). */
object HalalPrefs {
    var blurFlagged by androidx.compose.runtime.mutableStateOf(true)
    /** Sisters/Brothers circle declaration of the signed-in user (private). */
    var gender by androidx.compose.runtime.mutableStateOf(CircleGender.UNSET)
}

/** Post owner's "hide comments from non-followers" choice (public `commentsFrom`) + their follower uids. */
object CommentAudience {
    fun load(ownerUid: String, onResult: (limit: Boolean, followers: Set<String>) -> Unit) {
        if (ownerUid.isBlank()) return onResult(false, emptySet())
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return onResult(false, emptySet())
        db.collection("ummah_profiles").document(ownerUid).get().addOnCompleteListener { t ->
            if (t.result?.getString("commentsFrom") != "followers") return@addOnCompleteListener onResult(false, emptySet())
            db.collection("ummah_users").document(ownerUid).collection("followers").limit(5000).get()
                .addOnCompleteListener { f -> onResult(true, f.result?.documents.orEmpty().map { it.getString("uid") ?: it.id }.toSet()) }
        }
    }
}

/** Private safety settings live at users/{uid}/settings/safety (owner-only). commentsFrom is public on ummah_profiles. */
class SafetyStore(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    fun observe(onResult: (SafetySettings) -> Unit): ListenerRegistration? {
        val u = uid ?: return null
        return db.collection("users").document(u).collection("settings").document("safety").addSnapshotListener { s, _ ->
            onResult(SafetySettings(
                messageRequests = if (s?.getString("messageRequests") == "following") MessageRequestMode.FOLLOWING else MessageRequestMode.EVERYONE,
                hideNonFollowerComments = s?.getBoolean("hideNonFollowerComments") == true,
                blurFlagged = s?.getBoolean("blurFlagged") != false,
                gender = when (s?.getString("gender")) { "female" -> CircleGender.FEMALE; "male" -> CircleGender.MALE; else -> CircleGender.UNSET }
            ))
        }
    }

    fun save(s: SafetySettings, onDone: (Boolean) -> Unit = {}) {
        val u = uid ?: return onDone(false)
        val data = mapOf(
            "messageRequests" to if (s.messageRequests == MessageRequestMode.FOLLOWING) "following" else "everyone",
            "hideNonFollowerComments" to s.hideNonFollowerComments,
            "blurFlagged" to s.blurFlagged,
            "gender" to when (s.gender) { CircleGender.FEMALE -> "female"; CircleGender.MALE -> "male"; else -> "" }
        )
        db.collection("users").document(u).collection("settings").document("safety").set(data, SetOptions.merge())
            .addOnCompleteListener { t ->
                if (!t.isSuccessful) return@addOnCompleteListener onDone(false)
                db.collection("ummah_profiles").document(u)
                    .set(mapOf("commentsFrom" to if (s.hideNonFollowerComments) "followers" else "everyone"), SetOptions.merge())
                    .addOnCompleteListener { onDone(it.isSuccessful) }
            }
    }
}

data class ModerationItem(
    val id: String, val kind: String, val targetId: String, val creatorUid: String, val creatorName: String,
    val type: String, val caption: String, val mediaUrl: String, val thumbnailUrl: String,
    val reasons: List<String>, val scores: Map<String, String> = emptyMap(), val createdAt: Long = 0, val status: String = "open"
) {
    val previewUrl get() = thumbnailUrl.ifBlank { if (type == "image") mediaUrl else "" }
}

data class ModerationReport(
    val id: String, val targetType: String, val targetId: String, val targetUid: String, val reason: String,
    val details: String, val reporterUid: String, val createdAt: Long = 0, val status: String = "open"
)

data class AuditEntry(val id: String, val actorUid: String, val action: String, val kind: String, val subjectUid: String, val reason: String, val at: Long)

data class StrikeRecord(val count: Int = 0, val suspendedUntilMs: Long = 0, val banned: Boolean = false) {
    fun restricted(now: Long) = banned || suspendedUntilMs > now
}

data class ModerationNotice(val id: String, val kind: String, val reason: String, val createdAt: Long)

enum class ModAction(val wire: String, val label: String) {
    APPROVE("approve", "Approve"), REMOVE("remove", "Remove"), WARN("warn", "Warn"),
    SUSPEND("suspend", "Suspend"), BAN("ban", "Ban"), UNBAN("unban", "Unban"), DISMISS("dismiss", "Dismiss")
}

object ModerationText {
    private val LABELS = mapOf(
        "possible_adult" to "Possible nudity", "possible_racy" to "Possibly immodest", "possible_violence" to "Possible violence",
        "possible_adult_video" to "Video: possible nudity", "scan_error" to "Scan failed", "guard_error" to "Check failed",
        "image_unreadable" to "Image not readable", "video_too_large" to "Video too large to scan", "video_unscanned" to "Video not scanned",
        "new_account" to "New account (first posts)", "text_hate" to "Hate words", "text_scam" to "Scam pattern",
        "text_caption" to "Caption words", "text_arabicText" to "Arabic text words"
    )
    fun reason(code: String) = LABELS[code] ?: code.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** Moderator data access. Reads need the 1.1.5 rules; decisions go through the admin-checked callable. */
class ModeratorRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    companion object {
        const val DECISION_URL = "https://us-central1-noor-pro-d87e3.cloudfunctions.net/setModerationDecision"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val http by lazy { OkHttpClient.Builder().callTimeout(120, TimeUnit.SECONDS).build() }
    }
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    /** True for the `admin` custom claim or an admins/{uid} doc. Hidden entry points use this. */
    fun checkModerator(onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        user.getIdToken(false).addOnCompleteListener { t ->
            if (ModeratorAccess.isModerator(if (t.isSuccessful) t.result?.claims?.get("admin") else null, false)) return@addOnCompleteListener onResult(true)
            db.collection("admins").document(user.uid).get()
                .addOnSuccessListener { onResult(ModeratorAccess.isModerator(null, it.exists())) }
                .addOnFailureListener { onResult(false) }
        }
    }

    private fun ms(v: Any?): Long = (v as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L

    fun observeQueue(onResult: (List<ModerationItem>) -> Unit): ListenerRegistration =
        db.collection("moderation_queue").whereEqualTo("status", "open").limit(100)
            .addSnapshotListener { s, _ ->
                onResult(s?.documents.orEmpty().map { d ->
                    @Suppress("UNCHECKED_CAST")
                    val parts = ((d.get("scan") as? Map<String, Any?>)?.get("parts") as? List<Map<String, Any?>>).orEmpty()
                    @Suppress("UNCHECKED_CAST")
                    val scores = parts.flatMap { ((it["scores"] as? Map<String, Any?>).orEmpty()).entries }
                        .associate { it.key to it.value.toString() }
                    ModerationItem(d.id, d.getString("kind").orEmpty(), d.getString("targetId").orEmpty(), d.getString("creatorUid").orEmpty(),
                        d.getString("creatorName").orEmpty(), d.getString("type").orEmpty(), d.getString("caption").orEmpty(),
                        d.getString("mediaUrl").orEmpty(), d.getString("thumbnailUrl").orEmpty(),
                        (d.get("reasons") as? List<*>).orEmpty().map { it.toString() }, scores, ms(d.get("createdAt")))
                }.sortedByDescending { it.createdAt })
            }

    fun observeReports(onResult: (List<ModerationReport>) -> Unit): ListenerRegistration =
        db.collection("ummah_reports").whereEqualTo("status", "open").limit(100).addSnapshotListener { s, _ ->
            onResult(s?.documents.orEmpty().map { d ->
                ModerationReport(d.id, d.getString("targetType").orEmpty(), d.getString("targetId").orEmpty(), d.getString("targetUid").orEmpty(),
                    d.getString("reason").orEmpty(), d.getString("details").orEmpty(), d.getString("reporterUid").orEmpty(), ms(d.get("createdAt")))
            }.sortedByDescending { it.createdAt })
        }

    fun observeAudit(onResult: (List<AuditEntry>) -> Unit): ListenerRegistration =
        db.collection("moderation_audit").orderBy("at", Query.Direction.DESCENDING).limit(50).addSnapshotListener { s, _ ->
            onResult(s?.documents.orEmpty().map { d ->
                AuditEntry(d.id, d.getString("actorUid").orEmpty(), d.getString("action").orEmpty(), d.getString("kind").orEmpty(),
                    d.getString("subjectUid").orEmpty(), d.getString("reason").orEmpty(), ms(d.get("at")))
            })
        }

    fun strikes(uid: String, onResult: (StrikeRecord) -> Unit) {
        if (uid.isBlank()) return onResult(StrikeRecord())
        db.collection("user_strikes").document(uid).get().addOnCompleteListener { t ->
            val d = t.result
            onResult(StrikeRecord((d?.getLong("count") ?: 0).toInt(), d?.getLong("suspendedUntilMs") ?: 0, d?.getBoolean("banned") == true))
        }
    }

    fun decide(action: ModAction, target: String, id: String, reason: String = "", days: Int = 7,
               sensitive: Boolean = false, strike: Boolean = true, onResult: (Boolean, String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "Sign in required.")
        user.getIdToken(false).addOnCompleteListener { t ->
            val token = t.result?.token
            if (token == null) { main.post { onResult(false, "Sign in required.") }; return@addOnCompleteListener }
            val data = JSONObject().put("action", action.wire).put("target", target).put("id", id).put("reason", reason)
                .put("days", days).put("sensitive", sensitive).put("strike", strike)
            val req = Request.Builder().url(DECISION_URL).addHeader("Authorization", "Bearer $token")
                .post(JSONObject().put("data", data).toString().toRequestBody(JSON)).build()
            http.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { main.post { onResult(false, "Network error. Try again.") } }
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string().orEmpty()
                    val ok = response.isSuccessful && runCatching { JSONObject(body).getJSONObject("result").optBoolean("ok") }.getOrDefault(false)
                    val msg = if (ok) "${action.label} done." else runCatching { JSONObject(body).getJSONObject("error").optString("message") }
                        .getOrNull()?.takeIf { it.isNotBlank() } ?: "Couldn't apply (${response.code})."
                    main.post { onResult(ok, msg) }
                }
            })
        }
    }
}

/** The signed-in user's unseen moderation notices (blocked posts, removed comments, strikes). */
class ModerationNoticeStore(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun observe(onResult: (List<ModerationNotice>) -> Unit): ListenerRegistration? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return db.collection("moderation_notices").document(uid).collection("items").whereEqualTo("seen", false).limit(10)
            .addSnapshotListener { s, _ ->
                onResult(s?.documents.orEmpty().map { d ->
                    ModerationNotice(d.id, d.getString("kind").orEmpty(), d.getString("reason").orEmpty(),
                        (d.get("createdAt") as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L)
                }.sortedByDescending { it.createdAt })
            }
    }
    fun markSeen(id: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("moderation_notices").document(uid).collection("items").document(id).update("seen", true)
    }
}
