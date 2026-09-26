package com.divanshgandhi.attendai.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divanshgandhi.attendai.data.repository.AuthRepository
import com.divanshgandhi.attendai.data.repository.AuthResult
import com.divanshgandhi.attendai.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState = mutableUiState.asStateFlow()

    fun onRoleChange(role: UserRole) {
        if (uiState.value.isSubmitting || uiState.value.role == role) return
        mutableUiState.value = LoginUiState(role = role)
    }

    fun onUsernameChange(username: String) {
        if (uiState.value.isSubmitting) return
        mutableUiState.update {
            it.copy(username = username, isUsernameMissing = false, error = null)
        }
    }

    fun onPasswordChange(password: String) {
        if (uiState.value.isSubmitting) return
        mutableUiState.update {
            it.copy(password = password, isPasswordMissing = false, error = null)
        }
    }

    fun signIn() {
        val state = uiState.value
        if (state.isSubmitting) return

        val username = state.username.trim()
        val usernameMissing = username.isEmpty()
        val passwordMissing = state.password.isBlank()
        mutableUiState.update {
            it.copy(
                isUsernameMissing = usernameMissing,
                isPasswordMissing = passwordMissing,
                error = null,
            )
        }
        if (usernameMissing || passwordMissing) return

        mutableUiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.signIn(state.role, username, state.password)
            mutableUiState.update {
                it.copy(
                    isSubmitting = false,
                    password = if (result == AuthResult.Success) "" else it.password,
                    error = when (result) {
                        AuthResult.Success -> null
                        AuthResult.InvalidCredentials -> LoginError.INVALID_CREDENTIALS
                        AuthResult.StaffUnavailable -> LoginError.STAFF_UNAVAILABLE
                    },
                )
            }
        }
    }
}
