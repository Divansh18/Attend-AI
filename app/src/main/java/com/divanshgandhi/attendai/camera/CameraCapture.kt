package com.divanshgandhi.attendai.camera

import android.content.Context
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Screen-owned camera binding. It never retains an Activity in a ViewModel. */
class CameraCapture(private val context: Context) : AutoCloseable {
    private val worker = Executors.newSingleThreadExecutor()
    private val preview = Preview.Builder().build()
    private val capture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setResolutionSelector(
            ResolutionSelector.Builder().setResolutionStrategy(
                ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
            ).build(),
        ).build()
    private var provider: ProcessCameraProvider? = null
    private var previewView: PreviewView? = null
    private var closed = false

    suspend fun bind(owner: LifecycleOwner, view: PreviewView) = withContext(Dispatchers.Main.immediate) {
        val future = ProcessCameraProvider.getInstance(context)
        val cameraProvider = suspendCancellableCoroutine { continuation ->
            future.addListener({
                try { continuation.resume(future.get()) }
                catch (e: Exception) { continuation.resumeWithException(e) }
            }, ContextCompat.getMainExecutor(context))
        }
        check(!closed) { "Camera screen was closed." }
        check(cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) { "This device has no front camera." }
        preview.setSurfaceProvider(view.surfaceProvider)
        capture.targetRotation = view.display?.rotation ?: Surface.ROTATION_0
        cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, capture)
        provider = cameraProvider
        previewView = view
    }

    suspend fun takePhoto(): CapturedPhoto = withContext(Dispatchers.Main.immediate) {
        check(!closed && provider != null) { "Camera is not ready. Reopen this screen and retry." }
        capture.targetRotation = previewView?.display?.rotation ?: Surface.ROTATION_0
        suspendCancellableCoroutine { continuation ->
            capture.takePicture(worker, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        // toBitmap decodes the raw buffer. CameraX supplies rotation separately.
                        // In-memory ImageCapture pixels are unmirrored; PreviewView mirrors only
                        // the front-camera preview. Do not flip the inference pixels a second time.
                        val photo = CapturedPhoto(image.toBitmap(), image.imageInfo.rotationDegrees)
                        continuation.resume(photo) { _, value, _ -> value.bitmap.recycle() }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    override fun close() {
        closed = true
        provider?.unbind(preview, capture)
        provider = null
        previewView = null
        worker.shutdown()
    }
}
