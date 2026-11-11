package com.noorpro.app.data.onboarding

/**
 * 1.2.0 (115) post-login onboarding + scholar applications: pure rules (no Android / Firebase types) so they are
 * unit tested on the JVM. Firestore layout:
 *  - users/{uid}/private/profile   PRIVATE (owner + moderators): gender, dob, ageBand, street address, phone-free.
 *  - ummah_profiles/{uid}          PUBLIC card: name, handle, profileType (never "scholar"), city/country only if allowed.
 *  - usernames/{name}              {uid, at} unique reservation (transaction).
 *  - scholar_applications/{uid}    owner + moderators; status pending/approved/rejected/revoked.
 *  - scholars/{uid}                public {name, title, verified}; written by moderators on approval.
 */
enum class OnbGender(val code: String, val label: String) {
    MALE("male", "Male"), FEMALE("female", "Female"),
    /** The app's sisters/brothers circles already treat an unset gender as "neither", so this is safe to offer. */
    UNSPECIFIED("", "Prefer not to say");
    companion object { fun of(code: String?) = entries.firstOrNull { it.code == code } ?: UNSPECIFIED }
}

enum class ProfileType(val code: String, val label: String, val blurb: String) {
    REGULAR("regular", "Regular user", "Prayer, Qur'an and community"),
    STUDENT("student", "Student of knowledge", "Learning in a madrasa, course or with a teacher"),
    SCHOLAR("scholar", "Scholar / Alim(ah)", "Answer questions after your ijazah is verified"),
    CREATOR("creator", "Creator", "Share reels, posts and reminders"),
    MASJID("masjid", "Masjid / Madrasa", "An official account for an institution");
    companion object { fun of(code: String?) = entries.firstOrNull { it.code == code } }

    /** Public label: a self-declared "scholar" is not shown until an admin verifies them (then the badge shows). */
    val publicCode: String get() = if (this == SCHOLAR) REGULAR.code else code
}

/** Primary languages the app and Noor AI support. [noorAi] is the Noor AI answer language; [appUi] the AppLanguage name. */
data class PrimaryLanguage(val code: String, val label: String, val noorAi: String, val appUi: String)

val PRIMARY_LANGUAGES = listOf(
    PrimaryLanguage("en", "English", "en", "EN"),
    PrimaryLanguage("ur", "اردو · Urdu", "ur", "UR"),
    PrimaryLanguage("ur-Latn", "Roman Urdu", "ur-Latn", "EN"),
    PrimaryLanguage("hi", "हिन्दी · Hindi", "hi", "EN"),
    PrimaryLanguage("ar", "العربية · Arabic", "ar", "AR"),
    PrimaryLanguage("bn", "বাংলা · Bengali", "bn", "EN"),
)
fun primaryLanguage(code: String?) = PRIMARY_LANGUAGES.firstOrNull { it.code == code }

object UsernameRules {
    const val MIN = 3
    const val MAX = 20
    private val RESERVED = setOf("admin", "admins", "noorpro", "noor", "noor.pro", "noorai", "support", "help", "moderator", "official", "scholar", "scholars", "mufti", "imam", "root", "system", "null", "undefined", "me", "settings")

    /** Lowercases and strips @, spaces and anything outside a-z 0-9 . _ (keeps what the user meant). */
    fun normalize(raw: String): String = raw.trim().removePrefix("@").lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' }.take(MAX)

