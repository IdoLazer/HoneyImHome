package com.my.honeyimhome

import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.gson.Gson
import java.util.concurrent.TimeUnit


const val REQUEST_CODE_PERMISSION_LOCATION = 1234
const val REQUEST_CODE_PERMISSION_SMS = 1235
const val SHARED_PREFERENCES_NAME = "HoneyImHome"
const val CURRENTLY_TRACKING = "currentlyTracking"
const val HAS_HOME_LOCATION = "hasHomeLocation"
const val HOME_LOCATION = "homeLocation"
const val HAS_SMS_PHONE_NUM = "hasSendSMSPhoneNum"
const val SMS_PHONE_NUM = "sendSMSPhoneNum"

class MainActivity : AppCompatActivity() {

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            receivedLocation(context, intent)
        }
    }

    fun receivedLocation(context: Context?, intent: Intent?) {
        intent?.action ?: return
        if (intent.action.equals(LocationTracker.LOCATION_UPDATE_ACTION)) {
            var location =
                gson.fromJson<LocationInfo>(
                    intent.getStringExtra(LocationTracker.LOCATION_INFO_EXTRA),
                    LocationInfo::class.java
                )
            setHomeBtn.isVisible = location.accuracy!! < 50
            latitudeText.text = "Latitude: ${location.latitude}"
            longitudeText.text = "Longitude: ${location.longitude}"
            accuracyText.text = "Accuracy: ${location.accuracy}m"
        }
    }

    lateinit var locationTracker: LocationTracker
    lateinit var setSMSnumBtn: Button
    lateinit var testSMSBtn: Button
    lateinit var trackingBtn: Button
    lateinit var clearHomeBtn: Button
    lateinit var setHomeBtn: Button
    lateinit var smsNum: TextView
    lateinit var latitudeText: TextView
    lateinit var longitudeText: TextView
    lateinit var accuracyText: TextView
    lateinit var homeText: TextView

    var homeLocation: LocationInfo? = null
    lateinit var gson: Gson
    lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerReceiver(receiver, IntentFilter(LocationTracker.LOCATION_UPDATE_ACTION))
        gson = Gson()
        locationTracker = LocationTracker(this)
        setSMSnumBtn = findViewById<Button>(R.id.set_sms_num_btn)
        testSMSBtn = findViewById<Button>(R.id.test_sms_btn)
        trackingBtn = findViewById<Button>(R.id.tracking_btn)
        clearHomeBtn = findViewById<Button>(R.id.clear_home_btn)
        setHomeBtn = findViewById<Button>(R.id.set_home_btn)
        smsNum = findViewById<TextView>(R.id.sms_num)
        latitudeText = findViewById<TextView>(R.id.latitude)
        longitudeText = findViewById<TextView>(R.id.longitude)
        accuracyText = findViewById<TextView>(R.id.accuracy)
        homeText = findViewById<TextView>(R.id.home_location)


        trackingBtn.setBackgroundColor(Color.GREEN)
        testSMSBtn.isVisible = false
        clearHomeBtn.isVisible = false
        setHomeBtn.isVisible = false
        latitudeText.isVisible = false
        longitudeText.isVisible = false
        accuracyText.isVisible = false
        homeText.isVisible = false
        setSMSnumBtn.setOnClickListener(View.OnClickListener { _ ->
            if (checkSMSPermission()) setSMSPhoneNumber()
        })
        testSMSBtn.setOnClickListener(View.OnClickListener { _ ->
            if (checkSMSPermission() && sp.getBoolean(HAS_SMS_PHONE_NUM, false)) {
                val app = application as HoneyImHomeApp
                app.sendSMS(
                    sp.getString(SMS_PHONE_NUM, null)!!,
                    "Honey I'm Sending a Test Message!"
                )
            }
        })
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
            homeLocation = gson.fromJson<LocationInfo>(
                sp.getString(HOME_LOCATION, LocationInfo(null, null, null).toString()),
                LocationInfo::class.java
            )
            writeHomeLocation()
        }
        if (sp.getBoolean(HAS_SMS_PHONE_NUM, false)) {
            smsNum.text = "SMS number: ${sp.getString(SMS_PHONE_NUM, null)}"
            testSMSBtn.isVisible = true
        }
    }

    private fun checkSMSPermission(): Boolean {
        val hasSMSPermission = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasSMSPermission) return true
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.SEND_SMS),
            REQUEST_CODE_PERMISSION_SMS
        )
        return false
    }

    private fun clearHomeLocation() {
        sp.edit().putBoolean(HAS_HOME_LOCATION, false).apply()
        homeLocation = null;
        homeText.isVisible = false;
    }

    fun checkTrackingPermission(): Boolean {
        val hasTrackingPermission = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasTrackingPermission) return true
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_PERMISSION_LOCATION
        )
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSION_LOCATION) {
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
        if (requestCode == REQUEST_CODE_PERMISSION_SMS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setSMSPhoneNumber()
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Need Send SMS Permission")
                builder.setMessage("We can't send SMS messages without your permission")
                builder.setNeutralButton("Ok") { _, _ ->
                }
                builder.show()
            }
        }
    }

    private fun setSMSPhoneNumber() {
        var dialog = AlertDialog.Builder(this)
        dialog.setTitle("SMS Phone Number").setMessage("Please Enter Desired Number:")
        val input = EditText(this)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp
        dialog.setView(input)
        dialog.setNeutralButton("Confirm", DialogInterface.OnClickListener { _, _ ->
            val num = input.text.toString()
            if (num.isEmpty()) {
                sp.edit().putBoolean(HAS_SMS_PHONE_NUM, false).apply()
                smsNum.text = "SMS number: not set"
                testSMSBtn.isVisible = false
                return@OnClickListener
            }
            sp.edit().putBoolean(HAS_SMS_PHONE_NUM, true).putString(SMS_PHONE_NUM, num).apply()
            smsNum.text = "SMS number: $num"
            testSMSBtn.isVisible = true
        })
        dialog.show()
    }


    private fun startTracking() {
        if (!sp.getBoolean(CURRENTLY_TRACKING, false)) {
            sp.edit().putBoolean(CURRENTLY_TRACKING, true).apply()
            val locationWork =
                PeriodicWorkRequest.Builder(LocationWork::class.java, 15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.NONE)
                    .build()
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    locationWork
                )
        }

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
        homeLocation = locationTracker.getCurrentLocation()
        homeLocation ?: return
        sp.edit().putBoolean(HAS_HOME_LOCATION, true).apply()
        sp.edit().putString(HOME_LOCATION, gson.toJson(homeLocation)).apply()
        writeHomeLocation()
    }

    private fun writeHomeLocation() {
        homeText.text =
            "Your home location is defined as: ${homeLocation!!.latitude}, ${homeLocation!!.longitude}"
        homeText.isVisible = true
        clearHomeBtn.isVisible = true
    }

    private fun stopTracking() {
        sp.edit().putBoolean(CURRENTLY_TRACKING, false).apply()
        WorkManager.getInstance(this).cancelUniqueWork(WORK_NAME)
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
        unregisterReceiver(receiver)
    }


}
