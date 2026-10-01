package com.noorpro.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Before
import androidx.compose.ui.test.onRoot
import com.noorpro.app.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun initWorkManager() {
    WorkManagerTestInitHelper.initializeTestWorkManager(
      ApplicationProvider.getApplicationContext(),
      Configuration.Builder().build()
    )
  }

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { MyApplicationTheme { MainLayout() } }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
