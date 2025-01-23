package com.group5.safehomenotifier

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.group5.safehomenotifier.DeviceRepository.fetchUserDevices
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@ExperimentalMaterial3Api
@Composable
fun SafeHomeNotifierApp(
    historyImages: MutableList<String>,
    imageUrl: String?,
    onNavigateToSignIn: () -> Unit,
    deviceManager: DeviceManager,
) {
    val context = LocalContext.current
    val deviceName = (context as? android.app.Activity)?.intent?.getStringExtra("deviceName")
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    //val isRegisteredSuccessfully by rememberSaveable { mutableStateOf(isDeviceRegistered()) }
    var showHistory by remember { mutableStateOf(false) }
    var imageDisplay by remember { mutableStateOf(false) }
    var renameDevice by remember { mutableStateOf(false) }
    var changeToken by remember { mutableStateOf(false) }
    var forgotToken by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
   // val deviceName = intent.getStringExtra("deviceName")
    var showMyDevices by remember { mutableStateOf(false) }

    val userDevices = remember { mutableStateListOf<Map<String, String>>() }

    LaunchedEffect(Unit) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail != null) {
            fetchUserDevices(userEmail) { devices ->
                userDevices.addAll(devices)
            }
        }
    }



    Scaffold(
        content = {
            when {
                isRegistering -> {
                    RegisterEsp32Screen(
                        onBack = { isRegistering = false },
                        deviceManager = deviceManager
                    )
                }

                renameDevice -> {
                    RenameEsp32Screen(
                        onBack = { renameDevice = false },
                        deviceManager = deviceManager
                    )
                }

                showHistory -> {
                    HistoryScreen(
                        historyImages = historyImages,
                        deviceName = deviceName,
                        onBack = { showHistory = false },
                        context = LocalContext.current

                    )
                }

                showMyDevices -> {
                    MyDeviceScreen(
                        onBack = { showMyDevices = false },
                        email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                    )
                }

                imageDisplay -> {
                    DisplayNotificationImage(
                        imageUrl = imageUrl,
                        deviceName = deviceName,
                        onBack = { imageDisplay = false },
                        context = LocalContext.current
                    )
                }



                changeToken -> {
                    UpdateDeviceTokenScreen(
                        onBack = { changeToken = false },
                        deviceManager = deviceManager
                    )
                }

                forgotToken -> {
                    ForgotTokenScreen(
                        onBack = { forgotToken = false },
                        deviceManager = deviceManager
                    )
                }

                else -> {
                    // Main Screen Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF364559))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Welcome Message
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Welcome to ESP32Eye",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontFamily = poppinsFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your safety, our priority.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = poppinsFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Bottom Row with Icons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Notification Icon
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notification Icon",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { imageDisplay = true },
                                    tint = White
                                )

                                if (imageUrl != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .offset(x = 12.dp, y = (-4).dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                    )
                                }
                            }

                            // Add Device Icon
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Icon",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable {
                                        isRegistering = true
                                    },
                                tint = White
                            )

                            // Menu Icon
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = "Menu Icon",
                                    tint = White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        // Dropdown Menu
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    showHistory = true
                                },
                                text = { Text("History", fontFamily = poppinsFontFamily) }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    renameDevice = true
                                },
                                text = { Text("Rename Device", fontFamily = poppinsFontFamily) }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    changeToken = true
                                },
                                text = {
                                    Text(
                                        "Change Device Token",
                                        fontFamily = poppinsFontFamily
                                    )
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    forgotToken = true
                                },
                                text = { Text("Forgot Token", fontFamily = poppinsFontFamily) }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    showMyDevices = true
                                },
                                text = {
                                    Text(
                                        "My Devices",
                                        fontFamily = poppinsFontFamily
                                    )
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    FirebaseAuth.getInstance().signOut()
                                    onNavigateToSignIn()
                                },
                                text = { Text("Log Out", fontFamily = poppinsFontFamily) }
                            )
                        }
                    }
                }
            }
        }
    )
}
