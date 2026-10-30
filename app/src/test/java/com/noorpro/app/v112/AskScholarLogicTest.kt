package com.noorpro.app.v112

import com.noorpro.app.islamic.social.*
import org.junit.Assert.*
import org.junit.Test

class AskScholarLogicTest {
    private fun qa(id: String, text: String, answer: String, cat: ScholarCategory, at: Long, scholar: String = "Shaykh Abdullah") =
        ScholarQuestion(id, text, cat, answered = true, answer = answer, scholarUid = "s", scholarName = scholar, answeredAt = at)

    private val lib = listOf(
        qa("a", "Can I combine Dhuhr and Asr when travelling?", "Yes, the traveller may combine…", ScholarCategory.SALAH, 3),
        qa("b", "Is zakat due on gold jewellery I wear?", "Scholars differ; the safer view…", ScholarCategory.ZAKAT, 2, "Ustadha Maryam"),
        qa("c", "How do I make up missed fasts?", "Make them up one day for each…", ScholarCategory.FASTING, 1),
        ScholarQuestion("open", "Unanswered question about travel prayer", category = ScholarCategory.SALAH),
    )

    @Test fun validation() {
        assertNotNull(ScholarLogic.validateQuestion("Is it ok?"))
        assertNull(ScholarLogic.validateQuestion("Is it permissible to pray sitting on a long flight?"))
        assertNotNull(ScholarLogic.validateQuestion("x".repeat(1001)))
        assertNotNull(ScholarLogic.validateAnswer("Yes."))
        assertNull(ScholarLogic.validateAnswer("Yes, and the evidence is the hadith of Imran ibn Husain in Bukhari."))
    }

    @Test fun anonymousDocsCarryNoName() {
        val d = ScholarLogic.newDoc("A question long enough to pass", ScholarCategory.FAMILY, anonymous = true, name = "Yusuf")
        assertEquals("", d["name"]); assertEquals("open", d["status"]); assertFalse("uid" in d.keys)
        assertEquals("Yusuf", ScholarLogic.newDoc("q", ScholarCategory.FAMILY, false, " Yusuf ")["name"])
    }

    @Test fun searchMatchesAllWordsAcrossFieldsAndHidesOpen() {
        assertEquals(listOf("a"), ScholarLogic.search(lib, "travel").map { it.id })
        assertEquals(listOf("b"), ScholarLogic.search(lib, "GOLD zakat!").map { it.id })
        assertEquals(listOf("b"), ScholarLogic.search(lib, "maryam").map { it.id })          // scholar name
        assertEquals(listOf("c"), ScholarLogic.search(lib, "fasting").map { it.id })         // category label
        assertEquals(listOf("a", "b", "c"), ScholarLogic.search(lib, "").map { it.id })      // newest first, open hidden
        assertEquals(listOf("a"), ScholarLogic.search(lib, "", ScholarCategory.SALAH).map { it.id })
        assertTrue(ScholarLogic.search(lib, "travel gold").isEmpty())
    }

    @Test fun parsesAndFlagsNewAnswers() {
        val q = ScholarLogic.fromFirestore("x", mapOf("text" to "t", "category" to "FINANCE", "status" to "answered", "answer" to "a", "scholarName" to "S", "anonymous" to false, "name" to "Ali"))
        assertTrue(q.answered); assertEquals(ScholarCategory.FINANCE, q.category); assertEquals("Asked by Ali", q.askedBy)
        assertEquals("Asked anonymously", ScholarLogic.fromFirestore("y", emptyMap()).askedBy)
        assertEquals(listOf("b", "c"), ScholarLogic.newlyAnswered(lib, setOf("a")).map { it.id })
    }
}
