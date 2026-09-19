package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AWS reel pipeline: upload reel/short videos straight to S3 (via a short-lived presigned URL
 * minted by the `getReelUploadUrl` Cloud Function) and play them back through CloudFront so reels
 * load fast and cheap worldwide. AWS credentials stay server-side; the app never sees them.
 * The video is streamed to S3 (Phase 2) so memory stays flat regardless of file size.
 *
 * Flip [ENABLED] to true only AFTER the S3 bucket + CloudFront distribution exist and the function
 * has its AWS secrets/config set. Until then (or on any failure) callers fall back to Firebase
 * Storage automatically, so reels keep working.
 */
object ReelCdnUploader {
    // Turn on once AWS S3 + CloudFront + function secrets are configured.
    const val ENABLED = true

    private const val FUNCTION_URL =
        "https://us-central1-noor-pro-d87e3.cloudfunctions.net/getReelUploadUrl"
    private const val TRANSCODE_URL =
        "https://us-central1-noor-pro-d87e3.cloudfunctions.net/startReelTranscode"
    private const val PUBLISH_URL =
        "https://us-central1-noor-pro-d87e3.cloudfunctions.net/publishUmmahSubmissionNow"

    /** Result of a CDN upload: the immediately-playable MP4 URL and (if transcoding kicked off) the
     *  adaptive HLS URL the player prefers. */
    data class CdnUploadResult(val mp4Url: String, val hlsUrl: String)

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Attempts a CDN upload. Calls [onResult] with the CloudFront playback URL on success, or null
     * on any failure (so the caller can fall back to Firebase Storage). Runs network I/O off the
     * main thread.
     */
    fun upload(
        context: Context,
        uri: Uri,
        contentType: String,
        videoMetadata: ReelVideoMetadata,
        onResult: (CdnUploadResult?) -> Unit
    ) {
        if (!ENABLED) {
            onResult(null)
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(null)
            return
        }
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = task.result?.token
            if (!task.isSuccessful || token.isNullOrBlank()) {
                onResult(null)
                return@addOnCompleteListener
            }
            Thread {
                val result = runCatching {
                    doUpload(context, uri, contentType, videoMetadata, token)
                }.getOrNull()
                onResult(result)
            }.start()
        }
    }

    /** Ask the trusted backend to promote an authenticated submission. The Firestore create
     * trigger normally does this automatically; this endpoint also recovers reels uploaded before
     * that trigger was deployed. */
    fun requestPublication(submissionId: String, onResult: (Boolean) -> Unit = {}) {
        val cleanId = submissionId.trim()
        val user = FirebaseAuth.getInstance().currentUser
        if (cleanId.isBlank() || user == null) {
            onResult(false)
            return
        }
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = task.result?.token
            if (!task.isSuccessful || token.isNullOrBlank()) {
                onResult(false)
                return@addOnCompleteListener
            }
            Thread {
                val ok = runCatching {
                    val payload = JSONObject().put("submissionId", cleanId).toString()
                    val request = Request.Builder()
                        .url(PUBLISH_URL)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Content-Type", "application/json")
                        .post(payload.toRequestBody(jsonMediaType))
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use false
                        val result = JSONObject(response.body?.string().orEmpty())
                        result.optBoolean("published") || result.optString("status") == "published"
                    }
                }.getOrDefault(false)
                onResult(ok)
            }.start()
        }
    }

    private fun doUpload(
        context: Context,
        uri: Uri,
        contentType: String,
        videoMetadata: ReelVideoMetadata,
        token: String
    ): CdnUploadResult? {
        val type = contentType.ifBlank { "video/mp4" }

        // Require a known size for the presigned S3 request. The backend signs this exact length
        // and rejects files at or above the same 100 MB limit used by Firebase Storage.
        val size = resolveSize(context, uri)
        if (size <= 0L) return null

        // 1. Ask the Cloud Function for a presigned PUT URL + the final CloudFront URL.
        val payload = JSONObject()
            .put("contentType", type)
            .put("contentLength", size)
            .toString()
        val urlRequest = Request.Builder()
            .url(FUNCTION_URL)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        var uploadUrl = ""
        var playbackUrl = ""
        var key = ""
        client.newCall(urlRequest).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            val obj = JSONObject(body)
            uploadUrl = obj.optString("uploadUrl")
            playbackUrl = obj.optString("playbackUrl")
            key = obj.optString("key")
        }
        if (uploadUrl.isBlank() || playbackUrl.isBlank()) return null

        // 2. PUT the video to S3 using the presigned URL. Stream it straight from the content Uri
        // so memory stays flat no matter how large the video is. S3 presigned PUT needs a known
        // Content-Length, so we resolve the size first; if it's unknown we fall back to buffering.
        val mediaType = type.toMediaType()
        val body: RequestBody = object : RequestBody() {
            override fun contentType() = mediaType
            override fun contentLength() = size
            override fun writeTo(sink: BufferedSink) {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Cannot open video stream")
                input.use { sink.writeAll(it.source()) }
            }
        }

        val putRequest = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .build()
        client.newCall(putRequest).execute().use { putResponse ->
            if (!putResponse.isSuccessful) return null
        }

        // Confirm CloudFront can read the object before publishing its URL. If the distribution,
        // origin access policy, or bucket link is broken, return null and use Firebase Storage.
        if (!isPlaybackReachable(playbackUrl)) return null

        // 3. Kick off HLS transcoding (Phase 3). If it's configured, the function returns the HLS
        // playback URL the player will prefer; otherwise we just keep the MP4.
        val hlsUrl = if (key.isNotBlank()) requestTranscode(key, videoMetadata, token) else ""
        return CdnUploadResult(playbackUrl, hlsUrl)
    }

    private fun isPlaybackReachable(playbackUrl: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(playbackUrl)
                .addHeader("Range", "bytes=0-0")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                response.code == 200 || response.code == 206
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Asks the backend to start an HLS transcode of [key]. Returns the HLS URL, or "" if HLS isn't
     *  configured or the request fails (the app then just plays the MP4). */
    private fun requestTranscode(key: String, videoMetadata: ReelVideoMetadata, token: String): String {
        return try {
            val payload = JSONObject()
                .put("key", key)
                .put("mediaWidth", videoMetadata.width)
                .put("mediaHeight", videoMetadata.height)
                .put("mediaRotationDegrees", videoMetadata.rotationDegrees)
                .put("displayAspectRatio", videoMetadata.displayAspectRatio.toDouble())
                .toString()
            val request = Request.Builder()
                .url(TRANSCODE_URL)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return ""
                JSONObject(response.body?.string().orEmpty()).optString("hlsUrl")
            }
        } catch (_: Exception) {
            ""
        }
    }

    /** Best-effort byte size of a content Uri, or -1 if it can't be determined. */
    private fun resolveSize(context: Context, uri: Uri): Long {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) {
                    val size = cursor.getLong(idx)
                    if (size > 0) return size
                }
            }
        }
        runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                val length = afd.length
                if (length > 0) return length
            }
        }
        return -1L
    }
}
