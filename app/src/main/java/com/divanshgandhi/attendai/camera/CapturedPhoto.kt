package com.divanshgandhi.attendai.camera

import android.graphics.Bitmap

/** In-memory pixels; the consumer owns the bitmap. No photo is written to disk. */
data class CapturedPhoto(
    val bitmap: Bitmap,
    val rotationDegrees: Int,
    val isMirrored: Boolean = false,
)
