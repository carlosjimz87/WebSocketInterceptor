package com.carlosjimz87.websocketinterceptor5.ui.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carlosjimz87.websocketinterceptor5.providers.AuthStore
import com.carlosjimz87.websocketinterceptor5.ui.theme.WebSocketInterceptor5Theme
import org.koin.androidx.compose.koinViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebSocketInterceptor5Theme {
                val vm: MainViewModel = koinViewModel()
                val log by vm.log.collectAsState()
                val state by vm.state.collectAsState()

                var token by remember { mutableStateOf(AuthStore.currentAccessToken) }
                var msg by remember { mutableStateOf("Hello from Android!") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { StableTopBar("WebSocket + Authorization header") }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status chip
                        StatusChip(state)

                        OutlinedTextField(
                            value = token,
                            onValueChange = {
                                token = it
                                AuthStore.currentAccessToken = it   // TokenProvider reads this
                            },
                            label = { Text("Bearer token") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.connect() }) { Text("Connect") }
                            Button(onClick = { vm.disconnect() }) { Text("Disconnect") }
                        }

                        OutlinedTextField(
                            value = msg,
                            onValueChange = { msg = it },
                            label = { Text("Message to send") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = { vm.send(msg) }) { Text("Send") }

                        Divider()
                        Text("Log:", style = MaterialTheme.typography.titleMedium)
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(log) { line -> Text(line) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StableTopBar(title: String) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun StatusChip(state: MainViewModel.ConnState) {
    val (label, color) = when (state) {
        MainViewModel.ConnState.Disconnected -> "Disconnected" to MaterialTheme.colorScheme.outline
        MainViewModel.ConnState.Connecting   -> "Connecting…"  to MaterialTheme.colorScheme.tertiary
        MainViewModel.ConnState.Open         -> "Open"         to MaterialTheme.colorScheme.primary
        MainViewModel.ConnState.Closing      -> "Closing…"     to MaterialTheme.colorScheme.secondary
        MainViewModel.ConnState.Closed       -> "Closed"       to MaterialTheme.colorScheme.secondary
        MainViewModel.ConnState.Failure      -> "Failure"      to MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge
        )
    }
}