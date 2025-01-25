package com.group5.safehomenotifier

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Success(val message: String = "Success") : UiState()
    data class Error(val message: String) : UiState()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // UiState management
    private val _uiStates = MutableStateFlow<UiState>(UiState.Idle)
    val uiStates = _uiStates.asStateFlow()

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordError by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)

    fun signUp(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (passwordError.isEmpty()) {
            _uiStates.value = UiState.Loading
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        auth.currentUser?.sendEmailVerification()
                            ?.addOnCompleteListener { emailTask ->
                                if (emailTask.isSuccessful) {
                                    _uiStates.value = UiState.Success("Account created successfully. Please verify your email.")
                                    onSuccess()
                                } else {
                                    _uiStates.value = UiState.Error(emailTask.exception?.message ?: "Verification email error")
                                    onError(emailTask.exception?.message ?: "Verification email error")
                                }
                            }
                    } else {
                        _uiStates.value = UiState.Error(task.exception?.message ?: "Sign-up failed")
                        onError(task.exception?.message ?: "Sign-up failed")
                    }
                }
        }
    }

    fun signIn(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (passwordError.isEmpty()) {
            _uiStates.value = UiState.Loading
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user?.isEmailVerified == true) {
                            saveUserToFirestore(user.email!!)
                            _uiStates.value = UiState.Success("Welcome back!")
                            onSuccess()
                        } else {
                            _uiStates.value = UiState.Error("Please verify your email first.")
                            onError("Please verify your email first.")
                        }
                    } else {
                        _uiStates.value = UiState.Error(task.exception?.message ?: "Sign-in failed")
                        onError(task.exception?.message ?: "Sign-in failed")
                    }
                }
        }
    }

    fun saveUserToFirestore(email: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(email)

        val userData = hashMapOf(
            "email" to email,
            "devices" to mutableListOf<String>(),// List to store registered device IDs
        )

        userRef.set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "User data saved successfully")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error saving user data", e)
            }
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isNotEmpty()) {
            _uiStates.value = UiState.Loading
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _uiStates.value = UiState.Success("Password reset email sent.")
                        onSuccess()
                    } else {
                        _uiStates.value = UiState.Error(task.exception?.message ?: "Reset failed")
                        onError(task.exception?.message ?: "Reset failed")
                    }
                }
        } else {
            _uiStates.value = UiState.Error("Email cannot be empty")
            onError("Email cannot be empty")
        }
    }

}