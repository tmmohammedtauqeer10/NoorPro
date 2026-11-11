package com.noorpro.app.learning

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AcademyScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun open(dark: Boolean) {
        compose.setContent { MyApplicationTheme(darkTheme = dark) { AcademyScreen(onBack = {}) } }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Learn with intention.").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun lightCatalogRendersAndSearchFindsTheReadingCourse() {
        open(false)
        File("build/outputs/academy").mkdirs()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/academy/catalog-light.png")
        compose.onNodeWithText("Search courses, teachers or topics").performTextInput("Ihsan")
        compose.onNodeWithTag("academy_catalog").performScrollToNode(hasText("Ihsan & Character: A Practical Beginning"))
        compose.onNodeWithText("Ihsan & Character: A Practical Beginning").assertExists()
    }

    @Test fun darkCourseRequiresCorrectKnowledgeCheckBeforeCompletion() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val catalog = kotlinx.coroutines.runBlocking { LearningCatalog.load(context) }
        LearningProgressStore(context, "guest").reset(catalog.courses.first())
        open(true)
        File("build/outputs/academy").mkdirs()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/academy/catalog-dark.png")
        compose.onNodeWithText("Search courses, teachers or topics").performTextInput("Ihsan")
        compose.onNodeWithTag("academy_catalog").performScrollToNode(hasText("Ihsan & Character: A Practical Beginning"))
        compose.onNodeWithText("Ihsan & Character: A Practical Beginning").performClick()
        compose.onNodeWithTag("academy_course").performScrollToNode(hasText("Ihsan and the purpose of learning"))
        compose.onNodeWithText("Ihsan and the purpose of learning").performClick()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/academy/lesson-dark.png")
        compose.onNodeWithText("Complete lesson").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Collecting claims of spiritual rank").performScrollTo().performClick()
        compose.onNodeWithText("Check answer").performScrollTo().performClick()
        compose.onNodeWithText("Complete lesson").assertIsNotEnabled()
        compose.onNodeWithText("Sincere worship and good character").performScrollTo().performClick()
        compose.onNodeWithText("Check answer").performScrollTo().performClick()
        compose.onNodeWithText("Complete lesson").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Mark incomplete").fetchSemanticsNodes().isNotEmpty() }
        assertTrue("tas_lesson_01" in LearningProgressStore(context, "guest").completed())
    }

    @Test fun licensedLectureShowsPlaybackAndAttributionBeforeStartingNetworkMedia() {
        open(true)
        compose.onNodeWithText("Search courses, teachers or topics").performTextInput("How to Seek Knowledge")
        compose.onNodeWithTag("academy_catalog").performScrollToNode(hasText("How to Seek Knowledge in Islam"))
        compose.onNodeWithText("How to Seek Knowledge in Islam").performClick()
        compose.onNodeWithTag("academy_course").performScrollToNode(hasText("19 min · In-app video"))
        compose.onNodeWithText("19 min · In-app video").performClick()
        compose.onNodeWithText("Play lecture in NoorPro").performScrollTo().assertExists()
        compose.onNodeWithText("View CC BY 3.0 license").performScrollTo().assertExists()
        compose.onNodeWithText("Complete lesson").performScrollTo().assertIsNotEnabled()
    }
}
