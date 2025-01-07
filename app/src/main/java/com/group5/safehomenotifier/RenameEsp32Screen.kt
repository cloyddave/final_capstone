package com.group5.safehomenotifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group5.safehomenotifier.ui.theme.CharcoalBlue
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily

@Composable
fun RenameEsp32Screen(onBack: () -> Unit, deviceManager: DeviceManager) {
    var deviceId by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var tokenError by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var registrationStatus by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlue)
            .padding(16.dp), // Add padding to the Box if desired
        contentAlignment = Alignment.Center // Center the contents
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = White // Customize color if needed
                )
            }
            Text(
                "Rename Device",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = poppinsFontFamily,
                fontWeight = FontWeight.Normal,
                color = (Color.LightGray),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(15.dp))
            TextField(
                value = deviceId,
                onValueChange = { deviceId = it },
                label = { Text("Device ID", fontFamily = poppinsFontFamily) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = token,
                onValueChange = {
                    token = it
                    tokenError = validateToken(it)
                },
                label = { Text("Input Device Token", fontFamily = poppinsFontFamily) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = {
                    Text(
                        "What name you will be assign?",
                        fontFamily = poppinsFontFamily
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick@{
                    deviceManager.verifyDeviceCredentials(deviceId, token) { isValid ->
                        if (isValid) {
                            // You can replace this with dynamic name fetching logic if needed
                            deviceManager.renameEsp32Device(token,deviceId, deviceName) { success ->
                                registrationStatus =
                                    if (success) "Device renamed successfully." else "Failed to rename device."
                            }
                        } else {
                            registrationStatus = "Invalid device ID or token."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                )
            )

            {
                Text("Rename Device", fontFamily = poppinsFontFamily, color = Black)
            }
            if (registrationStatus.isNotEmpty()) {
                Text(
                    text = registrationStatus,
                    color = if (registrationStatus.contains("success")) Color.Green else Color.Red
                )
            }
        }
    }
}