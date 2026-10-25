package com.noorpro.app.ummah

import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.Track
import com.noorpro.app.data.media.edit.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/** 1.1.3 media editor: filter math, text/sticker layer model, crop, history, reel timeline + export params. */
class MediaEditLogicTest {
    private fun near(a: Float, b: Float, eps: Float = 1e-3f) = assertTrue("$a != $b", abs(a - b) < eps)

    // ---------- filter math ----------
    @Test fun identityAndOriginal() {
        assertTrue(ColorMatrix5().isIdentity())
        assertTrue(NoorFilter.ORIGINAL.matrix().isIdentity())
        assertTrue(colorMatrixFor(NoorFilter.ORIGINAL, 1f, Adjustments()).isIdentity())
        val v = ColorMatrix5().apply(0.2f, 0.5f, 0.9f); near(v[0], 0.2f); near(v[1], 0.5f); near(v[2], 0.9f)
    }

    @Test fun strengthBlendsToIdentity() {
        NoorFilter.entries.forEach { f ->
            assertTrue("$f at 0", colorMatrixFor(f, 0f, Adjustments()).isIdentity())
            val full = f.matrix().m; val s1 = colorMatrixFor(f, 1f, Adjustments()).m
            full.indices.forEach { near(full[it], s1[it]) }
        }
        val half = NoorFilter.SAHARA.matrix().strength(0.5f).m
        val full = NoorFilter.SAHARA.matrix().m
        half.indices.forEach { near(half[it], (ColorMatrix5.IDENTITY[it] + full[it]) / 2f) }
    }

    @Test fun tenFiltersAllDistinct() {
        assertEquals(10, NoorFilter.entries.size)
        val non = NoorFilter.entries.filter { it != NoorFilter.ORIGINAL }
        non.forEach { assertFalse("$it is identity", it.matrix().isIdentity()) }
        assertEquals(non.size, non.map { it.matrix().m.toList() }.toSet().size)
    }

    @Test fun contrastPivotsAtMidGrey() {
        val c = ColorMatrix5.contrast(1.5f)
        val mid = c.apply(0.5f, 0.5f, 0.5f); near(mid[0], 0.5f); near(mid[1], 0.5f)
        val hi = c.apply(0.8f, 0.8f, 0.8f); near(hi[0], 0.95f)
        val lo = c.apply(0.2f, 0.2f, 0.2f); near(lo[0], 0.05f)
    }

    @Test fun saturationZeroIsGreyAndQamarIsMono() {
        val g = ColorMatrix5.saturation(0f).apply(0.9f, 0.1f, 0.3f)
        near(g[0], g[1]); near(g[1], g[2])
        val q = NoorFilter.QAMAR.matrix().apply(0.9f, 0.1f, 0.3f)
        assertTrue(abs(q[0] - q[1]) < 0.05f && abs(q[1] - q[2]) < 0.05f)
    }

    @Test fun brightnessWarmthAndChainOrder() {
        val b = ColorMatrix5.brightness(0.1f).apply(0.5f, 0.5f, 0.5f); near(b[0], 0.6f)
        val w = ColorMatrix5.warmth(1f).apply(0.5f, 0.5f, 0.5f); assertTrue(w[0] > 0.5f && w[2] < 0.5f)
        // chain(a, b) applies a first, then b
        val ab = ColorMatrix5.chain(ColorMatrix5.brightness(0.1f), ColorMatrix5.scale(2f, 2f, 2f)).apply(0.2f, 0.2f, 0.2f)
        near(ab[0], 0.6f)
        val adj = Adjustments().with(AdjustKind.BRIGHTNESS, 0.5f)
        assertEquals(0.5f, adj.value(AdjustKind.BRIGHTNESS)); assertFalse(adj.isNeutral)
        assertTrue(Adjustments().isNeutral)
    }

