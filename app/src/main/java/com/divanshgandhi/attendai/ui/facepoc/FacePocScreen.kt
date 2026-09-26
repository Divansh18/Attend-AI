package com.divanshgandhi.attendai.ui.facepoc

import com.divanshgandhi.attendai.ui.components.CameraCapturePanel
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.divanshgandhi.attendai.R
import com.divanshgandhi.attendai.camera.CapturedPhoto
import java.util.Locale

@Composable
fun FacePocScreen(
    state: FacePocUiState,
    onCapture: (Boolean, suspend () -> CapturedPhoto) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(stringResource(R.string.face_poc_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.face_poc_explanation))
            CameraCapturePanel { ready, takePhoto ->
                Button(onClick = { onCapture(true, takePhoto) }, enabled = ready && !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.face_capture_reference))
                }
                Button(onClick = { onCapture(false, takePhoto) }, enabled = ready && !state.busy && state.referenceReady, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.face_capture_compare))
                }
            }
            Text(stringResource(if (state.referenceReady) R.string.face_reference_ready else R.string.face_reference_missing))
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.comparison?.let { result ->
                Text(
                    stringResource(if (result.isMatch) R.string.face_match else R.string.face_no_match),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(String.format(Locale.US, "Cosine: %.5f · angle: %.2f° · L2: %.5f", result.cosine, result.angleDegrees, result.distance))
            }
            Text(stringResource(R.string.face_threshold_note), style = MaterialTheme.typography.bodySmall)
            state.crop?.let {
                Image(it.asImageBitmap(), contentDescription = stringResource(R.string.face_aligned_crop), modifier = Modifier.size(112.dp))
            }
            state.elapsedMillis?.let { Text(stringResource(R.string.face_processing_time, it)) }
            OutlinedButton(onClick = onReset, enabled = !state.busy && state.referenceReady) {
                Text(stringResource(R.string.face_clear_reference))
            }
        }
    }
}
