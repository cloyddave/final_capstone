package com.group5.safehomenotifier

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)

    @Composable
    fun AppNavigation(
        imageUrl: String?,
        historyImages: MutableList<String>,
        deviceManager: DeviceManager
    ) {
        val navController = rememberNavController()
        val auth = FirebaseAuth.getInstance()
        val isUserLoggedIn = auth.currentUser != null

        NavHost(
            navController = navController,
            startDestination = if (isUserLoggedIn) "main_screen" else "sign_in"
        ) {
            composable("sign_in") {
                SignInScreen(
                    onSignInSuccess = {
                        navController.navigate("main_screen") {
                            popUpTo("sign_in") { inclusive = true }
                        }
                    },
                    navigateToSignUp = {
                        navController.navigate("sign_up")
                    },
                    navigateToForgotPassword = {
                        navController.navigate("forgot_password")
                    }
                )
            }

            composable("forgot_password") {
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

            composable("sign_up") {
                SignUpScreen(
                    onNavigateToSignIn = {
                        navController.navigate("sign_in") {
                            popUpTo("sign_up") { inclusive = true }
                        }
                    },
                    navigateToSignIn = {
                        navController.navigate("sign_in")
                    }
                )
            }

            composable("main_screen") {
                SafeHomeNotifierApp(
                    historyImages = historyImages,
                    imageUrl = imageUrl,
                    onNavigateToSignIn = {
                        auth.signOut() // Log out the user
                        navController.navigate("sign_in") {
                            popUpTo("main_screen") { inclusive = true }
                        }
                    },
                    deviceManager = deviceManager
                )
            }
        }
}
