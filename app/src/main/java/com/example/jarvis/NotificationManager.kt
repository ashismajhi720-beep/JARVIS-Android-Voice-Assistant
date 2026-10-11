package com.example.jarvis

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * NotificationManager: Centralized notification channel management
 *
 * Creates and manages notification channels for Android 8+
 * Required for all notifications on Android 8 and above.
 */
object NotificationChannels {

    private const val TAG = "NotificationChannels"

    const val CHANNEL_MICROPHONE = "jarvis_microphone_channel"
    const val CHANNEL_ALERTS = "jarvis_alerts_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.d(TAG, "Android version < 8, notification channels not needed")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Microphone service channel
        val microphoneChannel = NotificationChannel(
            CHANNEL_MICROPHONE,
            "JARVIS Microphone Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification for microphone service"
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(microphoneChannel)
        Log.d(TAG, "Created microphone notification channel")

        // Alerts channel
        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "JARVIS Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts and responses from JARVIS"
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(alertsChannel)
        Log.d(TAG, "Created alerts notification channel")
    }
}
