package com.divanshgandhi.attendai.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
import com.divanshgandhi.attendai.location.LocationFix
import com.divanshgandhi.attendai.storage.PrivateSelfieStorage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AttendancePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "attendance-test-${UUID.randomUUID()}.db"
    private val directory = File(context.cacheDir, name + "-selfies")
    private var database: AttendAiDatabase? = null
    private fun open(): AttendAiDatabase = Room.databaseBuilder(context, AttendAiDatabase::class.java, name)
        .addMigrations(MIGRATION_1_2).build().also { database = it }
    @After fun cleanup() { database?.close(); context.deleteDatabase(name); directory.deleteRecursively() }
    private fun photo(name: String): CapturedPhoto {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val original = assets.open("faces/$name").use { BitmapFactory.decodeStream(it)!! }
        val scale = 1280.0 / maxOf(original.width, original.height)
        val image = Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
        if (image !== original) original.recycle()
        return CapturedPhoto(image, 0)
    }
    @Test fun actualFaceVerificationStoresUprightSelfieAndLocationAcrossReopen() = runBlocking {
        var db = open()
        val staff = RoomStaffRepository(db.staffDao())
        val id = staff.addStaff("Fixture", "REAL-1")
        val engine = FaceRecognitionEngine(context.assets)
        try {
            val embedding = engine.captureEmbedding { photo("person_a_2009.jpg") }
            staff.saveFaceEmbedding(id, embedding)
            val verified = engine.verify({ photo("person_a_2012.jpg") }, staff.observeStaff(id).first()!!.faceEmbedding!!)
            assertTrue(verified.comparison.isMatch)
            val repo = RoomAttendanceRepository(db.attendanceDao(), PrivateSelfieStorage(directory))
            val row = repo.record(id, verified.jpeg, LocationFix(28.6139, 77.2090, 7.5f))
            assertTrue(row.timestamp > 0)
            val bitmap = BitmapFactory.decodeFile(File(directory, row.selfieFileName).absolutePath)
            assertNotNull(bitmap); assertTrue(bitmap.width > 112); assertTrue(bitmap.height > bitmap.width); bitmap.recycle()
            db.close(); db = open()
            val restored = db.attendanceDao().observeLatest(id).first()!!
            assertEquals(row, restored); assertTrue(File(directory, restored.selfieFileName).isFile)
            assertEquals(7.5f, restored.accuracyMeters!!, 0f)
        } finally { engine.close() }
    }
    @Test fun realDifferentFaceCannotProduceVerifiedSelfieOrAttendance() = runBlocking {
        val db = open(); val staff = RoomStaffRepository(db.staffDao()); val id = staff.addStaff("Fixture", "REAL-2")
        val engine = FaceRecognitionEngine(context.assets)
        try {
            val reference = engine.captureEmbedding { photo("person_a_2009.jpg") }
            try {
                val verified = engine.verify({ photo("person_b_2013.jpg") }, reference)
                RoomAttendanceRepository(db.attendanceDao(), PrivateSelfieStorage(directory)).record(id, verified.jpeg, LocationFix(0.0, 0.0, null))
                fail("Different person accepted")
            } catch (_: FaceMismatchException) { }
            assertNull(db.attendanceDao().observeLatest(id).first())
            assertTrue(directory.listFiles().isNullOrEmpty())
        } finally { engine.close() }
    }
    @Test fun foreignKeyFailureRollsBackSelfieAndLatestIsScopedToStaff() = runBlocking {
        val db = open(); val repo = RoomAttendanceRepository(db.attendanceDao(), PrivateSelfieStorage(directory))
        try { repo.record(999, byteArrayOf(1), LocationFix(0.0, 0.0, null)); fail("Missing staff accepted") }
        catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertTrue(directory.listFiles().isNullOrEmpty())
        val staff = RoomStaffRepository(db.staffDao()); val a = staff.addStaff("A", "A"); val b = staff.addStaff("B", "B")
        repo.record(a, byteArrayOf(1), LocationFix(0.0, 0.0, null))
        assertNotNull(repo.observeLatest(a).first()); assertNull(repo.observeLatest(b).first())
    }
    @Test fun migrationPreservesVersionOneStaffAndEmbeddings() = runBlocking {
        val path = context.getDatabasePath(name); path.parentFile!!.mkdirs()
        val vector = FloatArray(128) { 0.125f }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE staff (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, employeeId TEXT NOT NULL, name TEXT NOT NULL, faceEmbedding BLOB, faceModelId TEXT, faceEnrolledAt INTEGER)")
            old.execSQL("CREATE UNIQUE INDEX index_staff_employeeId ON staff(employeeId)")
            old.execSQL("INSERT INTO staff(id, employeeId, name, faceEmbedding, faceModelId, faceEnrolledAt) VALUES(42, 'OLD-1', 'Existing staff', ?, ?, 100)",
                arrayOf(EmbeddingConverters().toBytes(vector), TfliteFaceEmbedder.MODEL_SHA256))
            old.version = 1
        }
        val db = open()
        val restored = db.staffDao().observeById(42).first()!!
        assertEquals("Existing staff", restored.name); assertTrue(restored.isFaceEnrolled)
        assertArrayEquals(vector, restored.faceEmbedding, 0f)
        val result = RoomAttendanceRepository(db.attendanceDao(), PrivateSelfieStorage(directory)).record(42, byteArrayOf(1), LocationFix(1.0, 2.0, 3f))
        assertEquals(42L, result.staffId)
        assertEquals(2, db.openHelper.readableDatabase.version)
    }
}
