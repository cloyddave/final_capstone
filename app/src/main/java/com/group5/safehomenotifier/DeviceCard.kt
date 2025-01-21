package com.group5.safehomenotifier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily

@Composable
fun DeviceCard(
    deviceId: String,
    deviceName: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Device ID: $deviceId", fontFamily = poppinsFontFamily)
            Text("Device Name: $deviceName", fontFamily = poppinsFontFamily)
        }
    }
}

