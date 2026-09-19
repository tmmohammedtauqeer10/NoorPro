package com.example

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class TestCrash {

    @Test
    fun testActivityLaunch() {
        try {
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            scenario.onActivity { activity ->
                // Do nothing, just checking if it launches without crashing
                println("Activity launched successfully")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
