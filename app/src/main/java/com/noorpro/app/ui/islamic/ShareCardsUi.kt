package com.noorpro.app.ui.islamic

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.ComposerDraftStore
import com.noorpro.app.data.ComposerLaunch
import com.noorpro.app.islamic.social.*
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* 1.1.6 Ayah / Hadith share cards UI (share_cards_enabled). */

data class ShareCardsUi(
    val kind: CardKind = CardKind.AYAH,
    val ref: String = ShareCardLogic.AYAH_PICKS.first().ref,
    val custom: String = "",
    val tr: CardTranslation = CardTranslation.DEFAULT,
    val design: CardDesign = CardDesign.NOOR,
    val format: CardFormat = CardFormat.SQUARE,
    val showArabic: Boolean = true,
    val showTranslation: Boolean = true,
    val showReference: Boolean = true,
    val fontScale: Float = 1f,
    val align: CardAlign = CardAlign.CENTER,
    val colorway: CardColorway = CardColorway.TEMPLATE,
    val content: CardContent? = null,
    val notFound: Boolean = false,
    val fits: Boolean = true,
    val busy: Boolean = false,
) {
    val style: CardStyle get() = CardStyle(showArabic, showTranslation, showReference, fontScale, align, colorway)
    val range: Triple<Int, Int, Int>? get() = ShareCardLogic.parseAyahRange(ref)
}

class ShareCardsActions(
    val onBack: () -> Unit = {},
    val onKind: (CardKind) -> Unit = {},
    val onPick: (String) -> Unit = {},
    val onCustom: (String) -> Unit = {},
    val onTranslation: (CardTranslation) -> Unit = {},
    val onDesign: (CardDesign) -> Unit = {},
    val onFormat: (CardFormat) -> Unit = {},
    val onStep: (Int) -> Unit = {},
    val onSurahStep: (Int) -> Unit = {},
    val onRangeEnd: (Int) -> Unit = {},
    val onToggleArabic: () -> Unit = {},
    val onToggleTranslation: () -> Unit = {},
    val onToggleReference: () -> Unit = {},
    val onFontScale: (Float) -> Unit = {},
    val onAlign: (CardAlign) -> Unit = {},
    val onColorway: (CardColorway) -> Unit = {},
    val onFeed: () -> Unit = {},
    val onStory: () -> Unit = {},
    val onExternal: () -> Unit = {},
)

