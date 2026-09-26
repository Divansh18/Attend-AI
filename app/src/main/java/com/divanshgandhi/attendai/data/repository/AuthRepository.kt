package com.divanshgandhi.attendai.data.repository

import com.divanshgandhi.attendai.model.UserRole
import com.divanshgandhi.attendai.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Demo authentication only. Sessions intentionally end when the process ends. */
class AuthRepository {
    private val mutableSession = MutableStateFlow<UserSession?>(null)
    val session = mutableSession.asStateFlow()

    // Suspend leaves room for a local staff lookup in a later milestone.
    suspend fun signIn(role: UserRole, username: String, password: String): AuthResult {
        if (role == UserRole.STAFF) return AuthResult.StaffUnavailable
        if (username != "admin" || password != "admin123") {
            return AuthResult.InvalidCredentials
        }
        mutableSession.value = UserSession.Admin
        return AuthResult.Success
    }

    fun signOut() {
        mutableSession.value = null
    }
}

sealed interface AuthResult {
    data object Success : AuthResult
    data object InvalidCredentials : AuthResult
    data object StaffUnavailable : AuthResult
}
