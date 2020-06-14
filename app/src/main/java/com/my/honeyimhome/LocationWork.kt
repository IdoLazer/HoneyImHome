package com.my.honeyimhome

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson

class LocationWork(appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {
    private var callback: CallbackToFutureAdapter.Completer<Result>? = null
    private var receiver: BroadcastReceiver? = null
    private var app: HoneyImHomeApp? = null
    private var sp: SharedPreferences? = null
    private var locationTracker: LocationTracker? = null
    private var gson: Gson = Gson()

    @SuppressLint("RestrictedApi")
    override fun startWork(): ListenableFuture<Result> {
        app = applicationContext as HoneyImHomeApp
        sp = app!!.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        locationTracker = LocationTracker(app!!)

        var hasTrackingPermission = ActivityCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        var hasSendSmsPermission = ActivityCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasTrackingPermission ||
            !hasSendSmsPermission ||
            !sp!!.getBoolean(CURRENTLY_TRACKING, false) ||
            !sp!!.getBoolean(HAS_HOME_LOCATION, false) ||
            !sp!!.getBoolean(HAS_SMS_PHONE_NUM, false)
        ) {
            val future = SettableFuture.create<Result>()
            future.set(Result.success())
            return future
        }

        // 1. here we create the future and store the callback for later use
        val future =
            CallbackToFutureAdapter.getFuture { callback:
                                                CallbackToFutureAdapter.Completer<Result> ->
                this.callback = callback
                return@getFuture null
            }

        // we place the broadcast receiver and immediately return the "future" object
        placeReceiver()
        locationTracker!!.startTracking()
        return future
    }

    // 2. we place the broadcast receiver now, waiting for it to fire in the future
    private fun placeReceiver() {
        // create the broadcast object and register it:

        this.receiver = object : BroadcastReceiver() {
            // notice that the fun onReceive() will get called in the future, not now
            override fun onReceive(context: Context?, intent: Intent?) {
                // got broadcast!
                onReceivedBroadcast(context, intent)
            }
        }

        this.getApplicationContext().registerReceiver(
            this.receiver,
            IntentFilter(LocationTracker.LOCATION_UPDATE_ACTION)
        )
    }

    // 3. when the broadcast receiver fired, we finished the work!
    // so we will clean all and call the callback to tell WorkManager that we are DONE
    private fun onReceivedBroadcast(context: Context?, intent: Intent?) {
        val gson = Gson()
        applicationContext.unregisterReceiver(this.receiver)
        receivedLocation(context, intent)
        var currentLocation: LocationInfo? = gson.fromJson<LocationInfo>(
            sp!!.getString(CURRENT_LOCATION, null),
            LocationInfo::class.java
        )
        if (currentLocation!!.accuracy!! > 50) {
            return
        }

        if (sp!!.getString(PREVIOUS_LOCATION, null) != null) {
            var previousLocation = gson.fromJson<LocationInfo>(
                sp!!.getString(PREVIOUS_LOCATION, null),
                LocationInfo::class.java
            )
            val homeLocation = gson.fromJson<LocationInfo>(
                sp!!.getString(HOME_LOCATION, LocationInfo(null, null, null).toString()),
                LocationInfo::class.java
            )

            if (previousLocation.distanceFrom(currentLocation) > 50
                && currentLocation.distanceFrom(homeLocation!!) <= 50
            ) {
                app!!.sendSMS(sp!!.getString(PHONE_NUM_KEY, null)!!)
            }
        }

        locationTracker!!.stopTracking()
        this.callback?.set(Result.success())
    }

    fun receivedLocation(context: Context?, intent: Intent?) {
        intent?.action ?: return
        if (intent.action.equals(LocationTracker.LOCATION_UPDATE_ACTION)) {
            if (sp!!.getString(CURRENT_LOCATION, null) != null) {
                sp!!.edit().putString(PREVIOUS_LOCATION, sp!!.getString(CURRENT_LOCATION, null))
                    .apply()
            }
            sp!!.edit().putString(
                CURRENT_LOCATION,
                intent.getStringExtra(LocationTracker.LOCATION_INFO_EXTRA)
            ).apply()
        }
    }
}