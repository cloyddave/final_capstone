package com.group5.safehomenotifier

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging


class DeviceManager(private val db: FirebaseFirestore, private val functions: FirebaseFunctions) {

    fun verifyDeviceCredentials(deviceId: String, token: String, onResult: (Boolean) -> Unit) {
        functions
            .getHttpsCallable("checkDeviceCredentials")
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

    fun registerDevice(email: String, token: String, deviceId:String, deviceName: Any?, onResult: (Boolean) -> Unit) {
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
                    Log.d("Firestore", "Device registered successfully: ${deviceId}")
                    val userDeviceRef = db.collection("users")
                        .document(email)
                        .collection("devices")
                        .document(deviceId)

                    userDeviceRef.set(esp32Data)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Device added to user's devices collection: ${deviceId}")
                            onResult(true)
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error adding device to user's devices collection", e)
                            onResult(false)
                        }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error registering device", e)
                    onResult(false)
                }
        }
    }

    fun renameEsp32Device(
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
                    Log.d("Firestore", "Device renamed successfully: $deviceId")
                    onResult(true)
                }
                .addOnFailureListener { e: Exception ->
                    Log.w("Firestore", "Error renaming device", e)
                    onResult(false)
                }
        }
    }


    fun updateDeviceToken(deviceId: String, enteredToken: String, newToken: String, onResult: (Boolean) -> Unit) {
        val deviceRef = db.collection("devices").document(deviceId)

        deviceRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentData = document.data ?: hashMapOf<String, Any>()
                    val storedToken = currentData["token"] as? String

                    if (storedToken != null && storedToken == enteredToken) {
                        // Update token only if entered token matches the stored token
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
                        // The entered token does not match the stored token
                        Log.w("Firestore", "Incorrect current token for device: $deviceId")
                        onResult(false)
                    }
                } else {
                    // Document does not exist
                    Log.w("Firestore", "Device ID not found: $deviceId")
                    onResult(false)
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching device data", e)
                onResult(false)
            }
    }

    fun forgotToken(email: String, deviceId: String, onResult: (String?, Boolean) -> Unit) {
        val userRef = db.collection("users").document(email)

        userRef.collection("devices").document(deviceId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val token = document.getString("token")
                    if (!token.isNullOrEmpty()) {
                        Log.d("Firestore", "Token retrieved for device: $deviceId")
                        onResult(token, true)
                    } else {
                        Log.w("Firestore", "No token found for device: $deviceId")
                        onResult(null, false)
                    }
                } else {
                    Log.w("Firestore", "Device ID not found under email: $email")
                    onResult(null, false)
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching token for device: $deviceId", e)
                onResult(null, false)
            }
    }



}