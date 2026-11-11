package com.noorpro.app.ui.screens

/**
 * 1.2.0 (115): plain text for Copy / Share of a Noor AI answer (chat, Research mode, voice mode).
 * Strips markdown and the meta lines (FOLLOWUPS / FIQH / LANG) but keeps every reference.
 */
object NoorAiCopy {
    const val SHARE_FOOTER = "— via Noor AI (Noor Pro)"

    fun plain(answer: String): String {
        val body = com.noorpro.app.data.AiFeatureService.parseMeta(answer).body
        val blocks = NoorAiMarkdown.parse(body)
        val sb = StringBuilder()
        var prevList = false
        for (b in blocks) {
            val text = blockText(b).trimEnd()
            if (text.isEmpty()) continue
            val isList = b is MdBlock.Bullet || b is MdBlock.Numbered
            if (sb.isNotEmpty()) sb.append(if (isList && prevList) "\n" else "\n\n")
            sb.append(text)
            prevList = isList
        }
        return sb.toString().trim()
    }

    fun shareText(answer: String): String = plain(answer) + "\n\n" + SHARE_FOOTER

    private fun blockText(b: MdBlock): String = when (b) {
        is MdBlock.Heading -> NoorAiMarkdown.stripInline(b.text)
        is MdBlock.Paragraph -> NoorAiMarkdown.stripInline(b.text)
        is MdBlock.Bullet -> "  ".repeat(b.depth) + "• " + NoorAiMarkdown.stripInline(b.text)
        is MdBlock.Numbered -> "${b.number}. " + NoorAiMarkdown.stripInline(b.text)
        is MdBlock.Quote -> NoorAiMarkdown.stripInline(b.text).trim().let { q -> if (q.startsWith("\"") || q.startsWith("“")) q else "“$q”" }
        is MdBlock.Sources -> "Sources:\n" + b.refs.joinToString("\n") { "• " + NoorAiMarkdown.stripInline(it) }
        MdBlock.Rule -> ""
        is MdBlock.Table -> (listOf(b.header) + b.rows).joinToString("\n") { it.joinToString(" | ") { c -> NoorAiMarkdown.stripInline(c) } }
        is MdBlock.Code -> b.text
        is MdBlock.Evidence -> b.title + "\n" + b.blocks.joinToString("\n") { blockText(it) }
    }
}
