package com.group5.safehomenotifier


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily

@Composable
fun MyDeviceScreen(
    onBack: () -> Unit,
    email: String
) {
    var devices by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    LaunchedEffect(email) {
        DeviceRepository.fetchUserDevices(email) { result ->
            devices = result
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF364559))
            .padding(16.dp)
    ) {
        Column {
            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = White
                )
            }

            Text(
                text = "My Devices",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = poppinsFontFamily,
                fontWeight = FontWeight.Normal,
                color = Color.LightGray
            )

            LazyColumn {
                items(devices) { device ->
                    DeviceCard(
                        deviceId = device["device_id"] ?: "",
                        deviceName = device["deviceName"] ?: "Unnamed Device"
                    )
                }
            }
        }
    }
}
