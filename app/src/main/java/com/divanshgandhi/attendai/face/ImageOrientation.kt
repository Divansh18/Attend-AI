package com.divanshgandhi.attendai.face

import android.graphics.Bitmap
import android.graphics.Matrix

object ImageOrientation {
    /** Rotate the sensor buffer once, then undo mirroring only if pixels are mirrored. */
    fun upright(bitmap: Bitmap, rotationDegrees: Int, isMirrored: Boolean): Bitmap {
        require(rotationDegrees in setOf(0, 90, 180, 270)) { "Unsupported image rotation." }
        if (rotationDegrees == 0 && !isMirrored) return bitmap
        val transform = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (isMirrored) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, false)
    }
}
