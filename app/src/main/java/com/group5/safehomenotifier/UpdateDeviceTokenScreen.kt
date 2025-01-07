
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group5.safehomenotifier.ui.theme.CharcoalBlue
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily

@Composable
fun UpdateDeviceTokenScreen(onBack: () -> Unit, deviceManager: DeviceManager) {

    var deviceId by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var tokenError by remember { mutableStateOf("") }
    var newToken by remember { mutableStateOf("") }
    var updateStatus by remember { mutableStateOf("") }
    //var uiState by remember { mutableStateOf(UIState()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlue)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = White
                )
            }
            Text(
                "Update Device Token",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = poppinsFontFamily,
                fontWeight = FontWeight.Normal,
                color = Color.LightGray,
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
                onValueChange = { token = it },
                label = { Text("Enter Current Token", fontFamily = poppinsFontFamily) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = newToken,
                onValueChange = {
                    newToken = it
                    tokenError = validateToken(it)
                },
                label = { Text("New Device Token", fontFamily = poppinsFontFamily) },
                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image =
                        if (tokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription = "Toggle token visibility"
                        )
                    }
                }
            )

            if (tokenError.isNotEmpty()) {
                Text(
                    tokenError,
                    color = Color.Red,
                    fontFamily = poppinsFontFamily,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (tokenError.isEmpty()) {
                        // Call the updateDeviceToken function with entered token and new token
                        deviceManager.updateDeviceToken(deviceId, newToken) { success ->
                            updateStatus =
                                if (success) "Token updated successfully." else "Invalid device ID or token."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                enabled = tokenError.isEmpty() // Disable button if error exists
            ) {
                Text("Update Token", fontFamily = poppinsFontFamily, color = Black)
            }

            if (updateStatus.isNotEmpty()) {
                Text(
                    text = updateStatus,
                    color = if (updateStatus.contains("success")) Color.Green else Color.Red
                )
            }
        }
    }
}
