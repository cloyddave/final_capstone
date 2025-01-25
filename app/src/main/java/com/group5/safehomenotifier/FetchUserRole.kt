package com.group5.safehomenotifier

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

fun fetchUserRole(userEmail: String, context: Context, onRoleFetched: (UserRole) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    // Check if the email is not null or empty
    if (userEmail.isNotEmpty()) {
        firestore.collection("users")
            .whereEqualTo("email", userEmail) // Query based on email
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0] // Get the first document from the query
                    val role = document.getString("role")
                    when (role) {
                        "OWNER" -> onRoleFetched(UserRole.OWNER)
                        "VIEWER" -> onRoleFetched(UserRole.VIEWER)
                        else -> onRoleFetched(UserRole.VIEWER) // Default to viewer if role is missing or invalid
                    }
                } else {
                    onRoleFetched(UserRole.VIEWER) // Default to viewer if no document is found
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure to fetch role
                Toast.makeText(context, "Error fetching role: ${exception.message}", Toast.LENGTH_LONG).show()
                onRoleFetched(UserRole.VIEWER) // Default to viewer if error occurs
            }
    }
}