    /** Null when valid, else a short human message. */
    fun problem(name: String): String? = when {
        name.length < MIN -> "Use at least $MIN characters."
        name.length > MAX -> "Use at most $MAX characters."
        !name.all { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' } -> "Only lowercase letters, numbers, . and _"
        name.startsWith('.') || name.endsWith('.') -> "Can't start or end with a dot."
        ".." in name -> "Can't have two dots in a row."
        name.all { it.isDigit() } -> "Add at least one letter."
        name in RESERVED -> "That username is reserved."
        else -> null
    }

    fun isValid(name: String) = problem(name) == null

    /** Suggestions from a display name (Google name), e.g. "Mohammed Tauqeer" -> mohammed.tauqeer, mohammedtauqeer, … */
    fun suggestions(fullName: String, seed: Int = 0): List<String> {
        val parts = fullName.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return emptyList()
        val first = parts.first(); val last = parts.drop(1).lastOrNull().orEmpty()
        val n = (seed % 900 + 100)
        return listOfNotNull(
            if (last.isNotEmpty()) "$first.$last" else null,
            if (last.isNotEmpty()) "$first$last" else null,
            if (last.isNotEmpty()) "${first}_$last" else null,
            first, "$first$n", "$first.$n",
        ).map { normalize(it) }.filter { isValid(it) }.distinct().take(4)
    }
}

/** A calendar date without time zones. */
data class SimpleDate(val year: Int, val month: Int, val day: Int) {
    fun iso() = "%04d-%02d-%02d".format(year, month, day)
    companion object {
        fun parse(iso: String?): SimpleDate? = runCatching {
            val (y, m, d) = iso!!.trim().split("-").map { it.toInt() }
            if (m !in 1..12 || d !in 1..31) null else SimpleDate(y, m, d)
        }.getOrNull()
    }
}

object AgeRules {
    const val CHILD_UNDER = 13
    fun age(dob: SimpleDate, today: SimpleDate): Int {
        var a = today.year - dob.year
        if (today.month < dob.month || (today.month == dob.month && today.day < dob.day)) a--
        return a
    }
    fun isChild(dob: SimpleDate, today: SimpleDate) = age(dob, today) < CHILD_UNDER
    /** Only this band may ever be shown; the date itself stays private. */
    fun band(dob: SimpleDate, today: SimpleDate): String = when (val a = age(dob, today)) {
        in Int.MIN_VALUE until 13 -> "under-13"
        in 13..17 -> "13-17"
        in 18..24 -> "18-24"
        in 25..34 -> "25-34"
        in 35..44 -> "35-44"
        in 45..59 -> "45-59"
        else -> if (a >= 60) "60+" else "unknown"
    }
    fun plausible(dob: SimpleDate, today: SimpleDate): Boolean = age(dob, today) in 3..110 && (dob.year < today.year || (dob.year == today.year && (dob.month < today.month || (dob.month == today.month && dob.day <= today.day))))
}

data class OnboardingDraft(
    val fullName: String = "",
    val username: String = "",
    /** The username this account already holds in usernames/ (if any); kept when unchanged. */
    val reservedUsername: String = "",
    val gender: OnbGender? = null,
    val profileType: ProfileType? = null,
    val dob: SimpleDate? = null,
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val street: String = "",
    val postalCode: String = "",
    val showLocation: Boolean = true,
    val language: String = "",
    val photoUrl: String = "",
)

enum class OnbStep(val title: String) { NAME("Your name"), USERNAME("Username"), GENDER("About you"), TYPE("Profile type"), BIRTH("Date of birth"), ADDRESS("Where you live"), LANGUAGE("Primary language"), DONE("All set") }

object OnboardingRules {
    val STEPS = OnbStep.entries.filter { it != OnbStep.DONE }

    fun nameProblem(n: String): String? {
        val t = n.trim()
        return when {
            t.length < 2 -> "Please enter your name."
            t.length > 60 -> "Keep it under 60 characters."
            t.any { it.isDigit() } && t.count { it.isLetter() } < 2 -> "Use letters for your name."
            else -> null
        }
    }

    /** Null when the step can continue. Username availability is checked separately (server). */
    fun stepProblem(step: OnbStep, d: OnboardingDraft, today: SimpleDate): String? = when (step) {
        OnbStep.NAME -> nameProblem(d.fullName)
        OnbStep.USERNAME -> UsernameRules.problem(d.username)
        OnbStep.GENDER -> if (d.gender == null) "Please choose one." else null
        OnbStep.TYPE -> if (d.profileType == null) "Please choose one." else null
        OnbStep.BIRTH -> when {
            d.dob == null -> "Please pick your date of birth."
            !AgeRules.plausible(d.dob, today) -> "Please check the date."
            else -> null
        }
        OnbStep.ADDRESS -> when {
            d.country.isBlank() -> "Please add your country."
            d.city.isBlank() -> "Please add your city or town."
            else -> null
        }
        OnbStep.LANGUAGE -> if (primaryLanguage(d.language) == null) "Please choose a language." else null
        OnbStep.DONE -> null
    }

    /** Required steps still missing (used to show onboarding to existing users with an incomplete profile). */
    fun missing(d: OnboardingDraft, today: SimpleDate): List<OnbStep> = STEPS.filter { stepProblem(it, d, today) != null }

    fun isComplete(d: OnboardingDraft, today: SimpleDate) = missing(d, today).isEmpty()

    /** First step to show: the first incomplete one (existing users skip what they already filled). */
    fun firstStep(d: OnboardingDraft, today: SimpleDate): OnbStep = missing(d, today).firstOrNull() ?: OnbStep.DONE

    /** PRIVATE document: owner + moderators only. Street and postal code never leave it. */
    fun privateDoc(d: OnboardingDraft, today: SimpleDate): Map<String, Any> = buildMap {
        put("fullName", d.fullName.trim())
        put("username", d.username)
        put("gender", d.gender?.code ?: "")
        put("profileType", d.profileType?.code ?: ProfileType.REGULAR.code)
        d.dob?.let { put("dob", it.iso()); put("ageBand", AgeRules.band(it, today)); put("isChild", AgeRules.isChild(it, today)) }
        put("country", d.country.trim().take(60)); put("state", d.state.trim().take(60)); put("city", d.city.trim().take(60))
        put("street", d.street.trim().take(200)); put("postalCode", d.postalCode.trim().take(20))
        put("showLocation", d.showLocation)
        put("language", d.language)
        put("version", 1)
    }

