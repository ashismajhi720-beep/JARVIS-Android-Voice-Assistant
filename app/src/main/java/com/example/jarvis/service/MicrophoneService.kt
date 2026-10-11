package com.example.jarvis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.jarvis.MainActivity

/**
 * MicrophoneService: Foreground Service for Android 15
 *
 * This service runs continuously with a persistent notification to capture
 * microphone audio for wake word detection.
 *
 * Requirements met:
 * - Android 12+: declares foregroundServiceType="microphone"
 * - Android 14+: calls startForeground() within 10 seconds
 * - Android 15: compatible with new foreground service restrictions
 * - Does NOT auto-start on boot
 * - Does NOT claim wake word detection works (that's Step 3)
 */
class MicrophoneService : Service() {

    companion object {
        private const val TAG = "MicrophoneService"
        const val ACTION_START_LISTENING = "com.example.jarvis.ACTION_START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.example.jarvis.ACTION_STOP_LISTENING"
        const val NOTIFICATION_CHANNEL_ID = "jarvis_microphone_channel"
        const val NOTIFICATION_ID = 101
    }

    private var isListening = false
    private var audioRecord: AudioRecord? = null
    private var listeningThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    /**
     * onStartCommand is called when the service is started.
     * Must call startForeground() within 10 seconds (Android 14+ requirement)
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        Log.d(TAG, "onStartCommand called with action: $action")

        when (action) {
            ACTION_START_LISTENING -> {
                startForegroundListening()
            }
            ACTION_STOP_LISTENING -> {
                stopForegroundListening()
            }
            else -> {
                // Default: start listening
                startForegroundListening()
            }
        }

        return START_STICKY
    }

    /**
     * Starts the foreground microphone listening.
     * Creates notification and begins audio capture thread.
     */
    private fun startForegroundListening() {
        if (isListening) {
            Log.d(TAG, "Already listening, ignoring start request")
            return
        }

        isListening = true
        Log.d(TAG, "Starting foreground listening")

        // Create and show persistent notification (required by Android 14+)
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start audio capture in background thread
        listeningThread = Thread {
            captureAudio()
        }
        listeningThread?.start()

        Toast.makeText(this, "Microphone service started", Toast.LENGTH_SHORT).show()
    }

    /**
     * Stops the foreground listening and releases resources.
     */
    private fun stopForegroundListening() {
        if (!isListening) {
            Log.d(TAG, "Not listening, ignoring stop request")
            return
        }

        isListening = false
        Log.d(TAG, "Stopping foreground listening")

        // Stop audio capture
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        // Remove foreground notification
        stopForeground(STOP_FOREGROUND_REMOVE)

        // Stop the service
        stopSelf()

        Toast.makeText(this, "Microphone service stopped", Toast.LENGTH_SHORT).show()
    }

    /**
     * Captures audio from the device microphone.
     *
     * Current: Captures raw PCM audio
     * Step 3: Will pass audio buffer to ONNX wake-word detection model
     */
    private fun captureAudio() {
        try {
            val sampleRate = 16000 // Standard for speech recognition
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize <= 0) {
                Log.e(TAG, "Invalid buffer size: $minBufferSize")
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return
            }

            Log.d(TAG, "AudioRecord initialized. Starting recording.")
            audioRecord?.startRecording()

            val buffer = ByteArray(minBufferSize)
            var totalBytesRead = 0L

            while (isListening && audioRecord != null) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead
                    // TODO: Step 3 will pass buffer to ONNX model for wake-word detection
                    Log.d(TAG, "Audio chunk captured: $bytesRead bytes (total: $totalBytesRead)")
                } else if (bytesRead < 0) {
                    Log.e(TAG, "AudioRecord read error: $bytesRead")
                }
            }

            Log.d(TAG, "Audio capture loop ended. Total bytes: $totalBytesRead")
        } catch (e: Exception) {
            Log.e(TAG, "Audio capture exception: ${e.message}", e)
        } finally {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "Audio capture cleanup complete")
        }
    }

    /**
     * Builds the persistent notification for the foreground service.
     * Required by Android 8+. The notification must be shown while the service is running.
     */
    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MicrophoneService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("JARVIS Microphone Service")
            .setContentText("Listening for wake word detection...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(true) // Persistent notification
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Creates the notification channel for Android 8+.
     * Required for all notifications on Android 8 and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "JARVIS Microphone Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Microphone service for wake-word detection"
                // Do not play sound for persistent notification
                setSound(null, null)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // This is a started service, not a bound service
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isListening = false
        audioRecord?.release()
        audioRecord = null
    }
}
