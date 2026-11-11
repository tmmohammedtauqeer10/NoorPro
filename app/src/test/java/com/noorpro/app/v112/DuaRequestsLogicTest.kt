package com.noorpro.app.v112

import com.noorpro.app.islamic.community.CommunityError
import com.noorpro.app.islamic.community.CommunityException
import com.noorpro.app.islamic.community.CommunityText
import com.noorpro.app.islamic.social.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class DuaRequestsLogicTest {
    companion object { @BeforeClass @JvmStatic fun words() = loadHalalWords() }

    @Test fun anonymousDocsCarryNoIdentity() {
        val d = DuaLogic.newDoc("Please pray for my exams", DuaCategory.STUDIES, anonymous = true, name = "Yusuf", uid = "u1")
        assertEquals("", d["authorUid"]); assertEquals("", d["name"]); assertEquals(true, d["anonymous"])
        val named = DuaLogic.newDoc("Please pray for my exams", DuaCategory.STUDIES, anonymous = false, name = "  Yusuf  ", uid = "u1")
        assertEquals("u1", named["authorUid"]); assertEquals("Yusuf", named["name"]); assertEquals(0, named["ameenCount"])
        assertEquals(CommunityText.ANON_NAME, DuaRequest("x", "t", anonymous = true, name = "Yusuf").shownName)
    }

    @Test fun validationAndCategories() {
        assertNotNull(DuaLogic.validate("pray"))
        assertNull(DuaLogic.validate("Please pray for my father"))
        assertNotNull(DuaLogic.validate("a".repeat(401)))
        assertEquals(DuaCategory.OTHER, DuaCategory.of("nope"))
        assertEquals(DuaCategory.HEALTH, DuaCategory.of("HEALTH"))
        assertEquals("u1_ameen", DuaLogic.responseId("u1", DuaResponse.AMEEN))
    }

    @Test fun togglesNeverGoNegative() {
        val d = DuaRequest("a", "t", ameenCount = 0, prayedCount = 2)
        assertEquals(1, DuaLogic.toggle(d, DuaResponse.AMEEN, true).ameenCount)
        assertEquals(0, DuaLogic.toggle(d, DuaResponse.AMEEN, false).ameenCount)
        assertEquals(1, DuaLogic.toggle(d, DuaResponse.PRAYED, false).prayedCount)
    }

    @Test fun countLines() {
        assertEquals("Be the first to make dua", DuaLogic.countLine(DuaRequest("a", "t")))
        assertEquals("3 said Ameen", DuaLogic.countLine(DuaRequest("a", "t", ameenCount = 3)))
        assertEquals("2 prayed", DuaLogic.countLine(DuaRequest("a", "t", prayedCount = 2)))
        assertEquals("3 Ameen · 2 prayed", DuaLogic.countLine(DuaRequest("a", "t", ameenCount = 3, prayedCount = 2)))
    }

    @Test fun gentleNotificationOnlyOnNews() {
        val mine = listOf(DuaRequest("a", "t", ameenCount = 4, prayedCount = 1), DuaRequest("b", "t", ameenCount = 2))
        assertNull(DuaLogic.newSupport(mine, mapOf("a" to 5, "b" to 2)))
        assertEquals("Someone made dua for you", DuaLogic.newSupport(mine, mapOf("a" to 4, "b" to 2))!!.first)
        assertEquals("7 people made dua for you", DuaLogic.newSupport(mine, emptyMap())!!.first)
        // A drop (someone undid Ameen) never triggers anything.
        assertNull(DuaLogic.newSupport(mine, mapOf("a" to 9, "b" to 2)))
    }

    @Test fun expiryAndCategoryFilter() {
        val now = 1_800_000_000_000L
        val l = listOf(DuaRequest("old", "t", DuaCategory.HEALTH, createdAt = now - 31L * 86_400_000), DuaRequest("new", "t", DuaCategory.HEALTH, createdAt = now - 3_600_000),
            DuaRequest("fam", "t", DuaCategory.FAMILY, createdAt = now - 60_000), DuaRequest("pending", "t", DuaCategory.FAMILY, createdAt = 0))
        assertEquals(listOf("new", "fam", "pending"), DuaLogic.visible(l, null, now).map { it.id })
        assertEquals(listOf("new"), DuaLogic.visible(l, DuaCategory.HEALTH, now).map { it.id })
    }

    @Test fun halalGuardBlocksSevereText() {
        val ok = CommunityText.prepare("  Please   make dua for my mother  ", 400)
        assertEquals("Please make dua for my mother", ok.text); assertFalse(ok.review)
        // Same vectors as functions/halal-vectors.json: severe → blocked, mild → masked, scams → review.
        val e = runCatching { CommunityText.prepare("pornhub link in bio", 400) }.exceptionOrNull()
        assertTrue(e is CommunityException && e.error == CommunityError.BLOCKED_TEXT)
        assertEquals("s••• happens", CommunityText.prepare("sh!t happens", 400).text)
        assertTrue(CommunityText.prepare("double your money in 7 days", 400).review)
        assertTrue(CommunityText.hint("pornhub link in bio").isNotBlank()); assertEquals("", CommunityText.hint("Ya Allah grant shifa"))
        assertEquals("just now", CommunityText.ago(0)); assertEquals("2h", CommunityText.ago(10_000_000_000L - 2 * 3_600_000, 10_000_000_000L))
    }
}
