package com.my.honeyimhome

import android.location.Location

data class LocationInfo(
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?
) {
    fun distanceFrom(other: LocationInfo): Float {
        var results = FloatArray(1)
        Location.distanceBetween(
            latitude!!,
            longitude!!,
            other.latitude!!,
            other.longitude!!,
            results
        )
        return results[0]
    }
}