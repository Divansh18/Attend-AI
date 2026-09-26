package com.divanshgandhi.attendai.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.divanshgandhi.attendai.R
import com.divanshgandhi.attendai.camera.CameraCapture
import com.divanshgandhi.attendai.camera.CapturedPhoto
import kotlinx.coroutines.CancellationException

@Composable
fun CameraCapturePanel(actions: @Composable (ready: Boolean, takePhoto: suspend () -> CapturedPhoto) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    fun hasPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(hasPermission()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) granted = hasPermission() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    if (!granted) {
        Text(stringResource(R.string.face_camera_permission))
        Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
            Text(stringResource(R.string.face_grant_permission))
        }
        TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ("package:" + context.packageName).toUri()))
        }) { Text(stringResource(R.string.face_open_settings)) }
    } else {
        var retry by remember { mutableIntStateOf(0) }
        val camera = remember(owner, retry) { CameraCapture(context.applicationContext) }
        val preview = remember(camera) {
            PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
        }
        var ready by remember(camera) { mutableStateOf(false) }
        var cameraError by remember(camera) { mutableStateOf<String?>(null) }
        DisposableEffect(camera) { onDispose { camera.close() } }
        LaunchedEffect(camera, owner) {
            try {
                camera.bind(owner, preview)
                ready = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                cameraError = e.message ?: "Camera could not start."
            }
        }
        AndroidView(factory = { preview }, modifier = Modifier.fillMaxWidth().height(220.dp))
        cameraError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { retry++ }) { Text(stringResource(R.string.face_retry_camera)) }
        }
        actions(ready, camera::takePhoto)
    }
}