@Composable
fun ShareCardsScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ui by remember {
        val launch = ShareCardLaunch.consume()
        mutableStateOf(if (launch == null) ShareCardsUi() else ShareCardsUi(kind = if (ShareCardLogic.parseHadith(launch) != null) CardKind.HADITH else CardKind.AYAH, ref = launch))
    }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var thumbs by remember { mutableStateOf<Map<CardDesign, ImageBitmap>>(emptyMap()) }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    LaunchedEffect(ui.ref, ui.tr) {
        val c = withContext(Dispatchers.IO) { ShareCardSource.load(ctx, ui.ref, ui.tr) }
        ui = ui.copy(content = c, notFound = c == null)
    }
    LaunchedEffect(ui.content, ui.design, ui.format, ui.showArabic, ui.showTranslation, ui.showReference, ui.fontScale, ui.align, ui.colorway) {
        val c = ui.content ?: run { preview = null; return@LaunchedEffect }
        val r = withContext(Dispatchers.Default) { ShareCardRenderer.render(ctx, c, ui.design, ui.format, 0.5f, ui.tr, ui.style) }
        preview = r.bitmap.asImageBitmap(); ui = ui.copy(fits = r.fits)
    }
    LaunchedEffect(ui.content, ui.colorway) {
        val c = ui.content ?: return@LaunchedEffect
        thumbs = withContext(Dispatchers.Default) { CardDesign.entries.associateWith { ShareCardRenderer.render(ctx, c, it, CardFormat.SQUARE, 0.16f, ui.tr, ui.style).bitmap.asImageBitmap() } }
    }

    fun export(then: (java.io.File, CardContent) -> Unit) {
        val c = ui.content ?: return
        if (!ui.fits) { toast("This one is long for a card. Try the story size or another passage."); return }
        ui = ui.copy(busy = true)
        scope.launch {
            val f = withContext(Dispatchers.Default) { ShareCardExport.savePng(ctx, ShareCardRenderer.render(ctx, c, ui.design, ui.format, 1f, ui.tr, ui.style).bitmap) }
            ui = ui.copy(busy = false)
            then(f, c)
        }
    }
    fun toComposer(dest: ComposerDestination) = export { f, c ->
        scope.launch {
            val d = withContext(Dispatchers.IO) { ComposerDraftStore(ctx).save(ShareCardExport.draft(f, c, dest)) }
            ComposerLaunch.openDraft(d.id, dest)
            viewModel.navigateTo(DeenScreen.UMMAH_CREATE_POST)
        }
    }

    ShareCardsContent(ui, preview, thumbs, ShareCardsActions(
        onBack = { viewModel.goBack() },
        onKind = { k -> ui = ui.copy(kind = k, ref = (if (k == CardKind.AYAH) ShareCardLogic.AYAH_PICKS else ShareCardLogic.HADITH_PICKS).first().ref, custom = "") },
        onPick = { r -> ui = ui.copy(ref = r, custom = "") },
        onCustom = { s ->
            ui = ui.copy(custom = s)
            val ok = if (ui.kind == CardKind.AYAH) ShareCardLogic.parseAyah(s) != null else ShareCardLogic.parseHadith(s) != null
            if (ok) ui = ui.copy(ref = s.trim())
        },
        onTranslation = { ui = ui.copy(tr = it) },
        onDesign = { ui = ui.copy(design = it) },
        onFormat = { ui = ui.copy(format = it) },
        onStep = { delta ->
            val cur = ShareCardLogic.parseAyahRange(ui.ref)
            if (cur != null) {
                val (s, a) = ShareCardLogic.stepAyah(cur.first, cur.second, delta)
                val span = cur.third - cur.second
                val (from, to) = ShareCardLogic.clampRange(s, a, a + span)
                ui = ui.copy(ref = ShareCardLogic.formatAyahRef(s, from, to), custom = "")
            }
        },
        onSurahStep = { delta ->
            val cur = ShareCardLogic.parseAyahRange(ui.ref) ?: (1 to 1).let { Triple(it.first, it.second, it.second) }
            val s = (cur.first + delta).coerceIn(1, 114)
            val a = cur.second.coerceIn(1, ShareCardLogic.ayahCount(s))
            val (from, to) = ShareCardLogic.clampRange(s, a, a)
            ui = ui.copy(ref = ShareCardLogic.formatAyahRef(s, from, to), custom = "")
        },
        onRangeEnd = { end ->
            val cur = ShareCardLogic.parseAyahRange(ui.ref)
            if (cur != null) {
                val (from, to) = ShareCardLogic.clampRange(cur.first, cur.second, end)
                ui = ui.copy(ref = ShareCardLogic.formatAyahRef(cur.first, from, to), custom = "")
            }
        },
        onToggleArabic = { ui = ui.copy(showArabic = !ui.showArabic) },
        onToggleTranslation = { ui = ui.copy(showTranslation = !ui.showTranslation) },
        onToggleReference = { ui = ui.copy(showReference = !ui.showReference) },
        onFontScale = { ui = ui.copy(fontScale = it) },
        onAlign = { ui = ui.copy(align = it) },
        onColorway = { ui = ui.copy(colorway = it) },
        onFeed = { toComposer(ComposerDestination.FEED) },
        onStory = { ui = ui.copy(format = CardFormat.STORY); toComposer(ComposerDestination.STORY) },
        onExternal = { export { f, c -> runCatching { ctx.startActivity(ShareCardExport.externalIntent(ctx, f, c)) }.onFailure { toast("No app available to share to") } } },
    ))
}

