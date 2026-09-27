package com.divanshgandhi.attendai.ui.admin.staffprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.divanshgandhi.attendai.R
import com.divanshgandhi.attendai.ui.components.AttendanceDetails
import java.text.DateFormat
import java.util.Date

@Composable
fun StaffProfileScreen(
    state: StaffProfileUiState,
    enrollmentSaved: Boolean,
    onDismissConfirmation: () -> Unit,
    onEnroll: () -> Unit,
    onRetry: () -> Unit,
    onHistoryRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("staff-profile-list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                    Text(stringResource(R.string.staff_profile), style = MaterialTheme.typography.headlineSmall)
                    if (enrollmentSaved) {
                        Text("Face enrollment saved successfully.", color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = onDismissConfirmation) { Text("Dismiss") }
                    }
                }
            }
            when {
                state.loading -> item { CircularProgressIndicator() }
                state.error != null -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onRetry) { Text("Retry") }
                    }
                }
                state.staff == null -> item { Text("Staff member not found.") }
                else -> {
                    item {
                        val staff = state.staff
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(staff.name, style = MaterialTheme.typography.titleLarge)
                            Text("Employee ID: " + staff.employeeId)
                            Text(if (staff.isFaceEnrolled) "Face Enrolled" else "Face Not Enrolled")
                            staff.faceEnrolledAt?.let { Text("Last enrolled: " + DateFormat.getDateTimeInstance().format(Date(it))) }
                            Button(onClick = onEnroll) { Text(if (staff.isFaceEnrolled) "Re-enroll Face" else "Enroll Face") }
                            Text("Attendance history", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    when {
                        state.historyLoading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        state.historyError != null -> item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(state.historyError, color = MaterialTheme.colorScheme.error)
                                OutlinedButton(onClick = onHistoryRetry) { Text("Retry history") }
                            }
                        }
                        state.history.isEmpty() -> item { Text("No attendance recorded yet.") }
                        else -> items(state.history, key = { "attendance-${it.id}" }) { row ->
                            OutlinedCard(Modifier.fillMaxWidth().testTag("attendance-${row.id}")) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AttendanceDetails(row)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
