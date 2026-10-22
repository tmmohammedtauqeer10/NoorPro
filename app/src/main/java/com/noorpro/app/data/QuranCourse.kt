package com.noorpro.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.GZIPInputStream

/** Structured Quran reading course (Qaida → Tajweed → Intermediate → Practice → Advanced), bundled in assets/content/quran_course.jgz. */
data class CourseLetter(val ar: String, val name: String, val sound: String, val group: String)
data class CourseExample(val ar: String, val label: String, val ref: String?, val surah: Int?, val ayah: Int?, val rule: String?)
data class CourseQuiz(val question: String, val options: List<String>, val answer: Int, val ar: String?)
data class CourseLesson(
    val id: String, val title: String, val goal: String, val en: String, val ur: String,
    val points: List<String>, val examples: List<CourseExample>, val quiz: List<CourseQuiz>,
    val kind: String, val letters: List<CourseLetter>, val surahs: List<Int>,
)
data class CourseLevel(val id: String, val title: String, val subtitle: String, val lessons: List<CourseLesson>)
data class QuranCourse(val title: String, val sources: List<String>, val levels: List<CourseLevel>) {
    val allLessons: List<CourseLesson> get() = levels.flatMap { it.lessons }
}
data class CourseAyah(val surah: Int, val ayah: Int, val arabic: String, val english: String)

object QuranCourseRepository {
    @Volatile private var cached: QuranCourse? = null
    @Volatile private var ayahs: Map<Int, List<CourseAyah>>? = null
    @Volatile private var surahNames: List<String> = emptyList()

    private fun gz(ctx: Context, path: String) = GZIPInputStream(ctx.assets.open(path)).bufferedReader().use { it.readText() }
    private fun JSONArray?.strings() = if (this == null) emptyList() else (0 until length()).map { getString(it) }

    fun parse(json: String): QuranCourse {
        val root = JSONObject(json)
        val levels = root.getJSONArray("levels").let { arr ->
            (0 until arr.length()).map { i ->
                val lv = arr.getJSONObject(i)
                val ls = lv.getJSONArray("lessons")
                CourseLevel(lv.getString("id"), lv.getString("title"), lv.optString("subtitle"), (0 until ls.length()).map { j ->
                    val l = ls.getJSONObject(j)
                    val ex = l.optJSONArray("examples") ?: JSONArray()
                    val qz = l.optJSONArray("quiz") ?: JSONArray()
                    val lt = l.optJSONArray("letters") ?: JSONArray()
                    val su = l.optJSONArray("surahs") ?: JSONArray()
                    CourseLesson(
                        l.getString("id"), l.getString("title"), l.optString("goal"), l.optString("en"), l.optString("ur"),
                        l.optJSONArray("points").strings(),
                        (0 until ex.length()).map { k -> ex.getJSONObject(k).let { e ->
                            CourseExample(e.getString("ar"), e.optString("label"), e.optString("ref").ifBlank { null },
                                if (e.has("s")) e.getInt("s") else null, if (e.has("a")) e.getInt("a") else null, e.optString("rule").ifBlank { null })
                        } },
                        (0 until qz.length()).map { k -> qz.getJSONObject(k).let { q ->
                            CourseQuiz(q.getString("q"), q.getJSONArray("opts").strings(), q.getInt("a"), q.optString("ar").ifBlank { null })
                        } },
                        l.optString("kind", "lesson"),
                        (0 until lt.length()).map { k -> lt.getJSONObject(k).let { CourseLetter(it.getString("ar"), it.getString("name"), it.getString("sound"), it.getString("group")) } },
                        (0 until su.length()).map { su.getInt(it) },
                    )
                })
            }
        }
        return QuranCourse(root.optString("title"), root.optJSONArray("sources").strings(), levels)
    }

    fun load(ctx: Context): QuranCourse = cached ?: parse(gz(ctx, "content/quran_course.jgz")).also { cached = it }

    private val diac = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640\\uFEFF\\u0621]")
    private fun skel(s: String) = diac.replace(s, "").replace('ٱ', 'ا').replace('أ', 'ا').replace('إ', 'ا').replace(" ", "")

    /** Ayahs of one surah from the bundled quran_core (basmala prefix removed from ayah 1, except Al-Fatiha). */
    fun surahAyahs(ctx: Context, surah: Int): List<CourseAyah> {
        val all = ayahs ?: run {
            val root = JSONObject(gz(ctx, "quran/quran_core.jgz"))
            val s = root.getJSONArray("s")
            surahNames = (0 until s.length()).map { s.getJSONArray(it).getString(0) }
            val a = root.getJSONArray("a")
            val map = HashMap<Int, MutableList<CourseAyah>>()
            var basmala = ""
            for (i in 0 until a.length()) {
                val r = a.getJSONArray(i)
                val su = r.getInt(0); val ay = r.getInt(1)
                var text = r.getString(4).trimStart('\uFEFF')
                if (su == 1 && ay == 1) basmala = skel(text)
                if (ay == 1 && su != 1) {
                    val w = text.split(' ')
                    if (w.size > 4 && skel(w.take(4).joinToString(" ")) == basmala) text = w.drop(4).joinToString(" ")
                }
                map.getOrPut(su) { mutableListOf() }.add(CourseAyah(su, ay, text, r.optString(6)))
            }
            map.also { ayahs = it }
        }
        return all[surah].orEmpty()
    }

    fun surahName(ctx: Context, surah: Int): String { if (surahNames.isEmpty()) surahAyahs(ctx, 1); return surahNames.getOrElse(surah - 1) { "Surah $surah" } }

    fun husaryUrl(surah: Int, ayah: Int) = "https://everyayah.com/data/Husary_128kbps/%03d%03d.mp3".format(surah, ayah)
}

/** Local progress: completed lessons, best quiz score, last opened lesson. */
class QuranCourseProgress(ctx: Context) {
    private val p = ctx.getSharedPreferences("quran_course_progress", Context.MODE_PRIVATE)
    fun completed(): Set<String> = p.getStringSet("done", emptySet()).orEmpty()
    fun setCompleted(id: String, done: Boolean) {
        val s = completed().toMutableSet(); if (done) s.add(id) else s.remove(id)
        p.edit().putStringSet("done", s).apply()
    }
    fun bestScore(id: String): Int = p.getInt("score_$id", -1)
    fun saveScore(id: String, score: Int) { if (score > bestScore(id)) p.edit().putInt("score_$id", score).apply() }
    var lastLesson: String?
        get() = p.getString("last", null)
        set(v) { p.edit().putString("last", v).apply() }
}
