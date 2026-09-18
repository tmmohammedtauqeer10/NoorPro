package com.noorpro.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PilgrimageLogicTest {
    private fun progress() = JourneyProgress("tester", JourneyType.UMRAH, "tawaf")

    @Test
    fun tawafCounterNeverExceedsSevenOrDropsBelowZero() {
        val over = updateRitualCounter(progress().copy(ritualCounters = mapOf("tawaf" to 7)), "tawaf", 1, 7)
        val under = updateRitualCounter(progress(), "tawaf", -1, 7)
        assertEquals(7, over.ritualCounters["tawaf"])
        assertEquals(0, under.ritualCounters["tawaf"])
    }

    @Test
    fun progressPercentageUsesCompletedSteps() {
        assertEquals(40, progressPercentage(progress().copy(completedRitualIds = setOf("one", "two")), 5))
        assertEquals(0, progressPercentage(progress(), 0))
    }

    @Test
    fun nextActiveRitualSkipsCompletedGuides() {
        val review = ScholarReview(ScholarReviewStatus.SOURCE_REVIEWED, "reviewer", "2026-09-12", "")
        fun guide(id: String, order: Int) = PilgrimageGuide(
            id = id,
            journeyType = JourneyType.UMRAH,
            ritualOrder = order,
            title = id,
            titleUrdu = id,
            summary = id,
            summaryUrdu = id,
            instructions = emptyList(),
            prerequisites = emptyList(),
            duas = emptyList(),
            references = emptyList(),
            madhhabNotes = emptyList(),
            accessibilityAdvice = emptyList(),
            mistakes = emptyList(),
            fiqhClassification = "General",
            scholarReview = review,
            updatedAt = "2026-09-12",
            version = 1
        )
        assertEquals("two", nextActiveRitualId(listOf(guide("two", 2), guide("one", 1)), setOf("one")))
    }

    @Test
    fun onlyApprovedOfficialHostsAreAccepted() {
        assertTrue(isSafeOfficialUrl("https://services.nusuk.sa/"))
        assertTrue(isSafeOfficialUrl("https://my.gov.sa/en/emergency-contact"))
        assertFalse(isSafeOfficialUrl("http://nusuk.sa/"))
        assertFalse(isSafeOfficialUrl("https://nusuk.sa.example.com/fake"))
    }
}
