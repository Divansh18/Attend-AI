package com.divanshgandhi.attendai.data.repository

import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.model.UserRole
import com.divanshgandhi.attendai.model.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Demo-only shared passwords. Sessions intentionally end when the process ends. */
class AuthRepository(private val findStaff: suspend (String) -> StaffEntity? = { null }) {
    private val mutableSession = MutableStateFlow<UserSession?>(null)
    val session = mutableSession.asStateFlow()
    private var sessionVersion = 0

    suspend fun signIn(role: UserRole, username: String, password: String): AuthResult {
        val version = ++sessionVersion
        mutableSession.value = null
        val selected = if (role == UserRole.ADMIN) {
            if (username != "admin" || password != "admin123") return AuthResult.InvalidCredentials
            UserSession.Admin
        } else {
            if (password != "staff123") return AuthResult.InvalidCredentials
            val staff = try { findStaff(username.trim().uppercase(Locale.ROOT)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { return AuthResult.StorageError }
                ?: return AuthResult.InvalidCredentials
            UserSession.Staff(staff.id)
        }
        if (version != sessionVersion) return AuthResult.InvalidCredentials
        mutableSession.value = selected
        return AuthResult.Success
    }

    fun signOut() {
        sessionVersion++
        mutableSession.value = null
    }
}

sealed interface AuthResult {
    data object Success : AuthResult
    data object InvalidCredentials : AuthResult
    data object StorageError : AuthResult
}
