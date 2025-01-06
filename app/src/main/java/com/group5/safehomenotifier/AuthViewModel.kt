package com.group5.safehomenotifier

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
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
