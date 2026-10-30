package com.noorpro.app.qibla

import kotlin.math.*

/** Pure Qibla math (unit-tested). */
object QiblaMath {
    const val KAABA_LAT = 21.422487
    const val KAABA_LNG = 39.826206

    /** Great-circle initial bearing from (lat,lng) to the Kaaba, degrees clockwise from TRUE north, 0..360. */
    fun bearingToKaaba(lat: Double, lng: Double): Double {
        val p1 = Math.toRadians(lat); val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LNG - lng)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return normalize(Math.toDegrees(atan2(y, x)))
    }

    fun normalize(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0
    fun normalize(deg: Float): Float = ((deg % 360f) + 360f) % 360f

    /** Needle angle relative to the top of the phone: qibla bearing minus true heading. */
    fun needleAngle(bearingTrue: Float, headingTrue: Float): Float = normalize(bearingTrue - headingTrue)

    /** Shortest signed difference (-180..180] from a to b, for wrap-safe smoothing/animation. */
    fun shortestDelta(from: Float, to: Float): Float {
        var d = normalize(to - from)
        if (d > 180f) d -= 360f
        return d
    }

    /** Exponential smoothing that never spins the long way round 0/360. */
    fun smooth(previous: Float, target: Float, alpha: Float): Float = normalize(previous + shortestDelta(previous, target) * alpha)
}
