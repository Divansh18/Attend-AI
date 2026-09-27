package com.divanshgandhi.attendai

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.divanshgandhi.attendai.data.local.AttendAiDatabase
import com.divanshgandhi.attendai.data.repository.*
import com.divanshgandhi.attendai.di.appViewModelFactory
import com.divanshgandhi.attendai.face.FaceRecognitionEngine
import com.divanshgandhi.attendai.location.FusedLocationProvider
import com.divanshgandhi.attendai.navigation.AttendAiNavHost
import com.divanshgandhi.attendai.storage.PrivateSelfieStorage
import com.divanshgandhi.attendai.ui.theme.AttendAITheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StaffAttendanceNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AttendAiDatabase
    private lateinit var directory: File
    private lateinit var staff: RoomStaffRepository
    private var staffId = 0L
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AttendAiDatabase::class.java).build()
        directory = File(context.cacheDir, "staff-ui-${UUID.randomUUID()}")
        staff = RoomStaffRepository(database.staffDao())
        staffId = runBlocking { staff.addStaff("Staff Demo", "EMP-UI") }
        val auth = AuthRepository(staff::findByEmployeeId)
        val factory = appViewModelFactory(auth, staff, RoomAttendanceRepository(database.attendanceDao(), PrivateSelfieStorage(directory)), FusedLocationProvider(context)) { FaceRecognitionEngine(context.assets) }
        compose.setContent {
            val session by auth.session.collectAsStateWithLifecycle()
            AttendAITheme { key(session) { AttendAiNavHost(session, factory, auth::signOut) } }
        }
    }
    @After fun cleanup() { database.close(); directory.deleteRecursively() }
    private fun login() {
        compose.onNodeWithText("Staff", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Employee ID").performTextInput("emp-ui")
        compose.onNodeWithText("Password").performTextInput("staff123")
        compose.onNodeWithText("Sign in").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Staff Demo").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun unenrolledStaffCanLoginButCannotMarkAttendance() {
        login()
        compose.onNodeWithText("Face Not Enrolled").assertIsDisplayed()
        compose.onNodeWithText("Contact Admin to enroll your face before marking attendance.").assertIsDisplayed()
        compose.onNodeWithText("Mark Attendance").assertDoesNotExist()
        compose.onNodeWithText("Admin · Staff list").assertDoesNotExist()
        compose.onNodeWithText("Sign out").performClick()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
    }
    @Test fun enrolledStaffSeesAttendanceAndRequiredPermissionControls() {
        // Only UI-state setup uses a fixed vector; real-model persistence is tested separately.
        runBlocking { staff.saveFaceEmbedding(staffId, FloatArray(128) { 1f }) }
        login()
        compose.onNodeWithText("Face Enrolled").assertIsDisplayed()
        compose.onNodeWithText("Employee ID: EMP-UI").assertIsDisplayed()
        compose.onNodeWithText("Location settings").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Admin · Staff list").assertDoesNotExist()
    }
}