@Composable
fun ShareCardsContent(ui: ShareCardsUi, preview: ImageBitmap?, thumbs: Map<CardDesign, ImageBitmap>, actions: ShareCardsActions, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("cards_screen")) {
        IkHeader("Share cards", "Ayah & hadith, beautifully", actions.onBack)
        IkSegments(listOf("Ayah", "Hadith"), if (ui.kind == CardKind.AYAH) 0 else 1, Modifier.padding(horizontal = 16.dp), "cards_kind") {
            actions.onKind(if (it == 0) CardKind.AYAH else CardKind.HADITH)
        }
        Spacer(Modifier.height(8.dp))
        val picks = if (ui.kind == CardKind.AYAH) ShareCardLogic.AYAH_PICKS else ShareCardLogic.HADITH_PICKS
        IkPillRow<CardPick?>(picks, picks.firstOrNull { it.ref == ui.ref }, { "${it?.ref} · ${it?.label}" }, tag = { "cards_pick_${it?.ref?.replace(' ', '_')?.replace(':', '_')}" }) { p -> p?.let { actions.onPick(it.ref) } }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            IkField(ui.custom, actions.onCustom, if (ui.kind == CardKind.AYAH) "Or type a reference, e.g. 2:255" else "Or type Bukhari or Muslim + number",
                tag = "cards_custom", maxLen = 24)
            if (ui.notFound) Text("That reference isn't in the app's bundled text. Check the number.", color = IkAmber, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp).testTag("cards_not_found"))
            if (ui.kind == CardKind.AYAH) {
                Spacer(Modifier.height(10.dp))
                val range = ui.range
                if (range != null) {
                    val (surah, start, end) = range
                    CardStepperRow(
                        label = ShareCardLogic.surahName(surah) + "  ($surah)",
                        onPrev = { actions.onSurahStep(-1) },
                        onNext = { actions.onSurahStep(1) },
                        prevEnabled = surah > 1,
                        nextEnabled = surah < 114,
                        tag = "cards_surah"
                    )
                    Spacer(Modifier.height(6.dp))
                    CardStepperRow(
                        label = if (end > start) "Ayahs $start–$end" else "Ayah $start",
                        onPrev = { actions.onStep(-1) },
                        onNext = { actions.onStep(1) },
                        prevEnabled = surah > 1 || start > 1,
                        nextEnabled = surah < 114 || end < ShareCardLogic.ayahCount(surah),
                        tag = "cards_ayah"
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Range", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0 until ShareCardLogic.MAX_AYAH_RANGE).forEach { extra ->
                            val endAyah = start + extra
                            val enabled = endAyah <= ShareCardLogic.ayahCount(surah)
                            IkPill(
                                if (extra == 0) "1 ayah" else "${extra + 1} ayahs",
                                selected = enabled && end == endAyah,
                                tag = "cards_range_${extra + 1}"
                            ) { if (enabled) actions.onRangeEnd(endAyah) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Show on the card", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IkPill("Arabic", ui.showArabic, tag = "cards_toggle_arabic", onClick = actions.onToggleArabic)
                    IkPill("Translation", ui.showTranslation, tag = "cards_toggle_translation", onClick = actions.onToggleTranslation)
                    IkPill("Reference", ui.showReference, tag = "cards_toggle_reference", onClick = actions.onToggleReference)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IkPill("Text", ui.showTranslation, tag = "cards_toggle_translation", onClick = actions.onToggleTranslation)
                    IkPill("Reference", ui.showReference, tag = "cards_toggle_reference", onClick = actions.onToggleReference)
                }
            }
            Spacer(Modifier.height(12.dp))
            val ratio = ui.format.w.toFloat() / ui.format.h
            Box(Modifier.fillMaxWidth(if (ui.format == CardFormat.STORY) 0.62f else 1f).align(Alignment.CenterHorizontally).aspectRatio(ratio)
                .clip(RoundedCornerShape(18.dp)).background(ikSoft()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).testTag("cards_preview"),
                contentAlignment = Alignment.Center) {
                if (preview != null) Image(preview, ui.content?.let { "${it.reference} card" }, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Text("Preparing…", color = stitchMutedText(), fontSize = 13.sp)
            }
            if (!ui.fits) Text("This passage is long for one card — the story size gives it more room.", color = IkAmber, fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp).testTag("cards_too_long"))
            IkSection("Design", Modifier.padding(top = 14.dp))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardDesign.entries.forEach { d ->
                val sel = d == ui.design
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { actions.onDesign(d) }.testTag("cards_design_${d.name}")) {
                    Box(Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)).background(ikSoft())
                        .border(if (sel) 2.5.dp else 1.dp, if (sel) IkGold else stitchLine(), RoundedCornerShape(14.dp))) {
                        thumbs[d]?.let { Image(it, null, Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(d.label, color = if (sel) stitchText() else stitchMutedText(), fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            IkSection("Size")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardFormat.entries.forEach { f -> IkPill(f.label, f == ui.format, icon = if (f == CardFormat.SQUARE) Icons.Outlined.CropSquare else Icons.Outlined.CropPortrait, tag = "cards_format_${f.name}") { actions.onFormat(f) } }
            }
            IkSection("Text size", Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.8f to "Small", 1f to "Medium", 1.2f to "Large", 1.4f to "Extra large").forEach { (scale, label) ->
                    IkPill(label, kotlin.math.abs(ui.fontScale - scale) < 0.05f, tag = "cards_size_${scale}") { actions.onFontScale(scale) }
                }
            }
            IkSection("Alignment", Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CardAlign.entries.forEach { a ->
                    IkPill(a.label, a == ui.align, tag = "cards_align_${a.name}") { actions.onAlign(a) }
                }
            }
            IkSection("Colours", Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CardColorway.entries.forEach { c ->
                    IkPill(c.label, c == ui.colorway, tag = "cards_color_${c.name}") { actions.onColorway(c) }
                }
            }
            if (ui.kind == CardKind.AYAH) {
                IkSection("Translation", Modifier.padding(top = 14.dp))
                Spacer(Modifier.height(6.dp))
            }
        }
        if (ui.kind == CardKind.AYAH) IkPillRow(CardTranslation.entries, ui.tr, { it.label }, tag = { "cards_tr_${it.name}" }, onPick = actions.onTranslation)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IkButton("Post to feed", Modifier.weight(1f), Icons.Outlined.DynamicFeed, enabled = ui.content != null && !ui.busy, tag = "cards_feed", onClick = actions.onFeed)
                IkButton("Add to story", Modifier.weight(1f), Icons.Outlined.AddCircleOutline, enabled = ui.content != null && !ui.busy, tag = "cards_story", onClick = actions.onStory)
            }
            Spacer(Modifier.height(10.dp))
            IkButton(if (ui.busy) "Preparing image…" else "Share image to other apps", Modifier.fillMaxWidth(), Icons.Outlined.IosShare, primary = false,
                enabled = ui.content != null && !ui.busy, tag = "cards_external", onClick = actions.onExternal)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, stitchLine(), RoundedCornerShape(14.dp)).padding(12.dp)) {
                Icon(Icons.Outlined.Info, null, tint = stitchMutedText(), modifier = Modifier.size(16.dp).padding(top = 1.dp))
                Spacer(Modifier.width(8.dp))
                Text("Words come from the Quran and the Sahih al-Bukhari / Sahih Muslim collections bundled in NoorPro. The reference is always printed so anyone can check it. Feed and story posts open in the composer for you to review first.",
                    color = stitchMutedText(), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun CardStepperRow(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    prevEnabled: Boolean,
    nextEnabled: Boolean,
    tag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, stitchLine(), RoundedCornerShape(14.dp)).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, enabled = prevEnabled, modifier = Modifier.testTag("${tag}_prev").size(40.dp)) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous", tint = if (prevEnabled) stitchText() else stitchMutedText())
        }
        Text(label, color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).testTag(tag), maxLines = 1)
        IconButton(onClick = onNext, enabled = nextEnabled, modifier = Modifier.testTag("${tag}_next").size(40.dp)) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next", tint = if (nextEnabled) stitchText() else stitchMutedText())
        }
    }
}
