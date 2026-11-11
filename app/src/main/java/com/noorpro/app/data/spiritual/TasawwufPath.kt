package com.noorpro.app.data.spiritual

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 1.2.0 (116): Deenflow's 12-week Tasawwuf learning path (24 lessons, knowledge checks, glossary, FAQs, books), bundled offline. */
data class PathCheck(val question: String, val options: List<String>, val answer: Int, val explanation: String)
data class PathLesson(val id: String, val title: String, val minutes: Int, val summary: String, val practice: String, val reflection: String,
                      val sourceIds: List<String>, val check: PathCheck?)
data class PathModule(val id: String, val title: String, val week: Int, val stageId: String, val lessonIds: List<String>, val bookIds: List<String>)
data class PathStage(val id: String, val title: String, val description: String, val moduleIds: List<String>)
data class PathBook(val id: String, val title: String, val author: String, val level: String, val description: String, val guidance: String,
                    val url: String, val reader: String)
data class PathSource(val id: String, val label: String, val url: String)

data class TasawwufPath(
    val title: String, val description: String, val weeks: Int, val weeklyPlan: List<String>, val outcomes: List<String>,
    val stages: List<PathStage>, val modules: List<PathModule>, val lessons: List<PathLesson>, val glossary: List<Pair<String, String>>,
    val faqs: List<Pair<String, String>>, val sources: List<PathSource>, val teacherGuidance: List<String>, val books: List<PathBook>,
    val orientationTitle: String, val orientationBody: String, val orientationSources: List<String>, val reviewNote: String, val credit: String,
) {
    fun lesson(id: String): PathLesson? = lessons.firstOrNull { it.id == id }
    fun source(id: String): PathSource? = sources.firstOrNull { it.id == id }
    fun moduleOf(lessonId: String): PathModule? = modules.firstOrNull { lessonId in it.lessonIds }
    /** Lessons in course order (module by module). */
    val ordered: List<PathLesson> get() = modules.sortedBy { it.week }.flatMap { m -> m.lessonIds.mapNotNull(::lesson) }
    fun nextAfter(id: String): PathLesson? = ordered.let { l -> l.getOrNull(l.indexOfFirst { it.id == id } + 1) }
    /** First lesson not yet completed, or null when the course is finished. */
    fun resume(done: Set<String>): PathLesson? = ordered.firstOrNull { it.id !in done }
    fun progress(done: Set<String>): Float = if (lessons.isEmpty()) 0f else lessons.count { it.id in done } / lessons.size.toFloat()

    companion object {
        private fun strs(a: JSONArray?) = if (a == null) emptyList() else (0 until a.length()).map { a.optString(it) }
        private fun objs(a: JSONArray?) = if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.optJSONObject(it) }

        fun parse(json: String): TasawwufPath {
            val o = JSONObject(json)
            val or = o.optJSONObject("orientation") ?: JSONObject()
            return TasawwufPath(
                title = o.optString("title"), description = o.optString("description"), weeks = o.optInt("estimated_weeks", 12),
                weeklyPlan = strs(o.optJSONArray("weekly_plan")), outcomes = strs(o.optJSONArray("learning_outcomes")),
                stages = objs(o.optJSONArray("stages")).map { PathStage(it.optString("id"), it.optString("title"), it.optString("description"), strs(it.optJSONArray("module_ids"))) },
                modules = objs(o.optJSONArray("modules")).map {
                    PathModule(it.optString("id"), it.optString("title"), it.optInt("week"), it.optString("stage_id"), strs(it.optJSONArray("lesson_ids")), strs(it.optJSONArray("book_ids")))
                },
                lessons = objs(o.optJSONArray("lessons")).map { l ->
                    val c = l.optJSONObject("checkpoint")
                    PathLesson(l.optString("id"), l.optString("title"), l.optInt("estimated_minutes", 15), l.optString("summary"), l.optString("practice"),
                        l.optString("reflection"), strs(l.optJSONArray("source_ids")),
                        c?.let { PathCheck(it.optString("question"), strs(it.optJSONArray("options")), it.optInt("correct_index"), it.optString("explanation")) })
                },
                glossary = objs(o.optJSONArray("glossary")).map { it.optString("term") to it.optString("definition") },
                faqs = objs(o.optJSONArray("faqs")).map { it.optString("question") to it.optString("answer") },
                sources = objs(o.optJSONArray("sources")).map { PathSource(it.optString("id"), it.optString("label"), it.optString("url")) },
                teacherGuidance = strs(o.optJSONArray("teacher_guidance")),
                books = objs(o.optJSONArray("books")).map {
                    PathBook(it.optString("id"), it.optString("title"), it.optString("author"), it.optString("level"), it.optString("description"),
                        it.optString("guidance"), it.optString("url"), it.optString("reader"))
                },
                orientationTitle = or.optString("title"), orientationBody = or.optString("body"), orientationSources = strs(or.optJSONArray("sources")),
                reviewNote = o.optString("review_note"), credit = o.optString("credit"),
            )
        }

        @Volatile private var cached: TasawwufPath? = null
        fun load(ctx: Context): TasawwufPath = cached ?: ctx.assets.open("content/tasawwuf_path.json").bufferedReader(Charsets.UTF_8)
            .use { parse(it.readText()) }.also { cached = it }
    }
}

/** Device-local progress for the path: completed lessons and knowledge-check answers. Nothing is uploaded. */
class TasawwufPathStore(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("tasawwuf_path_v1", Context.MODE_PRIVATE)
    fun done(): Set<String> = p.getStringSet("done", emptySet()).orEmpty().toSet()
    fun setDone(id: String, value: Boolean): Set<String> {
        val s = done().toMutableSet().apply { if (value) add(id) else remove(id) }
        p.edit().putStringSet("done", s).apply(); return s
    }
    fun answer(id: String): Int? = p.getInt("ans.$id", -1).takeIf { it >= 0 }
    fun setAnswer(id: String, i: Int) { p.edit().putInt("ans.$id", i).apply() }
    fun reset() { p.edit().clear().apply() }
}
