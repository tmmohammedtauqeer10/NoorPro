package com.noorpro.app.v111

import com.noorpro.app.islamic.khatm.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GroupKhatmLogicTest {
    @Test fun codesAreUnambiguous() {
        val r = Random(7)
        repeat(500) {
            val c = KhatmLogic.newCode(r)
            assertTrue(c, KhatmLogic.isValidCode(c))
            assertFalse(c.any { it in "01OIL" })
        }
        assertFalse(KhatmLogic.isValidCode("ABC1EF"))
        assertFalse(KhatmLogic.isValidCode("abcdef"))
        assertFalse(KhatmLogic.isValidCode("ABCDE"))
    }

    @Test fun parsesInvitesFromLinksAndTypedCodes() {
        assertEquals("K7M2QP", KhatmLogic.parseInvite("https://noor-pro-d87e3.web.app/k?c=K7M2QP"))
        assertEquals("K7M2QP", KhatmLogic.parseInvite("Join us! https://noor-pro-d87e3.web.app/k?c=k7m2qp\nCode: K7M2QP"))
        assertEquals("K7M2QP", KhatmLogic.parseInvite(" k7m 2qp "))
        assertEquals("K7M2QP", KhatmLogic.parseInvite("K7M-2QP"))
        assertNull(KhatmLogic.parseInvite("hello"))
        assertNull(KhatmLogic.parseInvite("https://noor-pro-d87e3.web.app/k?c=K0M2QP"))
        assertEquals("https://noor-pro-d87e3.web.app/k?c=K7M2QP", KhatmLogic.inviteLink("K7M2QP"))
        val g = KhatmGroup("K7M2QP", "Family Khatm", "For Nani", "u", "Ayesha", listOf("u"))
        val share = KhatmLogic.shareText(g)
        assertTrue(share.contains("For Nani") && share.contains(KhatmLogic.inviteLink("K7M2QP")) && share.contains("Code: K7M2QP"))
        assertEquals("K7M2QP", KhatmLogic.parseInvite(share))
    }

    @Test fun progressAndViews() {
        val claims = mapOf(
            1 to ParaClaim(1, "me", "Tauqeer", ParaStatus.DONE),
            2 to ParaClaim(2, "me", "Tauqeer", ParaStatus.CLAIMED),
            3 to ParaClaim(3, "a", "Ayesha", ParaStatus.CLAIMED),
            4 to ParaClaim(4, "a", "Ayesha", ParaStatus.DONE),
        )
        val p = KhatmLogic.progress(claims.values + ParaClaim(31, "x", "x", ParaStatus.DONE))
        assertEquals(KhatmProgress(2, 2, 26), p)
        assertFalse(p.complete)
        assertEquals(ParaView.MINE_DONE, KhatmLogic.view(1, claims, "me"))
        assertEquals(ParaView.MINE, KhatmLogic.view(2, claims, "me"))
        assertEquals(ParaView.TAKEN, KhatmLogic.view(3, claims, "me"))
        assertEquals(ParaView.DONE, KhatmLogic.view(4, claims, "me"))
        assertEquals(ParaView.OPEN, KhatmLogic.view(5, claims, "me"))
        assertEquals(ParaView.TAKEN, KhatmLogic.view(2, claims, null))
        assertEquals(5, KhatmLogic.nextOpen(claims))
        val all = (1..30).associateWith { ParaClaim(it, "u", "n", ParaStatus.DONE) }
        assertTrue(KhatmLogic.progress(all.values).complete)
        assertNull(KhatmLogic.nextOpen(all))
    }

    @Test fun parasTable() {
        assertEquals(30, Paras.names.size)
        assertEquals(30, Paras.starts.size)
        assertEquals(2 to 142, Paras.starts[1])
        assertEquals(78 to 1, Paras.starts[29])
        assertEquals("Alif Lam Mim", Paras.name(1))
        assertTrue(Paras.starts.zipWithNext().all { (a, b) -> a.first < b.first || (a.first == b.first && a.second < b.second) })
    }

    @Test fun cleaningAndErrors() {
        assertEquals("Family Khatm", KhatmLogic.cleanTitle("  Family    Khatm "))
        assertEquals(80, KhatmLogic.cleanTitle("x".repeat(200)).length)
        assertEquals("A reader", KhatmLogic.cleanName("  "))
        assertEquals(KhatmError.TAKEN, KhatmError.from(RuntimeException("tx", KhatmException(KhatmError.TAKEN))))
        assertEquals(KhatmError.UNKNOWN, KhatmError.from(IllegalStateException()))
        assertTrue(KhatmError.entries.all { it.title.isNotBlank() && it.body.isNotBlank() })
    }
}
