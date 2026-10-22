package com.noorpro.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height as h
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.components.QuranArabicFont

/** Minimal markdown model for Noor AI answers (no raw ### / ** ever reaches the screen). */
sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class Bullet(val text: String, val depth: Int) : MdBlock()
    data class Numbered(val number: String, val text: String) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class Sources(val refs: List<String>) : MdBlock()
    object Rule : MdBlock()
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock()
    data class Code(val text: String) : MdBlock()
    /** A "### Evidence" style section, shown as a collapsible card. */
    data class Evidence(val title: String, val blocks: List<MdBlock>) : MdBlock()
}

object NoorAiMarkdown {
    private val heading = Regex("^(#{1,6})\\s+(.*)$")
    private val bullet = Regex("^(\\s*)[-*•+]\\s+(.*)$")
    private val numbered = Regex("^\\s*(\\d{1,3})[.)]\\s+(.*)$")
    private val rule = Regex("^\\s*([-*_]\\s*){3,}$")
    private val sourcesHeader = Regex("^\\s*(#{1,6}\\s*)?\\**\\s*(sources?|references?)\\s*:?\\s*\\**\\s*:?\\s*$", RegexOption.IGNORE_CASE)
    private val arabic = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

    fun isMostlyArabic(text: String): Boolean {
        val letters = text.count { it.isLetter() }
        if (letters == 0) return false
        return arabic.findAll(text).count() * 2 > letters
    }

    fun parse(md: String): List<MdBlock> {
        val out = mutableListOf<MdBlock>()
        val para = StringBuilder()
        var inSources = false
        val refs = mutableListOf<String>()
        fun flush() {
            if (para.isNotBlank()) out.add(MdBlock.Paragraph(para.toString().trim()))
            para.clear()
        }
        val tableLines = mutableListOf<String>()
        val code = StringBuilder()
        var inCode = false
        fun flushTable() {
            if (tableLines.isEmpty()) return
            val rows = tableLines.filterNot { tableSep.matches(it) }.map { splitRow(it) }
            tableLines.clear()
            if (rows.isEmpty()) return
            val width = rows.maxOf { it.size }
            val norm = rows.map { r -> r + List(width - r.size) { "" } }
            out.add(MdBlock.Table(norm.first(), norm.drop(1)))
        }
        for (raw in NoorAiMarkdown.withoutFollowups(md).replace("\r", "").lines()) {
            val line = raw.trimEnd()
            if (line.trimStart().startsWith("```")) {
                if (inCode) { out.add(MdBlock.Code(code.toString().trimEnd())); code.clear(); inCode = false }
                else { flush(); flushTable(); inCode = true }
                continue
            }
            if (inCode) { if (code.isNotEmpty()) code.append('\n'); code.append(raw); continue }
            if (line.trimStart().startsWith("|") && line.count { it == '|' } >= 2) { flush(); tableLines.add(line.trim()); continue }
            flushTable()
            if (inSources) {
                val t = line.trim()
                if (t.isEmpty()) continue
                val b = bullet.find(line)?.groupValues?.get(2) ?: numbered.find(line)?.groupValues?.get(2)
                if (b != null) { refs.add(stripInline(b)); continue }
                inSources = false
                out.add(MdBlock.Sources(refs.toList())); refs.clear()
            }
            when {
                line.isBlank() -> flush()
                sourcesHeader.matches(line) -> { flush(); inSources = true }
                rule.matches(line) -> { flush(); out.add(MdBlock.Rule) }
                heading.matches(line) -> { flush(); val m = heading.find(line)!!; out.add(MdBlock.Heading(m.groupValues[1].length, m.groupValues[2].trim().trim('#').trim())) }
                line.trimStart().startsWith(">") -> { flush(); out.add(MdBlock.Quote(line.trimStart().removePrefix(">").trim())) }
                bullet.matches(line) -> { flush(); val m = bullet.find(line)!!; out.add(MdBlock.Bullet(m.groupValues[2], if (m.groupValues[1].length >= 2) 1 else 0)) }
                numbered.matches(line) -> { flush(); val m = numbered.find(line)!!; out.add(MdBlock.Numbered(m.groupValues[1], m.groupValues[2])) }
                else -> { if (para.isNotEmpty()) para.append('\n'); para.append(line.trim()) }
            }
        }
        flush(); flushTable()
        if (inCode && code.isNotBlank()) out.add(MdBlock.Code(code.toString().trimEnd()))
        if (inSources && refs.isNotEmpty()) out.add(MdBlock.Sources(refs))
        // Merge consecutive quote lines.
        val merged = mutableListOf<MdBlock>()
        for (b in out) {
            val last = merged.lastOrNull()
            if (b is MdBlock.Quote && last is MdBlock.Quote) merged[merged.lastIndex] = MdBlock.Quote(last.text + "\n" + b.text)
            else merged.add(b)
        }
        // Fold an Evidence/Proof/Dalil section (until the next heading or Sources) into a collapsible card.
        val folded = mutableListOf<MdBlock>()
        var i = 0
        while (i < merged.size) {
            val b = merged[i]
            if (b is MdBlock.Heading && evidenceTitle.containsMatchIn(b.text)) {
                val inner = mutableListOf<MdBlock>()
                i++
                while (i < merged.size && merged[i] !is MdBlock.Heading && merged[i] !is MdBlock.Sources) { inner.add(merged[i]); i++ }
                folded.add(MdBlock.Evidence(stripInline(b.text), inner))
            } else { folded.add(b); i++ }
        }
        return folded
    }