    @Test fun androidAndGlLayouts() {
        val m = ColorMatrix5.chain(ColorMatrix5.offset(0.1f, 0.2f, 0.3f), ColorMatrix5.scale(1f, 2f, 3f))
        val a = m.toAndroid()
        near(a[4], m.m[4] * 255f, 1e-2f); near(a[6], 2f)
        val gl = m.toGl() // column-major 4x4: column 3 = offsets
        assertEquals(16, gl.size)
        near(gl[0], m.m[0]); near(gl[5], m.m[6]); near(gl[10], m.m[12])
        near(gl[12], m.m[4]); near(gl[13], m.m[9]); near(gl[14], m.m[14]); near(gl[15], 1f)
        // GL M * (rgb,1) equals apply()
        val v = floatArrayOf(0.1f, 0.1f, 0.02f, 1f)
        // (apply() clamps to 0..1, so keep the sample inside range)
        val out = FloatArray(3) { r -> (0..3).sumOf { c -> (gl[c * 4 + r] * v[c]).toDouble() }.toFloat() }
        val ref = m.apply(0.1f, 0.1f, 0.02f); (0..2).forEach { near(out[it], ref[it]) }
    }

    // ---------- text / sticker layers ----------
    @Test fun layerDragAndTransformClamp() {
        val l = EditorLayer.text("t1", "Jumu'ah")
        val d = l.dragBy(2f, -2f); assertEquals(0.98f, d.x); assertEquals(0.02f, d.y)
        assertEquals(EditorLayer.MAX_SCALE, l.transform(100f, 0f).scale)
        assertEquals(EditorLayer.MIN_SCALE, l.transform(0.01f, 0f).scale)
        near(l.transform(1f, 270f).rotation, -90f); near(l.transform(1f, -190f).rotation, 170f)
    }

    @Test fun textModelPicksNaskhForArabicAndCaps() {
        assertEquals(EditorFont.NASKH, EditorLayer.text("a", "الحمد لله").font)
        assertEquals(EditorFont.MODERN, EditorLayer.text("b", "Alhamdulillah").font)
        assertTrue(ArabicText.has("سبحان الله")); assertFalse(ArabicText.has("SubhanAllah"))
        assertEquals(EditorLayer.MAX_TEXT, EditorLayer.text("c", "x".repeat(400)).text.length)
        assertTrue(EditorLayer.text("d", "Hi").isText)
        val s = EditorLayer("s", sticker = StickerKind.JUMUAH); assertFalse(s.isText); assertEquals("Jumu'ah Mubarak", s.label)
        assertEquals("Ramadan Kareem", StickerKind.RAMADAN.text)
        assertTrue(StickerKind.LOCATION.dynamic && StickerKind.TIME.dynamic && StickerKind.HASHTAG.dynamic)
    }

    @Test fun layerTiming() {
        val l = EditorLayer.text("t", "Hi").timed(2000, 2100, 10_000)
        assertEquals(2000L, l.startMs); assertEquals(2500L, l.endMs)
        assertTrue(l.visibleAt(2000)); assertFalse(l.visibleAt(2500)); assertFalse(l.visibleAt(1999))
        val end = EditorLayer.text("t", "Hi").timed(9900, 20_000, 10_000)
        assertEquals(9500L, end.startMs); assertEquals(10_000L, end.endMs)
        assertTrue(EditorLayer.text("x", "always").visibleAt(Long.MAX_VALUE - 1))
    }

    @Test fun paletteContrast() {
        assertEquals(0xFF111111, EditorPalette.onColor(0xFFFFFFFF))
        assertEquals(0xFFFFFFFF, EditorPalette.onColor(0xFF0B6243))
    }

    // ---------- crop / photo edit / history ----------
    @Test fun cropFitPanZoom() {
        val sq = Crop.fit(4000, 3000, CropAspect.SQUARE)
        near(sq.pixelAspect(4000, 3000), 1f); near(sq.height, 1f); near((sq.left + sq.right) / 2, 0.5f)
        near(Crop.fit(1080, 1920, CropAspect.PORTRAIT).pixelAspect(1080, 1920), 0.8f)
        near(Crop.fit(1080, 1920, CropAspect.WIDE).pixelAspect(1080, 1920), 16f / 9f)
        assertTrue(Crop.fit(10, 10, CropAspect.FREE).isFull)
        val p = Crop.pan(sq, 5f, 0f); near(p.right, 1f); near(p.width, sq.width)
        val z = Crop.zoom(CropRect(), 2f); near(z.width, 0.5f); near(z.left, 0.25f)
        near(Crop.zoom(z, 0.1f).width, 1f)
        assertEquals(3000 to 4000, Crop.rotatedSize(4000, 3000, 1))
    }

