package com.noorpro.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.data.QuranCourseRepository
import com.noorpro.app.ui.screens.CourseHome
import com.noorpro.app.ui.screens.CourseLessonView
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class QuranCourseScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun courseParsesAndAyahsLoad() {
        val c = QuranCourseRepository.load(ctx)
        assertEquals(5, c.levels.size)
        assertEquals(42, c.allLessons.size)
        assertEquals(29, c.allLessons.first().letters.size)
        assertEquals(5, QuranCourseRepository.surahAyahs(ctx, 108).size + 2) // Al-Kawthar has 3 ayat
        assertTrue(QuranCourseRepository.surahAyahs(ctx, 112).first().arabic.startsWith("قُلْ"))
    }

    private fun lesson(id: String, file: String) {
        val c = QuranCourseRepository.load(ctx)
        val all = c.allLessons
        val i = all.indexOfFirst { it.id == id }
        rule.setContent {
            MyApplicationTheme {
                CourseLessonView(all[i], c.levels.first { lv -> lv.lessons.any { it.id == id } }.title, i, all.size, false, -1, {}, {}, {}, {}, {}, {})
            }
        }
        rule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$file.png")
    }

    @Test fun home() {
        val c = QuranCourseRepository.load(ctx)
        rule.setContent { MyApplicationTheme { CourseHome(c, setOf("l1_letters", "l1_pairs", "l1_joined"), "l1_muqattaat", {}, {}, {}, {}) } }
        rule.onRoot().captureRoboImage(filePath = "src/test/screenshots/quran_learning_home.png")
    }
    @Test fun letters() = lesson("l1_letters", "quran_learning_letters")
    @Test fun ikhfa() = lesson("l2_idgham", "quran_learning_tajweed")
}
