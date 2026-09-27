package com.divanshgandhi.attendai.ui.attendance

import com.divanshgandhi.attendai.camera.CapturedPhoto
import com.divanshgandhi.attendai.data.local.*
import com.divanshgandhi.attendai.data.repository.*
import com.divanshgandhi.attendai.face.*
import com.divanshgandhi.attendai.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {
    private val staff = FakeStaff()
    private val attendance = FakeAttendance()
    private val verifier = FakeVerifier()
    private val location = FakeLocation()
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun vm() = AttendanceViewModel(7, staff, attendance, verifier, location)
    private val capture: suspend () -> CapturedPhoto = { error("Test verifier supplies its own deterministic result") }

    @Test fun unenrolledStaffCannotCaptureOrCreateAttendance() = runTest {
        staff.row.value = StaffEntity(7, "A7", "Ada")
        val vm = vm(); advanceUntilIdle(); vm.markAttendance(capture); advanceUntilIdle()
        assertTrue(vm.state.value.error!!.contains("not enrolled")); assertEquals(0, verifier.calls); assertEquals(0, attendance.writes)
    }
    @Test fun mismatchNeverRequestsLocationOrWritesAttendance() = runTest {
        verifier.failure = FaceMismatchException(FaceComparison(0.0, 90.0, 1.414))
        val vm = vm(); advanceUntilIdle(); vm.markAttendance(capture); advanceUntilIdle()
        assertTrue(vm.state.value.mismatch); assertNull(vm.state.value.success)
        assertEquals(0, location.calls); assertEquals(0, attendance.writes)
    }
    @Test fun successfulMatchSavesForAuthenticatedStaffOnlyAndGuardsRepeatedTaps() = runTest {
        val vm = vm(); advanceUntilIdle(); vm.markAttendance(capture); vm.markAttendance(capture)
        assertTrue(vm.state.value.busy); advanceUntilIdle()
        assertEquals(7L, vm.state.value.success!!.staffId)
        assertArrayEquals(staff.row.value!!.faceEmbedding, verifier.reference, 0f)
        assertEquals(1, attendance.writes); assertFalse(vm.state.value.busy)
        vm.markAttendance(capture); advanceUntilIdle(); assertEquals(1, attendance.writes)
        vm.startAnother(); vm.markAttendance(capture); advanceUntilIdle(); assertEquals(2, attendance.writes)
    }
    @Test fun deniedDisabledUnavailableAndTimeoutLocationsDoNotWrite() = runTest {
        for (message in listOf("permission denied", "location disabled", "unavailable", "timeout")) {
            location.failure = LocationFailure(message)
            val vm = vm(); advanceUntilIdle(); vm.markAttendance(capture); advanceUntilIdle()
            assertEquals(message, vm.state.value.error); assertNull(vm.state.value.success)
        }
        assertEquals(0, attendance.writes)
    }
    @Test fun detectionErrorDoesNotRequestLocationOrWrite() = runTest {
        verifier.failure = FaceProcessingException(FaceError.MULTIPLE_FACES, "Multiple faces detected.")
        val vm = vm(); advanceUntilIdle(); vm.markAttendance(capture); advanceUntilIdle()
        assertEquals("Multiple faces detected.", vm.state.value.error); assertEquals(0, location.calls); assertEquals(0, attendance.writes)
    }
    @Test fun databaseFailureClearsBusyAndAllowsRetry() = runTest {
        attendance.failure = Exception("disk")
        val vm = vm(); advanceUntilIdle(); vm.markAttendance(capture); advanceUntilIdle()
        assertFalse(vm.state.value.busy); assertNull(vm.state.value.success); assertNotNull(vm.state.value.error)
        attendance.failure = null; vm.markAttendance(capture); advanceUntilIdle(); assertNotNull(vm.state.value.success)
    }
    @Test fun incompatibleModelRequiresReEnrollment() = runTest {
        staff.row.value = staff.row.value!!.copy(faceModelId = "different-model")
        val vm = vm(); advanceUntilIdle(); vm.markAttendance(capture); advanceUntilIdle()
        assertTrue(vm.state.value.error!!.contains("re-enroll")); assertEquals(0, verifier.calls); assertEquals(0, attendance.writes)
    }
    @Test fun lastAttendanceSurvivesNewViewModel() = runTest {
        val vm = vm(); advanceUntilIdle(); vm.markAttendance(capture); advanceUntilIdle()
        val reopened = vm(); advanceUntilIdle()
        assertEquals(7L, reopened.state.value.latest!!.staffId); assertNull(reopened.state.value.success)
    }
    private class FakeStaff : StaffRepository {
        val row = MutableStateFlow<StaffEntity?>(StaffEntity(7, "A7", "Ada", FloatArray(128) { 0.1f }, TfliteFaceEmbedder.MODEL_SHA256, 1))
        override fun observeStaff() = row.map { listOfNotNull(it) }
        override fun observeStaff(id: Long) = row.map { it?.takeIf { s -> s.id == id } }
        override suspend fun addStaff(name: String, employeeId: String) = error("unused")
        override suspend fun saveFaceEmbedding(staffId: Long, embedding: FloatArray) = Unit
    }
    private class FakeAttendance : AttendanceRepository {
        val last = MutableStateFlow<AttendanceEntity?>(null)
        var writes = 0; var failure: Exception? = null
        override fun observeHistory(staffId: Long) = last.map { listOfNotNull(it) }
        override fun observeLatest(staffId: Long) = last
        override suspend fun record(staffId: Long, selfieJpeg: ByteArray, location: LocationFix): AttendanceEntity {
            failure?.let { throw it }; writes++
            return AttendanceEntity(writes.toLong(), staffId, 1000, "test.jpg", location.latitude, location.longitude, location.accuracyMeters).also { last.value = it }
        }
    }
    private class FakeVerifier : AttendanceFaceVerifier {
        var calls = 0; var failure: Exception? = null; var reference: FloatArray? = null
        override suspend fun verify(takePhoto: suspend () -> CapturedPhoto, enrolledEmbedding: FloatArray): VerifiedSelfie {
            calls++; reference = enrolledEmbedding.copyOf(); failure?.let { throw it }
            return VerifiedSelfie(byteArrayOf(1, 2), FaceComparison(1.0, 0.0, 0.0))
        }
        override suspend fun close() = Unit
    }
    private class FakeLocation : LocationProvider {
        var calls = 0; var failure: Exception? = null
        override suspend fun currentLocation(): LocationFix {
            calls++; failure?.let { throw it }; return LocationFix(28.6, 77.2, 10f)
        }
    }
}
