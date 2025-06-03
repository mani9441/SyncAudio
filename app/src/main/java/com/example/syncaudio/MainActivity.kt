package com.example.syncaudio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import com.example.syncaudio.ui.SyncAudioApp

class MainActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    // Launcher for RECORD_AUDIO permission
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startMediaProjection()
    }

    // Launcher for screen capture permission
    private val mediaProjectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, AudioCaptureService::class.java).apply {
                putExtra("resultCode", result.resultCode)  // ✅ Actual resultCode
                putExtra("data", result.data)              // ✅ Actual Intent
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            // You can show a Toast or Snack bar here if the user denies permission
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize media projection manager
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)

        // Set the XML layout instead of Compose UI
        setContentView(R.layout.activity_main)

        val btnStartHost = findViewById<Button>(R.id.btn_start_host)
        val btnJoinClient = findViewById<Button>(R.id.btn_join_client)

        btnStartHost.setOnClickListener {
            // Show host UI
            setContentView(R.layout.activity_host)

            val btnStartCapture = findViewById<Button>(R.id.btn_start_capture)
            val sessionIdText = findViewById<TextView>(R.id.session_id)

            sessionIdText.text = "session ID: 12345" // Replace with dynamic ID if needed

            btnStartCapture.setOnClickListener {
                requestAudioCapturePermission()
            }
        }

        btnJoinClient.setOnClickListener {
            // Start client activity
            startActivity(Intent(this, ClientActivity::class.java))
        }
    }


    private fun requestAudioCapturePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED) {
            startMediaProjection()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startMediaProjection() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(captureIntent)
    }
}
