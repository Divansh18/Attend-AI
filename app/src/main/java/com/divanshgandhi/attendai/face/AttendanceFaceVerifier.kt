package com.divanshgandhi.attendai.face

import com.divanshgandhi.attendai.camera.CapturedPhoto

data class VerifiedSelfie(val jpeg: ByteArray, val comparison: FaceComparison)

interface AttendanceFaceVerifier {
    suspend fun verify(takePhoto: suspend () -> CapturedPhoto, enrolledEmbedding: FloatArray): VerifiedSelfie
    suspend fun close()
}

class FaceMismatchException(val comparison: FaceComparison) : Exception("Face does not match this employee's enrollment. Attendance was not recorded.")