    @Test fun photoEditOps() {
        assertTrue(PhotoEdit().isUntouched)
        val e = PhotoEdit().withAspect(CropAspect.SQUARE, 4000, 3000)
        assertEquals(CropAspect.SQUARE, e.aspect); assertFalse(e.isUntouched)
        val r = e.rotate(); assertEquals(1, r.quarterTurns); assertTrue(r.crop.isFull)
        assertEquals(0, PhotoEdit(quarterTurns = 3).rotate().quarterTurns)
        val l = EditorLayer.text("a", "x")
        val up = PhotoEdit().upsert(l).upsert(l.copy(text = "y"))
        assertEquals(1, up.layers.size); assertEquals("y", up.layers[0].text)
        assertTrue(up.remove("a").layers.isEmpty())
    }

    @Test fun historyUndoRedoReset() {
        val h = EditHistory(0, cap = 3)
        assertFalse(h.canUndo); h.commit(0); assertFalse(h.canUndo)
        h.commit(1); h.commit(2); assertEquals(1, h.undo()); assertTrue(h.canRedo)
        assertEquals(2, h.redo()); h.undo(); h.commit(5); assertFalse(h.canRedo)
        assertTrue(h.isDirty); assertEquals(0, h.reset()); assertFalse(h.isDirty)
        assertEquals(5, h.undo()) // reset is undoable
        (10..20).forEach { h.commit(it) }; var n = 0; while (h.canUndo) { h.undo(); n++ }; assertEquals(3, n)
    }

    // ---------- reel timeline ----------
    @Test fun trimAndSpeed() {
        val e = ReelEdit(30_000).trim(5_000, 25_000)
        assertEquals(20_000L, e.keptMs); assertEquals(20_000L, e.outputMs)
        assertEquals(10_000L, e.withSpeed(ReelSpeed.DOUBLE).outputMs)
        assertEquals(40_000L, e.withSpeed(ReelSpeed.HALF).outputMs)
        assertEquals(13_333L, e.withSpeed(ReelSpeed.FAST).outputMs)
        val tiny = ReelEdit(30_000).trim(10_000, 10_100); assertEquals(ReelEdit.MIN_CLIP_MS, tiny.keptMs)
        assertEquals(30_000L, ReelEdit(30_000).trim(-5, 99_999).trimEndMs)
    }

    @Test fun splitDeleteAndMapping() {
        val e = ReelEdit(30_000).splitAt(10_000)
        assertEquals(listOf(ReelClip(0, 10_000), ReelClip(10_000, 30_000)), e.clips)
        assertEquals(listOf(10_000L), e.cutsOnOutput())
        assertSame(e, e.splitAt(10_500)) // < 1s piece
        val d = e.deleteClip(0); assertEquals(listOf(ReelClip(10_000, 30_000)), d.clips); assertEquals(20_000L, d.outputMs)
        assertSame(d, d.deleteClip(0)) // never the last clip
        assertEquals(0 to 10_000L, d.sourceAt(0)); assertEquals(0 to 15_000L, d.sourceAt(5_000))
        val fast = d.withSpeed(ReelSpeed.DOUBLE); assertEquals(0 to 20_000L, fast.sourceAt(5_000))
        assertEquals(listOf(5_000L), ReelEdit(30_000).splitAt(10_000).withSpeed(ReelSpeed.DOUBLE).cutsOnOutput())
    }

    @Test fun speedRescalesTimedLayersAndClamps() {
        val l = EditorLayer.text("t", "Hi").timed(4_000, 8_000, 20_000)
        val e = ReelEdit(20_000).upsert(l).withSpeed(ReelSpeed.DOUBLE)
        assertEquals(2_000L, e.layers[0].startMs); assertEquals(4_000L, e.layers[0].endMs)
        val trimmed = ReelEdit(20_000).upsert(EditorLayer.text("t", "Hi").timed(15_000, 19_000, 20_000)).trim(0, 10_000)
        assertTrue(trimmed.layers[0].endMs <= 10_000 && trimmed.layers[0].startMs <= 9_500)
        val always = EditorLayer.text("a", "x")
        assertEquals(Long.MAX_VALUE, ReelEdit(20_000).upsert(always).trim(0, 5000).layers[0].endMs)
    }

