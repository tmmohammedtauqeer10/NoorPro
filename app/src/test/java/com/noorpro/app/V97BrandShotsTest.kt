package com.noorpro.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.ui.screens.NoorBrandMark
import com.noorpro.app.ui.screens.NoorWordmark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 1.0.97 brand previews -> build/v97/ (real resource renders: widgets, launcher layers, logo composables). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class V97BrandShotsTest {
    @get:Rule val rule = createComposeRule()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun save(b: Bitmap, name: String) {
        File("build/v97").mkdirs(); File("build/v97/$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun drawable(res: Int, name: String, w: Int, h: Int = w) {
        val d = ContextCompat.getDrawable(ctx, res)!!
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); d.setBounds(0, 0, w, h); d.draw(Canvas(b)); save(b, name)
    }
    private fun layout(res: Int, name: String, wDp: Int, hDp: Int) {
        val dm = ctx.resources.displayMetrics.density
        val v = LayoutInflater.from(ctx).inflate(res, null)
        val w = (wDp * dm).toInt(); val h = (hDp * dm).toInt()
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); v.draw(Canvas(b)); save(b, name)
    }

    @Test fun resources() {
        drawable(R.mipmap.ic_launcher, "adaptive_icon", 432)
        drawable(R.drawable.ic_launcher_background, "layer_bg", 432)
        drawable(R.drawable.ic_launcher_foreground, "layer_fg", 432)
        drawable(R.drawable.ic_launcher_monochrome, "layer_mono", 432)
        drawable(R.drawable.ic_stat_noorpro, "stat_96", 96)
        drawable(R.drawable.ic_splash_mark, "splash_icon", 432)
        drawable(R.drawable.splash_branding, "splash_branding", 500, 200)
        drawable(R.drawable.brand_n_tile, "brand_tile", 288)
        layout(R.layout.widget_preview_next, "widget_next", 170, 170)
        layout(R.layout.widget_preview_day, "widget_day", 340, 170)
    }

    @Test fun logoComposables() {
        rule.setContent {
            Box(Modifier.background(Color(0xFFF7F3EA)).padding(24.dp)) {
                Column {
                    NoorBrandMark(76.dp)
                    Spacer(Modifier.height(16.dp))
                    NoorWordmark(30.dp)
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.background(Color(0xFF0B6243)).padding(16.dp)) { NoorBrandMark(64.dp, onGreen = true) }
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage(filePath = "build/v97/logo_composables.png")
    }
}
