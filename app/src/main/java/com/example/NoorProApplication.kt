package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import com.noorpro.app.prayer.ongoing.NextPrayerOngoingScheduler

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

        AlNoorAudioSession.init(this)
        PrayerNotificationChannels.ensureAll(this)
        NextPrayerOngoingScheduler.schedule(this)

        // Initialize AdMob first so it runs even if Firebase fails to init. MAX_AD_CONTENT_RATING_G
        // keeps ads family-friendly (filters most gambling/alcohol/dating) — finer category blocking
        // is configured in the AdMob console (Blocking controls → Sensitive categories).
        MobileAds.initialize(this)
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build()
        )

        FirebaseApp.initializeApp(this) ?: return

        // Only ship crash data from real (release) installs; keep dev noise out of the dashboard.
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(NoorAppCheckProviderFactory.get())
    }
}
