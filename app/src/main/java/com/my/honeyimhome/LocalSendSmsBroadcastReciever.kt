package com.my.honeyimhome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat

const val PHONE_NUM_KEY = "PHONE"
const val MESSAGE_CONTENT_KEY = "CONTENT"

class LocalSendSmsBroadcastReciever() : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val hasTrackingPermission = ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasTrackingPermission) {
            Log.d("tag", "Need SMS permission")
            return
        }
        val num = intent?.getStringExtra(PHONE_NUM_KEY)
        if (num == null) {
            Log.d("tag", "Phone number is null")
        }
        val content = intent?.getStringExtra(MESSAGE_CONTENT_KEY)
        if (content == null) {
            Log.d("tag", "Phone number is null")
        }
        SmsManager.getDefault().sendTextMessage(num, null, content, null, null)
        val notificationFireHelper = NotificationFireHelper(context)
        notificationFireHelper.fireNotification("Sending SMS to $num: $content")
    }
}