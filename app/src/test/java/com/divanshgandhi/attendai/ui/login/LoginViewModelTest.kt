package com.divanshgandhi.attendai.ui.login

import com.divanshgandhi.attendai.data.repository.AuthRepository
import com.divanshgandhi.attendai.model.UserRole
import com.divanshgandhi.attendai.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        repository = AuthRepository()
        viewModel = LoginViewModel(repository)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun blankFieldsShowValidationWithoutAuthenticating() {
        viewModel.onUsernameChange("   ")
        viewModel.onPasswordChange("   ")
        viewModel.signIn()

        assertTrue(viewModel.uiState.value.isUsernameMissing)
        assertTrue(viewModel.uiState.value.isPasswordMissing)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(repository.session.value)
    }

    @Test
    fun editingFieldsClearsTheirValidation() {
        viewModel.signIn()
        viewModel.onUsernameChange("admin")
        assertFalse(viewModel.uiState.value.isUsernameMissing)
        assertTrue(viewModel.uiState.value.isPasswordMissing)
        viewModel.onPasswordChange("admin123")
        assertFalse(viewModel.uiState.value.isPasswordMissing)
    }

    @Test
    fun invalidPasswordShowsErrorAndCanBeCorrected() = runTest {
        viewModel.onUsernameChange("admin")
        viewModel.onPasswordChange("wrong")
        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(LoginError.INVALID_CREDENTIALS, viewModel.uiState.value.error)
        assertNull(repository.session.value)

        viewModel.onPasswordChange("admin123")
        assertNull(viewModel.uiState.value.error)
        viewModel.signIn()
        advanceUntilIdle()
        assertEquals(UserSession.Admin, repository.session.value)
    }

    @Test
    fun usernameIsTrimmedAndSuccessfulLoginClearsPassword() = runTest {
        viewModel.onUsernameChange(" admin ")
        viewModel.onPasswordChange("admin123")
        viewModel.signIn()

        assertTrue(viewModel.uiState.value.isSubmitting)
        advanceUntilIdle()

        assertEquals(UserSession.Admin, repository.session.value)
        assertEquals("", viewModel.uiState.value.password)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun passwordIsNotTrimmed() = runTest {
        viewModel.onUsernameChange("admin")
        viewModel.onPasswordChange("admin123 ")
        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(LoginError.INVALID_CREDENTIALS, viewModel.uiState.value.error)
        assertNull(repository.session.value)
    }

    @Test
    fun changingRoleClearsCredentialsAndStaffLoginRemainsUnavailable() = runTest {
        viewModel.onUsernameChange("admin")
        viewModel.onPasswordChange("admin123")
        viewModel.onRoleChange(UserRole.STAFF)
        assertEquals(LoginUiState(role = UserRole.STAFF), viewModel.uiState.value)

        viewModel.onUsernameChange("employee-1")
        viewModel.onPasswordChange("password")
        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(LoginError.STAFF_UNAVAILABLE, viewModel.uiState.value.error)
        assertNull(repository.session.value)
    }

    @Test
    fun pendingSignInIgnoresRepeatedSubmitAndFormChanges() = runTest {
        viewModel.onUsernameChange("admin")
        viewModel.onPasswordChange("admin123")
        viewModel.signIn()
        viewModel.signIn()
        viewModel.onRoleChange(UserRole.STAFF)
        viewModel.onPasswordChange("wrong")
        advanceUntilIdle()

        assertEquals(UserRole.ADMIN, viewModel.uiState.value.role)
        assertEquals(UserSession.Admin, repository.session.value)
    }
}
