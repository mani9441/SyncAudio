package com.example.syncaudio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.syncaudio.ClientAudioPlayer

@Composable
fun SyncAudioApp(
    onStartHost: () -> Unit,
    onJoinAsClient: () -> Unit
) {
    var isHost by remember { mutableStateOf(false) }
    var sessionId by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isHost) {
                Text("SyncAudio", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { isHost = true }) {
                    Text("Start as Host")
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sessionId,
                    onValueChange = { sessionId = it },
                    label = { Text("Enter Session ID") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    onJoinAsClient() // Let the Activity handle playback start
                }) {
                    Text("Join as Client")
                }
            } else {
                Text("Hosting Session", style = MaterialTheme.typography.headlineMedium)
                Text("Session ID: 12345") // Placeholder
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { onStartHost() }) {
                    Text("Start Audio Capture")
                }
            }
        }
    }
}
