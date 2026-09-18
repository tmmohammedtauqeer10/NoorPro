package com.noorpro.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DrivePdfRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Example JSON: [{"id":"1","title":"Book 1","author":"Auth","pdf_url":"https://...."}]
    suspend fun fetchBooksFromGithub(jsonUrl: String): List<DriveBook> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(jsonUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch books")
            val jsonString = response.body?.string() ?: "[]"
            
            val books = mutableListOf<DriveBook>()
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.optJSONObject(i)
                if (jsonObj != null) {
                    books.add(
                        DriveBook(
                            id = jsonObj.optString("id", i.toString()),
                            title = jsonObj.optString("title", "Unknown Title"),
                            author = jsonObj.optString("author", "Unknown Author"),
                            pdf_url = jsonObj.optString("pdf_url", "")
                        )
                    )
                }
            }
            return@withContext books
        }
    }

    suspend fun downloadPdf(url: String, fileName: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "$fileName.pdf")
        if (file.exists() && isPdfFile(file)) {
            return@withContext file
        } else if (file.exists()) {
            file.delete()
        }

        val request = Request.Builder().url(toDirectDownloadUrl(url)).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("Failed to download PDF")

            val contentType = response.header("Content-Type").orEmpty()
            if (contentType.contains("text/html", ignoreCase = true)) {
                throw java.io.IOException("This Drive PDF is not publicly available")
            }

            response.body?.let { body ->
                FileOutputStream(file).use { output ->
                    body.byteStream().copyTo(output)
                }
            }
        }
        if (!isPdfFile(file)) {
            file.delete()
            throw java.io.IOException("The selected Drive file is not a public PDF")
        }
        return@withContext file
    }

    private fun toDirectDownloadUrl(url: String): String {
        if (!url.contains("drive.google.com")) return url

        val id = when {
            "/file/d/" in url -> url.substringAfter("/file/d/").substringBefore("/")
            "id=" in url -> url.substringAfter("id=").substringBefore("&")
            else -> ""
        }
        return if (id.isBlank()) url
        else "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t"
    }

    private fun isPdfFile(file: File): Boolean {
        if (!file.exists() || file.length() < 5) return false
        return file.inputStream().buffered().use { input ->
            val signature = ByteArray(5)
            input.read(signature) == signature.size &&
                signature.toString(Charsets.US_ASCII) == "%PDF-"
        }
    }
}
