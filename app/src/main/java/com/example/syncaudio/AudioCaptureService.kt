package com.example.syncaudio

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class AudioCaptureService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val TAG = "AudioCaptureService"
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()

        Log.d(TAG, "Intent: $intent")
        Log.d(TAG, "Extras: ${intent?.extras}")
        Log.d(TAG, "Has data extra: ${intent?.hasExtra("data")}")

        val resultCode = intent?.getIntExtra("resultCode", android.app.Activity.RESULT_CANCELED)
        val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("data")
        }

        if (resultCode == android.app.Activity.RESULT_OK && resultData != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            if (mediaProjection != null) {
                Log.d(TAG, "Got valid media projection")
                startAudioCapture()
            } else {
                Log.e(TAG, "MediaProjection is still null 😢")
                stopSelf()
            }
        } else {
            Log.e(TAG, "Invalid media projection data from Intent extras")
            stopSelf()
        }

        return START_NOT_STICKY
    }



    private fun startAudioCapture() {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Record audio permission not granted")
            stopSelf()
            return
        }

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(BUFFER_SIZE)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord?.startRecording()

        scope.launch {
            try {
                DatagramSocket().use { udpSocket ->
                    // IMPORTANT: Replace with your hotspot broadcast IP address (e.g., 192.168.43.255)
                    val broadcastAddress = getBroadcastAddress() ?: InetAddress.getByName("255.255.255.255")
                    val buffer = ByteArray(BUFFER_SIZE)

                    while (true) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            val packet = DatagramPacket(buffer, read, broadcastAddress, 5001)
                            udpSocket.send(packet)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio transmission error: ${e.message}")
            }
        }
    }

    private fun getBroadcastAddress(): InetAddress? {
        // TODO: Implement a proper method to get broadcast IP of your hotspot network
        // For now, return null to use default 255.255.255.255
        return null
    }

    private fun startForegroundServiceNotification() {
        val channelId = "audio_capture_channel"
        val channelName = "Audio Capture"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SyncAudio")
            .setContentText("Capturing system audio...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        audioRecord?.stop()
        audioRecord?.release()
        mediaProjection?.stop()
        super.onDestroy()
    }
}
