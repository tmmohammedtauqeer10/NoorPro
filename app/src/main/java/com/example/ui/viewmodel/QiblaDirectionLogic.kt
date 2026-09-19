package com.example.ui.viewmodel

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val KAABA_LATITUDE = 21.422487
private const val KAABA_LONGITUDE = 39.826206

internal fun normalizeHeading(degrees: Float): Float {
    if (!degrees.isFinite()) return Float.NaN
    return ((degrees % 360f) + 360f) % 360f
}

/** Initial great-circle bearing from the user's WGS84 position to the Kaaba, from true north. */
internal fun qiblaTrueBearing(latitude: Double, longitude: Double): Float {
    if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
        !longitude.isFinite() || longitude !in -180.0..180.0
    ) return Float.NaN

    val kaabaLatitude = Math.toRadians(KAABA_LATITUDE)
    val userLatitude = Math.toRadians(latitude)
    val longitudeDifference = Math.toRadians(KAABA_LONGITUDE - longitude)
    val y = sin(longitudeDifference) * cos(kaabaLatitude)
    val x = cos(userLatitude) * sin(kaabaLatitude) -
        sin(userLatitude) * cos(kaabaLatitude) * cos(longitudeDifference)
    return normalizeHeading(Math.toDegrees(atan2(y, x)).toFloat())
}

/** Rotation-vector azimuth is magnetic; Qibla bearing is geographic, so convert it to true north. */
internal fun magneticToTrueHeading(magneticHeading: Float, declination: Float): Float =
    normalizeHeading(magneticHeading + declination)

internal fun relativeQiblaDirection(qiblaBearing: Float, trueHeading: Float): Float =
    normalizeHeading(qiblaBearing - trueHeading)

/** Circular smoothing avoids a jump from 359 degrees to 0 degrees. */
internal fun smoothCompassHeading(previous: Float?, measured: Float, weight: Float = 0.22f): Float {
    val current = normalizeHeading(measured)
    if (!current.isFinite()) return previous ?: Float.NaN
    val old = previous?.takeIf(Float::isFinite) ?: return current
    val delta = ((current - old + 540f) % 360f) - 180f
    return normalizeHeading(old + delta * weight.coerceIn(0f, 1f))
}
