package com.divanshgandhi.attendai.face

import android.content.res.AssetManager
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import com.divanshgandhi.attendai.camera.CapturedPhoto
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext

data class FaceSample(val embedding: FloatArray, val alignedFace: Bitmap, val elapsedMillis: Long, val selfie: Bitmap? = null)

class FaceRecognitionEngine(assets: AssetManager) : FaceEnrollmentProcessor, AttendanceFaceVerifier {
    private val mutex = Mutex()
    private val detector = MlKitFaceDetector()
    private val embedder = TfliteFaceEmbedder(assets)
    private var closed = false

    /** Takes ownership of photo.bitmap, including on failure. */
    suspend fun extract(photo: CapturedPhoto, retainSelfie: Boolean = false): FaceSample {
        var result: FaceSample? = null
        try {
            return withContext(Dispatchers.Default) {
                mutex.withLock {
                    var upright: Bitmap? = null
                    var crop: Bitmap? = null
                    try {
                        check(!closed) { "Face engine has been closed." }
                        val start = System.nanoTime()
                        upright = ImageOrientation.upright(photo.bitmap, photo.rotationDegrees, photo.isMirrored)
                        val points = detector.landmarks(upright)
                        currentCoroutineContext().ensureActive()
                        crop = FacePreprocessor.align(upright, points)
                        val embedding = embedder.embed(FacePreprocessor.input(crop))
                        FaceSample(embedding, crop, (System.nanoTime() - start) / 1_000_000,
                            if (retainSelfie) checkNotNull(upright.copy(Bitmap.Config.ARGB_8888, false)) else null).also { result = it }
                    } catch (e: Exception) {
                        crop?.recycle()
                        throw e
                    } finally {
                        if (upright !== photo.bitmap) upright?.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            // Also covers cancellation while dispatching the finished result to the UI.
            result?.alignedFace?.recycle()
            result?.selfie?.recycle()
            throw e
        } finally {
            photo.bitmap.recycle()
        }
    }

    override suspend fun captureEmbedding(takePhoto: suspend () -> CapturedPhoto): FloatArray {
        val sample = extract(withTimeout(15_000) { takePhoto() })
        return try { sample.embedding } finally { sample.alignedFace.recycle() }
    }

    override suspend fun verify(takePhoto: suspend () -> CapturedPhoto, enrolledEmbedding: FloatArray): VerifiedSelfie {
        val sample = extract(withTimeout(15_000) { takePhoto() }, retainSelfie = true)
        try {
            return withContext(Dispatchers.Default) {
                val comparison = FaceMatcher.compare(enrolledEmbedding, sample.embedding)
                if (!comparison.isMatch) throw FaceMismatchException(comparison)
                val bytes = ByteArrayOutputStream().use { output ->
                    check(checkNotNull(sample.selfie).compress(Bitmap.CompressFormat.JPEG, 90, output)) { "Selfie encoding failed." }
                    output.toByteArray()
                }
                VerifiedSelfie(bytes, comparison)
            }
        } finally {
            sample.alignedFace.recycle()
            sample.selfie?.recycle()
        }
    }

    override suspend fun close() = withContext(Dispatchers.Default) {
        mutex.withLock {
            closed = true
            detector.close()
            embedder.close()
        }
    }
}
