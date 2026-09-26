package com.divanshgandhi.attendai.ui.login

import com.divanshgandhi.attendai.model.UserRole

data class LoginUiState(
    val role: UserRole = UserRole.ADMIN,
    val username: String = "",
    val password: String = "",
    val isUsernameMissing: Boolean = false,
    val isPasswordMissing: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: LoginError? = null,
)

enum class LoginError { INVALID_CREDENTIALS, STAFF_UNAVAILABLE }
