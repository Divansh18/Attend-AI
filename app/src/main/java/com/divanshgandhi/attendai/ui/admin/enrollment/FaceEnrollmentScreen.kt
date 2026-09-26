package com.divanshgandhi.attendai.ui.admin.enrollment

import androidx.compose.ui.res.stringResource
import com.divanshgandhi.attendai.R
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.divanshgandhi.attendai.camera.CapturedPhoto
import com.divanshgandhi.attendai.ui.admin.StaffFormFrame
import com.divanshgandhi.attendai.ui.components.CameraCapturePanel

@Composable
fun FaceEnrollmentScreen(state: FaceEnrollmentUiState, onCapture: (suspend () -> CapturedPhoto) -> Unit, onRetry: () -> Unit, onBack: () -> Unit) {
    StaffFormFrame(stringResource(R.string.face_enrollment), onBack) {
        when {
            state.loading -> CircularProgressIndicator()
            state.loadError != null -> {
                Text(state.loadError, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            }
            state.staff == null -> Text("Staff member not found.")
            else -> {
                Text(state.staff.name, style = MaterialTheme.typography.titleLarge)
                Text("Employee ID: " + state.staff.employeeId)
                Text("Keep exactly one face in view and look straight at the camera. Only the embedding is saved; no enrollment selfie is stored.")
                if (state.staff.isFaceEnrolled) Text("A successful capture replaces the existing enrollment.")
                CameraCapturePanel { ready, takePhoto ->
                    Button(onClick = { onCapture(takePhoto) }, enabled = ready && !state.busy && !state.saved, modifier = Modifier.fillMaxWidth()) {
                        Text("Capture and enroll")
                    }
                }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
