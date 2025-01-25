package com.group5.safehomenotifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.group5.safehomenotifier.ui.theme.Typography


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
              SafeHomeNotifierApp(
                  historyImages = historyImages,
                  imageUrl = imageUrl,
                  onNavigateToSignIn = {
                      auth.signOut()
                  },
                  deviceManager = deviceManager
              )
                AppNavigation(
                    imageUrl = imageUrl,
                    historyImages = historyImages,
                    deviceManager = deviceManager
                )
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
}