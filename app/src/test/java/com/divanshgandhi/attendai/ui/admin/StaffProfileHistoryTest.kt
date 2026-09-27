package com.divanshgandhi.attendai.ui.admin

import androidx.lifecycle.ViewModelStore
import com.divanshgandhi.attendai.data.local.*
import com.divanshgandhi.attendai.data.repository.*
import com.divanshgandhi.attendai.location.LocationFix
import com.divanshgandhi.attendai.ui.admin.staffprofile.StaffProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class StaffProfileHistoryTest {
    private val staff = StaffSource()
    private val attendance = HistorySource()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun vm() = StaffProfileViewModel(7, staff, attendance).also { store.put("profile", it) }

    @Test fun loadingResolvesToEmptyHistoryForSelectedStaff() = runTest {
        val vm = vm()
        assertTrue(vm.state.value.historyLoading)
        advanceUntilIdle()
        assertEquals(7L, attendance.requestedId)
        assertFalse(vm.state.value.loading); assertFalse(vm.state.value.historyLoading)
        assertTrue(vm.state.value.history.isEmpty()); assertNull(vm.state.value.historyError)
    }
    @Test fun liveHistoryAndEnrollmentUpdatesDoNotOverwriteEachOther() = runTest {
        val vm = vm(); advanceUntilIdle()
        val rows = listOf(row(3, 300), row(2, 200))
        attendance.rows.value = rows; advanceUntilIdle()
        assertEquals(rows, vm.state.value.history)
        staff.row.value = staff.row.value!!.copy(faceEmbedding = FloatArray(128) { 1f }, faceModelId = "model", faceEnrolledAt = 100)
        advanceUntilIdle()
        assertTrue(vm.state.value.staff!!.isFaceEnrolled)
        assertEquals(rows, vm.state.value.history)
        attendance.rows.value = listOf(row(4, 400)) + rows; advanceUntilIdle()
        assertEquals(listOf(4L, 3L, 2L), vm.state.value.history.map { it.id })
    }
    @Test fun historyFailurePreservesProfileAndRetryRecovers() = runTest {
        attendance.fail = true
        val vm = vm(); advanceUntilIdle()
        assertEquals("Ada", vm.state.value.staff!!.name)
        assertNotNull(vm.state.value.historyError); assertFalse(vm.state.value.historyLoading)
        assertNull(vm.state.value.error)
        attendance.fail = false; attendance.rows.value = listOf(row(1, 100))
        vm.retryHistory()
        assertTrue(vm.state.value.historyLoading); assertFalse(vm.state.value.loading)
        advanceUntilIdle()
        assertNull(vm.state.value.historyError); assertEquals(1, vm.state.value.history.size)
    }
    private fun row(id: Long, timestamp: Long) = AttendanceEntity(id, 7, timestamp, "$id.jpg", 1.0, 2.0, null)
    private class StaffSource : StaffRepository {
        val row = MutableStateFlow<StaffEntity?>(StaffEntity(7, "EMP-7", "Ada"))
        override fun observeStaff() = row.map { listOfNotNull(it) }
        override fun observeStaff(id: Long) = row
        override suspend fun addStaff(name: String, employeeId: String): Long = error("unused")
        override suspend fun saveFaceEmbedding(staffId: Long, embedding: FloatArray) = Unit
    }
    private class HistorySource : AttendanceRepository {
        val rows = MutableStateFlow<List<AttendanceEntity>>(emptyList())
        var fail = false
        var requestedId: Long? = null
        override fun observeHistory(staffId: Long): Flow<List<AttendanceEntity>> {
            requestedId = staffId
            return if (fail) flow { error("read failure") } else rows
        }
        override fun observeLatest(staffId: Long) = rows.map { it.firstOrNull() }
        override suspend fun record(staffId: Long, selfieJpeg: ByteArray, location: LocationFix): AttendanceEntity = error("unused")
    }
}