    /** PUBLIC card fields: name, handle, profile type (scholar only after verification), city/country if allowed and not a child. */
    fun publicFields(d: OnboardingDraft, today: SimpleDate): Map<String, Any> {
        val child = d.dob?.let { AgeRules.isChild(it, today) } == true
        val show = d.showLocation && !child
        return buildMap {
            put("name", d.fullName.trim().take(60))
            put("handle", d.username)
            put("profileType", (d.profileType ?: ProfileType.REGULAR).publicCode)
            put("city", if (show) d.city.trim().take(60) else "")
            put("country", if (show) d.country.trim().take(60) else "")
            if (child) put("isPrivate", true)
        }
    }
}

// ---------------------------------------------------------------- scholar applications

enum class Maslak(val code: String, val label: String) {
    HANAFI("hanafi", "Sunni Hanafi"), SHAFII("shafii", "Sunni Shafi'i"), MALIKI("maliki", "Sunni Maliki"), HANBALI("hanbali", "Sunni Hanbali");
    companion object { fun of(code: String?) = entries.firstOrNull { it.code == code } }
}

enum class ApplicationStatus(val code: String, val label: String) {
    NONE("", "Not applied"), PENDING("pending", "Under review"), APPROVED("approved", "Approved"), REJECTED("rejected", "Not approved"), REVOKED("revoked", "Verification removed");
    companion object { fun of(code: String?) = entries.firstOrNull { it.code == code && it != NONE } ?: NONE }
}

data class ScholarApplication(
    val uid: String = "",
    val name: String = "",
    val title: String = "",
    val institution: String = "",
    val maslak: Maslak? = null,
    val silsila: String = "",
    val years: Int? = null,
    val refName: String = "",
    val refPhone: String = "",
    val bio: String = "",
    val certPath: String = "",
    val status: ApplicationStatus = ApplicationStatus.NONE,
    val reason: String = "",
    val submittedAt: Long = 0,
    val decidedAt: Long = 0,
)

object ScholarRules {
    val PHONE = Regex("^\\+?[0-9][0-9 ()-]{6,18}[0-9]$")

    /** Field -> problem; empty when the form can be submitted ([hasCertificate] = a photo/PDF is chosen or already uploaded). */
    fun problems(a: ScholarApplication, hasCertificate: Boolean): Map<String, String> = buildMap {
        if (a.name.trim().length !in 2..80) put("name", "Enter your full name.")
        if (a.title.trim().length !in 2..80) put("title", "e.g. Mufti, Maulana, Alimah, Shaykh.")
        if (a.institution.trim().length !in 2..120) put("institution", "Your madrasa or Dar-ul-Ifta.")
        if (a.maslak == null) put("maslak", "Choose your madhhab.")
        if (a.silsila.trim().length > 120) put("silsila", "Keep it under 120 characters.")
        if (a.years == null || a.years !in 0..80) put("years", "Years of teaching (0-80).")
        if (a.refName.trim().length !in 2..80) put("refName", "Name of someone who can confirm your studies.")
        if (!PHONE.matches(a.refPhone.trim())) put("refPhone", "Phone with country code, e.g. +91 98765 43210.")
        if (a.bio.trim().length !in 30..600) put("bio", "Write 30-600 characters about your studies and teaching.")
        if (!hasCertificate) put("cert", "Add a photo of your ijazah or certificate.")
    }

    /** Firestore document for scholar_applications/{uid} (status always pending on submit). */
    fun applicationDoc(a: ScholarApplication, uid: String, certPath: String): Map<String, Any> = mapOf(
        "uid" to uid, "name" to a.name.trim(), "title" to a.title.trim(), "institution" to a.institution.trim(),
        "maslak" to (a.maslak ?: Maslak.HANAFI).code, "silsila" to a.silsila.trim(), "years" to (a.years ?: 0),
        "refName" to a.refName.trim(), "refPhone" to a.refPhone.trim(), "bio" to a.bio.trim(), "certPath" to certPath, "status" to ApplicationStatus.PENDING.code,
    )

    fun certPath(uid: String, ext: String, now: Long) = "scholar_certificates/$uid/ijazah_$now.${ext.lowercase().ifBlank { "jpg" }}"

    /** Public scholars/{uid} written on approval. */
    fun scholarDoc(a: ScholarApplication): Map<String, Any> = mapOf("name" to a.name.trim().take(80), "title" to a.title.trim().take(80), "verified" to true)

    fun rejectProblem(reason: String): String? = if (reason.trim().length !in 5..500) "Give a short reason (5-500 characters)." else null

    fun canApply(status: ApplicationStatus) = status == ApplicationStatus.NONE || status == ApplicationStatus.REJECTED || status == ApplicationStatus.REVOKED
}
