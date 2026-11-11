package com.noorpro.app.ui.screens

import android.content.Context
import com.noorpro.app.data.NoorAiConversationEntity
import com.noorpro.app.data.NoorAiHistoryDb
import com.noorpro.app.data.NoorAiMessageEntity
import com.noorpro.app.data.NoorAiMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 1.2.0 (114): Noor AI chat history backed by Room (`noor_ai_history.db`, on this phone only). Chats saved by
 * earlier versions in SharedPreferences ([NoorAiChatStore]) are copied in once. The "current chat per mode"
 * pointer and the chat settings stay in [NoorAiChatStore].
 */
internal object NoorAiHistory {
    private val lock = Mutex()
    private const val MAX_MESSAGES = 200

    private fun dao(ctx: Context) = NoorAiHistoryDb.get(ctx).dao()

    private fun NoorAiConversationEntity.toUi(): NoorAiConversation? {
        val m = runCatching { NoorAiMode.valueOf(mode) }.getOrNull() ?: return null
        return NoorAiConversation(id, m, title, updated, pinned, customTitle)
    }

    /** One-time import of chats saved before 114. Safe to call often. */
    suspend fun migrate(ctx: Context) = withContext(Dispatchers.IO) {
        lock.withLock {
            val prefs = ctx.getSharedPreferences("noor_ai_chat", Context.MODE_PRIVATE)
            if (prefs.getBoolean("room_migrated_114", false)) return@withLock
            runCatching {
                val dao = dao(ctx)
                NoorAiChatStore.conversations(ctx).forEach { c ->
                    val items = NoorAiChatStore.load(ctx, c.id)
                    if (items.isNotEmpty() && dao.conversation(c.id) == null) {
                        dao.replaceMessages(
                            NoorAiConversationEntity(c.id, c.mode.name, c.title, c.updated, c.pinned, c.customTitle, items.first().time.takeIf { it > 0 } ?: c.updated),
                            items.mapIndexed { i, it -> NoorAiMessageEntity(c.id, it.id, i, it.text, it.isUser, it.time, it.feedback) },
                        )
                    }
                }
                prefs.edit().putBoolean("room_migrated_114", true).apply()
            }
        }
    }

    suspend fun conversations(ctx: Context): List<NoorAiConversation> = withContext(Dispatchers.IO) {
        runCatching { dao(ctx).conversations().mapNotNull { it.toUi() } }.getOrDefault(emptyList())
    }

    suspend fun load(ctx: Context, id: Long?): List<NoorAiChatItem> = withContext(Dispatchers.IO) {
        if (id == null) emptyList()
        else runCatching { dao(ctx).messages(id).map { NoorAiChatItem(it.text, it.isUser, it.time, it.feedback, it.msgId) } }.getOrDefault(emptyList())
    }

    /** Saves the messages of a chat; the chat appears in history once it has content. Keeps a renamed title. */
    suspend fun save(ctx: Context, mode: NoorAiMode, id: Long, items: List<NoorAiChatItem>) = withContext(Dispatchers.IO) {
        lock.withLock {
            runCatching {
                val dao = dao(ctx)
                if (items.isEmpty()) { dao.delete(id); return@runCatching }
                val old = dao.conversation(id)
                val kept = items.takeLast(MAX_MESSAGES)
                val title = if (old?.customTitle == true) old.title else NoorAiChatStore.titleOf(items)
                dao.replaceMessages(
                    NoorAiConversationEntity(id, mode.name, title, System.currentTimeMillis(), old?.pinned ?: false, old?.customTitle ?: false, old?.created ?: System.currentTimeMillis()),
                    kept.mapIndexed { i, it -> NoorAiMessageEntity(id, it.id, i, it.text, it.isUser, it.time, it.feedback) },
                )
            }
        }
        NoorAiChatStore.setCurrent(ctx, mode, if (items.isEmpty()) null else id)
    }

    suspend fun delete(ctx: Context, id: Long) = withContext(Dispatchers.IO) {
        lock.withLock { runCatching { dao(ctx).delete(id) } }
        NoorAiMode.values().forEach { if (NoorAiChatStore.currentId(ctx, it) == id) NoorAiChatStore.setCurrent(ctx, it, null) }
        // Also drop the pre-114 copy so it can never come back.
        runCatching { NoorAiChatStore.delete(ctx, id) }
    }

    suspend fun rename(ctx: Context, id: Long, title: String) = withContext(Dispatchers.IO) {
        val clean = title.replace("\n", " ").trim().take(60)
        lock.withLock {
            runCatching {
                if (clean.isBlank()) dao(ctx).rename(id, NoorAiChatStore.titleOf(load(ctx, id)), false) else dao(ctx).rename(id, clean, true)
            }
        }
    }

    suspend fun setPinned(ctx: Context, id: Long, pinned: Boolean) = withContext(Dispatchers.IO) {
        lock.withLock { runCatching { dao(ctx).setPinned(id, pinned) } }
    }

    /** Title or message text match; pinned first, then newest. */
    suspend fun search(ctx: Context, query: String): List<NoorAiConversation> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) conversations(ctx)
        else runCatching { dao(ctx).search(q.replace("%", "").replace("_", " ")).mapNotNull { it.toUi() } }.getOrDefault(emptyList())
    }
}
