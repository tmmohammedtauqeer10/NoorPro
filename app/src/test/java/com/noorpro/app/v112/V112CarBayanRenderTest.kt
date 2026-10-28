package com.noorpro.app.v112

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.audio.podcast.Episode
import com.noorpro.app.audio.podcast.EpisodeChapter
import com.noorpro.app.audio.podcast.PodcastRepository
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.audio.*
import com.noorpro.app.ui.islamic.*
import com.noorpro.app.ui.screens.Reels2Flags
import com.noorpro.app.ui.screens.moreCatalog
import com.noorpro.app.ui.viewmodel.DeenScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V112CarBayanRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @After fun tearDown() { Reels2Flags.override = null }

    private val shows = BayanLogic.shows()
    private val omar = PodcastRepository.podcasts.first { it.id == "mc-omar" }
    private val tj = PodcastRepository.podcasts.first { it.id == "tariq-jamil" }
    private val day = 86_400_000L
    private val notes = "In this reminder:\n00:00 Opening\n04:20 Why we lose hope\n17:45 The du'a of Yunus (AS)\n31:10 Turning back to Allah\n44:30 Closing du'a"
    private val eps = listOf(
        Episode("e1", "When Allah Tests You: Lessons from Yunus (AS)", "https://x/1.mp3", NOW - 2 * day, 2_940_000, null, notes),
        Episode("e2", "Mercy in the Middle of Hardship", "https://x/2.mp3", NOW - 9 * day, 2_100_000, null),
        Episode("e3", "The Night Prayer and the Quiet Heart", "https://x/3.mp3", NOW - 16 * day, 1_860_000, null),
        Episode("e4", "Gratitude That Changes Everything", "https://x/4.mp3", NOW - 23 * day, 2_460_000, null),
    )
    private fun tid(e: Episode) = BayanLogic.trackId(omar, e)
    private val resume = listOf(
        ResumePoint(tid(eps[0]), omar.id, eps[0].title, eps[0].audioUrl, 1_140_000, 2_940_000, NOW - 3_600_000),
        ResumePoint(BayanLogic.trackId(tj, eps[1]), tj.id, "Ikhlas aur Niyyat ki Ahmiyat", "u", 1_500_000, 4_800_000, NOW - 2 * day),
        ResumePoint(tid(eps[3]), omar.id, eps[3].title, eps[3].audioUrl, 2_440_000, 2_460_000, NOW - 3 * day), // finished
    )
    private val feed = BayanShowFeed(omar, null, eps)

    @Test fun bayanHomeLight() {
        var resumed: String? = null; var lang: String? = "x"; var opened: String? = null
        rule.show(false) { BayanPodcastsContent(BayanUi(shows, resume = resume), BayanActions(onResume = { resumed = it.trackId }, onLanguage = { lang = it }, onOpenShow = { opened = it.id })) }
        rule.onNodeWithText("CONTINUE LISTENING").assertExists()
        rule.onNodeWithText("30 min left", substring = true).assertExists()
        rule.onNodeWithTag("bayan_resume_top").performClick(); assertEquals(tid(eps[0]), resumed)
        rule.onNodeWithText("Ikhlas aur Niyyat ki Ahmiyat").assertExists()        // second in-progress
        rule.onNodeWithText(eps[3].title).assertDoesNotExist()                   // finished one hidden
        rule.onNodeWithTag("bayan_lang_Urdu").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); assertEquals("Urdu", lang)
        rule.onNodeWithTag("bayan_show_tariq-jamil").performScrollTo().performClick(); assertEquals("tariq-jamil", opened)
        rule.onAllNodes(hasTestTag("bayan_show_qc-urdu")).assertCountEquals(0)
        rule.onNodeWithTag("bayan_sources").performScrollTo().assertExists()
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_bayan_home_light")
    }

    @Test fun bayanFilteredDark() {
        rule.show(true) { BayanPodcastsContent(BayanUi(shows, language = "Urdu"), BayanActions()) }
        rule.onNodeWithText("LISTEN AND BENEFIT").assertExists()
        rule.onNodeWithTag("bayan_show_israr").assertExists()
        rule.onAllNodes(hasTestTag("bayan_show_yaqeen")).assertCountEquals(0)
        rule.shot("v112_bayan_urdu_dark")
    }

    @Test fun bayanEmptyAndNoMatch() {
        rule.show(true) { BayanPodcastsContent(BayanUi(emptyList()), BayanActions()) }
        rule.onNodeWithTag("bayan_empty").assertExists()
        rule.onNodeWithTag("bayan_search").assertDoesNotExist()
        rule.shot("v112_bayan_empty_dark")
    }

    @Test fun bayanNoMatch() {
        rule.show(false) { BayanPodcastsContent(BayanUi(shows, query = "zzzz"), BayanActions()) }
        rule.onNodeWithTag("bayan_no_match").performScrollTo().assertExists()
    }

    @Test fun bayanShowDark() {
        var played: Pair<String, Long>? = null; var opened: String? = null
        rule.show(true) { BayanPodcastsContent(BayanUi(shows, resume = resume, open = feed, nowTrackId = tid(eps[2]), nowPlaying = true), BayanActions(onPlay = { _, e, s -> played = e.guid to s }, onOpenEpisode = { opened = it.guid })) }
        rule.onNodeWithText("4 episodes · publisher's public feed").assertExists()
        rule.onNodeWithText("5 chapters", substring = true).assertExists()
        rule.onNodeWithTag("bayan_ep_play_e1").performClick(); assertEquals("e1" to 1_135_000L, played)
        rule.onNodeWithTag("bayan_ep_pause_e3").assertExists()
        rule.onNodeWithTag("bayan_ep_play_e4").performScrollTo().performClick(); assertEquals("e4" to 0L, played) // finished → start over
        rule.onNodeWithTag("bayan_ep_e2").performScrollTo().performClick(); assertEquals("e2", opened)
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_bayan_show_dark")
    }

    @Test fun bayanFeedError() {
        var retry = 0
        rule.show(false) { BayanPodcastsContent(BayanUi(shows, open = BayanShowFeed(tj, null, emptyList(), error = "HTTP 500")), BayanActions(onRetry = { retry++ })) }
        rule.onNodeWithTag("bayan_feed_error").assertExists()
        rule.onNodeWithTag("bayan_retry").performClick(); assertEquals(1, retry)
    }

    @Test fun episodeChaptersLight() {
        var chapter: Long? = null; var played: Long? = null
        val ch = BayanLogic.offlineChapters(eps[0])
        rule.show(false) { BayanPodcastsContent(BayanUi(shows, resume = resume, open = feed, episode = eps[0], chapters = ch), BayanActions(onChapter = { chapter = it.startMs }, onPlay = { _, _, s -> played = s })) }
        assertEquals(5, ch.size)
        rule.onNodeWithTag("bayan_detail_resume").assertTextContains("Resume at 18:55", substring = true).performClick(); assertEquals(1_135_000L, played)
        rule.onNodeWithTag("bayan_detail_restart").performClick(); assertEquals(0L, played)
        rule.onNodeWithText("The du'a of Yunus (AS)").assertExists()
        rule.onNodeWithTag("bayan_chapter_2").assertExists() // current chapter (19:00 is inside 17:45–31:10)
        rule.onNodeWithTag("bayan_chapter_3").performScrollTo().performClick(); assertEquals(1_870_000L, chapter)
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_bayan_episode_light")
    }

    @Test fun episodeNoChaptersDark() {
        rule.show(true) { BayanPodcastsContent(BayanUi(shows, open = feed, episode = eps[1], nowTrackId = tid(eps[1]), nowPlaying = true, nowPositionMs = 300_000), BayanActions()) }
        rule.onNodeWithTag("bayan_no_chapters").assertExists()
        rule.onNodeWithTag("bayan_detail_pause").assertExists()
        rule.shot("v112_bayan_episode_nochapters_dark")
    }

    @Test fun rssChaptersAndNotesParse() {
        val rss = """<?xml version="1.0"?><rss xmlns:psc="http://podlove.org/simple-chapters" xmlns:podcast="https://podcastindex.org/namespace/1.0"><channel><title>T</title>
            <item><title>Ep</title><guid>g</guid><enclosure url="https://x/a.mp3" type="audio/mpeg"/><itunes:duration>45:00</itunes:duration>
            <description><![CDATA[<p>Topics</p><p>00:00 Start<br/>10:00 Middle &amp; more</p>]]></description>
            <podcast:chapters url="https://x/ch.json" type="application/json+chapters"/>
            <psc:chapters><psc:chapter start="00:00:00.000" title="Intro"/><psc:chapter start="00:12:30" title="Main"/></psc:chapters></item></channel></rss>"""
        val r = PodcastRepository.parse(rss.byteInputStream())
        val e = r.episodes.single()
        assertEquals("https://x/ch.json", e.chaptersUrl)
        assertEquals(listOf(EpisodeChapter(0, "Intro"), EpisodeChapter(750_000, "Main")), e.chapters)
        assertTrue(e.notes.contains("10:00 Middle & more"))
        assertEquals(listOf("Start", "Middle & more"), BayanLogic.chaptersFromNotes(e.notes).map { it.title })
        val json = """{"version":"1.2.0","chapters":[{"startTime":0,"title":"Intro"},{"startTime":95.5,"title":"Hidden","toc":false},{"startTime":610,"title":"Story"}]}"""
        assertEquals(listOf(EpisodeChapter(0, "Intro"), EpisodeChapter(610_000, "Story")), BayanLogic.chaptersFromJson(json))
        assertTrue(BayanLogic.chaptersFromJson("not json").isEmpty())
    }

    @Test fun resumeStoreRoundTrip() {
        val store = BayanResumeStore(ApplicationProvider.getApplicationContext())
        resume.forEach { store.save(it) }
        assertEquals(3, store.all().size)
        assertEquals(1_140_000L, store.get(tid(eps[0]))?.positionMs)
        store.save(resume[0].copy(positionMs = 1_200_000, updatedAt = NOW))
        assertEquals(3, store.all().size); assertEquals(1_200_000L, store.all().first().positionMs)
        store.remove(tid(eps[0])); assertNull(store.get(tid(eps[0])))
    }

    private val car = CarModeUi(18, "18:42", CarNowPlaying("Al-Kahf", "Mahmoud Khalil Al-Husary", CarSource.QURAN, true), setOf(18, 36, 67), listOf(RoutineLogic.PRESETS[0], RoutineLogic.PRESETS[1]), resume[0], bayansOn = true)

    @Test fun carQuranLight() {
        var pp = 0; var back = 0; var fwd = 0; var surah: Int? = null; var routine: String? = null; var exit = false
        rule.show(false) { CarModeContent(car, CarModeActions(onPlayPause = { pp++ }, onBack = { back++ }, onForward = { fwd++ }, onSurah = { surah = it }, onRoutine = { routine = it.id }, onExit = { exit = true })) }
        rule.onNodeWithText("Good evening").assertExists()
        rule.onNodeWithContentDescription("Pause").performClick(); assertEquals(1, pp)
        rule.onNodeWithContentDescription("Previous surah").performClick(); assertEquals(1, back)
        rule.onNodeWithContentDescription("Next surah").performClick(); assertEquals(1, fwd)
        listOf("car_play", "car_back", "car_forward", "car_exit", "car_surah_18").forEach { t ->
            val b = rule.onNodeWithTag(t).fetchSemanticsNode().boundsInRoot
            val minPx = 64 * rule.activity.resources.displayMetrics.density - 1
            assertTrue("$t too small", b.width >= minPx && b.height >= minPx)
        }
        rule.onNodeWithTag("car_surah_18").assertTextContains("Saved · offline", substring = true).performClick(); assertEquals(18, surah)
        rule.onNodeWithTag("car_routine_${RoutineLogic.PRESETS[0].id}").performScrollTo().performClick(); assertEquals(RoutineLogic.PRESETS[0].id, routine)
        rule.onNodeWithTag("car_exit").performClick(); assertTrue(exit)
        rule.onNodeWithTag("car_auto").performScrollTo().assertExists()
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_car_quran_light")
    }

    @Test fun carBayanDark() {
        var resumed: String? = null; var back = 0
        rule.show(true) { CarModeContent(car.copy(hour = 7, clock = "7:15", now = CarNowPlaying("When Allah Tests You", "Omar Suleiman (Muslim Central)", CarSource.BAYAN, false, 0.39f)),
            CarModeActions(onResume = { resumed = it.trackId }, onBack = { back++ })) }
        rule.onNodeWithText("BAYAN").assertExists()
        rule.onNodeWithContentDescription("Play").assertExists()
        rule.onNodeWithContentDescription("Back 30 seconds").performClick(); assertEquals(1, back)
        rule.onNodeWithTag("car_resume").performClick(); assertEquals(tid(eps[0]), resumed)
        rule.shot("v112_car_bayan_dark")
    }

    @Test fun carIdleDark() {
        var pp = 0
        rule.show(true) { CarModeContent(car.copy(now = null, resume = null, routines = emptyList(), bayansOn = false, hour = 23, clock = "23:05"), CarModeActions(onPlayPause = { pp++ })) }
        rule.onNodeWithText("Pick something to listen to").assertExists()
        rule.onNodeWithTag("car_play").assertIsNotEnabled()
        rule.onNodeWithTag("car_bayans").assertDoesNotExist()
        rule.onNodeWithTag("car_resume").assertDoesNotExist()
        rule.shot("v112_car_idle_dark")
    }

    @Test fun autoServiceFollowsFlag() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cn = ComponentName(ctx, "com.noorpro.app.audio.session.NoorCarLibraryService")
        val pm = ctx.packageManager
        val info = pm.getServiceInfo(cn, PackageManager.MATCH_DISABLED_COMPONENTS)
        assertFalse("disabled in the manifest", info.enabled)
        CarComponents.sync(ctx)
        assertNotEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, pm.getComponentEnabledSetting(cn))
        Reels2Flags.override = mapOf(IslamicFlags.CAR_MODE to true)
        CarComponents.sync(ctx)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, pm.getComponentEnabledSetting(cn))
        Reels2Flags.override = mapOf(IslamicFlags.CAR_MODE to false)
        CarComponents.sync(ctx)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, pm.getComponentEnabledSetting(cn))
    }

    @Test fun moreEntriesFollowFlags() {
        fun visible() = moreCatalog.flatMap { it.items }.filter { it.visible }.mapNotNull { it.screen }
        assertFalse(visible().contains(DeenScreen.CAR_MODE)); assertFalse(visible().contains(DeenScreen.BAYAN_PODCASTS))
        Reels2Flags.override = mapOf(IslamicFlags.CAR_MODE to true, IslamicFlags.BAYAN_PODCASTS to true)
        assertTrue(visible().contains(DeenScreen.CAR_MODE)); assertTrue(visible().contains(DeenScreen.BAYAN_PODCASTS))
    }
}
