package com.noorpro.app.data.spiritual

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.GZIPInputStream

/** Full texts of the classic Tasawwuf books, bundled as gzip JSON (one asset per book edition). */
data class BookEdition(val lang: String, val file: String, val sections: Int, val completeness: String)
data class LibraryBook(val id: String, val title: String, val editions: List<BookEdition>, val author: String = "")
data class BookSection(val title: String, val level: Int, val paras: List<String>)
data class BookText(val id: String, val lang: String, val title: String, val rtl: Boolean, val sections: List<BookSection>, val author: String = "")

object BookLibrary {
    private const val DIR = "content/books"
    @Volatile private var index: List<LibraryBook>? = null
    @Volatile private var cached: BookText? = null

    private fun read(context: Context, name: String): String =
        GZIPInputStream(context.assets.open("$DIR/$name"), 64 * 1024).bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Synchronized
    fun index(context: Context): List<LibraryBook> {
        index?.let { return it }
        val arr = JSONArray(read(context, "index.jgz"))
        val list = (0 until arr.length()).map { i ->
            val b = arr.getJSONObject(i)
            val e = b.getJSONArray("editions")
            LibraryBook(b.getString("id"), b.getString("title"), (0 until e.length()).map { j ->
                val x = e.getJSONObject(j)
                BookEdition(x.getString("lang"), x.getString("file"), x.optInt("sections"), x.optString("completeness"))
            }, b.optString("author"))
        }
        index = list
        return list
    }

    fun find(context: Context, bookId: String): LibraryBook? = try { index(context).firstOrNull { it.id == bookId } } catch (_: Exception) { null }

    /** Only one book is kept in memory at a time (the Ihya alone is ~8 MB of text). */
    @Synchronized
    fun load(context: Context, bookId: String, lang: String): BookText {
        cached?.let { if (it.id == bookId && it.lang == lang) return it }
        cached = null
        val o = JSONObject(read(context, "$bookId.$lang.jgz"))
        val secs = o.getJSONArray("sections")
        val text = BookText(o.getString("id"), o.getString("lang"), o.optString("title"), o.optBoolean("rtl"),
            (0 until secs.length()).map { i ->
                val s = secs.getJSONObject(i)
                val p = s.getJSONArray("p")
                BookSection(s.optString("t"), s.optInt("l", 1), (0 until p.length()).map { p.getString(it) })
            }, o.optString("author"))
        cached = text
        return text
    }
}

data class BookMark(val section: Int, val para: Int, val label: String)

/** Last-read position and bookmarks, per book edition, in SharedPreferences. */
object BookPrefs {
    private const val FILE = "book_reader_prefs"
    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun lastRead(c: Context, key: String): Pair<Int, Int>? =
        prefs(c).getString("last.$key", null)?.split(':')?.takeIf { it.size == 2 }?.let { (a, b) -> (a.toIntOrNull() ?: 0) to (b.toIntOrNull() ?: 0) }

    fun setLastRead(c: Context, key: String, section: Int, para: Int) {
        prefs(c).edit().putString("last.$key", "$section:$para").apply()
    }

    fun fontSize(c: Context): Float = prefs(c).getFloat("font", 18f)
    fun setFontSize(c: Context, v: Float) { prefs(c).edit().putFloat("font", v).apply() }

    fun bookmarks(c: Context, key: String): List<BookMark> =
        prefs(c).getStringSet("bm.$key", emptySet()).orEmpty().mapNotNull { s ->
            val parts = s.split('|', limit = 3)
            if (parts.size < 3) null else BookMark(parts[0].toIntOrNull() ?: return@mapNotNull null, parts[1].toIntOrNull() ?: 0, parts[2])
        }.sortedWith(compareBy({ it.section }, { it.para }))

    fun toggleBookmark(c: Context, key: String, section: Int, para: Int, label: String): Boolean {
        val set = prefs(c).getStringSet("bm.$key", emptySet()).orEmpty().toMutableSet()
        val existing = set.firstOrNull { it.startsWith("$section|$para|") }
        val added = if (existing != null) { set.remove(existing); false } else { set.add("$section|$para|${label.take(80)}"); true }
        prefs(c).edit().putStringSet("bm.$key", set).apply()
        return added
    }
}
