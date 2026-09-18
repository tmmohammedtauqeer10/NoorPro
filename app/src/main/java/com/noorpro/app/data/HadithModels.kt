package com.noorpro.app.data

data class HadithBook(
    val id: String,
    val name: String,
    val hasChapters: Boolean = true
)

data class HadithChapter(
    val bookId: String,
    val chapterId: Int,
    val chapterName: String
)

data class HadithItem(
    val hadithNo: Int,
    val arabicText: String,
    val translationText: String,
    val chapterId: Int
)

data class Hadith(
    val id: Int,
    val text: String,
    val chapterNumber: Int,
    val chapterTitle: String,
    val bookCode: String
)