    private val tableSep = Regex("^\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?$")
    private val evidenceTitle = Regex("(?i)\\b(evidence|proofs?|dal[iī]l|daleel)\\b")
    fun splitRow(line: String): List<String> = line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
    fun withoutFollowups(md: String): String = com.noorpro.app.data.AiFeatureService.splitFollowups(md).first

    fun stripInline(s: String): String = s.replace("**", "").replace("__", "").replace("`", "").trim()

    private val refPattern = Regex("\\((?:Qur'?an|Quran|Sahih|Sunan|Jami|Surah|Al-|An-|At-|Ar-|Bukhari|Muslim|Tirmidhi|Abu Dawud|Ibn Majah|Nasa'?i)[^()]{0,80}\\)", RegexOption.IGNORE_CASE)

    /** **bold**, *italic*, _italic_, `code`; parenthesised references are muted. */
    fun inline(text: String, accent: Color, muted: Color, codeBg: Color): AnnotatedString = buildAnnotatedString {
        var i = 0
        val s = text
        while (i < s.length) {
            val rest = s.substring(i)
            val ref = refPattern.find(rest)?.takeIf { it.range.first == 0 }
            when {
                ref != null -> { pushStyle(SpanStyle(color = muted, fontSize = 13.sp)); append(ref.value); pop(); i += ref.value.length }
                rest.startsWith("**") && rest.indexOf("**", 2) > 2 -> {
                    val end = rest.indexOf("**", 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold)); append(rest.substring(2, end)); pop(); i += end + 2
                }
                rest.startsWith("`") && rest.indexOf('`', 1) > 1 -> {
                    val end = rest.indexOf('`', 1)
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg, color = accent)); append(rest.substring(1, end)); pop(); i += end + 1
                }
                (rest.startsWith("*") || rest.startsWith("_")) && rest.length > 2 && !rest[1].isWhitespace() &&
                    rest.indexOf(rest[0], 1).let { it > 1 && !rest[it - 1].isWhitespace() } &&
                    (i == 0 || !s[i - 1].isLetterOrDigit()) -> {
                    val end = rest.indexOf(rest[0], 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(rest.substring(1, end)); pop(); i += end + 1
                }
                else -> { append(s[i]); i++ }
            }
        }
    }
}

data class NoorAiMdColors(val text: Color, val muted: Color, val accent: Color, val quoteBar: Color, val quoteBg: Color, val chipBg: Color, val codeBg: Color)

