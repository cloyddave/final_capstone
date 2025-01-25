package com.group5.safehomenotifier

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
        val context = LocalContext.current
        val navController = rememberNavController()
        val auth = FirebaseAuth.getInstance()
        val isUserLoggedIn = auth.currentUser != null
        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val selectedRoleName = sharedPreferences.getString("USER_ROLE", null)
        val selectedRole = if (selectedRoleName != null) UserRole.valueOf(selectedRoleName) else null



    NavHost(
        navController = navController,
        startDestination = when {
            !isUserLoggedIn -> "sign_in"
            isUserLoggedIn && selectedRole != null -> "main_screen"
            else -> "role_selection"
        }
    ) {
            composable("sign_in") {
                SignInScreen(
                    onSignInSuccess = {
                        if (selectedRole != null) {
                            navController.navigate("main_screen") {
                                popUpTo("sign_in") { inclusive = true }
                            }
                        } else {
                            navController.navigate("role_selection") {
                                popUpTo("sign_in") { inclusive = true }
                            }
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

            composable("role_selection") {
                RoleSelectionScreen(navController = navController, context = context)
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


