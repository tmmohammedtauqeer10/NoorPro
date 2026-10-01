package com.noorpro.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.noorpro.app.BuildConfig
import com.noorpro.app.NoorAppCheckProviderFactory
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.noorpro.app.audio.session.AlNoorPlaybackChannels
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import com.noorpro.app.prayer.ongoing.NextPrayerOngoingScheduler

/**
 * App process entry under `com.noorpro.app` (same as Gradle `namespace` /
 * `applicationId`). Hand-written `BuildConfig` and `NoorAppCheckProviderFactory`
 * share this package for debug/release source sets.
 */
class NoorProApplication : Application(), ImageLoaderFactory {

    // App-wide Coil loader that can decode a video's first frame, so reels/post videos show their
    // real visual (like Instagram) instead of a black box when they have no separate thumbnail.
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        // NOTE: the Al Noor player/service is created lazily when audio is first used. Building an
        // ExoPlayer and calling startService() here ran on EVERY process start - including the
        // process the system spawns for an adhan alarm or BOOT_COMPLETED - and could throw
        // (background service start restrictions) before the receiver ever posted its notification.
        runCatching { AlNoorPlaybackChannels.ensure(this) }
        runCatching { PrayerNotificationChannels.ensureAll(this) }
        // Never let a scheduling failure (e.g. WorkManager unavailable) crash app startup.
        runCatching { NextPrayerOngoingScheduler.schedule(this) }
        // Always-on safety net: re-arms adhan alarms and refreshes widgets even if the ongoing
        // notification is turned off, and re-plans immediately on every app start.
        runCatching { com.noorpro.app.prayer.PrayerRefreshWorker.schedule(this) }
        runCatching { com.noorpro.app.prayer.PrayerScheduler.rescheduleAsync(this) }

        // AdMob is initialised by ConsentManager only after Google UMP consent has been gathered
        // (or is not required); MainActivity starts that flow. See ads/ConsentManager.kt.

        FirebaseApp.initializeApp(this) ?: return

        // Only ship crash data from real (release) installs; keep dev noise out of the dashboard.
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(NoorAppCheckProviderFactory.get())
    }
}
