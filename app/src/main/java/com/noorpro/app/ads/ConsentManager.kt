package com.noorpro.app.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google User Messaging Platform (UMP) consent flow. Ads are only requested after consent has been
 * gathered (or is not required). The message itself (GDPR / US states) is configured and published in
 * the AdMob console under Privacy & messaging.
 */
object ConsentManager {
    private val adsInitialized = AtomicBoolean(false)
    private val gathering = AtomicBoolean(false)

    /** Compose-observable: true once ads may be requested. */
    val canRequestAds = mutableStateOf(false)

    /** Compose-observable: true when the user must be offered a "privacy options" entry point. */
    val privacyOptionsRequired = mutableStateOf(false)

    /** Call from the launching Activity every time it starts; safe to call repeatedly. */
    fun gatherConsent(activity: Activity) {
        runCatching {
            val info = UserMessagingPlatform.getConsentInformation(activity)
            // Consent from an earlier session may already allow ads: initialise right away.
            refresh(activity, info)
            if (!gathering.compareAndSet(false, true)) return
            val params = ConsentRequestParameters.Builder().build()
            info.requestConsentInfoUpdate(
                activity, params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        gathering.set(false)
                        refresh(activity, info)
                    }
                },
                {
                    gathering.set(false)
                    refresh(activity, info)
                }
            )
        }.onFailure { gathering.set(false) }
    }

    private fun refresh(context: Context, info: ConsentInformation) {
        privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds()) {
            initializeAds(context)
            canRequestAds.value = true
        }
    }

    private fun initializeAds(context: Context) {
        if (!adsInitialized.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build()
        )
        MobileAds.initialize(context.applicationContext)
    }

    /** Opens Google's privacy-options form (revoke / change ad consent). */
    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        runCatching {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) {
                refresh(activity, UserMessagingPlatform.getConsentInformation(activity))
                onDone()
            }
        }.onFailure { onDone() }
    }
}
