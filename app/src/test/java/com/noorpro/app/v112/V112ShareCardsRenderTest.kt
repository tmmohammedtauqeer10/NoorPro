package com.noorpro.app.v112

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.islamic.social.*
import com.noorpro.app.ui.islamic.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V112ShareCardsRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun save(b: Bitmap, name: String) {
        val out = File(File("build/v112").apply { mkdirs() }, "$name.png")
        out.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(out.length() > 5000)
    }

    @Test fun bundledTextLoads() {
        val a = ShareCardSource.load(ctx, "94:6")!!
        assertEquals(CardKind.AYAH, a.kind)
        assertEquals("Surah Ash-Sharh 94:6".lowercase().take(10), a.reference.lowercase().take(10))
        assertTrue(a.reference.endsWith("94:6"))
        assertTrue(a.arabic.isNotBlank())
        assertTrue(ShareCardSource.load(ctx, "94:6", CardTranslation.SAHIH)!!.text.contains("hardship"))
        val ikhlas = ShareCardSource.ayah(ctx, 112, 1)!!
        assertFalse("basmala should be stripped", ShareCardLogic.skel(ikhlas.arabic).startsWith(ShareCardLogic.skel("بسم الله")))
        val h = ShareCardSource.load(ctx, "Bukhari 13")!!
        assertEquals("Sahih al-Bukhari 13", h.reference)
        assertTrue(h.text.contains("brother"))
        assertNull(ShareCardSource.load(ctx, "Muslim 99999"))
    }

    @Test fun everyPickFitsEveryDesign() {
        (ShareCardLogic.AYAH_PICKS + ShareCardLogic.HADITH_PICKS).forEach { p ->
            val c = ShareCardSource.load(ctx, p.ref)
            assertNotNull(p.ref, c)
            CardDesign.entries.forEach { d -> assertTrue("${p.ref} ${d.name}", ShareCardRenderer.render(ctx, c!!, d, CardFormat.SQUARE, 0.1f).fits) }
        }
        CardTranslation.entries.forEach { tr ->
            val c = ShareCardSource.load(ctx, "2:286", tr)!!
            assertTrue(tr.name, ShareCardRenderer.render(ctx, c, CardDesign.NOOR, CardFormat.STORY, 0.1f, tr).fits)
        }
        assertFalse("2:282 is too long for a square card", ShareCardRenderer.render(ctx, ShareCardSource.load(ctx, "2:282")!!, CardDesign.NOOR, CardFormat.SQUARE, 0.1f).fits)
    }

    @Test fun cardArtwork() {
        val ayah = ShareCardSource.load(ctx, "13:28")!!
        val hadith = ShareCardSource.load(ctx, "Bukhari 6464")!!
        save(ShareCardRenderer.render(ctx, ayah, CardDesign.NOOR, CardFormat.SQUARE).bitmap, "v112_card_noor")
        save(ShareCardRenderer.render(ctx, hadith, CardDesign.PARCHMENT, CardFormat.SQUARE).bitmap, "v112_card_parchment")
        save(ShareCardRenderer.render(ctx, ShareCardSource.load(ctx, "94:6")!!, CardDesign.NIGHT, CardFormat.STORY).bitmap, "v112_card_night_story")
        save(ShareCardRenderer.render(ctx, ShareCardSource.load(ctx, "39:53", CardTranslation.URDU)!!, CardDesign.DAWN, CardFormat.SQUARE, tr = CardTranslation.URDU).bitmap, "v112_card_dawn_urdu")
        save(ShareCardRenderer.render(ctx, ShareCardSource.load(ctx, "Bukhari 13")!!, CardDesign.MINIMAL, CardFormat.SQUARE).bitmap, "v112_card_minimal")
        val f = ShareCardExport.savePng(ctx, ShareCardRenderer.render(ctx, ayah, CardDesign.NOOR, CardFormat.SQUARE, 0.25f).bitmap)
        assertTrue(f.exists() && f.length() > 1000)
        val draft = ShareCardExport.draft(f, ayah, ComposerDestination.STORY)
        assertEquals(ComposerDestination.STORY, draft.destination)
        assertEquals("Quran", draft.topic)
        assertTrue(draft.media.single().startsWith("file:"))
        assertTrue(draft.text.endsWith(ayah.reference))
    }

    private fun content(dark: Boolean, ui0: ShareCardsUi, shot: String, extra: (ShareCardsUi) -> Unit = {}) {
        val c = ShareCardSource.load(ctx, ui0.ref, ui0.tr)!!
        val ui = ui0.copy(content = c)
        val prev = ShareCardRenderer.render(ctx, c, ui.design, ui.format, 0.5f, ui.tr).bitmap.asImageBitmap()
        val thumbs = CardDesign.entries.associateWith { ShareCardRenderer.render(ctx, c, it, CardFormat.SQUARE, 0.16f, ui.tr).bitmap.asImageBitmap() }
        var picked: String? = null; var design: CardDesign? = null; var fed = false
        rule.show(dark) { ShareCardsContent(ui, prev, thumbs, ShareCardsActions(onPick = { picked = it }, onDesign = { design = it }, onFeed = { fed = true })) }
        rule.onNodeWithTag("cards_preview").assertExists()
        extra(ui)
        rule.onNodeWithTag("cards_design_DAWN").performScrollTo().performClick()
        assertEquals(CardDesign.DAWN, design)
        rule.onNodeWithTag("cards_feed").performScrollTo().performClick()
        assertTrue(fed)
        rule.onNodeWithTag("cards_screen").performTouchInput { swipeDown() }
        rule.shot(shot)
        if (ui.kind == CardKind.AYAH) { rule.onNodeWithTag("cards_pick_2_152").performScrollTo().performClick(); assertEquals("2:152", picked) }
    }

    @Test fun ayahLight() = content(false, ShareCardsUi(ref = "13:28"), "v112_cards_ayah_light")

    @Test fun hadithDark() = content(true, ShareCardsUi(kind = CardKind.HADITH, ref = "Bukhari 6464", design = CardDesign.NIGHT), "v112_cards_hadith_dark") {
        rule.onNodeWithTag("cards_tr_SAHIH").assertDoesNotExist()
    }

    @Test fun storyParchmentLight() = content(false, ShareCardsUi(ref = "65:3", design = CardDesign.PARCHMENT, format = CardFormat.STORY, tr = CardTranslation.SAHIH), "v112_cards_story_light")

    @Test fun notFoundHint() {
        rule.show(false) { ShareCardsContent(ShareCardsUi(kind = CardKind.HADITH, custom = "Muslim 99999", notFound = true), null, emptyMap(), ShareCardsActions()) }
        rule.onNodeWithTag("cards_not_found").assertExists()
        rule.onNodeWithTag("cards_feed").assertHasClickAction()
    }
}
