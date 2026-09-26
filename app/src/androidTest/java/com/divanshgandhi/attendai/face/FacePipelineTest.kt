package com.divanshgandhi.attendai.face

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.divanshgandhi.attendai.camera.CapturedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class FacePipelineTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun fixture(name: String): Bitmap {
        val original = instrumentation.context.assets.open("faces/$name").use { BitmapFactory.decodeStream(it)!! }
        val scale = 1280.0 / maxOf(original.width, original.height)
        if (scale >= 1) return original
        return Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
            .also { original.recycle() }
    }

    @Test
    fun realModelSeparatesPhotographicFixturesAndHandlesOrientation() = runBlocking {
        val engine = FaceRecognitionEngine(instrumentation.targetContext.assets)
        try {
            suspend fun sample(name: String) = engine.extract(CapturedPhoto(fixture(name), 0))
            val reference = sample("person_a_2009.jpg")
            assertEquals(128, reference.embedding.size)
            assertEquals(1.0, reference.embedding.sumOf { it.toDouble() * it }, 1e-5)

            fun report(label: String, candidate: FaceSample): FaceComparison {
                val r = FaceMatcher.compare(reference.embedding, candidate.embedding)
                Log.i("AttendAI-FacePOC", String.format(Locale.US,
                    "%s cosine=%.8f angle=%.6f l2=%.8f match=%s ms=%d", label, r.cosine, r.angleDegrees, r.distance, r.isMatch, candidate.elapsedMillis))
                candidate.alignedFace.recycle()
                return r
            }

            repeat(3) { assertTrue(report("same_photo_repeat_$it", sample("person_a_2009.jpg")).cosine > 0.999) }
            val samePerson = report("same_person_independent_photo", sample("person_a_2012.jpg"))
            val different = report("different_person", sample("person_b_2013.jpg"))

            val source = fixture("person_a_2009.jpg")
            val dimmed = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            Canvas(dimmed).drawBitmap(source, 0f, 0f, Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                    0.8f, 0f, 0f, 0f, 15f, 0f, 0.8f, 0f, 0f, 15f,
                    0f, 0f, 0.8f, 0f, 15f, 0f, 0f, 0f, 1f, 0f,
                )))
            })
            source.recycle()
            assertTrue(report("same_photo_brightness_variant", engine.extract(CapturedPhoto(dimmed, 0))).isMatch)

            for (rotation in listOf(90, 180, 270)) {
                val base = fixture("person_a_2009.jpg")
                val rotated = ImageOrientation.upright(base, rotation, false)
                base.recycle()
                assertTrue(report("rotation_${rotation}_corrected", engine.extract(CapturedPhoto(rotated, 360 - rotation))).cosine > 0.999)
            }
            val base = fixture("person_a_2009.jpg")
            val mirrored = ImageOrientation.upright(base, 0, true)
            base.recycle()
            assertTrue(report("mirroring_corrected", engine.extract(CapturedPhoto(mirrored, 0, true))).cosine > 0.999)

            assertTrue("Same-person score must exceed different-person score", samePerson.cosine > different.cosine)
            assertTrue("Upstream threshold failed for the independent same-person fixture", samePerson.isMatch)
            assertFalse("Upstream threshold falsely accepted the different-person fixture", different.isMatch)
            reference.alignedFace.recycle()
        } finally { engine.close() }
    }

    @Test
    fun noFaceAndMultipleFacesAreRejected() = runBlocking {
        val engine = FaceRecognitionEngine(instrumentation.targetContext.assets)
        try {
            val blank = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
            try {
                engine.extract(CapturedPhoto(blank, 0))
                error("Blank image was accepted")
            } catch (e: FaceProcessingException) { assertEquals(FaceError.NO_FACE, e.reason) }

            val single = fixture("person_a_2009.jpg")
            val pair = Bitmap.createBitmap(single.width * 2, single.height, Bitmap.Config.ARGB_8888)
            Canvas(pair).apply {
                drawBitmap(single, 0f, 0f, null)
                drawBitmap(single, single.width.toFloat(), 0f, null)
            }
            single.recycle()
            try {
                engine.extract(CapturedPhoto(pair, 0))
                error("Two faces were accepted")
            } catch (e: FaceProcessingException) { assertEquals(FaceError.MULTIPLE_FACES, e.reason) }
        } finally { engine.close() }
    }

    @Test
    fun missingModelAndInvalidInferenceInputReturnExplicitErrors() = runBlocking {
        withContext(Dispatchers.Default) {
            TfliteFaceEmbedder(instrumentation.targetContext.assets, "missing-model.tflite").use {
                try { it.embed(FloatArray(3 * 112 * 112)); error("Missing model was accepted") }
                catch (e: FaceProcessingException) { assertEquals(FaceError.MODEL_LOADING, e.reason) }
            }
            TfliteFaceEmbedder(instrumentation.targetContext.assets).use {
                try { it.embed(floatArrayOf(Float.NaN)); error("Bad tensor was accepted") }
                catch (e: FaceProcessingException) { assertEquals(FaceError.INFERENCE, e.reason) }
            }
        }
    }

    @Test
    fun clockwiseRotationAndMirroringUseExpectedPixelOrder() {
        val pixels = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK, Color.YELLOW)
        val input = Bitmap.createBitmap(pixels, 2, 3, Bitmap.Config.ARGB_8888)
        val rotated = ImageOrientation.upright(input, 90, false)
        assertEquals(3, rotated.width)
        assertEquals(2, rotated.height)
        val actual = IntArray(6)
        rotated.getPixels(actual, 0, 3, 0, 0, 3, 2)
        assertArrayEquals(intArrayOf(Color.BLACK, Color.BLUE, Color.RED, Color.YELLOW, Color.WHITE, Color.GREEN), actual)
        val flipped = ImageOrientation.upright(rotated, 0, true)
        flipped.getPixels(actual, 0, 3, 0, 0, 3, 2)
        assertArrayEquals(intArrayOf(Color.RED, Color.BLUE, Color.BLACK, Color.GREEN, Color.WHITE, Color.YELLOW), actual)
        input.recycle(); rotated.recycle(); flipped.recycle()
    }
}
