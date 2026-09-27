package com.divanshgandhi.attendai.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class FusedLocationProvider(context: Context) : LocationProvider {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission") // Both permitted precisions are checked immediately before the request.
    override suspend fun currentLocation(): LocationFix {
        fun granted(permission: String) = ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION) && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            throw LocationFailure("Location permission is required. Grant it in app settings and retry.")
        }
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            throw LocationFailure("Device location is turned off. Turn it on in Location settings and retry.")
        }
        val cancellation = CancellationTokenSource()
        try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(30_000)
                .setDurationMillis(15_000)
                .build()
            val location = withTimeout(20_000) { client.getCurrentLocation(request, cancellation.token).await() }
                ?: throw LocationFailure("No current location available. Set the emulator location or move to an open area, then retry.")
            val ageMillis = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
            if (ageMillis !in 0..30_000) throw LocationFailure("The location is stale. Please retry for a fresh fix.")
            return LocationFix(location.latitude, location.longitude, if (location.hasAccuracy()) location.accuracy else null)
        } catch (e: TimeoutCancellationException) {
            throw LocationFailure("Location request timed out. Check device Location settings and retry.", e)
        } catch (e: CancellationException) { throw e
        } catch (e: LocationFailure) { throw e
        } catch (e: SecurityException) {
            throw LocationFailure("Location permission was removed. Grant it and retry.", e)
        } catch (e: Exception) {
            throw LocationFailure("Location provider unavailable. Check Google Play services and Location settings, then retry.", e)
        } finally { cancellation.cancel() }
    }
}
