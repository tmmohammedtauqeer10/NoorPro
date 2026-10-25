package com.noorpro.app.ummah

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.data.media.edit.*
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 1.1.3 editor pro renders (light + dark): photo pro tools, Islamic stickers, layers, templates, collage, multi-clip reel, captions, voice. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V107EditorTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private fun bmp(id: Int): Bitmap = BitmapFactory.decodeResource(ctx.resources, id)
    private val mosque by lazy { bmp(R.drawable.noor_home_hero_mosque) }
    private val rehal by lazy { bmp(R.drawable.noor_home_quran_rehal) }

    private fun show(dark: Boolean, content: @Composable () -> Unit) = rule.setContent {
        MyApplicationTheme(darkTheme = dark) { Box(Modifier.fillMaxSize().background(stitchBackground())) { content() } }
    }
    private fun save(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(b))
        val out = File(File("build/v107").apply { mkdirs() }, "$name.png")
        out.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name not rendered", out.length() > 5000)
    }
    private fun frame(src: Bitmap, tint: Int = 0): Bitmap {
        val b = Bitmap.createBitmap(540, 960, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        val s = maxOf(540f / src.width, 960f / src.height); val w = src.width * s; val h = src.height * s
        c.drawBitmap(src, null, android.graphics.RectF((540 - w) / 2, (960 - h) / 2, (540 + w) / 2, (960 + h) / 2), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        if (tint != 0) c.drawColor(tint)
        return b
    }
    private val strip by lazy { (0 until 8).map { i -> frame(if (i < 4) mosque else rehal, (i * 0x08000000) and 0x3F000000) } }
    private val sounds = listOf(
        ReelSound("hisn_morning", "Morning adhkar", "Hisn al-Muslim", "https://example.org/m.mp3", "Public Domain Mark 1.0", "https://example.org"),
        ReelSound("dua_rabbana", "Rabbana atina", "Quranic Duas", "https://example.org/r.mp3", "Public Domain Mark 1.0", "https://example.org"),
    )
    private val sources = StickerSources(
        ayahs = { q -> listOf(
            QuoteItem("Indeed, with hardship comes ease.", "Ash-Sharh 94:6", "إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا"),
            QuoteItem("So remember Me; I will remember you.", "Al-Baqarah 2:152", "فَٱذْكُرُونِىٓ أَذْكُرْكُمْ"),
            QuoteItem("And He is with you wherever you are.", "Al-Hadid 57:4"),
        ).filter { q.isBlank() || it.text.contains(q, true) || it.ref.contains(q, true) } },
        duas = { listOf(QuoteItem("Our Lord, give us good in this world and good in the Hereafter, and protect us from the Fire.", "Al-Baqarah 2:201")) },
        hadith = { listOf(QuoteItem("The best of you are those who learn the Qur'an and teach it.", "Sahih al-Bukhari 5027")) },
        hijri = { "4 Jumada al-Ula 1448 AH" }, prayer = { "Asr · 3:48 PM" }, countdown = { IslamicDates.countdownLabel(it, 117) }, handle = "noorpro"
    )

    // ------------------------------------------------------------------ photo pro

    @Test fun photoHslLight() {
        val s = PhotoEditorState(PhotoEdit(filter = NoorFilter.MADINAH, hsl = HslAdjust().with(HueBand.entries[3], HslShift(sat = 0.4f, lum = -0.1f))), PhotoTab.ADJUST)
        s.adjustMode = AdjustMode.HSL
        show(false) { NoorPhotoEditor(s, mosque, {}, {}) }
        rule.onNodeWithTag("editor_adjust_mode_HSL").assertIsDisplayed()
        rule.onNodeWithTag("editor_hsl_slider_1").assertIsDisplayed()
        save("photo_hsl_light")
    }

    @Test fun photoCurvesDark() {
        val s = PhotoEditorState(PhotoEdit(curve = ToneCurve.presets.getValue("Soft S"), adjust = Adjustments(shadows = 0.3f, grain = 0.2f)), PhotoTab.ADJUST)
        s.adjustMode = AdjustMode.CURVES
        show(true) { NoorPhotoEditor(s, rehal, {}, {}) }
        rule.onNodeWithTag("editor_curve_canvas").assertIsDisplayed()
        rule.onNodeWithTag("editor_curve_Matte").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(ToneCurve.presets.getValue("Matte"), s.edit.curve)
        save("photo_curves_dark")
    }

    @Test fun photoLightAutoEnhanceLight() {
        val s = PhotoEditorState(PhotoEdit(), PhotoTab.ADJUST)
        show(false) { NoorPhotoEditor(s, rehal, {}, {}) }
        rule.onNodeWithTag("editor_auto_enhance").performClick(); rule.waitUntil(5000) { !s.edit.adjust.isNeutral }
        save("photo_auto_enhance_light")
    }

    @Test fun photoTiltShiftLight() {
        val s = PhotoEditorState(PhotoEdit(tiltShift = TiltShift(0.55f, 0.12f, 0.15f), blurAmount = 0.8f), PhotoTab.BLUR)
        show(false) { NoorPhotoEditor(s, mosque, {}, {}) }
        rule.onNodeWithTag("editor_blur_TILT").assertIsDisplayed()
        save("photo_tiltshift_light")
    }

    @Test fun photoTextLooksDark() {
        val s = PhotoEditorState(PhotoEdit(filter = NoorFilter.LAYL, layers = listOf(
            EditorLayer.text("t1", "Ya Allah", EditorFont.CLASSIC).copy(y = 0.22f, look = TextLook.NEON, color = 0xFFF3D98B, scale = 1.4f),
            EditorLayer.text("t2", "Gratitude", EditorFont.STRONG).copy(y = 0.42f, look = TextLook.GRADIENT, scale = 1.3f),
            EditorLayer.text("t3", "Outline", EditorFont.MODERN).copy(y = 0.6f, look = TextLook.OUTLINE, color = 0xFFFFE27A),
            EditorLayer.text("t4", "Left aligned with a soft shadow\nover two lines", EditorFont.SERIF).copy(x = 0.42f, y = 0.78f, look = TextLook.SHADOW, align = TextAlignment.LEFT),
        )), PhotoTab.TEXT).apply { selected = "t1" }
        show(true) { NoorPhotoEditor(s, rehal, {}, {}) }
        rule.onNodeWithTag("editor_look_NEON").assertIsDisplayed()
        save("photo_text_looks_dark")
    }

    @Test fun photoStickerTrayLight() {
        val s = PhotoEditorState(PhotoEdit(layers = listOf(
            EditorLayer("h", sticker = StickerKind.HIJRI, text = "4 Jumada al-Ula 1448 AH", y = 0.16f),
            EditorLayer("p", sticker = StickerKind.PRAYER, text = "Asr · 3:48 PM", x = 0.3f, y = 0.3f),
            EditorLayer("m", sticker = StickerKind.MENTION, text = "@noorpro", x = 0.72f, y = 0.3f),
        )), PhotoTab.STICKERS)
        show(false) { NoorPhotoEditor(s, mosque, {}, {}, stickerSources = sources) }
        rule.onNodeWithTag("editor_sticker_AYAH").performScrollTo().assertIsDisplayed()
        save("photo_sticker_tray_light")
    }

    @Test fun photoAyahPickerDark() {
        val s = PhotoEditorState(PhotoEdit(), PhotoTab.STICKERS).apply { picker = StickerKind.AYAH }
        show(true) { NoorPhotoEditor(s, rehal, {}, {}, stickerSources = sources) }
        rule.onNodeWithTag("editor_quote_picker").assertIsDisplayed()
        rule.onNodeWithTag("editor_quote_0").performClick(); rule.waitForIdle()
        val l = s.edit.layers.single(); assertEquals(StickerKind.AYAH, l.sticker); assertEquals("Ash-Sharh 94:6", l.sub)
        save("photo_ayah_card_dark")
    }

    @Test fun photoPollCardLight() {
        val s = PhotoEditorState(PhotoEdit(layers = listOf(
            EditorLayer("q", sticker = StickerKind.QUIZ, text = "How many surahs are in the Qur'an?", options = listOf("110", "114", "120"), answer = 1, y = 0.42f),
            EditorLayer("c", sticker = StickerKind.COUNTDOWN, text = "117 days to Ramadan", sub = "Prepare your heart", y = 0.78f, scale = 0.8f),
        )), PhotoTab.STICKERS).apply { selected = "q" }
        show(false) { NoorPhotoEditor(s, mosque, {}, {}, stickerSources = sources, interactive = true) }
        rule.onNodeWithTag("editor_card_panel").assertIsDisplayed()
        save("photo_quiz_card_light")
    }

    @Test fun photoLayersDark() {
        val s = PhotoEditorState(PhotoEdit(layers = listOf(
            EditorLayer.text("t1", "After Fajr", EditorFont.CLASSIC).copy(y = 0.2f),
            EditorLayer("s1", sticker = StickerKind.DUA, text = "Our Lord, give us good in this world.", sub = "Al-Baqarah 2:201", y = 0.5f, locked = true),
            EditorLayer("s2", sticker = StickerKind.CRESCENT, x = 0.8f, y = 0.15f),
        )), PhotoTab.LAYERS).apply { selected = "s1" }
        show(true) { NoorPhotoEditor(s, rehal, {}, {}) }
        rule.onNodeWithTag("editor_layers_panel").assertIsDisplayed()
        rule.onNodeWithTag("editor_layer_up_t1").performClick(); rule.waitForIdle()
        assertEquals(listOf("s1", "t1", "s2"), s.edit.layers.map { it.id })
        save("photo_layers_dark")
    }

    @Test fun photoTemplatesLight() {
        val s = PhotoEditorState(PhotoEdit(), PhotoTab.TEMPLATES)
        show(false) { NoorPhotoEditor(s, mosque, {}, {}, stickerSources = sources) }
        rule.onNodeWithTag("editor_template_AYAH_OF_DAY").performClick(); rule.waitForIdle()
        assertTrue(s.edit.layers.any { it.sticker == StickerKind.AYAH }); assertEquals(NoorFilter.FAJR, s.edit.filter)
        save("photo_templates_light")
    }

    // ------------------------------------------------------------------ collage

    @Test fun collageLight() {
        val st = CollageEditorState(CollageEdit(layout = CollageLayout.TRIO_FEATURE))
        show(false) { NoorCollageEditor(st, listOf(mosque, rehal, frame(mosque, 0x40000000)), {}, {}) }
        rule.onNodeWithTag("collage_layout_TRIO_COLUMNS").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(CollageLayout.TRIO_COLUMNS, st.edit.layout)
        rule.onNodeWithTag("collage_layout_TRIO_FEATURE").performScrollTo().performClick()
        save("collage_light")
    }

    @Test fun collageDark() {
        val st = CollageEditorState(CollageEdit(layout = CollageLayout.QUAD, background = 0xFF06231A, gap = 0.02f, radius = 0.04f))
        show(true) { NoorCollageEditor(st, listOf(mosque, rehal, frame(rehal, 0x30000000), frame(mosque, 0x50000000)), {}, {}) }
        rule.onNodeWithTag("collage_canvas").assertIsDisplayed()
        save("collage_dark")
    }

    // ------------------------------------------------------------------ reel pro

    private fun multi() = ReelEdit(12_000).trim(1000, 9000).addSource(ReelSource("file:///b.mp4", 6000, "Recording"))
        .addSource(ReelSource("file:///c.mp4", 4000, "Clip 3")).allTransitions(ReelTransition.SLIDE).freezeAt(3000)

    @Test fun reelClipsLight() {
        val s = ReelEditorState(multi(), ReelTab.TRIM).apply { selectedClip = 2 }
        show(false) { NoorReelEditor(s, frame(mosque), strip, sounds, {}, {}, onAddClip = {}, onRecordClip = {}) }
        rule.onNodeWithTag("reel_clip_strip").assertIsDisplayed()
        rule.onNodeWithTag("reel_add_clip").performScrollTo().assertIsDisplayed()
        save("reel_clips_light")
    }

    @Test fun reelRampDark() {
        val s = ReelEditorState(ReelEdit(10_000).withRamp(SpeedRamp.HERO), ReelTab.SPEED)
        show(true) { NoorReelEditor(s, frame(rehal), strip, sounds, {}, {}) }
        rule.onNodeWithTag("reel_ramp_graph").assertIsDisplayed()
        rule.onNodeWithTag("reel_ramp_MONTAGE").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(SpeedRamp.MONTAGE, s.edit.ramp)
        save("reel_ramp_dark")
    }

    @Test fun reelCaptionsLight() {
        val caps = Captions.fromTranscript("Bismillah. Today a small reminder about patience and gratitude.", 0, 8000)
        val s = ReelEditorState(ReelEdit(8000).copy(captions = caps, captionStyle = CaptionStyle.NOOR), ReelTab.CAPTIONS).apply { playheadMs = 500; selectedCaption = caps.first().id }
        show(false) { NoorReelEditor(s, frame(mosque), strip, sounds, {}, {}) }
        rule.onNodeWithTag("reel_captions_panel").assertIsDisplayed()
        save("reel_captions_light")
    }

    @Test fun reelVoiceDark() {
        val s = ReelEditorState(ReelEdit(10_000).copy(voices = listOf(VoiceClip("v1", "/none.wav", 1000, 3500), VoiceClip("v2", "/none2.wav", 6000, 2500, 0.7f))), ReelTab.VOICE)
        val vc = VoiceControl({}, {}).apply { recording = true; level = 0.6f; elapsedMs = 2300 }
        show(true) { NoorReelEditor(s, frame(rehal), strip, sounds, {}, {}, voice = vc) }
        rule.onNodeWithTag("reel_voice_panel").assertIsDisplayed()
        save("reel_voice_dark")
    }

    @Test fun reelSoundFadesLight() {
        val s = ReelEditorState(ReelEdit(10_000).copy(sound = sounds[0], soundFadeInMs = 1500, soundFadeOutMs = 2000, soundVolume = 0.5f), ReelTab.AUDIO)
        show(false) { NoorReelEditor(s, frame(mosque), strip, sounds, {}, {}) }
        rule.onNodeWithTag("reel_fade_in").assertIsDisplayed()
        save("reel_sound_fades_light")
    }

    @Test fun reelQualityTemplatesDark() {
        val s = ReelEditorState(ReelEdit(9000).copy(quality = ExportQuality.FHD), ReelTab.COVER)
        show(true) { NoorReelEditor(s, frame(rehal), strip, sounds, {}, {}) }
        rule.onNodeWithTag("reel_quality_DATA_SAVER").performClick(); rule.waitForIdle()
        assertEquals(ExportQuality.DATA_SAVER, s.edit.quality)
        save("reel_quality_dark")
    }

    @Test fun reelTemplateTextAnimLight() {
        val s = ReelEditorState(ReelEdit(9000), ReelTab.TEMPLATES)
        show(false) { NoorReelEditor(s, frame(mosque), strip, sounds, {}, {}, stickerSources = sources) }
        rule.onNodeWithTag("editor_template_REMINDER").performClick(); rule.waitForIdle()
        assertTrue(s.edit.layers.any { it.anim == TextAnim.TYPEWRITER })
        s.playheadMs = 3000; s.tab = ReelTab.TEXT; s.selected = s.edit.layers.first().id
        save("reel_template_light")
    }
}
