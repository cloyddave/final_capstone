package com.group5.safehomenotifier

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.firebase.functions.FirebaseFunctions
import androidx.compose.ui.graphics.Color
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val historyImages = mutableStateListOf<String>()


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val sharedPreferences = getSharedPreferences("SafeHomePrefs", Context.MODE_PRIVATE)
        val hasStarted = sharedPreferences.getBoolean("hasStarted", false)
        sharedPreferences.getBoolean("isRegistered", false)


        val imageUrl = intent.getStringExtra("imageUrl")
        imageUrl?.let {
            // Add new images to the history list without resetting the state
            if (!historyImages.contains(it)) {
                historyImages.add(it)
            }
        }

        requestNotificationPermission()

        setContent {
            if (auth.currentUser != null) {
            SafeHomeNotifierApp(
                historyImages = historyImages, hasStarted = hasStarted,
                imageUrl = imageUrl
            )
        } else{
                SignInScreen(onSignInSuccess = {
                    // Navigate to main screen upon successful sign-in
                    recreate()
                })

            }        }
    }

    private fun setStarted() {
        val sharedPreferences = getSharedPreferences("SafeHomePrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("hasStarted", true)
            apply()
        }
    }



    private fun setRegistrationStatus(isRegistered: Boolean) {
        val sharedPreferences = getSharedPreferences("SafeHomePrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("isRegistered", isRegistered)
            apply()
        }
    }

    private fun isDeviceRegistered(): Boolean {
        val sharedPreferences = getSharedPreferences("SafeHomePrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isRegistered", false)
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

    @Composable
    fun SignInScreen(onSignInSuccess: () -> Unit) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                //visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // After successful sign-in, save the user info in Firestore (optional)
                                val user = auth.currentUser
                                val userEmail = user?.email
                                if (userEmail != null) {
                                    saveUserToFirestore(userEmail)
                                }
                                Toast.makeText(
                                    auth.app.applicationContext,
                                    "Sign-in successful",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSignInSuccess() // Proceed to main screen
                            } else {
                                Toast.makeText(
                                    auth.app.applicationContext,
                                    "Sign-in failed: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            ) {
                Text("Sign In")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    auth.app.applicationContext,
                                    "Sign-up successful",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSignInSuccess() // Proceed to main screen
                            } else {
                                Toast.makeText(
                                    auth.app.applicationContext,
                                    "Sign-up failed: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            ) {
                Text("Register")
            }
        }
    }

    fun saveUserToFirestore(email: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(email)

        val userData = hashMapOf(
            "email" to email,
            "devices" to mutableListOf<String>() // List to store registered device IDs
        )

        userRef.set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "User data saved successfully")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error saving user data", e)
            }
    }


    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @ExperimentalMaterial3Api
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SafeHomeNotifierApp(
        historyImages: MutableList<String>,
        hasStarted: Boolean,
        imageUrl: String?
    ) {
        val isStarted by rememberSaveable { mutableStateOf(hasStarted) }
        var isRegistering by rememberSaveable { mutableStateOf(false) }
        val isRegisteredSuccessfully by rememberSaveable { mutableStateOf(isDeviceRegistered()) }
        var showHistory by remember { mutableStateOf(false) }
        var imageDisplay by remember { mutableStateOf(false) }
        var renameDevice by remember { mutableStateOf(false) }
        val deviceName = intent.getStringExtra("deviceName")



        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Esp32Eye", color = Color.White) },
                    actions = {
                        if (isStarted && !isRegisteredSuccessfully) {
                            IconButton(onClick = { isRegistering = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Device")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF7B7D7E)) // Purple background
                )
            },
            content = { _ ->
                when {
                    isRegistering -> {
                        RegisterEsp32Screen(
                            onBack = { isRegistering = false }
                        )
                    }






                    showHistory -> {
                        HistoryScreen(
                            historyImages,
                            deviceName = deviceName,
                            onBack = { showHistory = false }
                        )
                        // Go back to main when back is clicked
                    }

                    imageDisplay -> {
                        DisplayNotificationImage(
                            imageUrl = imageUrl,
                            deviceName = deviceName,
                            onBack = { imageDisplay = false }
                        )
                        // Go back to main when back is clicked
                    }

                    renameDevice -> {
                        RenameEsp32Screen(
                            onBack = { renameDevice = false }
                        )
                    }


                    isRegisteredSuccessfully -> {
                        // Navigate to MainScreen once registered successfully
                        MainScreen(
                            isRegisteredSuccessfully,
                            onAddDevice = {
                                isRegistering = true
                            },
                            onShowHistory = { showHistory = true },
                            onDisplay = { imageDisplay = true },
                            onRenameDevice = {renameDevice = true}

                        )
                    }

                    else -> {
                        MainScreen(
                            isRegisteredSuccessfully,
                            onAddDevice = {
                                isRegistering = true
                            },
                            onShowHistory = { showHistory = true },
                            onDisplay = { imageDisplay = false },
                            onRenameDevice = {renameDevice = true}

                        )
                    }
                }
            }
        )
    }

    @SuppressLint("NewApi")
    @Composable
    fun MainScreen(
        isRegisteredSuccessfully: Boolean,
        onAddDevice: () -> Unit,
        onShowHistory: () -> Unit,
        onDisplay: () -> Unit,
        onRenameDevice: () -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween // This will space content evenly
            ) {
                // Main content
                if (isRegisteredSuccessfully) {
                    // Centering the welcome message
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f), // Use weight to allow this box to take up remaining space
                        contentAlignment = Alignment.Center // Center the content vertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally // Center horizontally within the Column
                        ) {
                            Text(
                                text = "Welcome to ESP32Eye",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center // Center the text
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your safety, our priority.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center // Center the text
                            )
                        }
                    }

                    // Bottom Row with Home, Notification, and Menu icons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly // Space evenly between icons
                    ) {
                        // Notification Icon
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Icon",
                            modifier = Modifier
                                .size(45.dp)
                                .clickable {
                                    onAddDevice()
                                },
                            tint = Color.Black
                        )

                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notification Icon",
                            modifier = Modifier
                                .size(45.dp)
                                .clickable {
                                    onDisplay()
                                },
                            tint = Color.Black
                        )

                        // Menu Icon for dropdown
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Menu Icon",
                                modifier = Modifier.size(45.dp)
                            )
                        }
                    }

                    // Dropdown menu
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                onShowHistory()
                            },
                            text = { Text("History") }
                        )

                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                onRenameDevice()  // Trigger the function to rename registered device
                            },
                            text = { Text("Rename Device") }
                        )
                    }
                } else {
                    // Optionally, you can display a message indicating that registration is needed
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Please register your device to access the main features.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }


    @Composable
    fun HistoryScreen(historyImages: List<String>, deviceName: String?, onBack: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = { onBack() }) {
                Text("Back to Main")
            }

            Column(modifier = Modifier.padding(16.dp)) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Gray // Customize color if needed
                    )
                }
                Text("History of Images:")
                Spacer(modifier = Modifier.height(16.dp))

                if (deviceName != null) {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                LazyColumn {
                    items(historyImages) { imageUrl ->
                        val painter = rememberAsyncImagePainter(imageUrl)
                        Image(
                            painter = painter,
                            contentDescription = "Historical image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

            }
        }
    }

//Verify or check credential to Firebase firestore
    private fun verifyDeviceCredentials(
        deviceId: String,
        token: String,
        onResult: (Boolean) -> Unit
    ) {
        val functions = FirebaseFunctions.getInstance()

        // Call the Firebase function 'checkDeviceCredentials'
        functions
            .getHttpsCallable("checkDeviceCredentials") // Name of your Firebase Cloud Function
            .call(hashMapOf("device_id" to deviceId, "token" to token))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result?.data as? Map<*, *>
                    val status = result?.get("status") as? String
                    onResult(status == "success")
                } else {
                    Log.e("Firebase", "Error calling the function", task.exception)
                    onResult(false)
                }
            }
    }
 //Registration User Interface(UI)
    @Composable
    fun RegisterEsp32Screen(onBack: () -> Unit) {
        var deviceId by remember { mutableStateOf("") }
        var token by remember { mutableStateOf("") }
        var deviceName by remember { mutableStateOf("") }
        var registrationStatus by remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add padding to the Box if desired
            contentAlignment = Alignment.Center // Center the contents
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Gray // Customize color if needed
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                TextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClick@{
                    verifyDeviceCredentials(deviceId, token) { isValid ->
                        if (isValid) {
                            registerEsp32Device(token, deviceId, deviceName) { success ->
                                registrationStatus =
                                    if (success) "Device registered successfully." else "Failed to register device."
                            }
                        } else {
                            registrationStatus = "Invalid device ID or token."
                        }
                    }
                })

                {
                    Text("Register Device")
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
 //Rename Screen User Interface
    @Composable
    fun RenameEsp32Screen(onBack: () -> Unit) {
        var deviceId by remember { mutableStateOf("") }
        var token by remember { mutableStateOf("") }
        var deviceName by remember { mutableStateOf("") }
        var registrationStatus by remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add padding to the Box if desired
            contentAlignment = Alignment.Center // Center the contents
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Gray // Customize color if needed
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                TextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("What name you will be assign?") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClick@{
                    verifyDeviceCredentials(deviceId, token) { isValid ->
                        if (isValid) {
                            renameEsp32Device(token, deviceId, deviceName) { success ->
                                registrationStatus =
                                    if (success) "Device renamed successfully." else "Failed to rename device."
                            }
                        } else {
                            registrationStatus = "Invalid device ID or token."
                        }
                    }
                })

                {
                    Text("Rename Device")
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
//Logic to rename device
    private fun renameEsp32Device(
        token: String,
        deviceId: String,
        deviceName: Any?,
        onResult: (Boolean) -> Unit
    ) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val fcmToken = task.result
            val esp32Data = hashMapOf(
                "device_id" to deviceId,
                "token" to token,
                "registration_time" to System.currentTimeMillis(),
                "deviceName" to deviceName,
                "fcmToken" to fcmToken,

            )

            db.collection("devices").document(deviceId).set(esp32Data)
                .addOnSuccessListener {
                    Log.d("Firestore", "Device registered successfully: $deviceId")
                    onResult(true)
                }
                .addOnFailureListener { e: Exception ->
                    Log.w("Firestore", "Error registering device", e)
                    onResult(false)
                }
        }
    }

//Logic to register device
    private fun registerEsp32Device(
        token: String,
        deviceId: String,
        deviceName: Any?,
        onResult: (Boolean) -> Unit
    ) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("Firebase", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val fcmToken = task.result
            val esp32Data = hashMapOf(
                "device_id" to deviceId,
                "token" to token,
                "registration_time" to System.currentTimeMillis(),
                "status" to "active",
                "fcmToken" to fcmToken,
                "deviceName" to deviceName
            )

            db.collection("devices").document(deviceId).set(esp32Data)
                .addOnSuccessListener {
                    Log.d("Firestore", "Device registered successfully: $deviceId")
                    onResult(true)
                }
                .addOnFailureListener { e: Exception ->
                    Log.w("Firestore", "Error registering device", e)
                    onResult(false)
                }
        }
    }
}
//Image Item received by application
@Composable
fun DisplayNotificationImage(imageUrl: String?, deviceName: String?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = { onBack() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Gray // Customize color if needed
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (deviceName != null) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (imageUrl != null) {
            val painter = rememberAsyncImagePainter(imageUrl)
            Image(
                painter = painter,
                contentDescription = "Hurry! Your house is on fire!",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Image not available")
            }
        }
    }
}
