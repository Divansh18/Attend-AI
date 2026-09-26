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

/** Works with synthetic or webcam front-camera input; fixtures test exact detection outcomes. */
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
        compose.onNodeWithText("Face recognition · Debug POC").performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("Capture reference") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        repeat(2) {
            compose.onNodeWithText("Capture reference").performScrollTo().performClick()
            compose.waitUntil(30_000) {
                val ready = compose.onAllNodes(hasText("Capture reference") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
                val success = compose.onAllNodesWithText("Reference embedding is held in memory.").fetchSemanticsNodes().isNotEmpty()
                val detectionError = listOf("No face detected.", "Multiple faces detected.", "Move closer", "Eyes, nose", "Keep the whole face").any {
                    compose.onAllNodesWithText(it, substring = true).fetchSemanticsNodes().isNotEmpty()
                }
                ready && (success || detectionError)
            }
        }
    }
}
