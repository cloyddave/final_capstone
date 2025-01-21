package com.group5.safehomenotifier

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group5.safehomenotifier.ui.theme.CharcoalBlue
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily

@Composable
fun ForgotPasswordScreen(
    onPasswordReset: () -> Unit,
    navigateToSignIn: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiStates.collectAsState()
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlue)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Forgot Password",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = poppinsFontFamily,
                fontWeight = FontWeight.Normal,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Enter your email") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Success -> {
                    Text(
                        text = (uiState as UiState.Success).message,
                        color = Color.Green
                    )
                    LaunchedEffect(uiState) {
                        onPasswordReset()  // Trigger the password reset logic
                        navigateToSignIn()  // Navigate to sign-in screen after reset
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = (uiState as UiState.Error).message,
                        color = Color.Red
                    )
                }

                UiState.Idle -> {
                    Button(
                        onClick = {
                            // Check if email is not empty
                            if (email.isNotEmpty()) {
                                authViewModel.resetPassword(
                                    email = email,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Password reset email sent.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onPasswordReset()  // Reset completed, notify the caller
                                        navigateToSignIn()  // Navigate to sign-in screen
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Enter your email", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    ) {
                        Text("Send Reset Email", color = Black)
                    }
                }
            }
        }
    }
}
