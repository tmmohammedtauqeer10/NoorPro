package com.noorpro.app.v112

import com.noorpro.app.data.GroupReadMarks
import org.junit.Assert.*
import org.junit.Test

class GroupReadMarksTest {
    @Test fun unreadWhenNewerThanLastOpenAndSentBySomeoneElse() {
        assertTrue(GroupReadMarks.isUnread(200, 100, "b", "me"))
        assertFalse(GroupReadMarks.isUnread(100, 200, "b", "me"))
        assertFalse(GroupReadMarks.isUnread(200, 100, "me", "me"))
        assertFalse(GroupReadMarks.isUnread(0, 0, "b", "me"))
        assertTrue(GroupReadMarks.isUnread(200, 100, "", "me"))
    }
}
