package com.noorpro.app.v112

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.noorpro.app.data.IslamicData
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.audio.*
import com.noorpro.app.prayer.AdhanSound
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
class V112OfflineAudioRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @After fun tearDown() { Reels2Flags.override = null }

    private fun r(id: Int) = IslamicData.reciters.first { it.id == id }
    private val husary = ReciterStorage(r(7), 96_400_000L, (78..114).toSet() + setOf(1, 18, 36, 67), setOf(2))
    private val storages = IslamicData.reciters.map { if (it.id == 7) husary else if (it.id == 5) ReciterStorage(it, 21_000_000L, setOf(1, 112, 113, 114), emptySet()) else ReciterStorage(it, 0, emptySet(), emptySet()) }
    private val offline = OfflineQuranUi(storages, selectedId = 7, playbackId = 7, freeBytes = 18_400_000_000L, loading = false, repeatTimes = 3, sleepAtSurahEnd = true)

    @Test fun offlineLight() {
        var pack: String? = null; var repeat = -9; var sleep: Int? = -9
        rule.show(false) { OfflineQuranContent(offline, OfflineQuranActions(onPack = { pack = it.id }, onRepeat = { repeat = it }, onSleep = { sleep = it })) }
        rule.onNodeWithTag("offline_count").assertTextEquals("41 of 114 surahs")
        rule.onNodeWithTag("offline_license").assertTextContains("EveryAyah.com", substring = true)
        rule.onNodeWithTag("offline_pack_juz30").assertDoesNotExist() // already complete
        rule.onNodeWithText("All 37 saved").assertExists()
        rule.onNodeWithTag("offline_pack_sunnah").performScrollTo().performClick()
        assertEquals("sunnah", pack)
        rule.onNodeWithTag("offline_resume_2").assertExists() // partial surahs listed first
        rule.onNodeWithTag("offline_surah_114").assertDoesNotExist()
        rule.onNodeWithTag("offline_show_all").performScrollTo().assertTextEquals("Show all 42").performClick()
        rule.onNodeWithTag("offline_surah_114").assertExists()
        rule.onNodeWithTag("offline_show_all").performScrollTo().performClick()
        rule.onNodeWithTag("offline_repeat_5").performScrollTo().performClick(); assertEquals(5, repeat)
        rule.onNodeWithTag("offline_sleep_status").assertTextEquals("Stops after this surah")
        rule.onNodeWithTag("offline_sleep_30").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); assertEquals(30, sleep)
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_offline_quran_light")
    }

    @Test fun offlineToolsDark() {
        rule.show(true) { OfflineQuranContent(offline.copy(download = OfflineDownloadState(55, 0.42f), sleepAtSurahEnd = false, sleepEndsIn = "28:41", sleepMinutes = 30), OfflineQuranActions()) }
        rule.onNodeWithTag("offline_progress").assertExists()
        rule.onNodeWithText("Saving Ar-Rahmaan").assertExists()
        rule.onNodeWithTag("offline_sleep_status").assertTextEquals("Stops in 28:41")
        rule.onNodeWithTag("offline_storage_5").performScrollTo()
        rule.shot("v112_offline_quran_tools_dark")
    }

    @Test fun offlineEmptyOtherReciterDark() {
        var use = false; var del: Int? = null
        rule.show(true) { OfflineQuranContent(offline.copy(selectedId = 3), OfflineQuranActions(onUseForPlayback = { use = true }, onDeleteReciter = { del = it })) }
        rule.onNodeWithTag("offline_empty").assertExists()
        rule.onNodeWithTag("offline_use").performClick(); assertTrue(use)
        rule.onNodeWithText("Needs ≈", substring = true).assertDoesNotExist()
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_offline_quran_empty_dark")
    }

    @Test fun offlineDeleteConfirmLight() {
        var del: Int? = null
        rule.show(false) { OfflineQuranContent(offline, OfflineQuranActions(onDeleteReciter = { del = it })) }
        rule.onNodeWithTag("offline_delete_all").performScrollTo().performClick()
        rule.onNodeWithTag("offline_delete_confirm").performClick()
        assertEquals(7, del)
    }

    private val routines = listOf(
        RoutineLogic.PRESETS[0],
        RoutineLogic.PRESETS[1],
        AudioRoutine("r1", "Morning Fatiha & Quls", listOf(1, 112, 113, 114), setOf(1, 2, 3, 4, 5), 6 * 60, enabled = false),
    )

    @Test fun routinesLight() {
        var played: String? = null; var added: String? = null; var toggled: Pair<String, Boolean>? = null
        rule.show(false) { AudioRoutinesContent(AudioRoutinesUi(routines, now = NOW), AudioRoutinesActions(onPlay = { played = it.id }, onAddPreset = { added = it.id },
            onToggle = { r, on -> toggled = r.id to on })) }
        rule.onNodeWithTag("routines_next").assertExists()
        rule.onNodeWithTag("routine_play_mulk_night").performClick(); assertEquals("mulk_night", played)
        rule.onNodeWithTag("routine_switch_r1").performClick(); assertEquals("r1" to true, toggled)
        rule.onNodeWithTag("routines_add_kahf_friday").assertDoesNotExist()
        rule.onNodeWithTag("routines_add_quls_night").performScrollTo().performClick(); assertEquals("quls_night", added)
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_routines_light")
    }

    @Test fun routinesPendingDark() {
        var played: String? = null
        rule.show(true) { AudioRoutinesContent(AudioRoutinesUi(routines, pending = routines[0], now = NOW), AudioRoutinesActions(onPlay = { played = it.id })) }
        rule.onNodeWithTag("routines_play_pending").performClick(); assertEquals("kahf_friday", played)
        rule.shot("v112_routines_pending_dark")
    }

    @Test fun routinesEmptyDark() {
        rule.show(true) { AudioRoutinesContent(AudioRoutinesUi(emptyList(), now = NOW), AudioRoutinesActions()) }
        rule.onNodeWithTag("routines_empty").assertExists()
        rule.onNodeWithText("Make the Quran part of your day").assertExists()
        rule.shot("v112_routines_empty_dark")
    }

    @Test fun routineEditorLight() {
        rule.show(false) {
            var d by remember { mutableStateOf(RoutineDraft(title = "Evening Quran", surahs = listOf(56), days = setOf(1, 2, 3, 4, 5), minuteOfDay = 19 * 60, query = "mul")) }
            AudioRoutinesContent(AudioRoutinesUi(draft = d), AudioRoutinesActions(onDraft = { d = it }))
        }
        rule.onNodeWithTag("routine_found_67").performClick()
        rule.onNodeWithTag("routine_surah_67").assertExists()
        rule.onNodeWithTag("routine_day_6").performClick()
        rule.onNodeWithText("Mon, Tue, Wed, Thu, Fri, Sat").assertExists()
        rule.onNodeWithTag("routine_time_plus").performClick()
        rule.onNodeWithTag("routine_time").assertTextEquals("7:15 PM")
        rule.onNodeWithTag("routine_at_1320").performScrollTo().performClick()
        rule.onNodeWithTag("routine_time").assertTextEquals("10:00 PM")
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_routine_editor_light")
    }

    @Test fun routineEditorErrorDark() {
        rule.show(true) { AudioRoutinesContent(AudioRoutinesUi(draft = RoutineDraft(id = "r1", title = "Morning", surahs = emptyList(), error = "Pick at least one surah.")), AudioRoutinesActions()) }
        rule.onNodeWithTag("routine_error").performScrollTo().assertExists()
        rule.onNodeWithTag("routine_delete").assertExists()
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_routine_editor_error_dark")
    }

    @Test fun adhanLight() {
        var sel: AdhanSound? = null; var prev: AdhanSound? = null; var applied = false
        rule.show(false) { AdhanVoicesContent(AdhanVoicesUi(AdhanSound.ALARM, AdhanSound.NOTIFICATION), AdhanVoicesActions(onSelect = { sel = it }, onPreview = { prev = it.sound }, onApply = { applied = true })) }
        rule.onNodeWithTag("adhan_applied").assertTextEquals("Alarm tone")
        rule.onNodeWithTag("adhan_option_SILENT").performClick(); assertEquals(AdhanSound.SILENT, sel)
        rule.onNodeWithTag("adhan_preview_NOTIFICATION").performClick(); assertEquals(AdhanSound.NOTIFICATION, prev)
        rule.onNodeWithTag("adhan_apply").performScrollTo().performClick(); assertTrue(applied)
        rule.onNodeWithTag("adhan_no_recordings").assertExists()
        repeat(4) { rule.onRoot().performTouchInput { swipeDown() } }
        rule.shot("v112_adhan_light")
    }

    @Test fun adhanPreviewDark() {
        rule.show(true) { AdhanVoicesContent(AdhanVoicesUi(AdhanSound.SILENT, AdhanSound.SILENT, previewing = AdhanSound.SILENT), AdhanVoicesActions()) }
        rule.onNodeWithTag("adhan_apply").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Stop preview").assertExists()
        rule.shot("v112_adhan_dark")
    }

    @Test fun moreEntriesFollowFlags() {
        fun items() = moreCatalog.flatMap { it.items }.filter { it.visible }.map { it.screen }
        Reels2Flags.override = emptyMap()
        assertFalse(items().any { it in setOf(DeenScreen.QURAN_OFFLINE, DeenScreen.AUDIO_ROUTINES, DeenScreen.ADHAN_VOICES) })
        Reels2Flags.override = mapOf(IslamicFlags.QURAN_OFFLINE to true, IslamicFlags.AUDIO_ROUTINES to true, IslamicFlags.ADHAN_VOICES to true)
        assertTrue(items().containsAll(listOf(DeenScreen.QURAN_OFFLINE, DeenScreen.AUDIO_ROUTINES, DeenScreen.ADHAN_VOICES)))
    }

    // org.json needs the Android runtime, so this lives in the Robolectric class.
    @Test fun routineJsonRoundTrip() {
        val list = RoutineLogic.PRESETS.take(2) + AudioRoutine("r1", "Mine", listOf(1, 112), setOf(1, 3), 61, enabled = false)
        assertEquals(list, RoutineLogic.fromJson(RoutineLogic.toJson(list)))
        assertEquals(emptyList<AudioRoutine>(), RoutineLogic.fromJson("not json"))
        assertEquals(emptyList<AudioRoutine>(), RoutineLogic.fromJson(null))
        val q = RoutineLogic.queue(list.last())
        assertEquals(listOf(1, 112), q.map { it.surahId }); assertTrue(q.all { it.ayah == null })
    }

}
