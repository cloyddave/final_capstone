package com.group5.safehomenotifier

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import androidx.compose.ui.tooling.preview.Preview
import com.group5.safehomenotifier.ui.theme.PushNotificationTestTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUrl = intent.getStringExtra("imageUrl")

        requestNotificationPermission()
        setContent {
            MainScreen()
            DisplayNotificationImage(imageUrl)
            RegisterEsp32Screen { token, deviceId ->
                registerEsp32Device(token, deviceId)
            }
            PushNotificationTestTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val scope = rememberCoroutineScope()
                    val token = remember {
                        mutableStateOf("")
                    }
                    LaunchedEffect(key1 = Unit) {
                        scope.launch {
                            token.value = Firebase.messaging.token.await()
                        }
                    }

                    if (imageUrl.isNullOrEmpty()) {
                        SelectionContainer(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .align(Alignment.Center)
                        ) {
                            Text(text = token.value)
                        }
                    } else {
                        DisplayNotificationImage(imageUrl = imageUrl)
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }
    }

    private fun registerEsp32Device(token: String, deviceId: String) {
        // Create a map with device data
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("Firebase", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM token
            val fcmToken = task.result

            val esp32Data = hashMapOf(
                "device_id" to deviceId,
                "token" to token,
                "registration_time" to System.currentTimeMillis(),
                "status" to "active",
                "fcmToken" to fcmToken
            )
            // Use deviceId as the document ID for easier retrieval

            // Add the device data to Firestore
            db.collection("devices").document(deviceId).set(esp32Data)
                .addOnSuccessListener {
                    Log.d("Firestore", "Device registered successfully: $deviceId")
                }
                .addOnFailureListener { e: Exception ->
                    Log.w("Firestore", "Error registering device", e)
                }
        }
    }


    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @ExperimentalMaterial3Api
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(
                                imageVector = Icons.Default.Menu, contentDescription = "Menu Icon"
                            )
                        }
                    },
                    title = {
                        Text(text = "Esp32Eye")
                    }

                )
            },
            content = {

            }

        )


    }

    @Composable
    fun DisplayNotificationImage(imageUrl: String?) {
        if (imageUrl != null) {
            val painter = rememberAsyncImagePainter(imageUrl)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hurry! Your house is on fire!",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painter,
                    contentDescription = "Hurry! Your house is on fire!",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Text(text = "No ")
        }
    }

    @Composable
    fun RegisterEsp32Screen(onRegister: (String, String) -> Unit) {
        var deviceId by remember { mutableStateOf("") }
        var token by remember { mutableStateOf("") }
        
        Column(modifier = Modifier.padding(16.dp)) {
            TextField(
                value = deviceId,
                onValueChange = { deviceId = it },
                label = { Text("Device ID") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Input Device Token") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onRegister(token, deviceId) }) {
                Text("Register Device")
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun PreviewRegisterEsp32Screen() {
        RegisterEsp32Screen { _, _ -> }
    }
}



