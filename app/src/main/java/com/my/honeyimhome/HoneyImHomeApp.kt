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
    }

    fun sendSMS(sms: String, message: String) {
        val intent = Intent(ACTION_SEND_SMS)
        intent.putExtra(PHONE_NUM_KEY, sms);
        intent.putExtra(MESSAGE_CONTENT_KEY, message);
        sendBroadcast(intent)
    }
}