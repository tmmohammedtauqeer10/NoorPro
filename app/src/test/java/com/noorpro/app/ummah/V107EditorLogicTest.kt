package com.noorpro.app.ummah

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.ComposerDraft
import com.noorpro.app.data.media.edit.*
import com.noorpro.app.ui.screens.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

/** 1.1.3 editor pro: pure logic (tone, HSL, auto-enhance, anim, reel timeline, captions, countdown, codec, snapping, voice, fades). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class V107EditorLogicTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    // ---------------------------------------------------------------- photo pro

    @Test fun curveLutIsMonotonicAndLinearIsIdentity() {
        val id = ToneCurve().lut(); assertTrue(id.indices.all { kotlin.math.abs(id[it] - it) <= 1 })
        ToneCurve.presets.values.forEach { c -> val l = c.lut(); for (i in 1..255) assertTrue("monotonic", l[i] >= l[i - 1]); assertTrue(l.all { it in 0..255 }) }
        val s = ToneCurve.presets.getValue("Soft S").lut(); assertTrue(s[50] < 50); assertTrue(s[205] > 205)
        assertEquals(1f, ToneCurve().with(4, 3f).points[4], 0f)
    }

    @Test fun shadowsLiftDarksHighlightsPullBrights() {
        val up = ProPixels.toneLut(Adjustments(shadows = 0.8f), ToneCurve()); assertTrue(up[30] > 30); assertTrue(kotlin.math.abs(up[240] - 240) <= 2)
        val down = ProPixels.toneLut(Adjustments(highlights = -0.8f), ToneCurve()); assertTrue(down[230] < 230); assertTrue(kotlin.math.abs(down[20] - 20) <= 2)
    }

    @Test fun hslShiftsOnlyItsBand() {
        val h = HslAdjust().with(HueBand.entries.first { it.name == "RED" }, HslShift(sat = -1f))
        val red = h.apply(0.9f, 0.1f, 0.1f); assertTrue("red desaturated", red[0] - red[1] < 0.5f)
        val blue = h.apply(0.1f, 0.2f, 0.9f); assertEquals(0.9f, blue[2], 0.02f)
        assertTrue(HslAdjust().with(HueBand.entries.first(), HslShift()).isNeutral)
    }

    @Test fun pixelPassSharpenGrainKeepRange() {
        val px = IntArray(16 * 16) { if (it % 2 == 0) 0xFF404040.toInt() else 0xFFC0C0C0.toInt() }
        ProPixels.process(px, 16, 16, Adjustments(shadows = 0.4f, grain = 0.5f, sharpen = 0.6f), ToneCurve.presets.getValue("Matte"), HslAdjust())
        assertTrue(px.all { (it ushr 24) == 0xFF })
    }

    @Test fun autoEnhanceBrightensDullDarkPhoto() {
        val px = IntArray(1000) { 0xFF000000.toInt() or (((30 + it % 40) * 0x010101)) }
        val a = AutoEnhance.suggest(AutoEnhance.stats(px))
        assertTrue(a.brightness > 0f); assertTrue(a.contrast > 0f); assertTrue(a.saturation > 0f)
        val ok = IntArray(1000) { val v = (it * 255 / 999); 0xFF000000.toInt() or (v shl 16) or ((255 - v) shl 8) or (v / 2) }
        assertTrue(AutoEnhance.suggest(AutoEnhance.stats(ok)).brightness < 0.1f)
    }

    @Test fun tiltShiftBand() {
        val t = TiltShift(centerY = 0.5f, band = 0.1f, feather = 0.2f)
        assertEquals(0f, t.blurAt(0.5f), 0f); assertEquals(1f, t.blurAt(0.95f), 0f); assertTrue(t.blurAt(0.7f) in 0.4f..0.6f)
    }

    @Test fun collageRectsLayoutsAndSwap() {
        val e = CollageEdit(layout = CollageLayout.QUAD, gap = 0f)
        val r = e.rects(1000, 1250); assertEquals(4, r.size); assertEquals(500f, r[1][0], 1f)
        val sw = e.swap(0, 3); assertEquals(1, sw.photoFor(0, 3)); assertEquals(3 % 3, sw.photoFor(3, 3))
        assertTrue(CollageLayout.forCount(2).all { it.count == 2 }); assertTrue(CollageLayout.forCount(6).any { it == CollageLayout.SIX })
        val (w, h) = CollageEdit(aspect = CropAspect.SQUARE).outputSize(1200); assertEquals(w, h)
    }

    // ---------------------------------------------------------------- text / layers

    @Test fun textAnimationsSettleAndTypewriterReveals() {
        TextAnim.entries.forEach { a -> assertEquals(AnimFrame.DONE, a.frame(5000, 20)) }
        assertEquals(0, TextAnim.TYPEWRITER.frame(0, 20).chars); assertTrue(TextAnim.TYPEWRITER.frame(450, 20).chars!! in 5..15)
        assertTrue(TextAnim.FADE.frame(100, 5).alpha < 1f); assertTrue(TextAnim.SLIDE.frame(10, 5).dy > 0f); assertTrue(TextAnim.POP.frame(20, 5).scale < 1f)
        val l = EditorLayer.text("t", "Salam").copy(anim = TextAnim.FADE, startMs = 1000)
        assertEquals(AnimFrame.DONE, l.animAt(null)); assertTrue(l.animAt(1100).alpha < 1f)
    }

    @Test fun layersReorderLockAndSnap() {
        val ls = listOf(EditorLayer("a"), EditorLayer("b"), EditorLayer("c"))
        assertEquals(listOf("b", "c", "a"), Layers.move(ls, "a", 5).map { it.id })
        assertEquals(listOf("c", "a", "b"), Layers.move(ls, "c", -2).map { it.id })
        assertTrue(Layers.toggleLock(ls, "b")[1].locked)
        assertEquals(listOf("a", "b", "c"), Layers.upsert(ls, ls[1].copy(text = "x")).map { it.id }) // in place
        assertEquals("x", Layers.upsert(ls, ls[1].copy(text = "x"))[1].text)
        assertEquals(listOf("a", "b", "c", "d"), Layers.upsert(ls, EditorLayer("d")).map { it.id })
        val s = Snap.apply(EditorLayer("x", x = 0.51f, y = 0.3f, rotation = 2f))
        assertTrue(s.vertical); assertFalse(s.horizontal); assertTrue(s.angle); assertEquals(0.5f, s.layer.x, 0f); assertEquals(0f, s.layer.rotation, 0f)
        assertFalse(Snap.apply(EditorLayer("y", x = 0.3f, y = 0.3f)).any)
    }

    @Test fun stickerDefaultsAndTemplates() {
        val src = StickerSources(ayahs = { listOf(QuoteItem("Indeed, with hardship comes ease.", "Ash-Sharh 94:6")) }, hijri = { "4 Jumada al-Ula 1448 AH" }, prayer = { "Asr · 3:48 PM" }, handle = "noor")
        assertEquals("@noor", stickerLayer(StickerKind.MENTION, "m", emptyMap(), src).text)
        assertEquals("Asr · 3:48 PM", stickerLayer(StickerKind.PRAYER, "p", emptyMap(), src).text)
        assertEquals(1, stickerLayer(StickerKind.QUIZ, "q", emptyMap(), src).answer)
        assertTrue(StickerKind.AYAH.needsPicker && !StickerKind.MOSQUE.needsPicker)
        val td = templateData(src); assertEquals("Ash-Sharh 94:6", td.ayahRef)
        var n = 0
        EditorTemplate.entries.forEach { t -> val (_, ls) = t.build(td, { "${it}${n++}" }, 9000); assertTrue(ls.isNotEmpty()); assertTrue(ls.all { it.endMs == 9000L }); assertEquals(ls.size, ls.map { it.id }.toSet().size) }
        assertTrue(EditorTemplate.AYAH_OF_DAY.build(td, { it }).second.any { it.sub == "Ash-Sharh 94:6" })
    }

    @Test fun interactivePayloadMatchesRulesShape() {
        assertNull(StoryInteractive.payload(listOf(EditorLayer("a", sticker = StickerKind.MOSQUE))))
        assertFalse(StoryInteractive.enabled)
        val quiz = StoryInteractive.payload(listOf(EditorLayer("q", sticker = StickerKind.QUIZ, text = "Surahs?", options = listOf("110", " 114 ", ""), answer = 1)))!!
        assertEquals("quiz", quiz["kind"]); assertEquals(listOf("110", "114"), quiz["options"]); assertEquals(1, quiz["answer"])
        val q = StoryInteractive.payload(listOf(EditorLayer("q", sticker = StickerKind.QUESTION, text = "Ask")))!!
        assertEquals(setOf("kind", "prompt"), q.keys)
    }

    @Test fun countdownWithFakeHijri() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 23) } // a Friday
        assertEquals(0, IslamicDates.daysUntil(CountdownKind.JUMUAH, now, { 1 to 1 }))
        // Fake calendar: every Gregorian day-of-year d → month = 1 + (d / 30) % 12, day = 1 + d % 30.
        val fake = IslamicDates.HijriOf { c -> val d = c.get(Calendar.DAY_OF_YEAR); (1 + (d / 30) % 12) to (1 + d % 30) }
        val days = IslamicDates.daysUntil(CountdownKind.RAMADAN, now, fake)!!
        val target = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, days) }
        assertEquals(9 to 1, fake.of(target))
        assertEquals("1 day to Ramadan", IslamicDates.countdownLabel(CountdownKind.RAMADAN, 1))
        assertEquals("12 Rabi' al-Awwal 1448 AH", IslamicDates.hijriLabel(12, 3, 1448, listOf("a", "b", "Rabi' al-Awwal")))
    }

    // ---------------------------------------------------------------- reel

    @Test fun rampSegmentsKeepSourceTimeAndChangeLength() {
        val e = ReelEdit(10_000).withRamp(SpeedRamp.HERO)
        assertTrue(e.segments.size >= 3); assertEquals(10_000L, e.segments.sumOf { it.srcEndMs - it.srcStartMs })
        assertTrue("slow-mo makes it longer", e.outputMs > 10_000)
        assertTrue(kotlin.math.abs(e.outputMs - e.outputAtKept(e.keptAt(e.outputMs))) <= 2)
        val fast = ReelEdit(10_000).withRamp(SpeedRamp.FLASH_IN); assertTrue(fast.outputMs < 10_000)
    }

    @Test fun multiClipReorderTransitionsFreeze() {
        var e = ReelEdit(6000).addSource(ReelSource("file:///b.mp4", 4000, "B"))
        assertEquals(2, e.clips.size); assertEquals(1, e.clips[1].source); assertEquals(ReelTransition.FADE, e.clips[1].transition); assertEquals(10_000, e.outputMs)
        e = e.moveClip(1, 0); assertEquals(1, e.clips[0].source)
        e = e.allTransitions(ReelTransition.ZOOM); assertEquals(ReelTransition.ZOOM, e.clips[1].transition); assertEquals(ReelTransition.NONE, e.clips[0].transition)
        val cut = e.cutsOnOutput().single(); assertEquals(4000L, cut)
        assertNotNull(e.transitionAt(cut)); assertNull(e.transitionAt(cut + 2000))
        val fr = e.freezeAt(1000, 1500); assertTrue(fr.hasFreeze); assertEquals(e.outputMs + 1500, fr.outputMs)
        assertTrue(ReelExportParams.needsFullExport(e))
        assertTrue(ReelTransition.FADE.look(0)!!.dim > 0.9f) // fully dim at the cut
        assertNull(ReelTransition.SLIDE.look(-1000))
    }

    @Test fun exportQualityScalesOutput() {
        assertEquals(1920 to 1080, ReelExportParams.outputSize(3840, 2160, ExportQuality.FHD.longSide))
        assertEquals(960, ReelExportParams.outputSize(1920, 1080, ExportQuality.DATA_SAVER.longSide).first)
        assertTrue(ReelExportParams.needsFullExport(ReelEdit(5000).copy(quality = ExportQuality.DATA_SAVER)))
        assertFalse(ReelExportParams.needsFullExport(ReelEdit(5000)))
        assertTrue(ExportQuality.DATA_SAVER.bitrate < ExportQuality.HD.bitrate && ExportQuality.HD.bitrate < ExportQuality.FHD.bitrate)
    }

    @Test fun captionsSplitAndBecomeLayers() {
        val cs = Captions.fromTranscript("Bismillah. Today we talk about patience and gratitude in hard times", 0, 6000)
        assertTrue(cs.size >= 2); assertEquals(0L, cs.first().startMs); assertEquals(6000L, cs.last().endMs)
        cs.zipWithNext().forEach { (a, b) -> assertEquals(a.endMs, b.startMs) }
        val e = ReelEdit(6000).copy(captions = cs, captionStyle = CaptionStyle.POP)
        assertEquals(cs.size, e.drawLayers().size); assertEquals(TextAnim.POP, e.drawLayers().first().anim)
        assertTrue(Captions.fromTranscript("  ", 0, 100).isEmpty())
    }

    @Test fun voiceMixFadesAndWav() {
        val a = VoiceClip("a", "/x", 0, 1000); val b = VoiceClip("b", "/y", 500, 1000, volume = 0.5f)
        val mixed = VoiceMix.mix(listOf(a to ShortArray(441) { 1000 }, b to ShortArray(441) { 1000 }), 2000, rate = 441)
        assertEquals(1000.toShort(), mixed[10]); assertEquals(1500.toShort(), mixed[230]); assertEquals(500.toShort(), mixed.last())
        assertTrue(VoiceMix.mix(listOf(a to ShortArray(441) { 30000 }, VoiceClip("c", "/z", 0, 1000) to ShortArray(441) { 30000 }), 1000, 441).all { it == Short.MAX_VALUE })
        assertEquals(0f, AudioFades.gain(0, 10_000, 2000, 2000), 0f); assertEquals(0.5f, AudioFades.gain(1000, 10_000, 2000, 2000), 0.01f)
        assertEquals(1f, AudioFades.gain(5000, 10_000, 2000, 2000), 0f); assertEquals(0.25f, AudioFades.gain(9500, 10_000, 2000, 2000), 0.01f)
        val f = File(ctx.cacheDir, "t.wav"); Wav.write(f, ShortArray(4410) { (it % 200).toShort() }, 44_100)
        assertEquals(4410, Wav.read(f).size); assertEquals(100L, Wav.durationMs(f))
        assertEquals(4, VoiceMix.peaks(ShortArray(100) { 16384 }, 4).count { it in 0.49f..0.51f })
    }

    // ---------------------------------------------------------------- drafts keep the editing steps

    @Test fun codecRoundTripsPhotoAndReel() {
        val photo = PhotoEdit(quarterTurns = 1, aspect = CropAspect.SQUARE, crop = CropRect(0.1f, 0.1f, 0.9f, 0.9f), filter = NoorFilter.FAJR, strength = 0.7f,
            adjust = Adjustments(brightness = 0.1f, highlights = -0.3f, grain = 0.2f, tint = 0.1f),
            layers = listOf(EditorLayer.text("t", "Salam", EditorFont.CLASSIC).copy(look = TextLook.NEON, align = TextAlignment.LEFT, anim = TextAnim.TYPEWRITER, locked = true),
                EditorLayer("p", sticker = StickerKind.POLL, text = "Q?", options = listOf("a", "b"))),
            strokes = listOf(BrushStroke(listOf(0.1f to 0.2f, 0.3f to 0.4f), glow = true)),
            curve = ToneCurve.presets.getValue("Matte"), hsl = HslAdjust().with(HueBand.entries.first(), HslShift(0.1f, -0.2f, 0.3f)),
            tiltShift = TiltShift(0.4f), blurStrokes = listOf(BrushStroke(listOf(0.5f to 0.5f), width = 0.07f)), blurAmount = 0.8f)
        assertEquals(photo, EditCodec.photo(JSONObject(EditCodec.photo(photo).toString())))
        val reel = ReelEdit(8000).addSource(ReelSource("file:///b.mp4", 3000, "B")).freezeAt(1000).withRamp(SpeedRamp.MONTAGE)
            .copy(quality = ExportQuality.FHD, soundFadeInMs = 1000, voices = listOf(VoiceClip("v", "/data/v.wav", 200, 900, 0.8f)),
                captions = listOf(Caption("c", "Bismillah", 0, 900)), captionStyle = CaptionStyle.NOOR,
                sound = ReelSound("id", "T", "A", "https://x/a.mp3", "CC0", "https://x"))
        assertEquals(reel, EditCodec.reel(JSONObject(EditCodec.reel(reel).toString())))
        assertEquals(PhotoEdit(), EditCodec.photo(JSONObject("{\"filter\":\"NOPE\",\"layers\":[{\"id\":\"x\",\"sticker\":\"GIF\"}]}")))
        assertNull(EditCodec.reel(JSONObject("{}")))
    }

    @Test fun draftCarriesEditsAndOldDraftsStillLoad() {
        val d = EditCodec.DraftEdits(mapOf("file:///e.jpg" to ("content://o/1" to PhotoEdit(filter = NoorFilter.NOOR))), null, null)
        val s = EditCodec.draft(d); assertEquals(d, EditCodec.draft(s))
        assertEquals(listOf("content://o/1"), d.files)
        assertEquals("X", d.mapFiles { "X" }.photos.values.single().first)
        assertEquals("", EditCodec.draft(EditCodec.DraftEdits()))
        val draft = ComposerDraft("id", ComposerDestination.FEED, "t", "Reflections", editJson = s)
        assertEquals(s, ComposerDraft.fromJson(draft.toJson())!!.editJson)
        val legacy = JSONObject(draft.toJson().toString()).apply { remove("editJson") }
        assertEquals("", ComposerDraft.fromJson(legacy)!!.editJson)
        assertTrue(EditCodec.draft("garbage").isEmpty)
    }

    @Test fun offlineStickerDataFromAppAssets() {
        val ay = EditorStickerData.ayahs(ctx, "2:255")
        if (ay.isNotEmpty()) assertTrue(ay.first().ref.endsWith("2:255"))
        assertTrue(EditorStickerData.duas("").isNotEmpty())
        assertTrue(EditorStickerData.countdown(CountdownKind.JUMUAH).isNotBlank())
        assertTrue(EditorStickerData.hijriToday().isNotBlank())
    }
}
