package com.noorpro.app.learning

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class LearningCatalogTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun catalog() = runBlocking { LearningCatalog.load(context) }
    private fun store(id: String = UUID.randomUUID().toString()) = LearningProgressStore(context, id)

    @Test fun bundledCatalogLoadsOfflineWithCompleteReferences() {
        val data = catalog()
        assertEquals(12, data.courses.size)
        assertEquals(7, data.providers.size)
        assertEquals(4, data.paths.size)
        val reading = data.courses.single { it.format == "reading" }
        assertEquals(24, reading.lessons.size)
        assertEquals(12, reading.modules.size)
        assertTrue(reading.lessons.all { it.quiz != null && it.resources.isNotEmpty() })
        assertEquals(10, data.courses.count { it.reuse == "external_link_only" })
    }

    @Test fun externalEntriesNeverClaimCopiedLectureMediaOrProviderCertification() {
        catalog().courses.filter { it.reuse == "external_link_only" }.forEach { c ->
            assertEquals("external_link_only", c.reuse)
            assertTrue(c.lessons.all { it.format == "external" && it.quiz == null })
            assertTrue(c.lessons.all { LearningCatalog.isSafeExternalUrl(it.url) })
            assertNotEquals("noorpro", c.providerId)
            assertTrue(c.certificate.isNotBlank())
        }
    }

    @Test fun progressIgnoresStaleIdsAndRequiresAllCurrentLessons() {
        val course = catalog().courses.first()
        assertEquals(0, completedLessonCount(course, setOf("removed_lesson")))
        assertFalse(isCourseComplete(course, setOf(course.lessons.first().id)))
        assertTrue(isCourseComplete(course, course.lessons.map { it.id }.toSet() + "old"))
    }

    @Test fun videoRequiresExplicitSupportedLicenseAndTrustedMediaHost() {
        val text = context.assets.open("learning/catalog.json").bufferedReader().use { it.readText() }
        val video = catalog().courses.single { it.format == "video" }
        assertEquals("licensed_media", video.reuse)
        assertEquals("Al Madrasatu Al Umariyyah", video.lessons.single().media!!.creator)
        listOf("license" to "All rights reserved", "url" to "https://example.com/video.webm", "source" to "javascript:alert(1)").forEach { (field, value) ->
            val root = JSONObject(text)
            val courses = root.getJSONArray("courses")
            val media = (0 until courses.length()).map { courses.getJSONObject(it) }.single { it.getString("format") == "video" }
                .getJSONArray("modules").getJSONObject(0).getJSONArray("lessons").getJSONObject(0).getJSONObject("media")
            media.put(field, value)
            assertThrows(IllegalArgumentException::class.java) { LearningCatalog.parse(root.toString()) }
        }
    }

    @Test fun progressPersistsAcrossStoreInstances() {
        val id = UUID.randomUUID().toString()
        val first = store(id)
        assertTrue(first.enroll("course"))
        assertTrue(first.setCompleted("lesson", true))
        val restored = store(id)
        assertEquals(setOf("course"), restored.enrolled())
        assertEquals(setOf("lesson"), restored.completed())
        assertTrue(restored.setCompleted("lesson", false))
        assertTrue(first.completed().isEmpty())
    }

    @Test fun accountsCannotReadEachOthersProgressOrNotes() {
        val first = store(); val second = store()
        first.enroll("private-course"); first.setCompleted("private-lesson", true); first.saveNote("private-lesson", "private note")
        assertTrue(second.enrolled().isEmpty())
        assertTrue(second.completed().isEmpty())
        assertEquals("", second.note("private-lesson"))
    }

    @Test fun resettingOneCourseLeavesOtherCoursesAndEnrollmentIntact() {
        val course = catalog().courses.first()
        val progress = store()
        progress.enroll(course.id)
        progress.setCompleted(course.lessons.first().id, true)
        progress.saveNote(course.lessons.first().id, "A thought")
        progress.setCompleted("other_lesson", true)
        assertTrue(progress.reset(course))
        assertEquals(setOf("other_lesson"), progress.completed())
        assertEquals("", progress.note(course.lessons.first().id))
        assertTrue(course.id in progress.enrolled())
    }

    @Test fun notesAreBoundedWithoutChangingCompletion() {
        val progress = store()
        assertTrue(progress.saveNote("lesson", "a".repeat(2500)))
        assertEquals(2000, progress.note("lesson").length)
        assertTrue(progress.completed().isEmpty())
    }

    @Test fun parserRejectsBrokenAnswerIndexesAndUntrustedUrlSchemes() {
        val json = context.assets.open("learning/catalog.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        root.getJSONArray("courses").getJSONObject(0).getJSONArray("modules").getJSONObject(0)
            .getJSONArray("lessons").getJSONObject(0).getJSONObject("quiz").put("correct_index", 9)
        assertThrows(IllegalArgumentException::class.java) { LearningCatalog.parse(root.toString()) }
        listOf("javascript:alert(1)", "file:///local/file", "http://example.com", "https://user:password@example.com", "https:///missing").forEach {
            assertFalse(it, LearningCatalog.isSafeExternalUrl(it))
        }
        assertTrue(LearningCatalog.isSafeExternalUrl("https://understandquran.com/courses/seerah-english/"))
    }
}
