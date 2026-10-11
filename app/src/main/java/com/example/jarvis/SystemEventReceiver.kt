package com.example.jarvis

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * SystemEventReceiver: Listens for system broadcasts
 *
 * Handles:
 * - ACTION_SCREEN_OFF: Screen turned off
 * - ACTION_SCREEN_ON: Screen turned on
 * - ACTION_BOOT_COMPLETED: Device boot completed
 *
 * Purpose: Placeholder for future screen-state-aware wake word detection (Step 3)
 *
 * Does NOT auto-start microphone service on boot.
 */
class SystemEventReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SystemEventReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Broadcast received: ${intent?.action}")

        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // Screen is OFF
                // Step 3: May optimize wake-word detection for locked screen
                Log.d(TAG, "Screen turned OFF")
            }
            Intent.ACTION_SCREEN_ON -> {
                // Screen is ON
                Log.d(TAG, "Screen turned ON")
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Device booted
                // Do NOT auto-start microphone service
                Log.d(TAG, "Device boot completed - NOT auto-starting service")
            }
        }
    }

    companion object Registry {
        fun register(context: Context) {
            val receiver = SystemEventReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_BOOT_COMPLETED)
            }

            try {
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                Log.d(TAG, "SystemEventReceiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register receiver: ${e.message}")
            }
        }
    }
}
