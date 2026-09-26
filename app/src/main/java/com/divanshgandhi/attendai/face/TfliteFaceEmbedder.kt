package com.divanshgandhi.attendai.face

import android.content.res.AssetManager
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/** Owned by FaceRecognitionEngine, which serializes all access on background threads. */
class TfliteFaceEmbedder(
    private val assets: AssetManager,
    private val modelPath: String = MODEL_PATH,
) : AutoCloseable {
    private var interpreter: Interpreter? = null
    private var modelBuffer: ByteBuffer? = null

    private fun load(): Interpreter {
        interpreter?.let { return it }
        try {
            val bytes = assets.open(modelPath).use { it.readBytes() }
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            check(hash == MODEL_SHA256) { "Model checksum differs from the pinned export." }
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply { put(bytes); rewind() }
            val loaded = Interpreter(buffer, Interpreter.Options().setNumThreads(2))
            try {
                check(loaded.inputTensorCount == 2 && loaded.outputTensorCount == 1)
                repeat(2) { index ->
                    check(loaded.getInputTensor(index).shape().contentEquals(intArrayOf(1, 3, 112, 112)))
                    check(loaded.getInputTensor(index).dataType() == DataType.FLOAT32)
                }
                check(loaded.getOutputTensor(0).shape().contentEquals(intArrayOf(2, 128)))
                check(loaded.getOutputTensor(0).dataType() == DataType.FLOAT32)
            } catch (e: Exception) {
                loaded.close()
                throw e
            }
            modelBuffer = buffer
            interpreter = loaded
            return loaded
        } catch (e: Exception) {
            throw FaceProcessingException(FaceError.MODEL_LOADING, "Cannot load the bundled face model. Reinstall the debug APK or check its assets.", e)
        } catch (e: LinkageError) {
            throw FaceProcessingException(FaceError.MODEL_LOADING, "LiteRT is unavailable on this device.", e)
        }
    }

    fun embed(input: FloatArray): FloatArray {
        val runtime = load()
        try {
            require(input.size == 3 * 112 * 112 && input.all { it.isFinite() && it in 0f..1f })
            val buffer = ByteBuffer.allocateDirect(input.size * 4).order(ByteOrder.nativeOrder())
            buffer.asFloatBuffer().put(input)
            val output = Array(2) { FloatArray(128) }
            // This publisher exports a pair graph. Duplicating the crop yields a standalone
            // embedding in row 0; row 1 is redundant. No reference photo needs to be retained.
            runtime.runForMultipleInputsOutputs(arrayOf(buffer, buffer.duplicate().order(ByteOrder.nativeOrder())), mutableMapOf<Int, Any>(0 to output))
            return FaceMatcher.normalize(output[0])
        } catch (e: Exception) {
            throw FaceProcessingException(FaceError.INFERENCE, "Face embedding inference failed. Try another capture.", e)
        }
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        modelBuffer = null
    }

    companion object {
        const val MODEL_PATH = "models/mobile_facenet/mobile_facenet.tflite"
        const val MODEL_SHA256 = "4a5cfa4491ca0b02d4ea1552b808568b1569921d999b36c82b79def393d116ea"
    }
}
