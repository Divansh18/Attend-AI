package com.divanshgandhi.attendai.face

data class FacePoint(val x: Double, val y: Double)

/** Least-squares rotation + uniform scale + translation; never reflects the face. */
data class SimilarityTransform(val a: Double, val b: Double, val tx: Double, val ty: Double) {
    fun map(p: FacePoint) = FacePoint(a * p.x - b * p.y + tx, b * p.x + a * p.y + ty)
    fun matrixValues() = floatArrayOf(a.toFloat(), -b.toFloat(), tx.toFloat(), b.toFloat(), a.toFloat(), ty.toFloat(), 0f, 0f, 1f)
}

object FaceAlignment {
    // foamliu's 96x112 template, made square by adding 8 to each x coordinate.
    // The upstream helper returns this template directly for output size 112x112.
    val target = listOf(
        FacePoint(38.29459953, 51.69630051), FacePoint(73.53179932, 51.50139999),
        FacePoint(56.02519989, 71.73660278), FacePoint(41.54930115, 92.3655014),
        FacePoint(70.72990036, 92.20410156),
    )

    fun estimate(source: List<FacePoint>, destination: List<FacePoint> = target): SimilarityTransform {
        require(source.size == destination.size && source.size >= 2)
        require((source + destination).all { it.x.isFinite() && it.y.isFinite() })
        val sx = source.map { it.x }.average()
        val sy = source.map { it.y }.average()
        val dx = destination.map { it.x }.average()
        val dy = destination.map { it.y }.average()
        var denominator = 0.0
        var real = 0.0
        var imaginary = 0.0
        source.indices.forEach { i ->
            val x = source[i].x - sx
            val y = source[i].y - sy
            val u = destination[i].x - dx
            val v = destination[i].y - dy
            denominator += x * x + y * y
            real += x * u + y * v
            imaginary += x * v - y * u
        }
        require(denominator > 1e-8) { "Face landmarks are degenerate." }
        val a = real / denominator
        val b = imaginary / denominator
        require(a * a + b * b > 1e-8) { "Face alignment failed." }
        return SimilarityTransform(a, b, dx - a * sx + b * sy, dy - b * sx - a * sy)
    }
}
