package com.noorpro.app

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.notify.NoorNotify
import com.noorpro.app.prayer.PrayerDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 1.0.97: the N-crescent logo is wired into every brand surface. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrandLogoTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun render(res: Int, w: Int = 96, h: Int = 96): Bitmap {
        val d = ContextCompat.getDrawable(ctx, res)!!
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, w, h); d.draw(Canvas(b)); return b
    }

    @Test fun brandDrawablesInflate() {
        listOf(
            R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground, R.drawable.ic_launcher_monochrome,
            R.drawable.ic_stat_noorpro, R.drawable.brand_n_tile, R.drawable.brand_n_glyph, R.drawable.brand_wordmark,
            R.drawable.widget_logo_tile_cream, R.drawable.widget_art_fallback, R.drawable.ic_splash_mark, R.drawable.splash_branding,
        ).forEach { assertNotNull(ContextCompat.getDrawable(ctx, it)); render(it) }
    }

    @Test fun smallIconIsTheNMark() {
        assertEquals(R.drawable.ic_stat_noorpro, NoorNotify.SMALL_ICON)
        assertEquals(0xFF0B6243.toInt(), NoorNotify.ACCENT)
    }

    @Test fun ongoingFallbackLargeIconIsBrandTile() {
        val b = PrayerDisplay.brandLargeIcon(ctx)
        assertNotNull(b); assertEquals(192, b!!.width)
    }

    @Test fun mediaFallbackArtStillBundled() {
        val b = NoorNotify.bitmap(ctx, R.drawable.noor_cover_audio)
        assertNotNull(b); assertTrue(b!!.width > 0)
    }
}
