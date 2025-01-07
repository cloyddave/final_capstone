package com.group5.safehomenotifier


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.firebase.auth.FirebaseAuth
import com.group5.safehomenotifier.ui.theme.CharcoalBlue
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily

@Composable
fun RegisterEsp32Screen(
    onBack: () -> Unit,
    deviceManager: DeviceManager
) {
    var deviceId by remember { mutableStateOf<String>("") }
    var token by remember { mutableStateOf<String>("") }
    var tokenError by remember { mutableStateOf<String>("") }
    var deviceName by remember { mutableStateOf<String>("") }
    var tokenVisible by remember { mutableStateOf<Boolean>(false) }
    var registrationStatus by remember { mutableStateOf<String>("") }

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
                "Add New Device",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = poppinsFontFamily,
                fontWeight = FontWeight.Normal,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                label = { Text("Input Device Token", fontFamily = poppinsFontFamily) },
                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image =
                        if (tokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle token visibility")
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
            TextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("Device Name", fontFamily = poppinsFontFamily) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (tokenError.isEmpty()) {
                        val userEmail = FirebaseAuth.getInstance().currentUser?.email
                        if (userEmail != null) {
                           deviceManager.verifyDeviceCredentials(deviceId, token) { isValid ->
                                if (isValid) {
                                    deviceManager.registerDevice(userEmail, token, deviceId, deviceName) { success ->
                                        registrationStatus = if (success) "Device registered successfully." else "Failed to register device."
                                    }
                                } else {
                                    registrationStatus = "Invalid device ID or token."
                                }
                            }
                        } else {
                            registrationStatus = "User not logged in."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                enabled = tokenError.isEmpty()
            ) {
                Text("Register Device", fontFamily = poppinsFontFamily, color = Black)
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

// Function to validate token complexity
fun validateToken(token: String): String {
    return when {
        token.length < 8 -> "Token must be at least 8 characters long."
        !token.any { it.isUpperCase() } -> "Token must include at least one uppercase letter."
        !token.any { it.isDigit() } -> "Token must include at least one number."
        !token.any { "!@#$%^&*()-_=+[{]}|;:'\",<.>/?".contains(it) } -> "Token must include at least one special character."
        else -> ""
    }
}