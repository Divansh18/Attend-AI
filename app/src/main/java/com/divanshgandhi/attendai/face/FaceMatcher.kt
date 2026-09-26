package com.divanshgandhi.attendai.face

import kotlin.math.acos
import kotlin.math.sqrt

data class FaceComparison(val cosine: Double, val angleDegrees: Double, val distance: Double) {
    val isMatch: Boolean get() = angleDegrees < FaceMatcher.UPSTREAM_ANGLE_THRESHOLD
}

object FaceMatcher {
    // Qualcomm v0.63.0 default, NOT a threshold calibrated for AttendAI/ML Kit.
    const val UPSTREAM_ANGLE_THRESHOLD = 74.18

    fun normalize(embedding: FloatArray): FloatArray {
        require(embedding.isNotEmpty() && embedding.all { it.isFinite() }) { "Invalid embedding values." }
        val norm = sqrt(embedding.sumOf { it.toDouble() * it })
        require(norm > 1e-12) { "The model returned an empty embedding." }
        return FloatArray(embedding.size) { (embedding[it] / norm).toFloat() }
    }

    fun compare(reference: FloatArray, candidate: FloatArray): FaceComparison {
        require(reference.size == candidate.size) { "Embedding dimensions differ." }
        val a = normalize(reference)
        val b = normalize(candidate)
        val cosine = a.indices.sumOf { a[it].toDouble() * b[it] }.coerceIn(-1.0, 1.0)
        return FaceComparison(cosine, Math.toDegrees(acos(cosine)), sqrt(2.0 - 2.0 * cosine))
    }
}
