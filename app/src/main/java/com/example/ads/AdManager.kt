package com.example.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Text
import com.example.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Debug builds use Google test banners. Only compact in-feed ads are displayed. */
object AdIds {
    private const val GOOGLE_TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"

    private fun resolve(configured: String): String {
        if (BuildConfig.DEBUG) return GOOGLE_TEST_BANNER
        val trimmed = configured.trim()
        return if (trimmed.isBlank() || trimmed.contains("3940256099942544") || trimmed.contains("XXXX")) {
            GOOGLE_TEST_BANNER
        } else {
            trimmed
        }
    }

    val FEED_BANNER: String
        get() = resolve(BuildConfig.ADMOB_FEED_BANNER)

    val BOTTOM_BANNER: String
        get() = resolve(BuildConfig.ADMOB_BOTTOM_BANNER)
}

@Composable
fun SponsoredFeedCard(modifier: Modifier = Modifier) {
    SponsoredCard(adUnitId = AdIds.FEED_BANNER, modifier = modifier)
}

/** Home placement uses its own unit so AdMob reporting can compare Home and Ummah performance. */
@Composable
fun SponsoredHomeCard(modifier: Modifier = Modifier) {
    SponsoredCard(adUnitId = AdIds.BOTTOM_BANNER, modifier = modifier)
}

@Composable
private fun SponsoredCard(adUnitId: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            "Sponsored",
            color = Color(0xFF8DA19A),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
        )
        SponsoredAdMedia(adUnitId = adUnitId)
    }
}

@Composable
fun SponsoredAdMedia(
    modifier: Modifier = Modifier,
    adUnitId: String = AdIds.FEED_BANNER
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (maxWidth >= 300.dp) {
            val context = LocalContext.current
            // Anchored adaptive banners use the available card width and choose a safe height for
            // this device/orientation, avoiding clipped or oversized ads on small phones/tablets.
            val availableWidth = maxWidth.value.toInt().coerceAtLeast(300)
            val adaptiveSize = remember(context, availableWidth) {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, availableWidth)
            }
            val adView = remember(context, adUnitId, adaptiveSize) {
                AdView(context).apply {
                    setAdSize(adaptiveSize)
                    this.adUnitId = adUnitId
                }
            }
            DisposableEffect(adView) {
                adView.loadAd(AdRequest.Builder().build())
                onDispose { adView.destroy() }
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(adaptiveSize.height.dp),
                factory = { adView }
            )
        }
    }
}
