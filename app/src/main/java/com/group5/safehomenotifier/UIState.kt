package com.group5.safehomenotifier

data class UIState(
    val deviceId: String = "",
    val token: String = "",
    val newToken: String = "", // For the new token input field
    val tokenError: String = "",
    val tokenVisible: Boolean = false,
    val deviceName: String = "",
    val updateStatus: String = "", // Status for token update or rename operation
    val deviceNameError: String = "",// Optional error message for device name if validation is needed
    val registrationStatus: String = "" // Status for device registration
)
