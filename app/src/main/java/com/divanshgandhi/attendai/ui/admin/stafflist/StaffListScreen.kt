package com.divanshgandhi.attendai.ui.admin.stafflist

import androidx.compose.ui.res.stringResource
import com.divanshgandhi.attendai.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divanshgandhi.attendai.BuildConfig

@Composable
fun StaffListScreen(state: StaffListUiState, onAdd: () -> Unit, onStaff: (Long) -> Unit, onRetry: () -> Unit, onSignOut: () -> Unit, onFacePoc: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.admin_staff_list), style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAdd) { Text(stringResource(R.string.add_staff)) }
                TextButton(onClick = onSignOut) { Text("Sign out") }
            }
            if (BuildConfig.DEBUG) {
                Text("Debug tools", style = MaterialTheme.typography.labelSmall)
                OutlinedButton(onClick = onFacePoc) { Text("Face recognition · Debug POC") }
            }
            when {
                state.loading -> CircularProgressIndicator()
                state.error != null -> {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry) { Text("Retry") }
                }
                state.staff.isEmpty() -> Text("No staff yet. Add your first staff member.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.staff, key = { it.id }) { staff ->
                        OutlinedCard(onClick = { onStaff(staff.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(staff.name, style = MaterialTheme.typography.titleMedium)
                                Text("Employee ID: " + staff.employeeId)
                                Text(if (staff.isFaceEnrolled) "Face Enrolled" else "Face Not Enrolled")
                            }
                        }
                    }
                }
            }
        }
    }
}
