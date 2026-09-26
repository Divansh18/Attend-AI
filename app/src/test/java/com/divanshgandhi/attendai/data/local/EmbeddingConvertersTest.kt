package com.divanshgandhi.attendai.data.local

import org.junit.Assert.*
import org.junit.Test

class EmbeddingConvertersTest {
    private val converter = EmbeddingConverters()
    @Test fun roundTripPreservesAll128FloatBits() {
        val values = FloatArray(128) { (it - 64) / 127f }
        val bytes = converter.toBytes(values)!!
        assertEquals(512, bytes.size)
        assertArrayEquals(values, converter.fromBytes(bytes), 0f)
        assertArrayEquals(byteArrayOf(0, 0, -128, 63), converter.toBytes(floatArrayOf(1f)))
    }
    @Test fun nullRoundTripMeansNotEnrolled() {
        assertNull(converter.toBytes(null)); assertNull(converter.fromBytes(null))
    }
    @Test fun malformedBlobsAreRejected() {
        for (bytes in listOf(byteArrayOf(), byteArrayOf(1, 2, 3))) {
            assertThrows(IllegalArgumentException::class.java) { converter.fromBytes(bytes) }
        }
    }
}