    // ---------- export params ----------
    private val pd = ReelSound("hisn_1", "Morning remembrance", "Hisn al-Muslim", "https://example.org/a.mp3", "Public Domain Mark 1.0", "https://example.org")

    @Test fun exportParams() {
        val plain = ReelEdit(30_000).trim(2_000, 12_000)
        assertFalse(ReelExportParams.needsFullExport(plain))
        assertTrue(ReelExportParams.needsFullExport(plain.withSpeed(ReelSpeed.FAST)))
        assertTrue(ReelExportParams.needsFullExport(plain.copy(filter = NoorFilter.FAJR)))
        assertTrue(ReelExportParams.needsFullExport(plain.copy(originalVolume = 0f)))
        assertTrue(ReelExportParams.needsFullExport(plain.copy(sound = pd)))
        assertTrue(ReelExportParams.needsFullExport(plain.splitAt(5_000)))
        assertFalse(ReelExportParams.needsFullExport(plain.copy(filter = NoorFilter.FAJR, strength = 0f)))

        val e = plain.copy(filter = NoorFilter.LAYL, originalVolume = 0f, sound = pd, soundVolume = 2f).withSpeed(ReelSpeed.DOUBLE)
        val p = ReelExportParams.from(e, 1920, 1080)
        assertTrue(p.removeAudio); assertEquals("https://example.org/a.mp3", p.soundUrl); assertEquals(1f, p.soundVolume)
        assertEquals(2f, p.speed); assertEquals(5_000L, p.expectedDurationMs); assertNotNull(p.colorMatrixGl)
        assertEquals(1280 to 720, p.outputWidth to p.outputHeight); assertTrue(p.needsVideoEffects)
        val q = ReelExportParams.from(ReelEdit(10_000), 720, 1280)
        assertNull(q.colorMatrixGl); assertFalse(q.removeAudio); assertNull(q.soundUrl); assertFalse(q.hasOverlay)
    }

    @Test fun outputSizeEvenAndCapped() {
        assertEquals(720 to 1280, ReelExportParams.outputSize(1080, 1920))
        assertEquals(540 to 960, ReelExportParams.outputSize(540, 960))
        val (w, h) = ReelExportParams.outputSize(1081, 1923)
        assertTrue(w % 2 == 0 && h % 2 == 0 && maxOf(w, h) <= 1280)
        val small = ReelExportParams.outputSize(361, 641); assertEquals(360 to 640, small)
    }

    // ---------- sound licensing ----------
    private fun track(lic: LicenseType, url: String = "https://cdn.example.org/dua.mp3", attr: String = "Hisn al-Muslim · Public Domain Mark 1.0") =
        Track("t", "Dua", "a", "Hisn al-Muslim", url, 60_000, license = lic, attributionText = attr, sourceUrl = "https://example.org")

    @Test fun onlyReusableAlNoorAudio() {
        assertTrue(ReelSounds.eligible(track(LicenseType.PUBLIC_DOMAIN)))
        assertTrue(ReelSounds.eligible(track(LicenseType.CC0)))
        assertTrue(ReelSounds.eligible(track(LicenseType.CC_BY)))
        listOf(LicenseType.SOURCE_TERMS, LicenseType.PUBLISHER_RSS, LicenseType.CC_BY_SA, LicenseType.PERMISSION_LETTER)
            .forEach { assertFalse("$it", ReelSounds.eligible(track(it))) }
        assertFalse(ReelSounds.eligible(track(LicenseType.PUBLIC_DOMAIN, url = "https://www.youtube.com/watch?v=x")))
        assertFalse(ReelSounds.eligible(track(LicenseType.PUBLIC_DOMAIN, url = "https://youtu.be/x")))
        assertFalse(ReelSounds.eligible(track(LicenseType.PUBLIC_DOMAIN, url = "http://cdn.example.org/a.mp3")))
        assertFalse(ReelSounds.eligible(track(LicenseType.CC_BY, attr = "")))
        val s = ReelSounds.from(track(LicenseType.PUBLIC_DOMAIN))!!
        assertEquals("Public Domain Mark 1.0", s.licenseLabel)
        assertEquals("Dua — Hisn al-Muslim (Public domain)", s.credit)
        assertNull(ReelSounds.from(track(LicenseType.SOURCE_TERMS)))
        assertTrue(s.copy(title = "x".repeat(200)).credit.length <= 80)
    }
}
