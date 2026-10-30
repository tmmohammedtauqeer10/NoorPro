package com.noorpro.app.qibla

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaMathTest {
    // Reference values from the standard great-circle initial-bearing formula (true north).
    @Test fun bengaluru() = assertEquals(288.5, QiblaMath.bearingToKaaba(12.9716, 77.5946), 0.5)
    @Test fun hagariBommanahalli() = assertEquals(286.0, QiblaMath.bearingToKaaba(15.0420, 76.2030), 0.5)
    @Test fun dueNorthFromSouth() = assertEquals(0.0, QiblaMath.bearingToKaaba(15.0, QiblaMath.KAABA_LNG), 0.01)
    @Test fun needle() { assertEquals(18.5f, QiblaMath.needleAngle(288.5f, 270f), 0.01f); assertEquals(298.5f, QiblaMath.needleAngle(288.5f, 350f), 0.01f) }
    @Test fun wrapSmoothing() {
        assertEquals(20f, QiblaMath.shortestDelta(350f, 10f), 0.01f)
        assertEquals(0f, QiblaMath.smooth(350f, 10f, 0.5f), 0.01f)
    }
}
