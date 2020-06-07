package com.my.honeyimhome

import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private var reciever: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action ?: return
            if (intent.action.equals(LocationTracker.LOCATION_UPDATE_ACTION)) {
                var location =
                    gson.fromJson<LocationInfo>(
                        intent.getStringExtra(LocationTracker.LOCATION_INFO_EXTRA),
                        LocationInfo::class.java
                    )
                if (location.accuracy!! < 50) {
                    setHomeBtn.isVisible = true;
                }
                latitudeText.text = "Latitude: ${location.latitude}"
                longitudeText.text = "Longitude: ${location.longitude}"
                accuracyText.text = "Accuracy: ${location.accuracy}m"
            }
        }
    }
    private final var REQUEST_CODE_PERMISSION_LOCATION = 1234
    private var SHARED_PREFERENCES_NAME = "HoneyImHome"
    private var CURRENTLY_TRACKING = "currentlyTracking"
    private var HAS_HOME_LOCATION = "hasHomeLocation"
    private var HOME_LOCATION = "homeLocation"

    private lateinit var locationTracker: LocationTracker
    private lateinit var trackingBtn: Button
    private lateinit var clearHomeBtn: Button
    private lateinit var setHomeBtn: Button
    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var homeText: TextView

    private var homeLocation: LocationInfo? = null
    private lateinit var gson: Gson
    private lateinit var sp : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerReceiver(reciever, IntentFilter(LocationTracker.LOCATION_UPDATE_ACTION))
        gson = Gson()
        locationTracker = LocationTracker(this)
        trackingBtn = findViewById<Button>(R.id.tracking_btn)
        clearHomeBtn = findViewById<Button>(R.id.clear_home_btn)
        setHomeBtn = findViewById<Button>(R.id.set_home_btn)
        latitudeText = findViewById<TextView>(R.id.latitude)
        longitudeText = findViewById<TextView>(R.id.longitude)
        accuracyText = findViewById<TextView>(R.id.accuracy)
        homeText = findViewById<TextView>(R.id.home_location)


        trackingBtn.setBackgroundColor(Color.GREEN)
        clearHomeBtn.isVisible = false
        setHomeBtn.isVisible = false
        latitudeText.isVisible = false
        longitudeText.isVisible = false
        accuracyText.isVisible = false
        homeText.isVisible = false
        trackingBtn.setOnClickListener(View.OnClickListener { _ ->
            if (checkTrackingPermission()) startTracking()
        })
        setHomeBtn.setOnClickListener(View.OnClickListener { _ -> setLocationAsHome() })
        clearHomeBtn.setOnClickListener(View.OnClickListener { _ -> clearHomeLocation() })
        sp = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (sp.getBoolean(CURRENTLY_TRACKING, false)) {
            startTracking()
        }
        if (sp.getBoolean(HAS_HOME_LOCATION, false)) {
            writeHomeLocation(
                gson.fromJson<LocationInfo>(
                    sp.getString(HOME_LOCATION, LocationInfo(null, null, null).toString()),
                    LocationInfo::class.java
                )
            )
        }
    }

    private fun clearHomeLocation() {
        sp.edit().putBoolean(HAS_HOME_LOCATION, false).apply()
        homeLocation = null;
        homeText.isVisible = false;
    }

    fun checkTrackingPermission(): Boolean {
        var hasTrackingPermission = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasTrackingPermission) {
            return true;
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_PERMISSION_LOCATION
            )
            return false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != REQUEST_CODE_PERMISSION_LOCATION) return
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTracking()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Need Location Data Permission")
            builder.setMessage("We can't track your location without your permission")

            builder.setNeutralButton("Ok") { _, _ ->
            }
            builder.show()
        }
    }

    private fun startTracking() {
        sp.edit().putBoolean(CURRENTLY_TRACKING, true).apply()
        locationTracker.startTracking()
        latitudeText.isVisible = true
        longitudeText.isVisible = true
        accuracyText.isVisible = true
        trackingBtn.text = "Stop Tracking Location"
        trackingBtn.setBackgroundColor(Color.RED)
        trackingBtn.setOnClickListener(View.OnClickListener { _ -> stopTracking() })
        setHomeBtn.setOnClickListener(View.OnClickListener { _ -> setLocationAsHome() })
    }

    private fun setLocationAsHome() {
        var homeLocation = locationTracker.getCurrentLocation()
        homeLocation ?: return
        sp.edit().putBoolean(HAS_HOME_LOCATION, true).apply()
        sp.edit().putString(HOME_LOCATION, gson.toJson(homeLocation)).apply()
        writeHomeLocation(homeLocation)
    }

    private fun writeHomeLocation(homeLocation: LocationInfo) {
        homeText.text =
            "Your home location is defined as: ${homeLocation.latitude}, ${homeLocation.longitude}"
        homeText.isVisible = true
        clearHomeBtn.isVisible = true
    }

    private fun stopTracking() {
        sp.edit().putBoolean(CURRENTLY_TRACKING, false).apply()
        locationTracker.stopTracking()
        latitudeText.isVisible = false
        longitudeText.isVisible = false
        accuracyText.isVisible = false
        setHomeBtn.isVisible = false
        trackingBtn.text = "Start Tracking Location"
        trackingBtn.setBackgroundColor(Color.GREEN)
        trackingBtn.setOnClickListener(View.OnClickListener { _ ->
            if (checkTrackingPermission()) startTracking()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        unregisterReceiver(reciever)
    }


}
