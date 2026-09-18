package com.noorpro.app.data

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object AuthEmailService {
    private const val PASSWORD_RESET_URL =
        "https://us-central1-noor-pro-d87e3.cloudfunctions.net/sendPasswordResetEmail"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = """{"email":"${escapeJson(email.trim())}"}"""
                .toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder()
                .url(PASSWORD_RESET_URL)
                .post(body)
                .header("Content-Type", "application/json")

            // App Check proves the call comes from this app (user may be logged out).
            runCatching {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { token ->
                requestBuilder.header("X-Firebase-AppCheck", token)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val detail = response.body?.string().orEmpty()
                    throw IllegalStateException(detail.ifBlank { "Password reset email failed." })
                }
            }
        }
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}

/** Shared helper for Cloud Function calls that need App Check (+ optional Auth). */
internal object FirebaseCallableHeaders {
    suspend fun appCheckToken(): String? = runCatching {
        FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
    }.getOrNull()?.takeIf { it.isNotBlank() }

    suspend fun idToken(): String? = runCatching {
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
