package com.divanshgandhi.attendai.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.divanshgandhi.attendai.data.local.AttendanceEntity
import com.divanshgandhi.attendai.data.local.AttendAiDatabase
import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.data.repository.RoomAttendanceRepository
import com.divanshgandhi.attendai.storage.PrivateSelfieStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AttendanceHistoryTest {
    private lateinit var database: AttendAiDatabase
    private lateinit var repository: RoomAttendanceRepository
    private lateinit var selfieDirectory: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AttendAiDatabase::class.java).build()
        selfieDirectory = File(context.cacheDir, "history-${UUID.randomUUID()}")
        repository = RoomAttendanceRepository(
            database.attendanceDao(),
            PrivateSelfieStorage(selfieDirectory),
        )
    }

    @After
    fun tearDown() {
        database.close()
        selfieDirectory.deleteRecursively()
    }

    @Test
    fun historyIsNewestFirstWithStableTiesAndIsolatedByStaff() = runBlocking {
        val firstStaff = insertStaff("EMP-A")
        val secondStaff = insertStaff("EMP-B")

        val middle = insertAttendance(firstStaff, timestamp = 2_000)
        val oldest = insertAttendance(firstStaff, timestamp = 1_000)
        insertAttendance(secondStaff, timestamp = 9_000)
        val sameTimeEarlierId = insertAttendance(firstStaff, timestamp = 3_000)
        val sameTimeLaterId = insertAttendance(firstStaff, timestamp = 3_000)

        val firstHistory = repository.observeHistory(firstStaff).first()
        assertEquals(
            listOf(sameTimeLaterId, sameTimeEarlierId, middle, oldest),
            firstHistory.map { it.id },
        )
        assertTrue(firstHistory.all { it.staffId == firstStaff })
        assertEquals(1, repository.observeHistory(secondStaff).first().size)
        assertTrue(repository.observeHistory(-1).first().isEmpty())
    }

    @Test
    fun emptyHistoryFlowUpdatesAfterAttendanceInsert() = runBlocking {
        val staffId = insertStaff("EMP-A")
        val firstEmission = CompletableDeferred<Unit>()
        val emissions = async {
            repository.observeHistory(staffId)
                .onEach { firstEmission.complete(Unit) }
                .take(2)
                .toList()
        }

        withTimeout(5_000) { firstEmission.await() }
        val insertedId = insertAttendance(staffId, timestamp = 1_000)
        val values = withTimeout(5_000) { emissions.await() }

        assertTrue(values.first().isEmpty())
        assertEquals(insertedId, values.last().single().id)
    }

    private suspend fun insertStaff(employeeId: String): Long = database.staffDao().insert(
        StaffEntity(employeeId = employeeId, name = employeeId),
    )

    private suspend fun insertAttendance(staffId: Long, timestamp: Long): Long =
        database.attendanceDao().insert(
            AttendanceEntity(
                staffId = staffId,
                timestamp = timestamp,
                selfieFileName = "${UUID.randomUUID()}.jpg",
                latitude = 1.0,
                longitude = 2.0,
                accuracyMeters = null,
            ),
        )
}
