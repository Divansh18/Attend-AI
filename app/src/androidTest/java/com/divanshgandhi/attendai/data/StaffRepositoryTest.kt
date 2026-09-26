package com.divanshgandhi.attendai.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.divanshgandhi.attendai.camera.CapturedPhoto
import com.divanshgandhi.attendai.data.local.*
import com.divanshgandhi.attendai.data.repository.*
import com.divanshgandhi.attendai.face.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StaffRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "staff-test-${UUID.randomUUID()}.db"
    private lateinit var database: AttendAiDatabase
    private lateinit var repository: RoomStaffRepository
    private fun open() {
        database = Room.databaseBuilder(context, AttendAiDatabase::class.java, name).build()
        repository = RoomStaffRepository(database.staffDao())
    }
    @Before fun setUp() = open()
    @After fun tearDown() { database.close(); context.deleteDatabase(name) }

    @Test fun creationTrimsFieldsAndObservesUnenrolledStaff() = runBlocking {
        assertTrue(repository.observeStaff().first().isEmpty())
        val id = repository.addStaff("  Ada Lovelace  ", " emp-01 ")
        assertTrue(id > 0)
        val row = repository.observeStaff(id).first()!!
        assertEquals("Ada Lovelace", row.name); assertEquals("EMP-01", row.employeeId)
        assertFalse(row.isFaceEnrolled); assertNull(row.faceEmbedding)
        assertEquals(id, repository.observeStaff().first().single().id)
    }
    @Test fun duplicateIdsIncludingCaseAndWhitespaceAreRejected() = runBlocking {
        repository.addStaff("First", "emp-01")
        try { repository.addStaff("Second", " EMP-01 "); fail("Accepted duplicate") }
        catch (_: DuplicateEmployeeIdException) { }
        try { database.staffDao().insert(StaffEntity(name = "Direct", employeeId = "EMP-01")); fail("Unique index missing") }
        catch (_: SQLiteConstraintException) { }
        assertEquals("First", repository.observeStaff().first().single().name)
    }
    @Test fun concurrentDuplicateSubmissionsCreateOnlyOneRow() = runBlocking {
        val results = coroutineScope { (1..2).map { async(Dispatchers.IO) {
            try { repository.addStaff("Staff $it", "RACE"); true } catch (_: DuplicateEmployeeIdException) { false }
        } }.awaitAll() }
        assertEquals(1, results.count { it }); assertEquals(1, repository.observeStaff().first().size)
    }
    @Test fun embeddingAndReEnrollmentSurviveDatabaseReopen() = runBlocking {
        val id = repository.addStaff("Ada", "A1")
        val first = FloatArray(128) { (it + 1).toFloat() }
        repository.saveFaceEmbedding(id, first)
        val enrolled = repository.observeStaff(id).first()!!
        assertTrue(enrolled.isFaceEnrolled); assertNotNull(enrolled.faceEnrolledAt)
        assertEquals(TfliteFaceEmbedder.MODEL_SHA256, enrolled.faceModelId)
        assertArrayEquals(FaceMatcher.normalize(first), enrolled.faceEmbedding, 0f)
        database.close(); open()
        assertArrayEquals(enrolled.faceEmbedding, repository.observeStaff(id).first()!!.faceEmbedding, 0f)
        val second = FloatArray(128) { (128 - it).toFloat() }
        repository.saveFaceEmbedding(id, second)
        database.close(); open()
        val replaced = repository.observeStaff(id).first()!!
        assertEquals(id, replaced.id); assertEquals("A1", replaced.employeeId)
        assertArrayEquals(FaceMatcher.normalize(second), replaced.faceEmbedding, 0f)
        assertTrue(replaced.faceEnrolledAt!! >= enrolled.faceEnrolledAt!!)
        assertEquals(1, repository.observeStaff().first().size)
    }
    @Test fun missingStaffAndInvalidVectorsDoNotCreateEnrollment() = runBlocking {
        assertNull(repository.observeStaff(-1).first())
        try { repository.saveFaceEmbedding(-1, FloatArray(128) { 1f }); fail("Missing staff accepted") }
        catch (_: StaffNotFoundException) { }
        val id = repository.addStaff("Ada", "A1")
        for (bad in listOf(floatArrayOf(1f), FloatArray(128), FloatArray(128) { Float.NaN })) {
            try { repository.saveFaceEmbedding(id, bad); fail("Bad vector accepted") }
            catch (_: IllegalArgumentException) { }
        }
        assertFalse(repository.observeStaff(id).first()!!.isFaceEnrolled)
        try { repository.addStaff(" ", "A2"); fail("Blank name accepted") } catch (_: IllegalArgumentException) { }
        try { repository.addStaff("Ada", " "); fail("Blank ID accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun realModelEmbeddingIsPersistedAndStillMatchesAfterReopen() = runBlocking {
        val engine = FaceRecognitionEngine(context.assets)
        try {
            val id = repository.addStaff("Fixture person", "FIXTURE")
            val embedding = engine.captureEmbedding {
                val assets = InstrumentationRegistry.getInstrumentation().context.assets
                val original = assets.open("faces/person_a_2009.jpg").use { BitmapFactory.decodeStream(it)!! }
                val scale = 1280.0 / maxOf(original.width, original.height)
                val resized = Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
                if (resized !== original) original.recycle()
                CapturedPhoto(resized, 0)
            }
            repository.saveFaceEmbedding(id, embedding)
            database.close(); open()
            val restored = repository.observeStaff(id).first()!!
            assertEquals(128, restored.faceEmbedding!!.size)
            assertTrue(FaceMatcher.compare(embedding, restored.faceEmbedding).cosine > 0.99999)
        } finally { engine.close() }
    }
}
