package com.group5.safehomenotifier

import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    navigateToSignUp: () -> Unit,
    navigateToForgotPassword: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiStates.collectAsState()
    val context = LocalContext.current // Get the context to show the Toast

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign In", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Email TextField
        TextField(
            value = authViewModel.email,
            onValueChange = { authViewModel.email = it },
            label = { Text("Email") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password TextField
        PasswordTextField(
            password = authViewModel.password,
            onPasswordChange = {
                authViewModel.password = it
                authViewModel.passwordError = validatePassword(it)
            },
            passwordVisible = authViewModel.passwordVisible,
            onToggleVisibility = { authViewModel.passwordVisible = !authViewModel.passwordVisible },
            error = authViewModel.passwordError
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Handle UI state (loading, success, error)
        when (uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator() // Show loading spinner while authenticating
            }

            is UiState.Success -> {
                // Show Toast message when sign-in is successful
                LaunchedEffect(uiState) {
                    Toast.makeText(
                        context,
                        "Sign-In Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    onSignInSuccess() // Navigate to the next screen upon success
                }
            }

            is UiState.Error -> {
                // Show error message but ensure the buttons are still visible
                Text(
                    text = (uiState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp)) // Add space to prevent pushing the button out of view
            }

            UiState.Idle -> {
                // This will be the default state with the "Sign In" button visible
            }
        }

        // Display the "Sign In" button when idle or after success/error is handled
        Button(onClick = {
            if (authViewModel.email.isBlank() || authViewModel.password.isBlank()) {
                // Show a Toast message if fields are empty
                if (authViewModel.email.isBlank()) {
                    Toast.makeText(context, "Email can't be left blank", Toast.LENGTH_SHORT).show()
                }
                if (authViewModel.password.isBlank()) {
                    Toast.makeText(context, "Password can't be left blank", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Proceed with sign-in logic if fields are not empty
                authViewModel.signIn(
                    onSuccess = { onSignInSuccess() },
                    onError = { /* Error handled by UiState */ }
                )
            }
        },
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
        ) {
            Text("Sign In", color = Black)
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up and Forgot Password navigation buttons
        TextButton(onClick = navigateToSignUp) {
            Text("Don't have an account? Sign Up")
        }

        TextButton(onClick = navigateToForgotPassword) {
            Text("Forgot Password?")
        }
    }
}
