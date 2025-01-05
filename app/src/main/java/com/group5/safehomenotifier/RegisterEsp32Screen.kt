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
    fun RegisterEsp32Screen(onBack: () -> Unit, deviceManager: DeviceManager) {
        var uiState by remember { mutableStateOf(UIState()) }

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
                    value = uiState.deviceId,
                    onValueChange = { uiState = uiState.copy(deviceId = it) },
                    label = { Text("Device ID", fontFamily = poppinsFontFamily) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = uiState.token,
                    onValueChange = {
                        uiState = uiState.copy(token = it)
                        uiState = uiState.copy(tokenError = validateToken(it))
                    },
                    label = { Text("Input Device Token", fontFamily = poppinsFontFamily) },
                    visualTransformation = if (uiState.tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (uiState.tokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { uiState = uiState.copy(tokenVisible = !uiState.tokenVisible) }) {
                            Icon(imageVector = image, contentDescription = "Toggle token visibility")
                        }
                    }
                )
                if (uiState.tokenError.isNotEmpty()) {
                    Text(
                        uiState.tokenError,
                        color = Color.Red,
                        fontFamily = poppinsFontFamily,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = uiState.deviceName,
                    onValueChange = { uiState = uiState.copy(deviceName = it) },
                    label = { Text("Device Name", fontFamily = poppinsFontFamily) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (uiState.tokenError.isEmpty()) {
                            val userEmail = FirebaseAuth.getInstance().currentUser?.email
                            if (userEmail != null) {
                                val device = Device(
                                    deviceId = uiState.deviceId,
                                    token = uiState.token,
                                    deviceName = uiState.deviceName
                                )
                                deviceManager.verifyDeviceCredentials(device.deviceId, device.token) { isValid ->
                                    if (isValid) {
                                        deviceManager.registerDevice(userEmail, device) { success ->
                                            uiState = uiState.copy(registrationStatus = if (success) "Device registered successfully." else "Failed to register device.")
                                        }
                                    } else {
                                        uiState = uiState.copy(registrationStatus = "Invalid device ID or token.")
                                    }
                                }
                            } else {
                                uiState = uiState.copy(registrationStatus = "User not logged in.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    enabled = uiState.tokenError.isEmpty()
                ) {
                    Text("Register Device", fontFamily = poppinsFontFamily, color = Black)
                }
                if (uiState.registrationStatus.isNotEmpty()) {
                    Text(
                        text = uiState.registrationStatus,
                        color = if (uiState.registrationStatus.contains("success")) Color.Green else Color.Red
                    )
                }
            }
        }
    }

    // Function to validate password complexity
    fun validateToken(token: String): String {
        return when {
            token.length < 8 -> "Token must be at least 8 characters long."
            !token.any { it.isUpperCase() } -> "Token must include at least one uppercase letter."
            !token.any { it.isDigit() } -> "Token must include at least one number."
            !token.any { "!@#$%^&*()-_=+[{]}|;:'\",<.>/?".contains(it) } -> "Token must include at least one special character."
            else -> ""
        }
    }
