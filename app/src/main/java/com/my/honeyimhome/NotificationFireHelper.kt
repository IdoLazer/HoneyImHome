package com.my.honeyimhome

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationFireHelper(private val context: Context) {


    private val channelId = "CHANNEL_ID_Send_SMS_Notifications"

    public fun fireNotification(msg: String) {
        createChannelIfNotExists()
        actualFire(msg)
    }


    private fun createChannelIfNotExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notificationChannels.forEach { channel ->
                if (channel.id == channelId) {
                    return
                }
            }

            // Create the NotificationChannel
            val name = "SMS_sent"
            val descriptionText = "channel for notifying the user that an SMS was sent"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            channel.description = descriptionText
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun actualFire(msg: String) {

        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(123, notification)
    }
}