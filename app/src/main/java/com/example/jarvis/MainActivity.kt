package com.example.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jarvis.service.MicrophoneService

class MainActivity : ComponentActivity() {

    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startMicrophoneService()
        } else {
            Toast.makeText(
                this,
                "Microphone permission is required for wake word detection",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JarvisTheme {
                JarvisMainScreen(
                    onStartListening = { requestMicrophonePermissionAndStart() },
                    onStopListening = { stopMicrophoneService() }
                )
            }
        }
    }

    private fun requestMicrophonePermissionAndStart() {
        val permission = Manifest.permission.RECORD_AUDIO

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            startMicrophoneService()
        } else {
            recordAudioPermissionLauncher.launch(permission)
        }
    }

    private fun startMicrophoneService() {
        val intent = Intent(this, MicrophoneService::class.java).apply {
            action = MicrophoneService.ACTION_START_LISTENING
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        Toast.makeText(this, "Microphone service started", Toast.LENGTH_SHORT).show()
    }

    private fun stopMicrophoneService() {
        val intent = Intent(this, MicrophoneService::class.java).apply {
            action = MicrophoneService.ACTION_STOP_LISTENING
        }

        stopService(intent)
        Toast.makeText(this, "Microphone service stopped", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun JarvisMainScreen(
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    var isListening by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready to listen") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050B14),
                        Color(0xFF0B1B2B),
                        Color(0xFF111B2C)
                    )
                )
            ),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "JARVIS",
                color = Color(0xFF8FE3FF),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            // Status Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101A2A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isListening) "🎤 Microphone Service" else "Foreground Service",
                            color = Color(0xFF8FE3FF),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            // Control Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (!isListening) {
                            onStartListening()
                            isListening = true
                            statusText = "Service running in foreground\nStep 3: Wake word detection coming soon"
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(110.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3DDCFF),
                        disabledContainerColor = Color(0xFF555555)
                    ),
                    enabled = !isListening
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Start listening",
                        tint = if (isListening) Color.Gray else Color.Black,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (isListening) {
                            onStopListening()
                            isListening = false
                            statusText = "Service stopped"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8FE3FF),
                        disabledContainerColor = Color(0xFF444444)
                    ),
                    enabled = isListening
                ) {
                    Icon(
                        imageVector = Icons.Filled.MicOff,
                        contentDescription = "Stop listening",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Stop Listening",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF3DDCFF),
            secondary = Color(0xFF8FE3FF),
            background = Color(0xFF040B13),
            surface = Color(0xFF0D1726),
            onPrimary = Color.Black,
            onSurface = Color.White
        ),
        content = content
    )
}
