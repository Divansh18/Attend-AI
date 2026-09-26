package com.divanshgandhi.attendai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.divanshgandhi.attendai.data.local.AttendAiDatabase
import com.divanshgandhi.attendai.data.repository.AuthRepository
import com.divanshgandhi.attendai.data.repository.RoomStaffRepository
import com.divanshgandhi.attendai.di.appViewModelFactory
import com.divanshgandhi.attendai.face.FaceRecognitionEngine
import com.divanshgandhi.attendai.navigation.AttendAiNavHost
import com.divanshgandhi.attendai.ui.theme.AttendAITheme
import org.junit.*
import org.junit.runner.RunWith

/** Uses the real navigation/Room repository with an isolated database; never wipes user staff. */
@RunWith(AndroidJUnit4::class)
class AdminStaffNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AttendAiDatabase
    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AttendAiDatabase::class.java).build()
        val auth = AuthRepository()
        val factory = appViewModelFactory(auth, RoomStaffRepository(database.staffDao())) { FaceRecognitionEngine(context.assets) }
        compose.setContent {
            val session by auth.session.collectAsStateWithLifecycle()
            AttendAITheme { key(session) { AttendAiNavHost(session, factory, auth::signOut) } }
        }
    }
    @After fun tearDown() { database.close() }
    private fun waitFor(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun login() {
        compose.onNodeWithText("Username").performTextInput("admin")
        compose.onNodeWithText("Password").performTextInput("admin123")
        compose.onNodeWithText("Sign in").performClick()
        waitFor("No staff yet. Add your first staff member.")
    }
    @Test fun loginCreateProfileAndEnrollmentNavigation() {
        login()
        compose.onNodeWithText("Add Staff").performClick()
        compose.onNodeWithText("Save Staff").performClick()
        compose.onNodeWithText("Enter a name.").assertIsDisplayed()
        compose.onNodeWithText("Name").performTextInput("Ada Test")
        compose.onNodeWithText("Employee ID").performTextInput("emp-001")
        compose.onNodeWithText("Save Staff").performClick()
        waitFor("Staff Profile"); waitFor("Ada Test")
        compose.onNodeWithText("Employee ID: EMP-001").assertIsDisplayed()
        compose.onNodeWithText("Face Not Enrolled").assertIsDisplayed()
        compose.onNodeWithText("Enroll Face").performClick()
        waitFor("Face Enrollment"); waitFor("Ada Test")
        compose.onNodeWithText("Back").performClick()
        waitFor("Staff Profile")
        compose.onNodeWithText("Back").performClick()
        waitFor("Admin · Staff list"); waitFor("Ada Test")
        compose.onNodeWithText("Ada Test").performClick(); waitFor("Staff Profile")
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Sign out").performClick()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
    }
    @Test fun duplicateEmployeeIdStaysOnFormAndCanBeCorrected() {
        login()
        fun fill(name: String, id: String) {
            compose.onNodeWithText("Add Staff").performClick()
            compose.onNodeWithText("Name").performTextInput(name)
            compose.onNodeWithText("Employee ID").performTextInput(id)
            compose.onNodeWithText("Save Staff").performClick()
        }
        fill("First", "A1"); waitFor("Staff Profile")
        compose.onNodeWithText("Back").performClick()
        fill("Second", " a1 "); waitFor("That employee ID already exists.")
        compose.onNodeWithText("Employee ID").performTextReplacement("A2")
        compose.onNodeWithText("Save Staff").performClick()
        waitFor("Staff Profile"); waitFor("Second")
        compose.onNodeWithText("Employee ID: A2").assertIsDisplayed()
    }
}
