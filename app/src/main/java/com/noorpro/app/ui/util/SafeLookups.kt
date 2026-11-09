package com.noorpro.app.ui.util

/**
 * 1.2.0: null-safe lookups that used to crash (stale ids, empty catalogs, poll tallies).
 * Pure JVM helpers so unit tests do not need Robolectric.
 */
object SafeLookups {
    /** indexOfFirst semantics that treat "not found" (-1) as null instead of a usable index. */
    fun indexOrNull(index: Int): Int? = index.takeIf { it >= 0 }

    fun <T> getOrNull(list: List<T>, index: Int): T? =
        if (index in list.indices) list[index] else null

    fun <T> firstOrNullMatching(list: List<T>, predicate: (T) -> Boolean): T? =
        list.firstOrNull(predicate)

    /** Poll bar fill; never throws when options and tallies disagree. */
    fun percentAt(percents: List<Int>, index: Int): Int = percents.getOrElse(index) { 0 }

    /**
     * Resolve a lesson id against a flat lesson list. Returns null when the id is unknown
     * (stale prefs / course JSON update) so the UI can fall back to Home.
     */
    fun lessonIndex(allLessonIds: List<String>, lessonId: String): Int? =
        indexOrNull(allLessonIds.indexOf(lessonId))

    fun levelIndexForLesson(levelLessonIds: List<List<String>>, lessonId: String): Int? {
        val i = levelLessonIds.indexOfFirst { ids -> lessonId in ids }
        return indexOrNull(i)
    }
}
