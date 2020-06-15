package com.my.honeyimhome

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.google.gson.Gson

const val ACTION_SEND_SMS = "ACTION_SEND_SMS"

class HoneyImHomeApp() : Application() {


    override fun onCreate() {
        super.onCreate()
        val receiver = LocalSendSmsBroadcastReciever()
        registerReceiver(receiver, IntentFilter(ACTION_SEND_SMS))
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
            .putString(CURRENT_LOCATION, Gson().toJson(LocationInfo(0.0, 0.0, 1.0f))).apply()
    }

    fun sendSMS(sms: String, message: String) {
        val intent = Intent(ACTION_SEND_SMS)
        intent.putExtra(PHONE_NUM_KEY, sms);
        intent.putExtra(MESSAGE_CONTENT_KEY, message);
        sendBroadcast(intent)
    }
}