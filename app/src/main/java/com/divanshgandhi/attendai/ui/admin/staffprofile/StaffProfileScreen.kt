package com.divanshgandhi.attendai.ui.admin.staffprofile

import androidx.compose.ui.res.stringResource
import com.divanshgandhi.attendai.R
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.divanshgandhi.attendai.ui.admin.StaffFormFrame
import java.text.DateFormat
import java.util.Date

@Composable
fun StaffProfileScreen(state: StaffProfileUiState, enrollmentSaved: Boolean, onDismissConfirmation: () -> Unit, onEnroll: () -> Unit, onRetry: () -> Unit, onBack: () -> Unit) {
    StaffFormFrame(stringResource(R.string.staff_profile), onBack) {
        if (enrollmentSaved) {
            Text("Face enrollment saved successfully.", color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onDismissConfirmation) { Text("Dismiss") }
        }
        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            }
            state.staff == null -> Text("Staff member not found.")
            else -> {
                val staff = state.staff
                Text(staff.name, style = MaterialTheme.typography.titleLarge)
                Text("Employee ID: " + staff.employeeId)
                Text(if (staff.isFaceEnrolled) "Face Enrolled" else "Face Not Enrolled")
                staff.faceEnrolledAt?.let { Text("Last enrolled: " + DateFormat.getDateTimeInstance().format(Date(it))) }
                Button(onClick = onEnroll) { Text(if (staff.isFaceEnrolled) "Re-enroll Face" else "Enroll Face") }
            }
        }
    }
}