@Composable
fun NoorAiMarkdownText(markdown: String, colors: NoorAiMdColors, modifier: Modifier = Modifier, evidenceOpen: Boolean = false) {
    val blocks = remember(markdown) { NoorAiMarkdown.parse(markdown) }
    MdBlocks(blocks, colors, modifier, evidenceOpen)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MdBlocks(blocks: List<MdBlock>, colors: NoorAiMdColors, modifier: Modifier = Modifier, evidenceOpen: Boolean = false) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { b ->
            when (b) {
                is MdBlock.Table -> MdTable(b, colors)
                is MdBlock.Code -> Text(
                    b.text, color = colors.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.codeBg)
                        .horizontalScroll(rememberScrollState()).padding(12.dp)
                )
                is MdBlock.Evidence -> EvidenceCard(b, colors, evidenceOpen)
                is MdBlock.Heading -> Text(
                    NoorAiMarkdown.stripInline(b.text), color = colors.text,
                    fontSize = if (b.level <= 2) 18.sp else 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
                is MdBlock.Paragraph -> MdLine(b.text, colors)
                is MdBlock.Bullet -> Row(Modifier.padding(start = (b.depth * 16).dp)) {
                    Text("•", color = colors.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
                    MdLine(b.text, colors, Modifier.weight(1f))
                }
                is MdBlock.Numbered -> Row {
                    Text("${b.number}.", color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(24.dp))
                    MdLine(b.text, colors, Modifier.weight(1f))
                }
                is MdBlock.Quote -> Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(10.dp)).background(colors.quoteBg)
                ) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(colors.quoteBar))
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        b.text.lines().forEach { MdLine(it, colors) }
                    }
                }
                MdBlock.Rule -> Spacer(Modifier.h(6.dp))
                is MdBlock.Sources -> Column(Modifier.padding(top = 4.dp)) {
                    Text("Sources", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.h(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        b.refs.forEach { r ->
                            Text(
                                r, color = colors.muted, fontSize = 12.sp,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.chipBg).padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MdTable(t: MdBlock.Table, colors: NoorAiMdColors) {
    val cellW = if (t.header.size <= 2) 150.dp else 120.dp
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, colors.chipBg, RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            Row(Modifier.background(colors.chipBg)) {
                t.header.forEach { h ->
                    Text(NoorAiMarkdown.inline(h, colors.accent, colors.muted, colors.codeBg), color = colors.text, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.width(cellW).padding(horizontal = 10.dp, vertical = 8.dp))
                }
            }
            t.rows.forEachIndexed { ri, r ->
                Row(Modifier.background(if (ri % 2 == 1) colors.quoteBg else Color.Transparent)) {
                    r.forEach { c ->
                        Text(NoorAiMarkdown.inline(c, colors.accent, colors.muted, colors.codeBg), color = colors.text, fontSize = 13.sp,
                            lineHeight = 18.sp, modifier = Modifier.width(cellW).padding(horizontal = 10.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceCard(e: MdBlock.Evidence, colors: NoorAiMdColors, startOpen: Boolean) {
    var open by remember(e) { mutableStateOf(startOpen) }
    val rot by animateFloatAsState(if (open) 180f else 0f, label = "evRot")
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(14.dp)).background(colors.quoteBg)
            .border(1.dp, colors.quoteBar.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(e.title.ifBlank { "Evidence" }, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("${e.blocks.size}", color = colors.muted, fontSize = 12.sp)
            Icon(Icons.Default.ExpandMore, if (open) "Collapse" else "Expand", tint = colors.muted, modifier = Modifier.rotate(rot))
        }
        AnimatedVisibility(open) {
            MdBlocks(e.blocks, colors, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
        }
    }
}

@Composable
private fun MdLine(text: String, colors: NoorAiMdColors, modifier: Modifier = Modifier) {
    // Lines whose label is "Arabic:" or which are mostly Arabic script are rendered RTL in Noto Naskh.
    val labelSplit = Regex("^\\**\\s*([A-Za-z ]{2,20}):\\s*\\**\\s*(.+)$").find(text)
    if (labelSplit != null && NoorAiMarkdown.isMostlyArabic(labelSplit.groupValues[2])) {
        Column(modifier) {
            Text(labelSplit.groupValues[1], color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            ArabicLine(NoorAiMarkdown.stripInline(labelSplit.groupValues[2]), colors)
        }
        return
    }
    if (NoorAiMarkdown.isMostlyArabic(text)) {
        Box(modifier) { ArabicLine(NoorAiMarkdown.stripInline(text), colors) }
        return
    }
    Text(
        NoorAiMarkdown.inline(com.noorpro.app.ui.theme.isolateArabic(text), colors.accent, colors.muted, colors.codeBg),
        color = colors.text, fontSize = 15.sp, lineHeight = 23.sp, modifier = modifier
    )
}

@Composable
private fun ArabicLine(text: String, colors: NoorAiMdColors) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text, color = colors.text, fontFamily = QuranArabicFont, fontSize = 22.sp, lineHeight = 38.sp,
            textAlign = TextAlign.Start, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
