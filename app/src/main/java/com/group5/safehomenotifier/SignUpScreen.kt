package com.group5.safehomenotifier

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignUpScreen(
    onNavigateToSignIn: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    navigateToSignIn: () -> Unit
) {
    val uiState by authViewModel.uiStates.collectAsState()
    val context = LocalContext.current  // Get the context for showing Toast

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign Up", style = MaterialTheme.typography.headlineMedium)

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
                CircularProgressIndicator() // Show loading spinner while signing up
            }

            is UiState.Success -> {
                // Show Toast message when sign-in is successful
                LaunchedEffect(uiState) {
                    Toast.makeText(
                        context,
                        "Sign-up successful. Please check your email to verify your account.",
                        Toast.LENGTH_LONG
                    ).show()
                    kotlinx.coroutines.delay(2000)
                    onNavigateToSignIn() // Navigate to the next screen upon success
                }
            }

            is UiState.Error -> {
                // Show error message but ensure the button stays visible
                Text(
                    text = (uiState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                // Add a spacer to ensure the error doesn't push the button out of view
                Spacer(modifier = Modifier.height(16.dp))
            }

            UiState.Idle -> {
                // Display the "Sign Up" button when idle
            }
        }

        // Display the "Sign Up" button when idle or after success/error is handled
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
                authViewModel.signUp(
                    onSuccess = { onNavigateToSignIn() },
                    onError = { /* Error handled by UiState */ }
                )
            }
        },
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
            ) {
            Text("Sign Up", color = Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign In navigation button
        TextButton(onClick = navigateToSignIn) {
            Text("Already have an account? Sign In")
        }
    }
}
