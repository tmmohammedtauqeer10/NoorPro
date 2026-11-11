package com.noorpro.app.ui.screens

/**
 * 1.2.0 (114): ChatGPT-style history sections. Pure (no Android) so it is unit tested on the JVM.
 * Pinned chats first, then Today, Yesterday, Previous 7 days, Previous 30 days, then month names / Older.
 */
internal data class NoorAiHistorySection(val title: String, val items: List<NoorAiConversation>)

internal fun groupConversations(
    list: List<NoorAiConversation>,
    now: Long = System.currentTimeMillis(),
    zone: java.util.TimeZone = java.util.TimeZone.getDefault(),
): List<NoorAiHistorySection> {
    val cal = java.util.Calendar.getInstance(zone).apply {
        timeInMillis = now; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }
    val today = cal.timeInMillis
    val day = 86_400_000L
    val pinned = list.filter { it.pinned }.sortedByDescending { it.updated }
    val rest = list.filterNot { it.pinned }.sortedByDescending { it.updated }
    val out = mutableListOf<NoorAiHistorySection>()
    if (pinned.isNotEmpty()) out += NoorAiHistorySection("Pinned", pinned)
    val buckets = linkedMapOf<String, MutableList<NoorAiConversation>>()
    rest.forEach { c ->
        val key = when {
            c.updated >= today -> "Today"
            c.updated >= today - day -> "Yesterday"
            c.updated >= today - 7 * day -> "Previous 7 days"
            c.updated >= today - 30 * day -> "Previous 30 days"
            else -> "Older"
        }
        buckets.getOrPut(key) { mutableListOf() } += c
    }
    buckets.forEach { (k, v) -> out += NoorAiHistorySection(k, v) }
    return out
}

/** The few most recent chats shown on the empty "new chat" screen (ChatGPT-style "Recent"). */
internal fun recentConversations(list: List<NoorAiConversation>, exclude: Long?, max: Int = 4): List<NoorAiConversation> =
    list.filter { it.id != exclude }.sortedByDescending { it.updated }.take(max)
