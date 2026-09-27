package com.divanshgandhi.attendai

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the production enrollment screen with either synthetic or webcam front-camera input. */
@RunWith(AndroidJUnit4::class)
class FaceCameraSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun frontCameraCanCaptureRepeatedlyAndFinishProcessing() {
        assumeTrue(Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.CAMERA)
        compose.runOnIdle { (compose.activity.application as AttendAiApplication).container.authRepository.signOut() }
        compose.onNodeWithText("Username").performTextInput("admin")
        compose.onNodeWithText("Password").performTextInput("admin123")
        compose.onNodeWithText("Sign in").performClick()
        waitForText("Add Staff")
        compose.onNodeWithText("Add Staff").performClick()
        compose.onNodeWithText("Name").performTextInput("Camera Smoke Test")
        compose.onNodeWithText("Employee ID").performTextInput("CAM-${System.currentTimeMillis()}")
        compose.onNodeWithText("Save Staff").performClick()
        waitForText("Staff Profile")
        compose.onNodeWithText("Enroll Face").performClick()

        repeat(2) {
            waitForEnabledCapture()
            compose.onNodeWithText("Capture and enroll").performScrollTo().performClick()
            compose.waitUntil(5_000) { !enabledCaptureExists() }
            compose.waitUntil(30_000) {
                enabledCaptureExists() ||
                    compose.onAllNodesWithText("Face enrollment saved successfully.").fetchSemanticsNodes().isNotEmpty()
            }
            if (it == 0 && !enabledCaptureExists()) {
                compose.onNodeWithText("Re-enroll Face").performClick()
            }
        }
        compose.runOnIdle { (compose.activity.application as AttendAiApplication).container.authRepository.signOut() }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForEnabledCapture() {
        compose.waitUntil(20_000) { enabledCaptureExists() }
    }

    private fun enabledCaptureExists() =
        compose.onAllNodes(hasText("Capture and enroll") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
}
