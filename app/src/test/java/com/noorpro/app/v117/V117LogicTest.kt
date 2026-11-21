package com.noorpro.app.v117

import com.noorpro.app.audio.catalog.BarakahMelodies
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.Track
import com.noorpro.app.data.NoorAiOptions
import com.noorpro.app.data.StreamAccumulator
import com.noorpro.app.data.onboarding.*
import com.noorpro.app.islamic.social.CardContent
import com.noorpro.app.islamic.social.CardKind
import com.noorpro.app.islamic.social.ShareCardLogic
import com.noorpro.app.ui.screens.ProfileEditLogic
import com.noorpro.app.ui.screens.adhkarShareText
import com.noorpro.app.audio.licensing.BarakahStats
import com.noorpro.app.audio.licensing.LicensedAudioGate
import com.noorpro.app.data.media.edit.ReelSounds
import org.junit.Assert.*
import org.junit.Test

/** 1.2.0 (117): profile edit, Noor AI streaming + Fast default, Barakah Melodies licence rules, copy text. */
class V117LogicTest {
    private val today = SimpleDate(2026, 11, 11)
    private val full = OnboardingDraft(
        fullName = "Mohammed Tauqeer", username = "tauqeer", reservedUsername = "tauqeer", gender = OnbGender.MALE,
        profileType = ProfileType.STUDENT, dob = SimpleDate(1998, 4, 2), country = "India", state = "Telangana", city = "Hyderabad",
        language = "ur",
    )

    @Test fun profileEditMergeAndValidate() {
        val m = ProfileEditLogic.merge(full, "  Mohammed T  ", "@Tauqeer.Noor")
        assertEquals("Mohammed T", m.fullName)
        assertEquals("tauqeer.noor", m.username)
        assertTrue(ProfileEditLogic.usernameChanged(m))
        assertFalse(ProfileEditLogic.usernameChanged(full))
        assertNull(ProfileEditLogic.problem(full, today))
        assertTrue(ProfileEditLogic.problem(full.copy(city = ""), today)!!.startsWith("Where you live"))
        assertTrue(ProfileEditLogic.problem(full.copy(dob = null), today)!!.startsWith("Date of birth"))
        assertNotNull(ProfileEditLogic.problem(full.copy(fullName = ""), today))
    }

    @Test fun profileEditDatesAndDisplay() {
        assertEquals(SimpleDate(1998, 4, 2), ProfileEditLogic.date("2", "4", "1998"))
        assertNull(ProfileEditLogic.date("31", "2", "2000"))   // no 31 Feb
        assertNull(ProfileEditLogic.date("29", "2", "2023"))   // not a leap year
        assertEquals(SimpleDate(2024, 2, 29), ProfileEditLogic.date("29", "2", "2024"))
        assertNull(ProfileEditLogic.date("", "4", "1998"))
        assertEquals("Hyderabad, India", ProfileEditLogic.place(full))
        assertEquals("Student of knowledge · Urdu", ProfileEditLogic.aboutLine(full))
        assertEquals("", ProfileEditLogic.aboutLine(full.copy(profileType = ProfileType.REGULAR, language = "")))
    }

    @Test fun privateDetailsStayOffThePublicCard() {
        val pub = OnboardingRules.publicFields(full, today)
        assertEquals(setOf("name", "handle", "profileType", "city", "country"), pub.keys)
        listOf("dob", "gender", "street", "postalCode", "phone", "phoneNumber", "language").forEach { assertFalse(it in pub) }
    }

    @Test fun noorAiFastIsDefaultAndStreamFolding() {
        assertEquals(NoorAiOptions.FAST, NoorAiOptions.normTier(null))
        assertEquals(NoorAiOptions.BALANCED, NoorAiOptions.normTier("balanced"))
        assertEquals("Fast", NoorAiOptions.tierLabel(""))
        val a = StreamAccumulator()
        assertEquals(StreamAccumulator.Step.NONE, a.onEvent("start", ""))
        assertEquals(StreamAccumulator.Step.TEXT, a.onEvent("delta", "Salam "))
        assertEquals(StreamAccumulator.Step.TEXT, a.onEvent("delta", "alaykum"))
        assertEquals("Salam alaykum", a.text)
        assertEquals(StreamAccumulator.Step.TEXT, a.onEvent("reset", ""))
        assertEquals("", a.text)
        assertEquals(StreamAccumulator.Step.ANSWER, a.onEvent("answer", ""))
        assertEquals(StreamAccumulator.Step.ERROR, a.onEvent("error", ""))
    }

