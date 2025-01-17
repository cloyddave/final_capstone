package com.group5.safehomenotifier

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.dp

@Composable
fun ForgotTokenScreen(onBack: () -> Unit, deviceManager: DeviceManager) {
    var email by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var token by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(onClick = { onBack() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = White // Customize color if needed
            )
        }
        Text("Forgot Token", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Registered Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = deviceId,
            onValueChange = { deviceId = it },
            label = { Text("Device ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                errorMessage = null // Reset errors
                token = null // Reset token
                deviceManager.forgotToken(email, deviceId) { retrievedToken, success ->
                    if (success) {
                        token = retrievedToken
                    } else {
                        errorMessage = "Failed to retrieve token. Ensure the Device ID and Email match."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retrieve Token")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (token != null) {
            Text("Token: $token", style = MaterialTheme.typography.bodyLarge)
        }

        if (errorMessage != null) {
            Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
    }
}
