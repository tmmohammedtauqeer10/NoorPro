package com.noorpro.app.utils

import com.example.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException

/**
 * Central sink for caught-but-non-fatal exceptions.
 *
 * Replaces scattered `e.printStackTrace()` calls: in debug it still prints the stack trace,
 * and in every build it records the throwable as a Crashlytics non-fatal so production
 * failures are actually visible. Coroutine cancellation is never reported (it is normal
 * structured-concurrency control flow, not an error).
 */
object CrashReporter {
    fun report(throwable: Throwable, message: String? = null) {
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace()
        }
        // Don't treat coroutine cancellation as a crash.
        if (throwable is CancellationException) return
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (message != null) crashlytics.log(message)
            crashlytics.recordException(throwable)
        } catch (_: Throwable) {
            // Crashlytics unavailable (e.g. Firebase not initialized) — never crash while reporting.
        }
    }
}