    @Test fun barakahMelodiesLicenceRules() {
        assertEquals("https://open.spotify.com/artist/048kFRuX8hS3DJSbayEKWt", BarakahMelodies.SPOTIFY_URL)
        assertTrue(BarakahMelodies.CREDIT.contains("Barakah Melodies") && BarakahMelodies.CREDIT.contains("Safar Media"))
        assertTrue(BarakahMelodies.entries.isEmpty()) // files not delivered yet: nothing ripped, nothing streamed
        assertFalse(BarakahMelodies.available)
        assertFalse(BarakahMelodies.isAllowedSource("https://open.spotify.com/track/x"))
        assertFalse(BarakahMelodies.isAllowedSource("https://p.scdn.co/mp3-preview/x"))
        assertFalse(BarakahMelodies.isAllowedSource("https://www.youtube.com/watch?v=x"))
        assertFalse(BarakahMelodies.isAllowedSource("http://cdn.noorpro.app/a.mp3"))
        assertTrue(BarakahMelodies.isAllowedSource("https://firebasestorage.googleapis.com/v0/b/noor/o/bm%2Fa.mp3"))
        val t = BarakahMelodies.toTrack(BarakahMelodies.Entry("tala", "Tala'al Badru", "https://cdn.example/a.mp3"))
        assertEquals(LicenseType.PERMISSION_LETTER, t.license)
        assertEquals("Barakah Melodies · Tala'al Badru", BarakahMelodies.credit(t))
        assertTrue(t.attributionText.contains("Barakah Melodies") && t.attributionText.contains("Tala'al Badru"))
        assertEquals(30_000L, BarakahMelodies.reelClipLimitMs(t))
        assertFalse(BarakahMelodies.canShareOutside(t))
        val other = Track("x", "Other", "maf-nasheed", "MAF", "https://a/b.mp3", 0, license = LicenseType.PERMISSION_LETTER, attributionText = "", sourceUrl = "")
        assertNull(BarakahMelodies.reelClipLimitMs(other)); assertTrue(BarakahMelodies.canShareOutside(other)); assertNull(BarakahMelodies.credit(other))
    }

    @Test fun copyTextForCardsAndAdhkar() {
        val c = CardContent(CardKind.AYAH, "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "Allah does not burden a soul beyond what it can bear.", "Qur'an 2:286", "2:286")
        assertEquals("لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا\n\nAllah does not burden a soul beyond what it can bear.\n\n— Qur'an 2:286", ShareCardLogic.copyText(c))
        assertEquals("Allah does not burden a soul beyond what it can bear.\n\n— Qur'an 2:286", ShareCardLogic.copyText(c, withArabic = false))
        assertEquals("Ayat al-Kursi\n\nاللَّهُ لَا إِلَٰهَ\n\nMeaning\n\n— Bukhari 2311", adhkarShareText("Ayat al-Kursi", "اللَّهُ لَا إِلَٰهَ", "", "Meaning", "Bukhari 2311"))
    }

    @Test fun barakahNoAdsNoAiNoDownloadNoExternalReels() {
        LicensedAudioGate.resetForTests()
        val bm = BarakahMelodies.toTrack(BarakahMelodies.Entry("zikr", "Zikr of the Heart", "https://cdn.noorpro.app/bm/zikr.m4a"))
        val other = Track("q1", "Al-Fatiha", "alafasy", "Alafasy", "https://a/b.mp3", 0, license = LicenseType.PUBLIC_DOMAIN, attributionText = "x", sourceUrl = "")
        assertTrue(LicensedAudioGate.adsAllowed)
        assertEquals("bm-zikr", LicensedAudioGate.onPlayer(bm, audible = true)) // first play counted
        assertFalse(LicensedAudioGate.adsAllowed)                                // no ads while it plays
        assertNull(LicensedAudioGate.onPlayer(bm, audible = false))              // pause: not a new play
        assertTrue(LicensedAudioGate.adsAllowed)
        assertNull(LicensedAudioGate.onPlayer(bm, audible = true))               // resume: still one play
        assertNull(LicensedAudioGate.onPlayer(other, audible = true)); assertTrue(LicensedAudioGate.adsAllowed)
        assertEquals("bm-zikr", LicensedAudioGate.onPlayer(bm, audible = true))  // played again later: counted
        LicensedAudioGate.resetForTests()
        assertFalse(BarakahMelodies.aiMayProcess(bm)); assertFalse(BarakahMelodies.aiMayProcess("bm-zikr")); assertTrue(BarakahMelodies.aiMayProcess(other))
        assertFalse(BarakahMelodies.canDownload(bm)); assertTrue(BarakahMelodies.canDownload(other))
        assertFalse(BarakahMelodies.canShareOutside("bm-zikr")); assertTrue(BarakahMelodies.canShareOutside("q1"))
        assertNull(ReelSounds.from(bm)) // not in the reel sound library until the in-app-only reel path is enabled
        assertEquals("♪ Barakah Melodies · Zikr of the Heart (licensed by Safar Media)", BarakahMelodies.reelCredit("Zikr of the Heart"))
    }

    @Test fun barakahQuarterlyReport() {
        assertEquals(listOf("2027-04", "2027-05", "2027-06"), BarakahStats.quarterMonths(2027, 2))
        assertEquals("zikr", BarakahStats.trackKey("bm-zikr"))
        val csv = BarakahStats.toCsv(listOf(
            BarakahStats.MonthReport("2027-04", 120, mapOf("zikr" to 300L, "our-duaa" to 45L)),
            BarakahStats.MonthReport("2027-05", 0, emptyMap())))
        assertEquals("month,listeners,track,plays\n2027-04,120,our-duaa,45\n2027-04,120,zikr,300\n2027-05,0,,0\n", csv)
    }
}
