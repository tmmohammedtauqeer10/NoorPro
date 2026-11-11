package com.noorpro.app.learning

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class LearningResource(val title: String, val url: String)
data class LearningQuiz(val question: String, val options: List<String>, val correctIndex: Int, val explanation: String)
data class AcademyLesson(
    val id: String, val title: String, val format: String, val minutes: Int,
    val body: String, val url: String, val resources: List<LearningResource>, val quiz: LearningQuiz?
)
data class AcademyModule(val id: String, val title: String, val lessons: List<AcademyLesson>)
data class AcademyProvider(val id: String, val name: String, val url: String, val accessNote: String)
data class AcademyPath(val id: String, val title: String, val description: String, val courseIds: List<String>)
data class AcademyCourse(
    val id: String, val title: String, val subject: String, val language: String,
    val level: String, val providerId: String, val instructor: String, val format: String,
    val description: String, val accessNote: String, val sourceUrl: String, val checkedOn: String,
    val reuse: String, val certificate: String, val outcomes: List<String>, val prerequisites: String,
    val modules: List<AcademyModule>, val resources: List<LearningResource>
) {
    val lessons: List<AcademyLesson> get() = modules.flatMap { it.lessons }
}
data class AcademyCatalog(val providers: List<AcademyProvider>, val paths: List<AcademyPath>, val courses: List<AcademyCourse>)

object LearningCatalog {
    suspend fun load(context: Context): AcademyCatalog = withContext(Dispatchers.IO) {
        parse(context.assets.open("learning/catalog.json").bufferedReader().use { it.readText() })
    }

    fun isSafeExternalUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)

    fun parse(text: String): AcademyCatalog {
        val root = JSONObject(text)
        require(root.getInt("version") == 1) { "Unsupported learning catalog" }
        val providers = root.getJSONArray("providers").objects().map {
            AcademyProvider(it.getString("id"), it.getString("name"), it.getString("url"), it.getString("access_note"))
        }
        val courses = root.getJSONArray("courses").objects().map { c ->
            AcademyCourse(
                id = c.getString("id"), title = c.getString("title"), subject = c.getString("subject"),
                language = c.getString("language"), level = c.getString("level"), providerId = c.getString("provider_id"),
                instructor = c.getString("instructor"), format = c.getString("format"), description = c.getString("description"),
                accessNote = c.getString("access_note"), sourceUrl = c.getString("source_url"), checkedOn = c.getString("checked_on"),
                reuse = c.getString("reuse"), certificate = c.getString("certificate"), outcomes = c.getJSONArray("outcomes").strings(),
                prerequisites = c.getString("prerequisites"), resources = c.getJSONArray("resources").resources(),
                modules = c.getJSONArray("modules").objects().map { m ->
                    AcademyModule(m.getString("id"), m.getString("title"), m.getJSONArray("lessons").objects().map { l ->
                        val q = l.optJSONObject("quiz")
                        AcademyLesson(l.getString("id"), l.getString("title"), l.getString("format"), l.getInt("minutes"),
                            l.getString("body"), l.getString("url"), l.getJSONArray("resources").resources(),
                            q?.let { LearningQuiz(it.getString("question"), it.getJSONArray("options").strings(), it.getInt("correct_index"), it.getString("explanation")) })
                    })
                }
            )
        }
        val paths = root.getJSONArray("learning_paths").objects().map {
            AcademyPath(it.getString("id"), it.getString("title"), it.getString("description"), it.getJSONArray("course_ids").strings())
        }
        require(providers.map { it.id }.distinct().size == providers.size)
        require(courses.isNotEmpty() && courses.map { it.id }.distinct().size == courses.size)
        val courseIds = courses.map { it.id }.toSet()
        val providerIds = providers.map { it.id }.toSet()
        val allLessons = courses.flatMap { it.lessons }
        require(allLessons.map { it.id }.distinct().size == allLessons.size)
        courses.forEach { course ->
            require(course.providerId in providerIds && course.title.isNotBlank() && course.lessons.isNotEmpty())
            require(course.format in setOf("reading", "external", "directory"))
            require(isSafeExternalUrl(course.sourceUrl))
            require(course.reuse == if (course.format == "reading") "original_content" else "external_link_only")
            (course.resources + course.lessons.flatMap { it.resources }).forEach { require(isSafeExternalUrl(it.url)) }
            course.lessons.forEach { lesson ->
                require(lesson.title.isNotBlank() && lesson.body.isNotBlank() && lesson.minutes >= 0)
                require(lesson.format in setOf("reading", "external"))
                if (lesson.format == "external") require(isSafeExternalUrl(lesson.url))
                lesson.quiz?.let { require(it.options.size == 4 && it.options.distinct().size == 4 && it.correctIndex in 0..3) }
            }
        }
        paths.forEach { require(it.courseIds.isNotEmpty() && it.courseIds.all(courseIds::contains)) }
        return AcademyCatalog(providers, paths, courses)
    }

    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    private fun JSONArray.resources() = objects().map { LearningResource(it.getString("title"), it.getString("url")) }
}

/** Counts only valid lesson IDs: removed/stale content never increases a progress score. */
fun completedLessonCount(course: AcademyCourse, completed: Set<String>): Int = course.lessons.count { it.id in completed }
fun isCourseComplete(course: AcademyCourse, completed: Set<String>): Boolean =
    course.lessons.isNotEmpty() && completedLessonCount(course, completed) == course.lessons.size
