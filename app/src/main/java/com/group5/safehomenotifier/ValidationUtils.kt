package com.group5.safehomenotifier


fun validatePassword(password: String): String {
    return when {
        password.length < 8 -> "Password must be at least 8 characters long."
        !password.any { it.isUpperCase() } -> "Include at least one uppercase letter."
        !password.any { it.isDigit() } -> "Include at least one number."
        !password.any { "!@#$%^&*()".contains(it) } -> "Include at least one special character."
        else -> ""
    }
}