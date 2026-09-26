package com.divanshgandhi.attendai.face

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class MlKitFaceDetector : AutoCloseable {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.1f)
            .build(),
    )

    suspend fun landmarks(bitmap: Bitmap): List<FacePoint> {
        val faces = try {
            // ML Kit's native task is not cancelled by coroutine cancellation. Wait for it
            // to release the pixels before the engine recycles the bitmap during cleanup.
            withContext(NonCancellable) { detector.process(InputImage.fromBitmap(bitmap, 0)).await() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw FaceProcessingException(FaceError.DETECTION, "Face detection failed. Try another capture.", e)
        }
        if (faces.isEmpty()) throw FaceProcessingException(FaceError.NO_FACE, "No face detected. Face the camera in good light.")
        if (faces.size != 1) throw FaceProcessingException(FaceError.MULTIPLE_FACES, "Multiple faces detected. Only one person may be in the frame.")
        val face = faces.single()
        if (face.boundingBox.width() < 100 || face.boundingBox.height() < 100 ||
            abs(face.headEulerAngleY) > 25 || abs(face.headEulerAngleX) > 25
        ) throw FaceProcessingException(FaceError.POOR_FACE, "Move closer and look straight at the camera.")

        fun point(type: Int): FacePoint {
            val p = face.getLandmark(type)?.position
                ?: throw FaceProcessingException(FaceError.POOR_FACE, "Eyes, nose and mouth must be visible. Try again.")
            if (p.x !in 0f..bitmap.width.toFloat() || p.y !in 0f..bitmap.height.toFloat()) {
                throw FaceProcessingException(FaceError.POOR_FACE, "Keep the whole face inside the frame.")
            }
            return FacePoint(p.x.toDouble(), p.y.toDouble())
        }
        // Template order is image-left then image-right, not the subject's anatomical left.
        val eyes = listOf(point(FaceLandmark.LEFT_EYE), point(FaceLandmark.RIGHT_EYE)).sortedBy { it.x }
        val mouth = listOf(point(FaceLandmark.MOUTH_LEFT), point(FaceLandmark.MOUTH_RIGHT)).sortedBy { it.x }
        return eyes + point(FaceLandmark.NOSE_BASE) + mouth
    }

    override fun close() = detector.close()
}
