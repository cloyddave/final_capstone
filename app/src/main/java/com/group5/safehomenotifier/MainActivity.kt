package com.group5.safehomenotifier

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.URL
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.lint.kotlin.metadata.Visibility
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.group5.safehomenotifier.ui.theme.CharcoalBlue
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff



class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val historyImages = mutableStateListOf<String>()


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val sharedPreferences = getSharedPreferences("SafeHomePrefs", MODE_PRIVATE)
        sharedPreferences.getBoolean("hasStarted", false)
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
            val navController = rememberNavController()
            val auth = FirebaseAuth.getInstance()
            val isUserLoggedIn = remember { mutableStateOf(auth.currentUser != null) }

            // Observe FirebaseAuth state
            LaunchedEffect(auth) {
                auth.addAuthStateListener { firebaseAuth ->
                    isUserLoggedIn.value = firebaseAuth.currentUser != null
                }
            }

            NavHost(
                navController = navController,
                startDestination = if (isUserLoggedIn.value) "main_screen" else "sign_in"
            ) {
                // Sign-In Screen
                composable("sign_in") {
                    SignInScreen(
                        onSignInSuccess = {
                            navController.navigate("main_screen") {
                                popUpTo("sign_in") { inclusive = true }
                            }
                        },
                        navigateToSignUp = {
                            navController.navigate("sign_up") // Navigate to sign-up screen
                        }
                    )
                }

                // Sign-Up Screen
                composable("sign_up") {
                    SignUpScreen(
                        onNavigateToSignIn = {
                            // Navigate to the sign-in screen after successful sign-up
                            navController.navigate("sign_in") {
                                popUpTo("sign_up") { inclusive = true }
                            }
                        },
                        navigateToSignIn = {
                            navController.navigate("sign_in") // Navigate to sign-in screen
                        }
                    )
                }


                // Main Screen
                composable("main_screen") {
                    SafeHomeNotifierApp(
                        historyImages = historyImages,
                        imageUrl = imageUrl,
                        onNavigateToSignIn = {
                            auth.signOut() // Log out the user
                            navController.navigate("sign_in") {
                                popUpTo("main_screen") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }


    }


    private fun setStarted() {
        val sharedPreferences = getSharedPreferences("SafeHomePrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("hasStarted", true)
            apply()
        }
    }


    private fun setRegistrationStatus(isRegistered: Boolean) {
        val sharedPreferences = getSharedPreferences("SafeHomePrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("isRegistered", isRegistered)
            apply()
        }
    }

    private fun isDeviceRegistered(): Boolean {
        val sharedPreferences = getSharedPreferences("SafeHomePrefs", MODE_PRIVATE)
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
    fun SignUpScreen(onNavigateToSignIn: () -> Unit, navigateToSignIn: () -> Unit) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf("") } // State for error message
        var passwordVisible by remember { mutableStateOf(false) } // State for password visibility
        val auth = FirebaseAuth.getInstance()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CharcoalBlue)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CharcoalBlue)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Sign Up",
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = poppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(15.dp))
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", fontFamily = poppinsFontFamily) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = validatePassword(it) // Update error state
                    },
                    label = { Text("Password", fontFamily = poppinsFontFamily) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )
                // Display password error
                if (passwordError.isNotEmpty()) {
                    Text(
                        passwordError,
                        color = Color.Red,
                        fontFamily = poppinsFontFamily,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (passwordError.isEmpty()) { // Proceed only if password is valid
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        saveUserToFirestore(user?.email!!)
                                        user.sendEmailVerification()
                                            .addOnCompleteListener { emailTask ->
                                                if (emailTask.isSuccessful) {
                                                    Toast.makeText(
                                                        auth.app.applicationContext,
                                                        "Sign-up successful! Please check your email to verify your account before logging in.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    onNavigateToSignIn()
                                                } else {
                                                    Toast.makeText(
                                                        auth.app.applicationContext,
                                                        "Error sending verification email: ${emailTask.exception?.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    } else {
                                        Toast.makeText(
                                            auth.app.applicationContext,
                                            "Sign-up failed: ${task.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    enabled = passwordError.isEmpty() // Disable button if error exists
                ) {
                    Text("Sign Up", fontFamily = poppinsFontFamily, color = Black)
                }

                ClickableText(
                    text = AnnotatedString.Builder().apply {
                        append("Already have an account?")
                        withStyle(
                            style = SpanStyle(
                                color = Color.Cyan,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(" Sign In")
                        }
                    }.toAnnotatedString(),
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.LightGray),
                    onClick = { offset ->
                        if (offset in "Already have an account? ".length until "Already have an account? Sign In".length) {
                            navigateToSignIn()
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }

    // Function to validate password complexity
    private fun validatePassword(password: String): String {
        return when {
            password.length < 8 -> "Password must be at least 8 characters long."
            !password.any { it.isUpperCase() } -> "Password must include at least one uppercase letter."
            !password.any { it.isDigit() } -> "Password must include at least one number."
            !password.any { "!@#$%^&*()-_=+[{]}|;:'\",<.>/?".contains(it) } -> "Password must include at least one special character."
            else -> ""
        }
    }

    @Composable
    fun SignInScreen(onSignInSuccess: () -> Unit, navigateToSignUp: () -> Unit) {
        var email by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) } // State for password visibility
        var passwordError by remember { mutableStateOf("") } // State for error message
        var password by remember { mutableStateOf("") }
        val auth = FirebaseAuth.getInstance()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CharcoalBlue)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CharcoalBlue)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Sign In",
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = poppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(15.dp))
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", fontFamily = poppinsFontFamily) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = validatePassword(it) // Update error state
                    },
                    label = { Text("Password", fontFamily = poppinsFontFamily) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )
                if (passwordError.isNotEmpty()) {
                    Text(
                        passwordError,
                        color = Color.Red,
                        fontFamily = poppinsFontFamily,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (passwordError.isEmpty()) {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        if (user?.isEmailVerified == true) {
                                            saveUserToFirestore(user.email!!)
                                            Toast.makeText(
                                                auth.app.applicationContext,
                                                "Sign-In Successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onSignInSuccess() // Proceed to the main screen
                                        } else {
                                            Toast.makeText(
                                                auth.app.applicationContext,
                                                "Please verify your email before signing in.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            auth.app.applicationContext,
                                            "Sign-in failed: ${task.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    enabled = passwordError.isEmpty() // Disable button if error exists
                ) {
                    Text("Sign In", fontFamily = poppinsFontFamily, color = Black)
                }
                Spacer(modifier = Modifier.height(8.dp))

                ClickableText(
                    text = AnnotatedString.Builder().apply {
                        append("Didn't already sign in? ")
                        withStyle(
                            style = SpanStyle(
                                color = Color.Cyan, // Highlight color for "Sign Up"
                                textDecoration = TextDecoration.Underline // Underline for "Sign Up"
                            )
                        ) {
                            append("Sign Up")
                        }
                    }.toAnnotatedString(),
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.LightGray),
                    onClick = { offset ->
                        if (offset in "Didn't already sign in? ".length until "Didn't already sign in? Sign Up".length) {
                            navigateToSignUp()
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }




    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @ExperimentalMaterial3Api
    @Composable
    fun SafeHomeNotifierApp(
        historyImages: MutableList<String>,
        imageUrl: String?,
        onNavigateToSignIn: () -> Unit,
    ) {
        var isRegistering by rememberSaveable { mutableStateOf(false) }
        //val isRegisteredSuccessfully by rememberSaveable { mutableStateOf(isDeviceRegistered()) }
        var showHistory by remember { mutableStateOf(false) }
        var imageDisplay by remember { mutableStateOf(false) }
        var renameDevice by remember { mutableStateOf(false) }
        var changeToken by remember { mutableStateOf(false) }
        var expanded by remember { mutableStateOf(false) }
        val deviceName = intent.getStringExtra("deviceName")

        Scaffold(
            content = {
                when {
                    isRegistering -> {
                        RegisterEsp32Screen(onBack = { isRegistering = false })
                    }

                    showHistory -> {
                        HistoryScreen(
                            historyImages = historyImages,
                            deviceName = deviceName,
                            onBack = { showHistory = false }
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

                    renameDevice -> {
                        RenameEsp32Screen(onBack = { renameDevice = false })
                    }

                    changeToken -> {
                        UpdateDeviceTokenScreen(onBack = { changeToken = false })
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
                                        color = Color.White,
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
                                        tint = Color.White
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
                                        .clickable { isRegistering = true },
                                    tint = Color.White
                                )

                                // Menu Icon
                                IconButton(onClick = { expanded = true }) {
                                    Icon(
                                        Icons.Filled.Menu,
                                        contentDescription = "Menu Icon",
                                        tint = Color.White,
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
                    "Add New Device",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = poppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = (Color.LightGray),
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
                    onValueChange = { token = it },
                    label = { Text("Input Device Token", fontFamily = poppinsFontFamily) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name", fontFamily = poppinsFontFamily) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val userEmail = FirebaseAuth.getInstance().currentUser?.email
                        if (userEmail != null) {
                            verifyDeviceCredentials(deviceId, token) { isValid ->
                                if (isValid) {
                                    registerEsp32Device(userEmail, token, deviceId, deviceName) { success ->
                                        registrationStatus =
                                            if (success) "Device registered successfully." else "Failed to register device."
                                    }
                                } else {
                                    registrationStatus = "Invalid device ID or token."
                                }
                            }
                        } else {
                            registrationStatus = "User not logged in."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray
                    )
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


    // Updated Logic to Register Device
    //Logic to register device
    private fun registerEsp32Device(
        email: String,
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

                    val userDeviceRef = db.collection("users")
                        .document(email)
                        .collection("devices")
                        .document(deviceId)

                    userDeviceRef.set(esp32Data)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Device added to user's devices collection: $deviceId")
                            onResult(true)
                        }
                        .addOnFailureListener { e: Exception ->
                            Log.w("Firestore", "Error adding device to user's devices collection", e)
                            onResult(false)
                        }
                }
                .addOnFailureListener { e: Exception ->
                    Log.w("Firestore", "Error registering device", e)
                    onResult(false)
                }
        }
    }



    @Composable
    fun UpdateDeviceTokenScreen(onBack: () -> Unit) {
        var deviceId by remember { mutableStateOf("") }
        var token by remember { mutableStateOf("") }
        var newToken by remember { mutableStateOf("") }
        var updateStatus by remember { mutableStateOf("") }

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
                    onValueChange = { newToken = it },
                    label = { Text("New Device Token", fontFamily = poppinsFontFamily) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        updateDeviceToken(deviceId, token, newToken) { success ->
                            updateStatus =
                                if (success) "Token updated successfully." else "Failed to update token."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
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

    // Function to update only the token
    private fun updateDeviceToken(
        deviceId: String,
        token: String,
        newToken: String,
        onResult: (Boolean) -> Unit
    ) {
        val deviceRef = db.collection("devices").document(deviceId)

        deviceRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentData = document.data ?: hashMapOf<String, Any>()
                    val updatedData = currentData.toMutableMap().apply {
                        this["token"] = newToken
                    }
                    deviceRef.set(updatedData)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Token updated successfully for device: $deviceId")
                            onResult(true)
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error updating token", e)
                            onResult(false)
                        }
                } else {
                    Log.w("Firestore", "Device not found: $deviceId")
                    onResult(false)
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching device data", e)
                onResult(false)
            }
    }


    @Composable
    fun HistoryScreen(historyImages: List<String>, deviceName: String?, onBack: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CharcoalBlue)
                .padding(16.dp), // Add padding to the Box if desired
            contentAlignment = Alignment.Center // Center the contents
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = White // Customize color if needed
                        )
                    }
                    Text(
                        "History of Images:",
                        fontFamily = poppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = White
                    )
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
    }

    //Verify or check credential to Firebase firestore

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
                    onValueChange = { token = it },
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


    //Image Item received by application
    @Composable
    fun DisplayNotificationImage(
        imageUrl: String?,
        deviceName: String?,
        onBack: () -> Unit,
        context: Context
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CharcoalBlue)
                .padding(16.dp), // Add padding to the Box if desired
            contentAlignment = Alignment.Center // Center the contents
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = AbsoluteAlignment.Left,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = White // Customize color if needed
                    )
                }
                Text(
                    "Notification",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = poppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = (Color.LightGray),
                    textAlign = TextAlign.Center
                )
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
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            downloadImageUsingMediaStore(context, imageUrl)
                        }
                    }) {
                        Text("Download", fontFamily = poppinsFontFamily)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Image not available",
                                fontFamily = poppinsFontFamily,
                                color = White
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun downloadImageUsingMediaStore(context: Context, imageUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                // Download the image as a Bitmap
                val bitmap = BitmapFactory.decodeStream(URL(imageUrl).openStream())

                // Prepare the content values
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "downloaded_image.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/SafeHomeNotifier"
                    )
                }

                // Insert the image into MediaStore
                val resolver = context.contentResolver
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { outputStream: OutputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Image downloaded successfully", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to download image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}