package com.divanshgandhi.attendai.ui.attendance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.divanshgandhi.attendai.R
import com.divanshgandhi.attendai.camera.CapturedPhoto
import com.divanshgandhi.attendai.ui.components.AttendanceDetails
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.divanshgandhi.attendai.ui.components.CameraCapturePanel

@Composable
fun AttendanceScreen(state: AttendanceUiState, onMark: (suspend () -> CapturedPhoto) -> Unit, onRetry: () -> Unit, onAnother: () -> Unit, onSignOut: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    fun hasLocation() = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    var locationGranted by remember { mutableStateOf(hasLocation()) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { locationGranted = hasLocation() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) locationGranted = hasLocation() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.attendance), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onSignOut, enabled = !state.busy) { Text(stringResource(R.string.sign_out)) }
            when {
                state.loading -> CircularProgressIndicator()
                state.loadError != null -> {
                    Text(state.loadError, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry) { Text("Retry") }
                }
                state.staff == null -> Text("Staff member not found. Contact Admin.")
                else -> {
                    Text(state.staff.name, style = MaterialTheme.typography.titleLarge)
                    Text("Employee ID: " + state.staff.employeeId)
                    Text(if (state.staff.isFaceEnrolled) "Face Enrolled" else "Face Not Enrolled")
                    // Keep live progress/errors above the preview so they remain easy to notice.
                    Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.busy) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(state.progress.orEmpty())
                        }
                        if (state.mismatch) Text("Face mismatch", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    if (!state.staff.isFaceEnrolled) {
                        Text("Contact Admin to enroll your face before marking attendance.")
                    } else if (state.success != null) {
                        Text("Attendance recorded", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        AttendanceDetails(state.success)
                        OutlinedButton(onClick = onAnother) { Text("Record another attendance") }
                    } else {
                        if (!locationGranted) {
                            Text("Location permission is required to record attendance. Approximate location is accepted; its accuracy is saved.")
                            Button(onClick = { permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) {
                                Text("Grant location access")
                            }
                            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ("package:" + context.packageName).toUri())) }) {
                                Text("Open app settings")
                            }
                        }
                        CameraCapturePanel { ready, takePhoto ->
                            Button(onClick = { onMark(takePhoto) }, enabled = ready && locationGranted && !state.busy, modifier = Modifier.fillMaxWidth()) {
                                Text("Mark Attendance")
                            }
                        }
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }, enabled = !state.busy) { Text("Location settings") }
                        Text("Face matching uses the provisional 74.18° rule, not a production-calibrated threshold.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.success == null) state.latest?.let {
                        Text("Last recorded attendance", style = MaterialTheme.typography.titleMedium)
                        AttendanceDetails(it)
                    }
                    state.latestError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Retry") }
                    }
                }
            }
        }
    }
}
