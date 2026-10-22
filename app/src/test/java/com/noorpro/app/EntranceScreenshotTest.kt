package com.noorpro.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.ui.screens.AuthContent
import com.noorpro.app.ui.screens.BrandSplash
import com.noorpro.app.ui.screens.EntrancePermState
import com.noorpro.app.ui.screens.OnboardingPager
import com.noorpro.app.ui.theme.AppLanguage
import com.noorpro.app.ui.theme.MyApplicationTheme
import com.noorpro.app.ui.viewmodel.CalculationMethod
import com.noorpro.app.ui.viewmodel.Madhab
import com.noorpro.app.ui.viewmodel.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Entrance previews -> build/entrance/entrance_<screen>_<light|dark>.png */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class EntranceScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shots(name: String, content: @Composable () -> Unit) {
        val dark = mutableStateOf(false)
        rule.setContent { MyApplicationTheme(darkTheme = dark.value) { content() } }
        for (d in listOf(false, true)) {
            dark.value = d; rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
            rule.onRoot().captureRoboImage(filePath = "build/entrance/entrance_${name}_${if (d) "dark" else "light"}.png")
        }
    }

    private fun page(i: Int) = shots(listOf("onboarding_welcome", "onboarding_features", "onboarding_personalise", "onboarding_permissions")[i]) {
        OnboardingPager(
            ThemeMode.SYSTEM, {}, CalculationMethod.KARACHI, {}, Madhab.HANAFI, {}, AppLanguage.EN, {},
            EntrancePermState(location = true, notifications = false, exactAlarm = false, exactNeeded = true),
            {}, {}, {}, {}, initialPage = i,
        )
    }

    @Test fun splash() = shots("splash") { BrandSplash() }
    @Test fun welcome() = page(0)
    @Test fun features() = page(1)
    @Test fun personalise() = page(2)
    @Test fun permissions() = page(3)

    private fun auth(name: String, reg: Boolean, err: String?) = shots(name) {
        AuthContent(
            isRegistering = reg, name = if (reg) "Aisha Khan" else "", email = "aisha@example.com", password = "secret1",
            passwordVisible = false, acceptedTerms = false, isLoading = false, errorMessage = err, googleErrorMessage = null,
            onName = {}, onEmail = {}, onPassword = {}, onTogglePassword = {}, onAcceptTerms = {}, onSubmit = {}, onForgot = {},
            onGoogle = {}, onToggleMode = {}, onGuest = {}, onTerms = {}, onPrivacy = {},
        )
    }
    @Test fun login() = auth("login", false, null)
    @Test fun signup() = auth("signup", true, "Please accept the Terms of Service to create an account.")
}
