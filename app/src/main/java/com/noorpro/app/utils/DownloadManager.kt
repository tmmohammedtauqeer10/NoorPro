package com.noorpro.app.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import android.os.Environment

object DownloadManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun downloadPdf(
        context: Context,
        bookId: String,
        pdfUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (directory != null && !directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "$bookId.pdf")
        if (file.exists() && file.length() > 0) {
            val isPdf = try {
                val bytes = ByteArray(4)
                java.io.FileInputStream(file).use { it.read(bytes) }
                String(bytes) == "%PDF"
            } catch (e: Exception) { false }
            
            if (isPdf) {
                onProgress(100f)
                return@withContext file
            } else {
                file.delete()
            }
        }

        try {
            val request = Request.Builder()
                .url(pdfUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val fileLength = body.contentLength()
            val input = body.byteStream()
            val output = FileOutputStream(file)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            var lastProgress = 0f
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    val progress = (total.toFloat() / fileLength.toFloat()) * 100f
                    if (progress - lastProgress >= 1f || progress == 100f) {
                        lastProgress = progress
                        onProgress(progress)
                    }
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()
            response.close()

            val isPdf = try {
                val bytes = ByteArray(4)
                java.io.FileInputStream(file).use { it.read(bytes) }
                String(bytes) == "%PDF"
            } catch (e: Exception) { false }
            
            if (!isPdf) {
                file.delete()
                return@withContext null
            }

            file
        } catch (e: Exception) {
            CrashReporter.report(e)
            if (file.exists()) {
                file.delete()
            }
            null
        }
    }
}
