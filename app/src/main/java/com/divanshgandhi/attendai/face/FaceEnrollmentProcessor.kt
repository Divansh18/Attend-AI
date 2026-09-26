package com.divanshgandhi.attendai.face

import com.divanshgandhi.attendai.camera.CapturedPhoto

/** Enrollment needs only the embedding; the existing engine owns and discards all image pixels. */
interface FaceEnrollmentProcessor {
    suspend fun captureEmbedding(takePhoto: suspend () -> CapturedPhoto): FloatArray
    suspend fun close()
}
