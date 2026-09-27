package com.divanshgandhi.attendai.location

data class LocationFix(val latitude: Double, val longitude: Double, val accuracyMeters: Float?) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        require(accuracyMeters == null || (accuracyMeters.isFinite() && accuracyMeters >= 0))
    }
}

interface LocationProvider {
    suspend fun currentLocation(): LocationFix
}

class LocationFailure(message: String, cause: Throwable? = null) : Exception(message, cause)
