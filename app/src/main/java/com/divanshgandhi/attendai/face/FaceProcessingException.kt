package com.divanshgandhi.attendai.face

enum class FaceError {
    NO_FACE, MULTIPLE_FACES, POOR_FACE, DETECTION, MODEL_LOADING, INFERENCE,
}

class FaceProcessingException(
    val reason: FaceError,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
