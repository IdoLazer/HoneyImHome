package com.my.honeyimhome

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.gson.Gson

class LocationTracker(val context: Context) {

    companion object {
        const val LOCATION_UPDATE_ACTION = "location updated"
        const val LOCATION_INFO_EXTRA = "location"
    }

    var tracking = false
    private var fusedLocationServices: FusedLocationProviderClient? = null
    private var currentLocation: LocationInfo? = null
    private var gson: Gson = Gson()
    private var locationCallback: LocationCallback? = null

    fun startTracking() {
        var hasTrackingPermission = ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasTrackingPermission) return
        tracking = true;
        fusedLocationServices = LocationServices.getFusedLocationProviderClient(context)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (!tracking) return
                var location = locationResult?.lastLocation
                currentLocation = LocationInfo(
                    location?.latitude,
                    location?.longitude,
                    location?.accuracy
                )
                notifyChangedLocation()
            }
        }
        startLocationUpdate()
    }

    fun stopTracking() {
        tracking = false;
        fusedLocationServices?.removeLocationUpdates(locationCallback)
        fusedLocationServices = null
        locationCallback = null
    }

    fun startLocationUpdate() {
        fusedLocationServices?.requestLocationUpdates(
            LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000),
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun notifyChangedLocation() {
        var intent = Intent(LOCATION_UPDATE_ACTION)
        intent.putExtra(LOCATION_INFO_EXTRA, gson.toJson(currentLocation));
        context.sendBroadcast(intent)
    }

    fun getCurrentLocation(): LocationInfo? {
        return currentLocation
    }
}