package com.divanshgandhi.attendai.data.local

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Lossless IEEE-754 floats, four bytes each, explicit little-endian order. Null means not enrolled. */
class EmbeddingConverters {
    @TypeConverter
    fun toBytes(values: FloatArray?): ByteArray? = values?.let {
        ByteBuffer.allocate(it.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
            it.forEach { value -> putFloat(value) }
        }.array()
    }

    @TypeConverter
    fun fromBytes(bytes: ByteArray?): FloatArray? = bytes?.let {
        require(it.isNotEmpty() && it.size % 4 == 0) { "Invalid stored embedding." }
        val buffer = ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN)
        FloatArray(it.size / 4) { buffer.float }
    }
}
