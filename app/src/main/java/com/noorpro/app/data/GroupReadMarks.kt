package com.noorpro.app.data

import android.content.Context

/**
 * Local last-opened marks for community groups. Groups do not have a server unread
 * counter, so the list badge is derived from [UmmahGroup.updatedAt] and who sent last.
 */
object GroupReadMarks {
    private const val PREF = "ummah_group_reads"

    fun lastRead(ctx: Context, groupId: String): Long =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(groupId, 0L)

    fun mark(ctx: Context, groupId: String, at: Long = System.currentTimeMillis()) {
        if (groupId.isBlank()) return
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putLong(groupId, at).apply()
    }

    fun isUnread(updatedAt: Long, lastReadAt: Long, lastSenderUid: String, myUid: String): Boolean {
        if (updatedAt <= 0L) return false
        if (lastSenderUid.isNotBlank() && lastSenderUid == myUid) return false
        return updatedAt > lastReadAt
    }

    fun isUnread(ctx: Context, group: UmmahGroup, myUid: String): Boolean =
        isUnread(group.updatedAt, lastRead(ctx, group.id), group.lastSenderUid, myUid)
}
