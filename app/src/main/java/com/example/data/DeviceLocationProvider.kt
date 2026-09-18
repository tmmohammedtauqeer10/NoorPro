package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.atomic.AtomicBoolean

/** Retrieves a real device location with Google Play Services and Android-provider fallbacks. */
object DeviceLocationProvider {
    private const val MAX_CACHED_LOCATION_AGE_MS = 24L * 60L * 60L * 1000L
    private const val MAX_CACHED_ACCURACY_METERS = 20_000f
    private const val PLATFORM_TIMEOUT_MS = 12_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, onResult: (Result<Location>) -> Unit) {
        val appContext = context.applicationContext
        if (!hasPermission(appContext)) {
            onResult(Result.failure(IllegalStateException("Location permission needed")))
            return
        }
        if (!isLocationEnabled(appContext)) {
            onResult(Result.failure(IllegalStateException("Turn on device location")))
            return
        }

        val completed = AtomicBoolean(false)
        val finish: (Result<Location>) -> Unit = { result ->
            if (completed.compareAndSet(false, true)) onResult(result)
        }
        val fallback = { requestFromAndroidProviders(appContext, finish) }

        try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                .setMaxUpdateAgeMillis(2L * 60L * 1000L)
                .setDurationMillis(15_000L)
                .build()
            val token = CancellationTokenSource()
            LocationServices.getFusedLocationProviderClient(appContext)
                .getCurrentLocation(request, token.token)
                .addOnSuccessListener { location ->
                    if (location != null && isValid(location)) {
                        finish(Result.success(location))
                    } else {
                        fallback()
                    }
                }
                .addOnFailureListener { fallback() }
        } catch (_: Exception) {
            fallback()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestFromAndroidProviders(
        context: Context,
        finish: (Result<Location>) -> Unit
    ) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            finish(Result.failure(IllegalStateException("Location service unavailable")))
            return
        }

        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).filter { provider ->
            runCatching { manager.getProvider(provider) != null && manager.isProviderEnabled(provider) }
                .getOrDefault(false)
        }

        val recentLocation = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.filter(::isUsableCachedLocation).maxByOrNull { it.time }

        if (recentLocation != null) {
            finish(Result.success(recentLocation))
            return
        }

        val activeProviders = providers.filter { it != LocationManager.PASSIVE_PROVIDER }
        requestProviderAtIndex(context, manager, activeProviders, 0, finish)
    }

    @SuppressLint("MissingPermission")
    private fun requestProviderAtIndex(
        context: Context,
        manager: LocationManager,
        providers: List<String>,
        index: Int,
        finish: (Result<Location>) -> Unit
    ) {
        if (index >= providers.size) {
            finish(Result.failure(IllegalStateException("Unable to get location. Move near a window and try again.")))
            return
        }

        val advanced = AtomicBoolean(false)
        val cancellationSignal = CancellationSignal()
        val handler = Handler(Looper.getMainLooper())
        val advance: (Location?) -> Unit = { location ->
            if (advanced.compareAndSet(false, true)) {
                if (location != null && isValid(location)) {
                    finish(Result.success(location))
                } else {
                    requestProviderAtIndex(context, manager, providers, index + 1, finish)
                }
            }
        }
        val timeout = Runnable {
            cancellationSignal.cancel()
            advance(null)
        }
        handler.postDelayed(timeout, PLATFORM_TIMEOUT_MS)

        try {
            LocationManagerCompat.getCurrentLocation(
                manager,
                providers[index],
                cancellationSignal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                handler.removeCallbacks(timeout)
                advance(location)
            }
        } catch (_: Exception) {
            handler.removeCallbacks(timeout)
            advance(null)
        }
    }

    private fun isValid(location: Location): Boolean =
        location.latitude.isFinite() && location.latitude in -90.0..90.0 &&
            location.longitude.isFinite() && location.longitude in -180.0..180.0

    private fun isUsableCachedLocation(location: Location): Boolean {
        val ageMs = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        return isValid(location) &&
            ageMs <= MAX_CACHED_LOCATION_AGE_MS &&
            (!location.hasAccuracy() || location.accuracy <= MAX_CACHED_ACCURACY_METERS)
    }
}
