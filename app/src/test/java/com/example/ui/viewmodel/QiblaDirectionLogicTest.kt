package com.example.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaDirectionLogicTest {
    @Test
    fun knownCitiesUseGreatCircleTrueBearing() {
        assertEquals(58.5f, qiblaTrueBearing(40.7128, -74.0060), 0.6f) // New York
        assertEquals(118.9f, qiblaTrueBearing(51.5074, -0.1278), 0.6f) // London
        assertEquals(266.6f, qiblaTrueBearing(28.6139, 77.2090), 0.6f) // Delhi
        assertEquals(277.5f, qiblaTrueBearing(-33.8688, 151.2093), 0.6f) // Sydney
    }

    @Test
    fun magneticDeclinationProducesTrueNorthHeading() {
        assertEquals(107f, magneticToTrueHeading(100f, 7f), 0.001f)
        assertEquals(357f, magneticToTrueHeading(2f, -5f), 0.001f)
    }

    @Test
    fun pointerIsRelativeToTruePhoneHeading() {
        assertEquals(30f, relativeQiblaDirection(120f, 90f), 0.001f)
        assertEquals(350f, relativeQiblaDirection(10f, 20f), 0.001f)
    }

    @Test
    fun compassSmoothingCrossesNorthByShortestPath() {
        val smoothed = smoothCompassHeading(previous = 359f, measured = 1f, weight = 0.5f)
        assertTrue(smoothed < 1f || smoothed > 359f)
        assertEquals(0f, smoothed, 0.001f)
    }
}
