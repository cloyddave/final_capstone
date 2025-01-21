package com.group5.safehomenotifier

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object DeviceRepository {
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance() // Lazily retrieves the instance

    fun fetchUserDevices(
        email: String,
        onResult: (List<Map<String, String>>) -> Unit
    ) {
        db.collection("users").document(email).collection("devices")
            .get()
            .addOnSuccessListener { result ->
                val devices = result.map { document ->
                    document.data.mapValues { it.value.toString() }
                }
                onResult(devices)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching devices", e)
                onResult(emptyList())
            }
    }
}

