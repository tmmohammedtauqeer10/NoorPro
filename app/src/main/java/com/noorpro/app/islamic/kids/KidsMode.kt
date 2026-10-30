package com.noorpro.app.islamic.kids

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.noorpro.app.ui.viewmodel.DeenScreen
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.random.Random

/*
 * 1.1.6 Family & Kids (Remote Config family_kids_enabled, default OFF). Everything is on-device:
 * no account, no network, nothing uploaded. Kids mode hides Reels, Ummah social and messaging
 * until a parent enters the PIN (salted SHA-256, never stored in clear).
 */

object KidsMode {
    /** Compose-observable so MainActivity / More re-filter the moment kids mode flips. */
    var active by mutableStateOf(false)
        internal set
    private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        active = KidsStore(context).isActive()
    }

    /** Social / messaging / creator surfaces that children shouldn't reach. */
    fun blocks(screen: DeenScreen): Boolean = screen.name.startsWith("UMMAH") || screen.name.startsWith("REELS") ||
        screen in setOf(DeenScreen.CREATOR_STUDIO, DeenScreen.DISCOVER_GROUPS, DeenScreen.AI_HUB, DeenScreen.MODERATOR_PANEL, DeenScreen.ACCOUNT_SWITCHER)

    /** Kids mode only applies while family_kids_enabled is on (so a remote switch-off can never trap a child's device). */
    val enforced: Boolean get() = active && com.noorpro.app.islamic.IslamicFlags.isOn(com.noorpro.app.islamic.IslamicFlags.FAMILY_KIDS)

    /** Where a blocked screen lands instead while kids mode is on. */
    fun redirect(screen: DeenScreen): DeenScreen = if (enforced && blocks(screen)) DeenScreen.FAMILY_KIDS else screen
}

object KidsPin {
    const val LENGTH = 4
    const val MAX_TRIES = 5
    const val LOCK_MS = 30_000L
    fun isValidNew(pin: String) = pin.length == LENGTH && pin.all { it.isDigit() } && pin.toSet().size > 1 && pin !in setOf("1234", "4321", "0123", "9876")
    fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray()).joinToString("") { "%02x".format(it) }

    /** "Forgot PIN" parental gate: a sum a young child is unlikely to solve. */
    data class Gate(val a: Int, val b: Int, val c: Int) { val question get() = "$a × $b + $c"; val answer get() = a * b + c }
    fun gate(r: Random = Random): Gate = Gate(r.nextInt(6, 10), r.nextInt(6, 10), r.nextInt(11, 30))
}

class KidsStore(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val prefs = context.applicationContext.getSharedPreferences("family_kids_v1", Context.MODE_PRIVATE)

    fun hasPin() = prefs.contains("pin_hash")
    fun setPin(pin: String): Boolean {
        if (!KidsPin.isValidNew(pin)) return false
        val salt = KidsPin.newSalt()
        prefs.edit().putString("pin_salt", salt).putString("pin_hash", KidsPin.hash(pin, salt)).putInt("tries", 0).putLong("lock_until", 0).apply()
        return true
    }
    fun lockedForMs(): Long = (prefs.getLong("lock_until", 0) - now()).coerceAtLeast(0)
    /** True when the PIN matches. Five wrong tries lock entry for 30 s. */
    fun checkPin(pin: String): Boolean {
        if (lockedForMs() > 0) return false
        val ok = KidsPin.hash(pin, prefs.getString("pin_salt", "").orEmpty()) == prefs.getString("pin_hash", null)
        val tries = if (ok) 0 else prefs.getInt("tries", 0) + 1
        prefs.edit().putInt("tries", if (tries >= KidsPin.MAX_TRIES) 0 else tries)
            .putLong("lock_until", if (tries >= KidsPin.MAX_TRIES) now() + KidsPin.LOCK_MS else prefs.getLong("lock_until", 0)).apply()
        return ok
    }
    fun clearPin() { prefs.edit().remove("pin_hash").remove("pin_salt").putInt("tries", 0).apply() }

    fun isActive() = prefs.getBoolean("active", false)
    fun setActive(on: Boolean) { prefs.edit().putBoolean("active", on).apply(); KidsMode.active = on }

    fun childName(): String = prefs.getString("child", "").orEmpty()
    fun setChildName(n: String) { prefs.edit().putString("child", n.trim().take(24)).apply() }

    // Stars: one per activity per day, so it rewards habits rather than tapping.
    fun stars(): Int = prefs.getInt("stars", 0)
    fun earnedToday(key: String, day: Long): Boolean = prefs.getStringSet("earned_$day", emptySet())!!.contains(key)
    fun todayCount(day: Long): Int = prefs.getStringSet("earned_$day", emptySet())!!.size
    fun earn(key: String, day: Long): Boolean {
        val set = prefs.getStringSet("earned_$day", emptySet())!!
        if (key in set) return false
        prefs.edit().putStringSet("earned_$day", set + key).putInt("stars", stars() + 1).apply()
        return true
    }
}

