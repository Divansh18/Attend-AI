package com.divanshgandhi.attendai.data.repository

import com.divanshgandhi.attendai.model.UserRole
import com.divanshgandhi.attendai.model.UserSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun correctAdminCredentialsCreateSessionAndSignOutClearsIt() = runTest {
        val repository = AuthRepository()
        assertNull(repository.session.value)

        assertEquals(AuthResult.Success, repository.signIn(UserRole.ADMIN, "admin", "admin123"))
        assertEquals(UserSession.Admin, repository.session.value)

        repository.signOut()
        assertNull(repository.session.value)
    }

    @Test
    fun incorrectCredentialsNeverCreateSession() = runTest {
        val repository = AuthRepository()
        listOf("Admin" to "admin123", "admin" to "wrong", "admin" to "admin123 ").forEach { (username, password) ->
            assertEquals(AuthResult.InvalidCredentials, repository.signIn(UserRole.ADMIN, username, password))
            assertNull(repository.session.value)
        }
    }

    @Test
    fun staffCannotSignInWithAdminCredentials() = runTest {
        val repository = AuthRepository()
        assertEquals(AuthResult.StaffUnavailable, repository.signIn(UserRole.STAFF, "admin", "admin123"))
        assertNull(repository.session.value)
    }
}
