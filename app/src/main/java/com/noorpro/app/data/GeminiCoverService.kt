package com.noorpro.app.data

import android.content.Context
object GeminiCoverService {
    suspend fun getCoverImage(context: Context, book: BookItem): String {
        return book.coverUrl
    }
}
