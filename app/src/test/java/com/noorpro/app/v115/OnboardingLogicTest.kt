package com.noorpro.app.v115

import com.noorpro.app.data.onboarding.*
import org.junit.Assert.*
import org.junit.Test

class OnboardingLogicTest {
    private val today = SimpleDate(2026, 11, 11)
    private val full = OnboardingDraft(
        fullName = "Mohammed Tauqeer", username = "tauqeer", reservedUsername = "tauqeer", gender = OnbGender.MALE, profileType = ProfileType.STUDENT,
        dob = SimpleDate(1998, 4, 2), country = "India", state = "Telangana", city = "Hyderabad", language = "ur",
    )

    @Test fun usernameNormalizeAndRules() {
        assertEquals("tauqeer.m_1", UsernameRules.normalize("  @Tauqeer.M_1!! "))
        assertEquals(20, UsernameRules.normalize("a".repeat(40)).length)
        assertTrue(UsernameRules.isValid("abc"))
        assertTrue(UsernameRules.isValid("noor.pro_2026"))
        listOf("ab", "a".repeat(21), ".abc", "abc.", "a..b", "12345", "ABC", "a b").forEach { assertFalse(it, UsernameRules.isValid(it)) }
        assertFalse(UsernameRules.isValid("admin"))
        assertNotNull(UsernameRules.problem("admin"))
    }

    @Test fun usernameSuggestionsAreValidAndUnique() {
        val s = UsernameRules.suggestions("Mohammed Tauqeer", 7)
        assertTrue(s.isNotEmpty())
        assertEquals(s.size, s.toSet().size)
        s.forEach { assertTrue(it, UsernameRules.isValid(it)) }
        UsernameRules.suggestions("محمد", 3).forEach { assertTrue(it, UsernameRules.isValid(it)) }
    }

    @Test fun datesAndAges() {
        assertEquals("1998-04-02", SimpleDate(1998, 4, 2).iso())
        assertEquals(SimpleDate(1998, 4, 2), SimpleDate.parse("1998-04-02"))
        assertNull(SimpleDate.parse("not a date"))
        assertNull(SimpleDate.parse(null))
        assertEquals(28, AgeRules.age(SimpleDate(1998, 4, 2), today))
        assertEquals(12, AgeRules.age(SimpleDate(2013, 11, 12), today)) // birthday tomorrow
        assertEquals(13, AgeRules.age(SimpleDate(2013, 11, 11), today))
        assertTrue(AgeRules.isChild(SimpleDate(2014, 1, 1), today))
        assertFalse(AgeRules.isChild(SimpleDate(2013, 11, 11), today))
        assertEquals("under-13", AgeRules.band(SimpleDate(2016, 1, 1), today))
        assertEquals("25-34", AgeRules.band(SimpleDate(1998, 4, 2), today))
        assertEquals("60+", AgeRules.band(SimpleDate(1950, 1, 1), today))
        assertFalse(AgeRules.plausible(SimpleDate(2027, 1, 1), today))
        assertFalse(AgeRules.plausible(SimpleDate(1900, 1, 1), today))
        assertTrue(AgeRules.plausible(SimpleDate(1998, 4, 2), today))
    }

    @Test fun stepsAndCompleteness() {
        assertTrue(OnboardingRules.isComplete(full, today))
        assertEquals(OnbStep.DONE, OnboardingRules.firstStep(full, today))
        assertEquals(OnbStep.NAME, OnboardingRules.firstStep(OnboardingDraft(), today))
        assertEquals(OnbStep.GENDER, OnboardingRules.firstStep(full.copy(gender = null), today))
        assertTrue(OnboardingRules.isComplete(full.copy(gender = OnbGender.UNSPECIFIED), today)) // "Prefer not to say" is an answer
        assertEquals(OnbStep.ADDRESS, OnboardingRules.firstStep(full.copy(city = " "), today))
        assertTrue(OnboardingRules.isComplete(full.copy(state = "", street = "", postalCode = ""), today)) // optional
        assertEquals(OnbStep.BIRTH, OnboardingRules.firstStep(full.copy(dob = SimpleDate(2030, 1, 1)), today))
        assertEquals(OnbStep.LANGUAGE, OnboardingRules.firstStep(full.copy(language = "xx"), today))
        assertEquals(OnbStep.USERNAME, OnboardingRules.firstStep(full.copy(username = "a"), today))
        assertNotNull(OnboardingRules.nameProblem("A"))
        assertNull(OnboardingRules.nameProblem("Aisha"))
    }

    @Test fun privateVsPublicFields() {
        val priv = OnboardingRules.privateDoc(full.copy(street = "12 Park Rd", postalCode = "500001"), today)
        assertEquals("1998-04-02", priv["dob"]); assertEquals("12 Park Rd", priv["street"]); assertEquals("male", priv["gender"])
        val pub = OnboardingRules.publicFields(full.copy(street = "12 Park Rd"), today)
        assertEquals("Mohammed Tauqeer", pub["name"]); assertEquals("tauqeer", pub["handle"]); assertEquals("student", pub["profileType"])
        assertEquals("Hyderabad", pub["city"]); assertEquals("India", pub["country"])
        listOf("dob", "street", "postalCode", "gender", "ageBand", "state", "isChild", "language").forEach { assertFalse(it, it in pub) }
        val hidden = OnboardingRules.publicFields(full.copy(showLocation = false), today)
        assertEquals("", hidden["city"]); assertEquals("", hidden["country"]) // cleared on merge
        val child = OnboardingRules.publicFields(full.copy(dob = SimpleDate(2016, 5, 5)), today)
        assertEquals("", child["city"]); assertEquals(true, child["isPrivate"])
        assertEquals(true, OnboardingRules.privateDoc(full.copy(dob = SimpleDate(2016, 5, 5)), today)["isChild"])
    }

