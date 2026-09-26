package com.divanshgandhi.attendai.face

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceMathTest {
    @Test
    fun rgbUsesChannelFirstLayoutAndUnitRange() {
        val input = FacePreprocessor.rgbNchw(intArrayOf(0xffff0000.toInt(), 0xff00ff80.toInt()))
        assertArrayEquals(floatArrayOf(1f, 0f, 0f, 1f, 0f, 128f / 255f), input, 0.000001f)
    }

    @Test
    fun alignmentRecoversScaleRotationAndTranslation() {
        val source = listOf(FacePoint(0.0, 0.0), FacePoint(1.0, 0.0), FacePoint(0.0, 1.0))
        val dest = listOf(FacePoint(10.0, 20.0), FacePoint(10.0, 22.0), FacePoint(8.0, 20.0))
        val transform = FaceAlignment.estimate(source, dest)
        val actual = transform.map(FacePoint(2.0, 3.0))
        assertEquals(4.0, actual.x, 1e-8)
        assertEquals(24.0, actual.y, 1e-8)
    }

    @Test
    fun canonicalLandmarksRemainUnchanged() {
        val t = FaceAlignment.estimate(FaceAlignment.target)
        assertEquals(1.0, t.a, 1e-8)
        assertEquals(0.0, t.b, 1e-8)
        assertEquals(0.0, t.tx, 1e-8)
        assertEquals(0.0, t.ty, 1e-8)
    }

    @Test
    fun degenerateLandmarksAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            FaceAlignment.estimate(List(5) { FacePoint(1.0, 1.0) })
        }
    }

    @Test
    fun matchingIsScaleInvariantAndComputesExpectedDistances() {
        val same = FaceMatcher.compare(floatArrayOf(3f, 4f), floatArrayOf(6f, 8f))
        assertEquals(1.0, same.cosine, 1e-6)
        assertTrue(same.isMatch)
        val orthogonal = FaceMatcher.compare(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f))
        assertEquals(0.0, orthogonal.cosine, 1e-8)
        assertEquals(90.0, orthogonal.angleDegrees, 1e-8)
        assertEquals(kotlin.math.sqrt(2.0), orthogonal.distance, 1e-8)
        assertFalse(orthogonal.isMatch)
    }

    @Test
    fun invalidEmbeddingsFailRatherThanProduceAMatch() {
        listOf(floatArrayOf(), floatArrayOf(0f, 0f), floatArrayOf(Float.NaN), floatArrayOf(Float.POSITIVE_INFINITY)).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { FaceMatcher.normalize(invalid) }
        }
        assertThrows(IllegalArgumentException::class.java) { FaceMatcher.compare(floatArrayOf(1f), floatArrayOf(1f, 2f)) }
    }

    @Test
    fun upstreamAngleBoundaryIsStrict() {
        assertTrue(FaceComparison(0.0, 74.179, 0.0).isMatch)
        assertFalse(FaceComparison(0.0, 74.18, 0.0).isMatch)
    }
}
