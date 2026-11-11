package com.noorpro.app.v115

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.islamic.*
import com.noorpro.app.islamic.social.*
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.data.onboarding.*
import com.noorpro.app.ui.onboarding.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 1.2.0 (115) previews -> build/v115/<name>.png (onboarding pages, scholar form, admin review). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class OnboardingPreviewScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private val content = mutableStateOf<@Composable () -> Unit>({})
    private var started = false
    private val dark = mutableStateOf(false)

    private fun shot(name: String, isDark: Boolean = false, c: @Composable () -> Unit) {
        content.value = c; dark.value = isDark
        if (!started) { started = true; rule.setContent { MyApplicationTheme(darkTheme = dark.value) { content.value() } } }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onRoot().captureRoboImage(filePath = "build/v115/$name.png")
    }

    private val today = SimpleDate(2026, 11, 11)
    private val d = OnboardingDraft(fullName = "Mohammed Tauqeer", username = "tauqeer", gender = OnbGender.MALE, profileType = ProfileType.STUDENT,
        dob = SimpleDate(1998, 4, 2), country = "India", state = "Telangana", city = "Hyderabad", language = "ur")

    private fun onb(name: String, s: OnboardingUiState, isDark: Boolean = false) = shot(name, isDark) { OnboardingContent(s, OnboardingActions()) }

    @Test fun onboardingPages() {
        onb("onb_1_name", OnboardingUiState(step = OnbStep.NAME, draft = d.copy(username = ""), today = today))
        onb("onb_2_username", OnboardingUiState(step = OnbStep.USERNAME, draft = d, today = today, nameCheck = NameCheck.AVAILABLE,
            suggestions = listOf("mohammed.tauqeer", "tauqeer_m", "m.tauqeer26")))
        onb("onb_2b_username_taken", OnboardingUiState(step = OnbStep.USERNAME, draft = d.copy(username = "mohammed"), today = today, nameCheck = NameCheck.TAKEN,
            suggestions = listOf("mohammed.tauqeer", "tauqeer_m")))
        onb("onb_3_gender", OnboardingUiState(step = OnbStep.GENDER, draft = d, today = today))
        onb("onb_4_profile_type", OnboardingUiState(step = OnbStep.TYPE, draft = d.copy(profileType = ProfileType.SCHOLAR), today = today))
        onb("onb_5_birth", OnboardingUiState(step = OnbStep.BIRTH, draft = d, today = today))
        onb("onb_5b_birth_child", OnboardingUiState(step = OnbStep.BIRTH, draft = d.copy(dob = SimpleDate(2016, 6, 20)), today = today))
        onb("onb_6_address", OnboardingUiState(step = OnbStep.ADDRESS, draft = d, today = today))
        onb("onb_7_language", OnboardingUiState(step = OnbStep.LANGUAGE, draft = d, today = today))
        onb("onb_8_done_scholar", OnboardingUiState(step = OnbStep.DONE, draft = d.copy(profileType = ProfileType.SCHOLAR), today = today))
        onb("onb_3_gender_dark", OnboardingUiState(step = OnbStep.GENDER, draft = d, today = today), isDark = true)
    }

    private val app = ScholarApplication(uid = "u1", name = "Aisha Siddiqa", title = "Alimah", institution = "Jamia Darul Uloom, Hyderabad", maslak = Maslak.HANAFI,
        silsila = "Chishti", years = 6, refName = "Mufti Yusuf Qasmi", refPhone = "+91 98765 43210",
        bio = "Completed the Dars-e-Nizami and the Ifta course; I teach fiqh and hadith to sisters at our madrasa.", certPath = "scholar_certificates/u1/ijazah_1.jpg",
        status = ApplicationStatus.PENDING, submittedAt = System.currentTimeMillis() - 3 * 3_600_000)

    @Test fun scholarForm() {
        shot("scholar_apply_form") { ScholarApplyContent(ScholarApplyUiState(form = app.copy(status = ApplicationStatus.NONE, certPath = ""), certLabel = "ijazah_scan.jpg"), ScholarApplyActions()) }
        shot("scholar_apply_errors") { ScholarApplyContent(ScholarApplyUiState(form = ScholarApplication(name = "Aisha"), showErrors = true, error = "Please fix the highlighted fields."), ScholarApplyActions()) }
        shot("scholar_apply_status_pending") { ScholarApplyContent(ScholarApplyUiState(existing = app), ScholarApplyActions()) }
        shot("scholar_apply_status_rejected") { ScholarApplyContent(ScholarApplyUiState(existing = app.copy(status = ApplicationStatus.REJECTED, reason = "The certificate photo is blurry. Please upload a clearer scan.")), ScholarApplyActions()) }
    }

    @Test fun adminReview() {
        val now = System.currentTimeMillis()
        val list = listOf(app, app.copy(uid = "u2", name = "Abdul Rahman", title = "Mufti", institution = "Darul Ifta Deoband", maslak = Maslak.HANAFI, years = 14, submittedAt = now - 26 * 3_600_000),
            app.copy(uid = "u3", name = "Yahya Ibrahim", title = "Shaykh", institution = "Dar al-Hadith, Tarim", maslak = Maslak.SHAFII, years = 9, submittedAt = now - 50 * 3_600_000))
        shot("admin_review_list") { ScholarReviewContent(ScholarReviewUiState(isModerator = true, pending = list, approved = listOf(app.copy(uid = "u9", name = "Maryam Ali", status = ApplicationStatus.APPROVED)), now = now), ScholarReviewActions()) }
        shot("admin_review_detail") { ScholarReviewContent(ScholarReviewUiState(isModerator = true, pending = list, selected = list[1], now = now), ScholarReviewActions()) }
        shot("admin_review_reject") { ScholarReviewContent(ScholarReviewUiState(isModerator = true, pending = list, selected = list[1], decision = "reject",
            reason = "Reference could not be reached. Please add another reference.", now = now), ScholarReviewActions()) }
        shot("admin_review_denied") { ScholarReviewContent(ScholarReviewUiState(isModerator = false), ScholarReviewActions()) }
    }

    // ---- 1.2.0 (115) Noor AI copy / paste / select
    private val researchAnswer = """## Combining prayers while travelling
- In the **Hanafi** madhhab, Dhuhr and Asr are prayed each in its own time, even on a journey (except at Arafah and Muzdalifah).
- The **Shafi'i**, Maliki and Hanbali schools allow joining them as a concession for the traveller.

> "Prayer has been decreed upon the believers at fixed times." (An-Nisa 4:103)

Sources: Qur'an 4:103; Sahih Muslim 705; Al-Hidayah, Kitab al-Salah
FOLLOWUPS: What distance counts as travel? | How do I shorten the prayer?"""

    private fun chat(name: String, items: List<NoorAiChatItem>, input: String = "", tier: String = com.noorpro.app.data.NoorAiOptions.BALANCED, after: () -> Unit = {}) {
        shot(name) {
            NoorAiChatContent(mode = com.noorpro.app.data.NoorAiMode.AskNoor, messages = items, input = input, isTyping = false, showTip = false, revealId = null,
                onBack = {}, onInput = {}, onSend = {}, onSuggestion = {}, onStop = {}, onModeChange = {}, onClearChat = {},
                onDismissTip = {}, onCopy = {}, onShare = {}, onRegenerate = {}, onFeedback = { _, _ -> }, onRevealDone = {}, tier = tier)
        }
        after()
    }

    @Test fun noorAiCopyPaste() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val items = listOf(NoorAiChatItem("Can I combine Dhuhr and Asr while travelling for work?", true, 1_760_000_000_000L, id = 1),
            NoorAiChatItem(researchAnswer, false, 1_760_000_060_000L, id = 2))
        chat("noorai_copy_buttons", items, tier = com.noorpro.app.data.NoorAiOptions.RESEARCH)
        rule.onNodeWithTag("noorai_copy").performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(200)
        rule.onRoot().captureRoboImage(filePath = "build/v115/noorai_copied.png")
        rule.mainClock.advanceTimeBy(2000)
        // Long-press selection itself can't be rendered under Robolectric (no window surface for the text toolbar / magnifier).
        (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
            .setPrimaryClip(android.content.ClipData.newPlainText("q", "What is the ruling on qasr for a daily commute?"))
        chat("noorai_paste_chip", items.take(1))
        shot("noorai_voice_copy") {
            NoorVoiceAssistantOverlay(state = com.noorpro.app.data.AssistantState(phase = com.noorpro.app.data.AssistantPhase.IDLE, heard = "Can I combine Dhuhr and Asr while travelling?",
                answer = researchAnswer), onMic = {}, onStop = {}, onClose = {}, onHandsFree = {}, onKeyboard = {})
        }
    }

    // ---- 1.2.0 (115) community refresh: Halaqa, Ask a Scholar, Dua requests
    @Test fun communityRedesign() {
        val now = System.currentTimeMillis()
        fun ago(min: Int) = now - min * 60_000L
        val rooms = listOf(
            HalaqaRoom("r1", "Tafseer of Surah Al-Kahf — verses 1 to 10", HalaqaTopic.DARS, "A Friday reflection on the opening verses and the young men of the cave.",
                "hana", "Ustadha Hana Siddiqui", HalaqaStatus.LIVE, ago(25)),
            HalaqaRoom("r2", "Tajweed circle: Surah Al-Mulk", HalaqaTopic.QURAN, "", "omar", "Qari Omar Farooq", HalaqaStatus.LIVE, ago(50)),
            HalaqaRoom("r3", "Evening adhkar together", HalaqaTopic.DHIKR, "", "maryam", "Maryam Khan", HalaqaStatus.SCHEDULED, now + 3 * 3_600_000L),
            HalaqaRoom("r4", "Seerah Q&A for new Muslims", HalaqaTopic.QA, "Bring your questions about the life of the Prophet ﷺ.", "yusuf", "Mufti Yusuf Ali", HalaqaStatus.SCHEDULED, now + 26 * 3_600_000L),
        )
        val verified = setOf("hana", "yusuf")
        shot("halaqa_home") { HalaqaHomeContent(HalaqaHomeUi(rooms = rooms, reminders = setOf("r3"), now = now, verifiedHosts = verified), HalaqaHomeActions()) }
        shot("halaqa_home_empty") { HalaqaHomeContent(HalaqaHomeUi(now = now), HalaqaHomeActions()) }
        val members = listOf(HalaqaMember("hana", "Hana Siddiqui", HalaqaRole.HOST, muted = false, joinedAt = 1), HalaqaMember("ibrahim", "Ibrahim Patel", HalaqaRole.SPEAKER, muted = false, joinedAt = 2),
            HalaqaMember("me", "Tauqeer", HalaqaRole.LISTENER, joinedAt = 3), HalaqaMember("sara", "Sara Ahmed", HalaqaRole.LISTENER, hand = true, joinedAt = 4),
            HalaqaMember("bilal", "Bilal", HalaqaRole.LISTENER, joinedAt = 5))
        shot("halaqa_room") { HalaqaRoomContent(HalaqaRoomUi(rooms[0], members = members, myUid = "me", link = HalaqaLink.CONNECTED, now = now, verifiedHosts = verified), HalaqaRoomActions()) }

        val travel = ScholarQuestion("q1", "I travel for work two days every week. May I shorten and combine Dhuhr and Asr on those days?", ScholarCategory.SALAH,
            anonymous = true, answered = true,
            answer = "If your trip reaches the distance considered travel and you are not staying long at the destination, you may shorten the four-unit prayers to two. " +
                "In the Hanafi school each prayer is still prayed in its own time; the Shafi'i school also allows combining. Once you reach home, pray in full again.",
            scholarUid = "s1", scholarName = "Mufti Abdul Rahman", scholarTitle = "Darul Ifta Deoband", createdAt = ago(4 * 24 * 60), answeredAt = ago(2 * 24 * 60))
        val gold = ScholarQuestion("q2", "Is zakat due on gold jewellery that I wear regularly?", ScholarCategory.ZAKAT, anonymous = false, name = "Aisha", answered = true,
            answer = "In the Hanafi school zakat is due on gold jewellery once it reaches the nisab and a lunar year passes, even if it is worn.",
            scholarUid = "s2", scholarName = "Alimah Aisha Siddiqa", scholarTitle = "Jamia Darul Uloom, Hyderabad", createdAt = ago(9 * 24 * 60), answeredAt = ago(6 * 24 * 60))
        shot("ask_scholar_library") { AskScholarContent(ScholarUi(library = listOf(travel, gold), saved = setOf("q2"), myUid = "me", now = now), ScholarActions()) }
        shot("ask_scholar_admin_empty") { AskScholarContent(ScholarUi(myUid = "me", isModerator = true, tab = 1, now = now), ScholarActions()) }

        val requests = listOf(
            DuaRequest("d1", "My mother has heart surgery on Thursday morning. Please make dua for shifa and for the doctors' hands to be guided.", DuaCategory.HEALTH,
                name = "Aisha Rahman", authorUid = "aisha", ameenCount = 128, prayedCount = 41, createdAt = ago(95)),
            DuaRequest("d2", "Please pray that I find halal work soon. It has been seven months and my family is depending on me.", DuaCategory.RIZQ,
                anonymous = true, ameenCount = 56, createdAt = ago(5 * 60)),
            DuaRequest("d4", "Final exams start Monday. Make dua for barakah in my time and a calm heart.", DuaCategory.STUDIES,
                name = "Zainab", authorUid = "zainab", ameenCount = 12, prayedCount = 3, answered = true, createdAt = ago(3 * 24 * 60)),
        )
        shot("dua_requests") { DuaFeedContent(DuaFeedUi(requests = requests, responded = setOf("d1/ameen"), myUid = "me"), DuaActions()) }
        shot("dua_requests_mine_empty") { DuaFeedContent(DuaFeedUi(requests = requests, tab = 1, myUid = "me"), DuaActions()) }
    }

    // ---- 1.2.0 (115) quiz + hadith shortcuts
    @Test fun quizAndHadith() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val bank = com.noorpro.app.data.quiz.QuizBank.load(app)
        val counts = bank.groupingBy { it.cat }.eachCount()
        val home = com.noorpro.app.ui.quiz.QuizHomeUi(streak = 6, xp = 1240, accuracy = 82, mistakes = 4, counts = counts,
            best = mapOf(com.noorpro.app.data.quiz.QuizCat.SEERAH to 9, com.noorpro.app.data.quiz.QuizCat.FIQH to 7), level = 2)
        shot("quiz_home") { com.noorpro.app.ui.quiz.QuizHomeContent(home, com.noorpro.app.ui.quiz.QuizHomeActions()) }
        shot("quiz_home_done_dark", isDark = true) { com.noorpro.app.ui.quiz.QuizHomeContent(home.copy(dailyScore = 8, timer = true, madhhab = "shafii"), com.noorpro.app.ui.quiz.QuizHomeActions()) }
        shot("quiz_home_urdu") { com.noorpro.app.ui.quiz.QuizHomeContent(home.copy(lang = "ur"), com.noorpro.app.ui.quiz.QuizHomeActions()) }
        val daily = com.noorpro.app.data.quiz.QuizEngine.daily(bank, "2026-11-11")
        val q = daily.first { !it.hasMadhhabVariants && it.level <= 2 }
        shot("quiz_play") { com.noorpro.app.ui.quiz.QuizPlayContent(com.noorpro.app.ui.quiz.QuizPlayUi("Today's quiz", q, 2, 10, timeLeft = 18, score = 2), com.noorpro.app.ui.quiz.QuizPlayActions()) }
        shot("quiz_play_correct") { com.noorpro.app.ui.quiz.QuizPlayContent(com.noorpro.app.ui.quiz.QuizPlayUi("Today's quiz", q, 2, 10, picked = q.answer, score = 3), com.noorpro.app.ui.quiz.QuizPlayActions()) }
        val witr = bank.first { it.textFor("en").startsWith("What is the ruling of the Witr prayer") }
        shot("quiz_play_fiqh_hanafi_wrong") { com.noorpro.app.ui.quiz.QuizPlayContent(com.noorpro.app.ui.quiz.QuizPlayUi("Fiqh", witr, 4, 10, picked = 2, score = 3), com.noorpro.app.ui.quiz.QuizPlayActions()) }
        shot("quiz_play_fiqh_shafii") { com.noorpro.app.ui.quiz.QuizPlayContent(com.noorpro.app.ui.quiz.QuizPlayUi("Fiqh", witr, 4, 10, madhhab = "shafii", picked = 2, score = 4), com.noorpro.app.ui.quiz.QuizPlayActions()) }
        val n = q.optionsFor("en").size
        val urdu = com.noorpro.app.data.quiz.QuizAi.applyTranslation(q, "ur", "hanafi", org.json.JSONObject()
            .put("q", "یہ سوال کا اردو ترجمہ ہے؟").put("o", org.json.JSONArray((1..n).map { "جواب $it" })).put("e", "یہ وضاحت کا اردو ترجمہ ہے۔"))
        shot("quiz_play_urdu") { com.noorpro.app.ui.quiz.QuizPlayContent(com.noorpro.app.ui.quiz.QuizPlayUi("آج کا کوئز", urdu, 2, 10, lang = "ur", picked = q.answer), com.noorpro.app.ui.quiz.QuizPlayActions()) }
        shot("quiz_result") { com.noorpro.app.ui.quiz.QuizResultContent(com.noorpro.app.ui.quiz.QuizResultUi("Today's quiz", 8, 10, 145, streak = 7, wrong = listOf(witr, daily.last())), com.noorpro.app.ui.quiz.QuizResultActions()) }
        shot("home_essentials_learn") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.ikScreen().then(androidx.compose.ui.Modifier.padding(16.dp))) {
                HomeDailyEssentials(onTool = {}, onViewAll = {})
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(16.dp))
                LearnShortcutRow(onHadith = {}, onQuiz = {})
            }
        }
        val vm = com.noorpro.app.ui.viewmodel.DeenViewModel(app)
        shot("hadith_library") { HadithLibraryScreen(vm) }
        vm.selectHadithBook("malik")
        waitUntil { !vm.isLoadingHadith.value && vm.hadithChapters.value.firstOrNull()?.bookId == "malik" }
        shot("hadith_chapters_malik") { HadithChaptersScreen(vm) }
        vm.selectHadithBook("nawawi")
        waitUntil { !vm.isLoadingHadith.value && vm.hadithChapters.value.firstOrNull()?.bookId == "nawawi" }
        vm.selectHadithChapter(1)
        waitUntil { !vm.isLoadingHadith.value && vm.hadithList.value.isNotEmpty() }
        shot("hadith_reading_nawawi") { HadithReadingScreen(vm) }
    }

    private fun waitUntil(cond: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!cond() && System.currentTimeMillis() < end) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50) }
    }
}
