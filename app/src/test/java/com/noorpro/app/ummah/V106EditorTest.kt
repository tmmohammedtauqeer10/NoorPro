package com.noorpro.app.ummah

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.media.edit.*
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 1.1.3 in-app media editor: photo (crop/filters/adjust/text/stickers) + reel (trim/speed/text timing/sound/export). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V106EditorTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private fun bmp(id: Int): Bitmap = BitmapFactory.decodeResource(ctx.resources, id)
    private val mosque by lazy { bmp(R.drawable.noor_home_hero_mosque) }
    private val rehal by lazy { bmp(R.drawable.noor_home_quran_rehal) }

    @After fun tearDown() { ComposerPreview.images = emptyMap() }

    private fun show(dark: Boolean, content: @Composable () -> Unit) = rule.setContent {
        MyApplicationTheme(darkTheme = dark) { Box(Modifier.fillMaxSize().background(stitchBackground())) { content() } }
    }
    private fun save(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        val out = File(File("build/v106").apply { mkdirs() }, "$name.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name not rendered", out.length() > 5000)
    }

    private val labels = mapOf(StickerKind.LOCATION to "Masjid an-Nabawi", StickerKind.TIME to "Fri · 1:15 PM", StickerKind.HASHTAG to "#Jumuah")

    /** A vertical "video frame" built from the app's own art (no real footage). */
    private fun frame(src: Bitmap, tint: Int = 0): Bitmap {
        val b = Bitmap.createBitmap(540, 960, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        val s = maxOf(540f / src.width, 960f / src.height)
        val w = src.width * s; val h = src.height * s
        c.drawBitmap(src, null, android.graphics.RectF((540 - w) / 2, (960 - h) / 2, (540 + w) / 2, (960 + h) / 2), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        if (tint != 0) c.drawColor(tint)
        return b
    }
    private val strip by lazy { (0 until 8).map { i -> frame(if (i < 4) mosque else rehal, (i * 0x08000000) and 0x3F000000) } }

    private val sounds = listOf(
        ReelSound("hisn_morning", "Morning adhkar", "Hisn al-Muslim", "https://example.org/m.mp3", "Public Domain Mark 1.0", "https://example.org"),
        ReelSound("dua_rabbana", "Rabbana atina", "Quranic Duas", "https://example.org/r.mp3", "Public Domain Mark 1.0", "https://example.org"),
        ReelSound("ubaid_sabr", "Dua for patience", "Ubaid", "https://example.org/u.mp3", "CC BY 4.0", "https://example.org"),
    )

    // ------------------------------------------------------------------ photo

    @Test fun photoFiltersLight() {
        val s = PhotoEditorState(PhotoEdit(filter = NoorFilter.MADINAH, strength = 0.8f), PhotoTab.FILTERS)
        show(false) { NoorPhotoEditor(s, mosque, {}, {}, positionLabel = "1 of 2") }
        rule.onNodeWithTag("editor_filters").assertIsDisplayed()
        rule.onNodeWithTag("editor_strength").assertIsDisplayed()
        rule.onNodeWithTag("editor_filter_FAJR").performClick()
        rule.waitForIdle()
        assertEquals(NoorFilter.FAJR, s.edit.filter); assertTrue(s.canUndo)
        rule.onNodeWithTag("editor_undo").performClick(); rule.waitForIdle()
        assertEquals(NoorFilter.MADINAH, s.edit.filter)
        save("photo_filters_light")
    }

    @Test fun photoAdjustDark() {
        val s = PhotoEditorState(PhotoEdit(adjust = Adjustments(brightness = 0.15f, contrast = 0.2f, warmth = 0.35f, vignette = 0.5f)), PhotoTab.ADJUST)
        s.adjustKind = AdjustKind.WARMTH
        show(true) { NoorPhotoEditor(s, mosque, {}, {}) }
        rule.onNodeWithTag("editor_adjust_panel").assertIsDisplayed()
        rule.onNodeWithTag("editor_adjust_slider").assertIsDisplayed()
        rule.onNodeWithTag("editor_adjust_VIGNETTE").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(AdjustKind.VIGNETTE, s.adjustKind)
        save("photo_adjust_dark")
    }

    private fun textEdit() = PhotoEdit(filter = NoorFilter.NOOR, layers = listOf(
        EditorLayer.text("t1", "After Fajr, the heart is light", EditorFont.CLASSIC).copy(x = 0.5f, y = 0.2f, pill = TextPill.SOLID, pillColor = 0xFF0B6243, scale = 1.05f),
        EditorLayer.text("t2", "الحمد لله").copy(x = 0.32f, y = 0.42f, color = 0xFFD4AF37, scale = 1.3f, rotation = -8f),
        EditorLayer("s1", sticker = StickerKind.JUMUAH, x = 0.7f, y = 0.6f, rotation = 6f),
        EditorLayer("s2", sticker = StickerKind.CRESCENT, x = 0.18f, y = 0.72f, scale = 0.9f),
        EditorLayer("s3", sticker = StickerKind.LOCATION, text = "Masjid an-Nabawi", x = 0.5f, y = 0.86f),
    ))

    @Test fun photoTextLight() {
        val s = PhotoEditorState(textEdit(), PhotoTab.TEXT).apply { selected = "t1" }
        show(false) { NoorPhotoEditor(s, rehal, {}, {}, stickerLabels = labels) }
        rule.onNodeWithTag("editor_text_panel").assertIsDisplayed()
        rule.onNodeWithTag("editor_font_NASKH").assertExists()
        rule.onNodeWithTag("editor_font_STRONG").performClick(); rule.waitForIdle()
        assertEquals(EditorFont.STRONG, s.edit.layers.first { it.id == "t1" }.font)
        save("photo_text_light")
    }

    @Test fun photoStickersDark() {
        val s = PhotoEditorState(textEdit(), PhotoTab.STICKERS)
        show(true) { NoorPhotoEditor(s, rehal, {}, {}, stickerLabels = labels) }
        rule.onNodeWithTag("editor_stickers").assertIsDisplayed()
        val before = s.edit.layers.size
        rule.onNodeWithTag("editor_sticker_MOSQUE").performClick(); rule.waitForIdle()
        assertEquals(before + 1, s.edit.layers.size)
        assertEquals(StickerKind.MOSQUE, s.edit.layers.last().sticker)
        save("photo_stickers_dark")
    }

    @Test fun photoCropDarkAndLight() {
        val s = PhotoEditorState(PhotoEdit().withAspect(CropAspect.PORTRAIT, mosque.width, mosque.height), PhotoTab.CROP)
        show(true) { NoorPhotoEditor(s, mosque, {}, {}) }
        rule.onNodeWithTag("editor_crop_panel").assertIsDisplayed()
        rule.onNodeWithTag("editor_aspect_SQUARE").performClick(); rule.waitForIdle()
        assertEquals(CropAspect.SQUARE, s.edit.aspect)
        save("photo_crop_dark")
    }

    @Test fun photoCropLightDraw() {
        val stroke = BrushStroke((0..30).map { i -> 0.2f + i * 0.02f to 0.55f + 0.08f * kotlin.math.sin(i / 3f) }, glow = true)
        val s = PhotoEditorState(PhotoEdit(aspect = CropAspect.WIDE, strokes = listOf(stroke)).let { it.withAspect(CropAspect.WIDE, mosque.width, mosque.height).copy(strokes = it.strokes) }, PhotoTab.DRAW)
        show(false) { NoorPhotoEditor(s, mosque, {}, {}) }
        rule.onNodeWithTag("editor_draw_panel").assertIsDisplayed()
        save("photo_draw_light")
    }

    @Test fun photoRenderProducesEditedBitmap() {
        val e = textEdit().withAspect(CropAspect.SQUARE, mosque.width, mosque.height)
        val out = PhotoRenderer.render(mosque, e, EditorTypefaces.load(ctx))
        assertEquals(out.width, out.height)
        val plain = PhotoRenderer.render(mosque, PhotoEdit(), EditorTypefaces.load(ctx))
        assertEquals(mosque.width, plain.width)
        val r = PhotoRenderer.render(mosque, PhotoEdit().rotate(), EditorTypefaces.load(ctx))
        assertEquals(mosque.height, r.width)
        File(File("build/v106").apply { mkdirs() }, "photo_rendered.jpg").outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    }

    // ------------------------------------------------------------------ reel

    private fun reel() = ReelEdit(42_000).trim(3_000, 33_000)

    @Test fun reelTrimLight() {
        val s = ReelEditorState(reel().splitAt(12_000), ReelTab.TRIM).apply { playheadMs = 12_000 }
        show(false) { NoorReelEditor(s, strip[2], strip, sounds, {}, {}) }
        rule.onNodeWithTag("reel_trim_panel").assertIsDisplayed()
        rule.onNodeWithTag("reel_filmstrip").assertIsDisplayed()
        rule.onNodeWithTag("reel_split").assertIsDisplayed()
        save("reel_trim_light")
    }

    @Test fun reelTrimDarkSplit() {
        val s = ReelEditorState(reel(), ReelTab.TRIM).apply { playheadMs = 9_000 }
        show(true) { NoorReelEditor(s, strip[1], strip, sounds, {}, {}) }
        rule.onNodeWithTag("reel_split").performClick(); rule.waitForIdle()
        assertEquals(2, s.edit.clips.size)
        save("reel_trim_dark")
    }

    @Test fun reelSpeedDark() {
        val s = ReelEditorState(reel(), ReelTab.SPEED).apply { playheadMs = 4_000 }
        show(true) { NoorReelEditor(s, strip[3], strip, sounds, {}, {}) }
        rule.onNodeWithTag("reel_speed_panel").assertIsDisplayed()
        rule.onNodeWithTag("reel_speed_FAST").performClick(); rule.waitForIdle()
        assertEquals(ReelSpeed.FAST, s.edit.speed); assertEquals(20_000L, s.edit.outputMs)
        save("reel_speed_dark")
    }

    @Test fun reelTextTimingLight() {
        val e = reel().copy(filter = NoorFilter.FAJR)
            .upsert(EditorLayer.text("t1", "Dhikr by the sea").copy(y = 0.22f, pill = TextPill.SOLID, pillColor = 0xFF0B6243).timed(2_000, 14_000, 30_000))
            .upsert(EditorLayer("s1", sticker = StickerKind.SUBHANALLAH, y = 0.7f).timed(8_000, 24_000, 30_000))
        val s = ReelEditorState(e, ReelTab.TEXT).apply { selected = "t1"; playheadMs = 9_000 }
        show(false) { NoorReelEditor(s, strip[0], strip, sounds, {}, {}, stickerLabels = labels) }
        rule.onNodeWithTag("reel_layer_lanes").assertIsDisplayed()
        rule.onNodeWithTag("editor_layer_timing").assertExists()
        save("reel_text_timing_light")
    }

    @Test fun reelAudioDark() {
        val e = reel().copy(originalVolume = 0f, sound = sounds[0], soundVolume = 0.7f)
        val s = ReelEditorState(e, ReelTab.AUDIO).apply { playheadMs = 5_000 }
        show(true) { NoorReelEditor(s, strip[5], strip, sounds, {}, {}) }
        rule.onNodeWithTag("reel_audio_panel").assertIsDisplayed()
        rule.onNodeWithTag("reel_sound_credit").assertExists()
        rule.onNodeWithTag("reel_sound_dua_rabbana").performClick(); rule.waitForIdle()
        assertEquals("dua_rabbana", s.edit.sound?.trackId)
        assertTrue(ReelExportParams.needsFullExport(s.edit))
        save("reel_audio_dark")
    }

    @Test fun reelExportProgressLight() {
        val e = reel().copy(filter = NoorFilter.SAHARA).withSpeed(ReelSpeed.FAST)
            .upsert(EditorLayer("s1", sticker = StickerKind.RAMADAN, y = 0.3f))
        val s = ReelEditorState(e, ReelTab.FILTERS).apply { exportProgress = 0.62f; playheadMs = 3_000 }
        show(false) { NoorReelEditor(s, strip[4], strip, sounds, {}, {}) }
        rule.onNodeWithTag("reel_export_progress").assertIsDisplayed()
        rule.onNodeWithText("62%", substring = true).assertExists()
        save("reel_export_light")
    }

    // ------------------------------------------------------------------ composer wiring

    @Test fun composerShowsEditEntryPoints() {
        ComposerPreview.images = mapOf("content://t/1" to mosque, "content://t/2" to rehal)
        val edited = Uri.parse("file:///cache/noor_edit_1.jpg")
        ComposerPreview.images = ComposerPreview.images + (edited.toString() to rehal)
        val s = ComposerUiState(ComposerDestination.FEED).apply {
            val t = "Jumu'ah after Fajr #Jumuah"; text = TextFieldValue(t, TextRange(t.length))
            photos = listOf(Uri.parse("content://t/1"), edited)
            photoOrigins = mapOf(edited to (Uri.parse("content://t/2") to PhotoEdit(filter = NoorFilter.NOOR)))
        }
        var opened = -1
        show(false) { NoorComposer(s, ComposerActions(onEditPhoto = { opened = it }), "Mohammed Tauqeer", "") }
        rule.onNodeWithTag("composer_edit_photo_0").performClick(); rule.waitForIdle()
        assertEquals(0, opened)
        rule.onNodeWithText("Edited").assertExists()
        save("composer_edit_entry_light")
    }

    @Test fun composerReelEditButtonDark() {
        val s = ComposerUiState(ComposerDestination.REEL).apply {
            val t = "Dhikr by the sea #Dhikr"; text = TextFieldValue(t, TextRange(t.length))
            reelSource = Uri.parse("content://t/v.mp4"); reelEdit = reel().withSpeed(ReelSpeed.FAST)
            video = Uri.parse("file:///cache/reel_edit_1.mp4"); videoDurationMs = 20_000; trimStartMs = 0; trimEndMs = 20_000
            coverFrame = mosque; coverTouched = true; credit = "Sound: Morning adhkar — Hisn al-Muslim (Public domain)".removePrefix("Sound: ")
        }
        var opened = false
        show(true) { NoorComposer(s, ComposerActions(onEditVideo = { opened = true }), "Mohammed Tauqeer", "") }
        rule.onNodeWithText("Edit again").performClick(); rule.waitForIdle()
        assertTrue(opened)
        rule.onNodeWithTag("composer_trim").assertDoesNotExist() // trim already baked into the exported file
        save("composer_reel_edit_dark")
    }
}
