package com.noorpro.app.learning

import android.content.Context
import java.security.MessageDigest

/** Account-scoped local study log. No provider enrollment, exam verification or cloud sync. */
class LearningProgressStore(context: Context, accountId: String) {
    private val scope = MessageDigest.getInstance("SHA-256").digest(accountId.toByteArray()).joinToString("") { "%02x".format(it) }
    private val prefs = context.getSharedPreferences("academy_$scope", Context.MODE_PRIVATE)

    fun enrolled(): Set<String> = prefs.getStringSet("enrolled", emptySet())!!.toSet()
    fun completed(): Set<String> = prefs.getStringSet("completed", emptySet())!!.toSet()
    fun note(lessonId: String): String = prefs.getString("note_$lessonId", "").orEmpty()
    fun enroll(courseId: String): Boolean = prefs.edit().putStringSet("enrolled", enrolled() + courseId).commit()
    fun setCompleted(lessonId: String, done: Boolean): Boolean {
        val ids = completed().toMutableSet()
        if (done) ids.add(lessonId) else ids.remove(lessonId)
        return prefs.edit().putStringSet("completed", ids).commit()
    }
    fun saveNote(lessonId: String, text: String): Boolean = prefs.edit().putString("note_$lessonId", text.take(2000)).commit()
    fun reset(course: AcademyCourse): Boolean {
        val editor = prefs.edit().putStringSet("completed", completed() - course.lessons.map { it.id }.toSet())
        course.lessons.forEach { editor.remove("note_${it.id}") }
        return editor.commit()
    }
}
