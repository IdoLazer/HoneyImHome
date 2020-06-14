package com.my.honeyimhome

import android.app.Application
import android.content.Intent
import android.content.IntentFilter

const val ACTION_SEND_SMS = "ACTION_SEND_SMS"

class HoneyImHomeApp() : Application() {


    override fun onCreate() {
        super.onCreate()
        val receiver = LocalSendSmsBroadcastReciever()
        registerReceiver(receiver, IntentFilter(ACTION_SEND_SMS))
    }

    fun sendSMS(sms: String) {
        val intent = Intent(ACTION_SEND_SMS)
        intent.putExtra(PHONE_NUM_KEY, sms);
        intent.putExtra(MESSAGE_CONTENT_KEY, "Honey I'm Sending a Test Message!");
        sendBroadcast(intent)
    }
}