package com.noorpro.app.ui.util

import org.junit.Assert.*
import org.junit.Test

class SafeLookupsTest {
    @Test fun indexOrNullRejectsMinusOne() {
        assertNull(SafeLookups.indexOrNull(-1))
        assertEquals(0, SafeLookups.indexOrNull(0))
        assertEquals(3, SafeLookups.indexOrNull(3))
    }

    @Test fun getOrNullBounds() {
        val list = listOf("a", "b")
        assertEquals("a", SafeLookups.getOrNull(list, 0))
        assertNull(SafeLookups.getOrNull(list, -1))
        assertNull(SafeLookups.getOrNull(list, 2))
    }

    @Test fun percentAtNeverThrows() {
        assertEquals(0, SafeLookups.percentAt(emptyList(), 0))
        assertEquals(40, SafeLookups.percentAt(listOf(40, 60), 0))
        assertEquals(0, SafeLookups.percentAt(listOf(40), 5))
    }

    @Test fun staleLessonFallsBack() {
        val ids = listOf("l1", "l2", "l3")
        assertEquals(1, SafeLookups.lessonIndex(ids, "l2"))
        assertNull(SafeLookups.lessonIndex(ids, "gone"))
        val levels = listOf(listOf("l1"), listOf("l2", "l3"))
        assertEquals(1, SafeLookups.levelIndexForLesson(levels, "l3"))
        assertNull(SafeLookups.levelIndexForLesson(levels, "missing"))
    }
}
