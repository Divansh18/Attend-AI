package com.divanshgandhi.attendai.data.repository

import com.divanshgandhi.attendai.data.local.*
import com.divanshgandhi.attendai.location.LocationFix
import com.divanshgandhi.attendai.storage.PrivateSelfieStorage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import java.io.File

class AttendanceStorageTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun failedInsertRemovesItsSelfie() = runTest {
        val dir = folder.newFolder()
        val repo = RoomAttendanceRepository(FakeDao(fail = true), PrivateSelfieStorage(dir))
        try { repo.record(1, byteArrayOf(1, 2), LocationFix(1.0, 2.0, null)); fail("Expected failure") } catch (_: IllegalStateException) { }
        assertTrue(dir.listFiles()!!.isEmpty())
    }
    @Test fun successfulSaveKeepsOnlyReferencedFileAndCleansInterruptedWrites() = runTest {
        val dir = folder.newFolder(); File(dir, "unfinished.jpg.tmp").writeBytes(byteArrayOf(9))
        val dao = FakeDao(); val repo = RoomAttendanceRepository(dao, PrivateSelfieStorage(dir))
        val row = repo.record(1, byteArrayOf(1, 2), LocationFix(1.0, 2.0, 3f))
        assertEquals(1L, row.id); assertArrayEquals(byteArrayOf(1, 2), File(dir, row.selfieFileName).readBytes())
        assertEquals(1, dir.listFiles()!!.size)
        repo.record(1, byteArrayOf(3), LocationFix(1.0, 2.0, null))
        assertEquals(2, dir.listFiles()!!.size)
    }
    @Test fun invalidCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { LocationFix(Double.NaN, 0.0, null) }
        assertThrows(IllegalArgumentException::class.java) { LocationFix(91.0, 0.0, null) }
        assertThrows(IllegalArgumentException::class.java) { LocationFix(0.0, 0.0, -1f) }
    }
    private class FakeDao(val fail: Boolean = false) : AttendanceDao {
        val rows = mutableListOf<AttendanceEntity>()
        override suspend fun insert(attendance: AttendanceEntity): Long {
            if (fail) error("disk failure")
            rows += attendance; return rows.size.toLong()
        }
        override fun observeHistory(staffId: Long) = flowOf(rows.filter { it.staffId == staffId }.sortedByDescending { it.timestamp })
        override fun observeLatest(staffId: Long) = flowOf(rows.lastOrNull())
        override suspend fun selfieFileNames() = rows.map { it.selfieFileName }
    }
}
