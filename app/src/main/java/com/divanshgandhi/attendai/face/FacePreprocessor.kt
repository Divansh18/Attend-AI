package com.divanshgandhi.attendai.face

import androidx.core.graphics.createBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint

object FacePreprocessor {
    const val SIZE = 112

    fun align(bitmap: Bitmap, landmarks: List<FacePoint>): Bitmap {
        val transform = FaceAlignment.estimate(landmarks)
        return createBitmap(SIZE, SIZE).also { crop ->
            Canvas(crop).apply {
                drawColor(Color.BLACK)
                drawBitmap(bitmap, Matrix().apply { setValues(transform.matrixValues()) }, Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }
    }

    fun input(crop: Bitmap): FloatArray {
        require(crop.width == SIZE && crop.height == SIZE)
        val pixels = IntArray(SIZE * SIZE)
        crop.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        return rgbNchw(pixels)
    }

    /** RGB, channel-first, [0,1]. ImageNet mean/std and flip TTA are inside the model. */
    fun rgbNchw(pixels: IntArray): FloatArray = FloatArray(pixels.size * 3).also { result ->
        pixels.forEachIndexed { index, color ->
            result[index] = ((color ushr 16) and 255) / 255f
            result[pixels.size + index] = ((color ushr 8) and 255) / 255f
            result[2 * pixels.size + index] = (color and 255) / 255f
        }
    }
}