/* ── Family prayer tracker (local only) ─────────────────────────────────────────────────────── */

data class FamilyMember(val id: String, val name: String, val hue: Int)

val FAMILY_PRAYERS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

object FamilyLogic {
    fun has(mask: Int, prayer: Int) = mask and (1 shl prayer) != 0
    fun toggle(mask: Int, prayer: Int) = mask xor (1 shl prayer)
    fun count(mask: Int) = Integer.bitCount(mask and 0b11111)
    /** Prayers logged for a member over [days] (each 0..5). */
    fun week(log: Map<Long, Map<String, Int>>, memberId: String, days: List<Long>) = days.map { count(log[it]?.get(memberId) ?: 0) }
    fun cleanName(s: String) = s.trim().replace(Regex("\\s+"), " ").take(24)
}

class FamilyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("family_prayer_v1", Context.MODE_PRIVATE)
    companion object { const val MAX_MEMBERS = 10; const val KEEP_DAYS = 60 }

    fun members(): List<FamilyMember> = runCatching {
        val a = JSONArray(prefs.getString("members", "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> FamilyMember(o.getString("id"), o.getString("name"), o.optInt("hue")) } }
    }.getOrDefault(emptyList())

    private fun saveMembers(l: List<FamilyMember>) = prefs.edit().putString("members",
        JSONArray(l.map { JSONObject().put("id", it.id).put("name", it.name).put("hue", it.hue) }).toString()).apply()

    fun addMember(name: String): FamilyMember? {
        val n = FamilyLogic.cleanName(name); val l = members()
        if (n.isEmpty() || l.size >= MAX_MEMBERS) return null
        val m = FamilyMember("m" + System.nanoTime().toString(36), n, (l.maxOfOrNull { it.hue } ?: -1) + 1)
        saveMembers(l + m); return m
    }
    fun removeMember(id: String) = saveMembers(members().filter { it.id != id })

    fun log(): Map<Long, Map<String, Int>> = runCatching {
        val o = JSONObject(prefs.getString("log", "{}"))
        o.keys().asSequence().associate { d -> d.toLong() to o.getJSONObject(d).let { m -> m.keys().asSequence().associateWith { m.getInt(it) } } }
    }.getOrDefault(emptyMap())

    fun toggle(day: Long, memberId: String, prayer: Int): Map<Long, Map<String, Int>> {
        val cur = log().toMutableMap()
        val dayMap = cur[day].orEmpty().toMutableMap()
        dayMap[memberId] = FamilyLogic.toggle(dayMap[memberId] ?: 0, prayer)
        cur[day] = dayMap
        val kept = cur.filterKeys { it > day - KEEP_DAYS }
        prefs.edit().putString("log", JSONObject().apply { kept.forEach { (d, m) -> put(d.toString(), JSONObject(m as Map<*, *>)) } }.toString()).apply()
        return kept
    }
}
