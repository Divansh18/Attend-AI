package com.divanshgandhi.attendai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun signOut() {
        compose.runOnIdle {
            (compose.activity.application as AttendAiApplication).container.authRepository.signOut()
        }
    }

    @Test
    fun validatesCredentialsThenOpensAdminAndSupportsRecreationAndLogout() {
        compose.onNodeWithText("Sign in").performClick()
        compose.onNodeWithText("Enter your username or employee ID.").assertIsDisplayed()
        compose.onNodeWithText("Enter your password.").assertIsDisplayed()

        compose.onNodeWithText("Username").performTextInput("admin")
        compose.onNodeWithText("Password").performTextInput("wrong")
        compose.onNodeWithText("Sign in").performClick()
        compose.onNodeWithText("Incorrect username or password.").assertIsDisplayed()

        compose.onNodeWithText("Password").performTextReplacement("admin123")
        compose.onNodeWithText("Sign in").performClick()
        compose.onNodeWithText("Admin · Staff list").assertIsDisplayed()

        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Admin · Staff list").assertIsDisplayed()
        compose.onNodeWithText("Sign out").performClick()
        compose.onNodeWithText("Sign in").assertIsDisplayed()

        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
        compose.onNodeWithText("Admin · Staff list").assertDoesNotExist()
    }

    @Test
    fun staffSignInDoesNotOpenAdminDestination() {
        compose.onNodeWithText("Staff").performClick()
        compose.onNodeWithText("Employee ID").performTextInput("admin")
        compose.onNodeWithText("Password").performTextInput("admin123")
        compose.onNodeWithText("Sign in").performClick()

        compose.onNodeWithText("Incorrect username or password.").assertIsDisplayed()
        compose.onNodeWithText("Admin · Staff list").assertDoesNotExist()
    }
}
