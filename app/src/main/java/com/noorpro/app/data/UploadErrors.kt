package com.noorpro.app.data

/** Turns raw Firebase/OkHttp upload failures into messages a member can act on. */
object UploadErrors {
    fun httpCodeOf(error: Throwable?): Int? {
        var e = error
        while (e != null) {
            val se = e as? com.google.firebase.storage.StorageException
            if (se != null && se.httpResultCode > 0) return se.httpResultCode
            Regex("""\b(40[0-9]|41[0-9]|429|50[0-9])\b""").find(e.message.orEmpty())?.let { return it.value.toInt() }
            e = e.cause
        }
        return null
    }

    fun isRetryable(error: Throwable?): Boolean {
        val code = httpCodeOf(error) ?: return true
        return code == 408 || code == 429 || code >= 500
    }

    fun friendly(error: Throwable?, isVideo: Boolean): String {
        val what = if (isVideo) "reel" else "photo"
        val se = error as? com.google.firebase.storage.StorageException
        return when {
            se?.errorCode == com.google.firebase.storage.StorageException.ERROR_NOT_AUTHENTICATED ->
                "Please sign in again to upload your $what."
            se?.errorCode == com.google.firebase.storage.StorageException.ERROR_QUOTA_EXCEEDED ->
                "Uploads are temporarily unavailable. Please try again later."
            se?.errorCode == com.google.firebase.storage.StorageException.ERROR_RETRY_LIMIT_EXCEEDED ->
                "Your connection dropped while uploading. Check your internet and tap Retry."
            httpCodeOf(error) == 402 || httpCodeOf(error) == 403 ->
                "Uploads are temporarily unavailable on our side. Your $what was not posted — please try again later."
            httpCodeOf(error) == 413 -> "This $what is too large. Choose one smaller than 100 MB."
            error is java.io.IOException -> "Network problem while uploading. Check your internet and tap Retry."
            else -> "Couldn't upload your $what. Please tap Retry."
        }
    }
}
