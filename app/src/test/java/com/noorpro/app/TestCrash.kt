package com.noorpro.app

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Before
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class TestCrash {

    @Before
    fun initWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext(),
            Configuration.Builder().build()
        )
    }

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