    @Test fun scholarTypeIsNeverPublicUntilVerified() {
        assertEquals("regular", ProfileType.SCHOLAR.publicCode)
        assertEquals("regular", OnboardingRules.publicFields(full.copy(profileType = ProfileType.SCHOLAR), today)["profileType"])
        assertEquals("masjid", ProfileType.MASJID.publicCode)
        assertEquals("scholar", OnboardingRules.privateDoc(full.copy(profileType = ProfileType.SCHOLAR), today)["profileType"])
    }

    @Test fun languages() {
        assertEquals("UR", primaryLanguage("ur")!!.appUi)
        assertEquals("AR", primaryLanguage("ar")!!.appUi)
        assertEquals("EN", primaryLanguage("ur-Latn")!!.appUi)
        assertEquals("ur-Latn", primaryLanguage("ur-Latn")!!.noorAi)
        assertNull(primaryLanguage("xx"))
        assertEquals(PRIMARY_LANGUAGES.size, PRIMARY_LANGUAGES.map { it.code }.toSet().size)
    }

    private val good = ScholarApplication(name = "Aisha Siddiqa", title = "Alimah", institution = "Jamia Darul Uloom", maslak = Maslak.HANAFI,
        years = 6, refName = "Mufti Yusuf", refPhone = "+91 98765 43210", bio = "Studied the Dars-e-Nizami for eight years and teach fiqh.")

    @Test fun scholarApplicationValidation() {
        assertTrue(ScholarRules.problems(good, hasCertificate = true).isEmpty())
        assertEquals(setOf("cert"), ScholarRules.problems(good, hasCertificate = false).keys)
        assertTrue("maslak" in ScholarRules.problems(good.copy(maslak = null), true))
        assertTrue("refPhone" in ScholarRules.problems(good.copy(refPhone = "call me"), true))
        assertTrue("bio" in ScholarRules.problems(good.copy(bio = "Short"), true))
        assertTrue("years" in ScholarRules.problems(good.copy(years = 99), true))
        assertTrue("silsila" in ScholarRules.problems(good.copy(silsila = "x".repeat(121)), true))
        assertTrue(ScholarRules.problems(good.copy(silsila = "Chishti"), true).isEmpty())
    }

    @Test fun scholarDocs() {
        val path = ScholarRules.certPath("u1", "JPG", 42)
        assertEquals("scholar_certificates/u1/ijazah_42.jpg", path)
        val d = ScholarRules.applicationDoc(good, "u1", path)
        assertEquals("pending", d["status"]); assertEquals("hanafi", d["maslak"]); assertEquals(6, d["years"]); assertEquals("u1", d["uid"])
        assertFalse("submittedAt" in d) // set with serverTimestamp by the app
        assertEquals(mapOf("name" to "Aisha Siddiqa", "title" to "Alimah", "verified" to true), ScholarRules.scholarDoc(good))
        assertNotNull(ScholarRules.rejectProblem("no"))
        assertNull(ScholarRules.rejectProblem("Please upload a clearer ijazah."))
        assertTrue(ScholarRules.canApply(ApplicationStatus.NONE)); assertTrue(ScholarRules.canApply(ApplicationStatus.REJECTED))
        assertFalse(ScholarRules.canApply(ApplicationStatus.PENDING)); assertFalse(ScholarRules.canApply(ApplicationStatus.APPROVED))
        assertEquals(ApplicationStatus.NONE, ApplicationStatus.of("weird")); assertEquals(ApplicationStatus.REVOKED, ApplicationStatus.of("revoked"))
    }
}

class NoorAiCopyTest {
    private val answer = """## Combining prayers
- In the **Hanafi** madhhab each prayer is in its own time.
- Others allow joining for the traveller.

Sources: Qur'an 4:103; Sahih Muslim 705
FOLLOWUPS: What distance counts? | How to shorten?"""

    @Test fun plainKeepsReferencesDropsMarkdownAndMeta() {
        val t = com.noorpro.app.ui.screens.NoorAiCopy.plain(answer)
        assertTrue(t, t.startsWith("Combining prayers"))
        assertTrue(t.contains("• In the Hanafi madhhab each prayer is in its own time.\n• Others allow"))
        assertTrue(t.contains("Qur'an 4:103")); assertTrue(t.contains("Sahih Muslim 705"))
        assertFalse(t.contains("**")); assertFalse(t.contains("##")); assertFalse(t.contains("FOLLOWUPS")); assertFalse(t.contains("What distance counts"))
    }

    @Test fun shareAddsFooter() {
        val s = com.noorpro.app.ui.screens.NoorAiCopy.shareText("Hello **world**")
        assertTrue(s.startsWith("Hello world")); assertTrue(s.endsWith(com.noorpro.app.ui.screens.NoorAiCopy.SHARE_FOOTER))
    }
}
