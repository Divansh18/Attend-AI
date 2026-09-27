package com.divanshgandhi.attendai.data.repository

import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.model.UserRole
import com.divanshgandhi.attendai.model.UserSession
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class StaffAuthenticationTest {
    @Test fun staffLookupNormalizesIdAndSessionContainsOnlySelectedDatabaseId() = runTest {
        val auth = AuthRepository { id -> if (id == "EMP-1") StaffEntity(42, id, "Ada") else null }
        assertEquals(AuthResult.Success, auth.signIn(UserRole.STAFF, " emp-1 ", "staff123"))
        assertEquals(UserSession.Staff(42), auth.session.value)
        auth.signOut(); assertNull(auth.session.value)
    }
    @Test fun unknownStaffAndWrongPasswordNeverAuthenticate() = runTest {
        val auth = AuthRepository { if (it == "A1") StaffEntity(1, it, "Ada") else null }
        assertEquals(AuthResult.InvalidCredentials, auth.signIn(UserRole.STAFF, "A1", "wrong"))
        assertEquals(AuthResult.InvalidCredentials, auth.signIn(UserRole.STAFF, "missing", "staff123"))
        assertNull(auth.session.value)
    }
    @Test fun lookupFailureShowsStorageError() = runTest {
        val auth = AuthRepository { error("disk") }
        assertEquals(AuthResult.StorageError, auth.signIn(UserRole.STAFF, "A1", "staff123"))
        assertNull(auth.session.value)
    }
    @Test fun signOutDuringLookupCannotRestoreSession() = runTest {
        val pending = CompletableDeferred<StaffEntity?>()
        val auth = AuthRepository { pending.await() }
        val task = async { auth.signIn(UserRole.STAFF, "A1", "staff123") }
        runCurrent(); auth.signOut(); pending.complete(StaffEntity(1, "A1", "Ada")); task.await()
        assertNull(auth.session.value)
    }
}
