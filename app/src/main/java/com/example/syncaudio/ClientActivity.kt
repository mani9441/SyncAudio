package com.example.syncaudio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket

class ClientActivity : ComponentActivity() {
    private val clientAudioPlayer = ClientAudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the XML layout instead of Compose
        setContentView(R.layout.activity_client)

        // Reference the button and text view
        val playbackButton = findViewById<Button>(R.id.btn_start_playback)
        val statusText = findViewById<TextView>(R.id.session_id)

        var isPlaying = false

        playbackButton.setOnClickListener {
            if (isPlaying) {
                clientAudioPlayer.stopPlayback()
                playbackButton.text = "Start playback"
                statusText.text = "Stopped"
            } else {
                clientAudioPlayer.startPlayback()
                playbackButton.text = "Stop playback"
                statusText.text = "Playing..."
            }
            isPlaying = !isPlaying
        }
    }

    override fun onDestroy() {
        clientAudioPlayer.stopPlayback()
        super.onDestroy()
    }

    class ClientAudioPlayer {
        private var audioTrack: AudioTrack? = null
        private var udpSocket: DatagramSocket? = null
        private val scope = CoroutineScope(Dispatchers.IO)

        companion object {
            private const val TAG = "ClientAudioPlayer"
        }

        fun startPlayback() {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build()

            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build()

            try {
                udpSocket = DatagramSocket(5001)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create UDP socket", e)
                return
            }

            val buffer = ByteArray(bufferSize)
            audioTrack?.play()

            scope.launch {
                try {
                    while (true) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        udpSocket?.receive(packet)
                        Log.d(TAG, "Received packet size: ${packet.length}")
                        audioTrack?.write(packet.data, 0, packet.length)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error receiving audio packets", e)
                }
            }
        }

        fun stopPlayback() {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AudioTrack", e)
            }
            try {
                udpSocket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing UDP socket", e)
            }
        }
    }
}
