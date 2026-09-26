package com.divanshgandhi.attendai.ui.admin.addstaff

import androidx.compose.ui.res.stringResource
import com.divanshgandhi.attendai.R
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.divanshgandhi.attendai.ui.admin.StaffFormFrame

@Composable
fun AddStaffScreen(state: AddStaffUiState, onName: (String) -> Unit, onEmployeeId: (String) -> Unit, onSubmit: () -> Unit, onBack: () -> Unit) {
    StaffFormFrame(stringResource(R.string.add_staff), onBack) {
        OutlinedTextField(value = state.name, onValueChange = onName, label = { Text("Name") }, singleLine = true,
            enabled = !state.submitting, isError = state.nameError != null, modifier = Modifier.fillMaxWidth())
        state.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(value = state.employeeId, onValueChange = onEmployeeId, label = { Text("Employee ID") }, singleLine = true,
            enabled = !state.submitting, isError = state.employeeIdError != null, modifier = Modifier.fillMaxWidth())
        state.employeeIdError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Employee IDs are stored in uppercase and must be unique.", style = MaterialTheme.typography.bodySmall)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.submitting) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = onSubmit, enabled = !state.submitting && state.createdId == null) { Text("Save Staff") }
    }
}
