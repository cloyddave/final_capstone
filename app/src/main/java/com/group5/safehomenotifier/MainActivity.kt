package com.group5.safehomenotifier

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.group5.safehomenotifier.DeviceRepository.fetchUserDevices
import com.group5.safehomenotifier.ui.theme.CharcoalBlue
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val functions = FirebaseFunctions.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var deviceManager = DeviceManager(db, functions)
    private val auth = FirebaseAuth.getInstance()
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
            MaterialTheme {
                AppNavigation(
                    imageUrl = imageUrl
                )
            }
        }


    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppNavigation(imageUrl: String?){

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
                        navController.navigate("sign_up")
                        // Navigate to sign-up screen
                    },
                    navigateToForgotPassword = {
                        navController.navigate("forgot_password")
                    }
                )
            }

            composable("forgot_password"){
                ForgotPasswordScreen(
                    onPasswordReset = {
                        navController.navigate("sign_in") {
                            popUpTo("forgot_password") { inclusive = true }
                        }
                    },
                    navigateToSignIn = {
                        navController.navigate("sign_in")
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
        var forgotToken by remember { mutableStateOf(false) }
        var expanded by remember { mutableStateOf(false) }
        val deviceName = intent.getStringExtra("deviceName")
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
        fun HistoryScreen(
            historyImages: List<String>, deviceName: String?, onBack: () -> Unit, context: Context,
                         ) {

            val database = HistoryDatabase.getDatabase(context)
            val historyDao = remember { database.historyImageDao() }
            var imageList by remember { mutableStateOf(listOf<HistoryImage>()) }
            //val context = LocalContext.current

            LaunchedEffect(Unit) {
                imageList = withContext(Dispatchers.IO) {
                    historyDao.getAllImages()
                }
            }


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

                        LazyColumn {
                            items(imageList) { image ->
                                val painter = rememberAsyncImagePainter(image.imageUrl)
                                val readableDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(image.timestamp))

                                Column(modifier = Modifier.padding(8.dp)) {
                                    Image(
                                        painter = painter,
                                        contentDescription = "Historical image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Received at: $readableDate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Delete button
                                    Button(
                                        onClick = {
                                            // Perform the delete operation
                                            deleteImageAndUpdateList(image, historyDao, context)
                                            { updatedList ->
                                                imageList = updatedList
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Text(
                                            text = "Delete",
                                            color = White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun deleteImageAndUpdateList(
        image: HistoryImage,
        historyDao: HistoryImageDao,
        context: Context,
        updateImageList: (List<HistoryImage>) -> Unit
    ) {
        // Perform the delete operation in a background thread
        CoroutineScope(Dispatchers.IO).launch {
            historyDao.deleteImage(image)

            // Once deleted, update the image list on the main thread
            val updatedList = historyDao.getAllImages()
            withContext(Dispatchers.Main) {
                // Update the UI state in a Composable way
                updateImageList(updatedList)
                Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }



    //Verify or check credential to Firebase firestore

    //Rename Screen User Interface

    //Image Item received by application
    @Composable
    fun DisplayNotificationImage(
        imageUrl: String?,
        deviceName: String?,
        onBack: () -> Unit,
        context: Context
    ) {
        val currentDateTime = remember {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        }

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
                        color = White,
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
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Received at: $currentDateTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            downloadImageUsingMediaStore(context, imageUrl)
                        }
                    }) {
                        Text("Download", fontFamily = poppinsFontFamily)
                    }
                    Button(onClick = {
                        saveImageToHistory(context, imageUrl)
                    }) {
                        Text("Save to History")
                    }

                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "No smoke or flame detected.",
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