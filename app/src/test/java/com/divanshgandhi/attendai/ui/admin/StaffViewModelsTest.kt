package com.divanshgandhi.attendai.ui.admin

import com.divanshgandhi.attendai.data.local.AttendanceEntity
import com.divanshgandhi.attendai.location.LocationFix
import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.data.repository.*
import com.divanshgandhi.attendai.camera.CapturedPhoto
import com.divanshgandhi.attendai.face.*
import com.divanshgandhi.attendai.ui.admin.addstaff.AddStaffViewModel
import com.divanshgandhi.attendai.ui.admin.enrollment.FaceEnrollmentViewModel
import com.divanshgandhi.attendai.ui.admin.stafflist.StaffListViewModel
import com.divanshgandhi.attendai.ui.admin.staffprofile.StaffProfileViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModelsTest {
    private val repository = FakeStaffRepository()
    private val historyRepository = object : AttendanceRepository {
        override fun observeHistory(staffId: Long) = flowOf(emptyList<AttendanceEntity>())
        override fun observeLatest(staffId: Long) = flowOf<AttendanceEntity?>(null)
        override suspend fun record(staffId: Long, selfieJpeg: ByteArray, location: LocationFix): AttendanceEntity = error("unused")
    }
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun blankAddFormDoesNotCallRepository() {
        val vm = AddStaffViewModel(repository)
        vm.onNameChange(" "); vm.onEmployeeIdChange(" "); vm.submit()
        assertEquals("Enter a name.", vm.state.value.nameError)
        assertEquals("Enter an employee ID.", vm.state.value.employeeIdError)
        assertEquals(0, repository.addCalls)
        vm.onNameChange("Ada"); vm.onEmployeeIdChange("A1")
        assertNull(vm.state.value.nameError); assertNull(vm.state.value.employeeIdError)
    }
    @Test fun successfulAddExposesIdAndBlocksRepeatedTaps() = runTest {
        val vm = AddStaffViewModel(repository)
        vm.onNameChange("Ada"); vm.onEmployeeIdChange("A1"); vm.submit(); vm.submit()
        assertTrue(vm.state.value.submitting)
        vm.onNameChange("Changed")
        advanceUntilIdle()
        assertEquals(1L, vm.state.value.createdId)
        assertFalse(vm.state.value.submitting)
        assertEquals("Ada", repository.rows.value.single().name)
        vm.submit(); advanceUntilIdle(); assertEquals(1, repository.addCalls)
    }
    @Test fun duplicateIdShowsFieldErrorAndCanBeRetried() = runTest {
        val vm = AddStaffViewModel(repository)
        vm.onNameChange("Ada"); vm.onEmployeeIdChange("A1")
        repository.failure = DuplicateEmployeeIdException(); vm.submit(); advanceUntilIdle()
        assertEquals("That employee ID already exists.", vm.state.value.employeeIdError)
        assertNull(vm.state.value.createdId)
        repository.failure = null; vm.onEmployeeIdChange("A2"); vm.submit(); advanceUntilIdle()
        assertEquals(1L, vm.state.value.createdId)
    }
    @Test fun writeFailureKeepsFormAndAllowsRetry() = runTest {
        val vm = AddStaffViewModel(repository)
        vm.onNameChange("Ada"); vm.onEmployeeIdChange("A1")
        repository.failure = IllegalStateException("disk"); vm.submit(); advanceUntilIdle()
        assertNotNull(vm.state.value.error); assertFalse(vm.state.value.submitting)
        assertEquals("Ada", vm.state.value.name)
    }
    @Test fun listAndProfileObserveEnrollmentChanges() = runTest {
        val list = StaffListViewModel(repository); val profile = StaffProfileViewModel(1, repository, historyRepository)
        advanceUntilIdle(); assertTrue(list.state.value.staff.isEmpty()); assertNull(profile.state.value.staff)
        repository.addStaff("Ada", "A1"); advanceUntilIdle()
        assertFalse(profile.state.value.staff!!.isFaceEnrolled)
        repository.saveFaceEmbedding(1, FloatArray(128) { 0.1f }); advanceUntilIdle()
        assertTrue(profile.state.value.staff!!.isFaceEnrolled)
        assertTrue(list.state.value.staff.single().isFaceEnrolled)
    }
    @Test fun invalidIdShowsMissingWithoutCapture() = runTest {
        val processor = FakeProcessor()
        val vm = FaceEnrollmentViewModel(-1, repository, processor)
        val profile = StaffProfileViewModel(-1, repository, historyRepository)
        advanceUntilIdle(); vm.enroll { error("Should not capture") }; advanceUntilIdle()
        assertFalse(vm.state.value.loading); assertNull(vm.state.value.staff)
        assertNull(profile.state.value.staff); assertEquals(0, processor.calls)
    }
    @Test fun enrollmentSavesOnceAndUpdatesStatus() = runTest {
        repository.addStaff("Ada", "A1")
        val processor = FakeProcessor(); val vm = FaceEnrollmentViewModel(1, repository, processor)
        advanceUntilIdle(); vm.enroll { error("Unit test processor does not use pixels") }; vm.enroll { error("duplicate") }
        assertTrue(vm.state.value.busy); advanceUntilIdle()
        assertTrue(vm.state.value.saved); assertFalse(vm.state.value.busy)
        assertTrue(repository.rows.value.single().isFaceEnrolled)
        assertEquals(1, processor.calls); assertEquals(1, repository.saveCalls)
    }
    @Test fun failedReEnrollmentKeepsOldEmbeddingAndCanRetry() = runTest {
        repository.addStaff("Ada", "A1"); repository.saveFaceEmbedding(1, FloatArray(128) { 1f })
        val processor = FakeProcessor().apply { failure = FaceProcessingException(FaceError.NO_FACE, "No face detected.") }
        val vm = FaceEnrollmentViewModel(1, repository, processor); advanceUntilIdle()
        vm.enroll { error("unused") }; advanceUntilIdle()
        assertEquals("No face detected.", vm.state.value.error)
        assertEquals(1f, repository.rows.value.single().faceEmbedding!![0], 0f)
        assertFalse(vm.state.value.saved)
        processor.failure = null; vm.enroll { error("unused") }; advanceUntilIdle()
        assertTrue(vm.state.value.saved)
        assertEquals(0.1f, repository.rows.value.single().faceEmbedding!![0], 0f)
    }
    @Test fun saveFailureDoesNotReportEnrollmentSuccess() = runTest {
        repository.addStaff("Ada", "A1")
        val vm = FaceEnrollmentViewModel(1, repository, FakeProcessor()); advanceUntilIdle()
        repository.failure = IllegalStateException("disk"); vm.enroll { error("unused") }; advanceUntilIdle()
        assertFalse(vm.state.value.saved); assertNotNull(vm.state.value.error)
        assertFalse(repository.rows.value.single().isFaceEnrolled)
    }
    @Test fun staffRemovedDuringCaptureReportsMissing() = runTest {
        repository.addStaff("Ada", "A1")
        val vm = FaceEnrollmentViewModel(1, repository, FakeProcessor()); advanceUntilIdle()
        repository.failure = StaffNotFoundException(); vm.enroll { error("unused") }; advanceUntilIdle()
        assertNull(vm.state.value.staff); assertFalse(vm.state.value.saved)
    }
    @Test fun readFailuresExposeRetryAndRecover() = runTest {
        repository.readFailure = true
        val list = StaffListViewModel(repository); val profile = StaffProfileViewModel(1, repository, historyRepository)
        val enroll = FaceEnrollmentViewModel(1, repository, FakeProcessor()); advanceUntilIdle()
        assertNotNull(list.state.value.error); assertNotNull(profile.state.value.error); assertNotNull(enroll.state.value.loadError)
        repository.readFailure = false; list.retry(); profile.retry(); enroll.retry(); advanceUntilIdle()
        assertNull(list.state.value.error); assertNull(profile.state.value.error); assertNull(enroll.state.value.loadError)
    }

    private class FakeProcessor : FaceEnrollmentProcessor {
        var calls = 0
        var failure: Exception? = null
        override suspend fun captureEmbedding(takePhoto: suspend () -> CapturedPhoto): FloatArray {
            calls++; failure?.let { throw it }; return FloatArray(128) { 0.1f }
        }
        override suspend fun close() = Unit
    }
    private class FakeStaffRepository : StaffRepository {
        val rows = MutableStateFlow<List<StaffEntity>>(emptyList())
        var addCalls = 0; var saveCalls = 0; var failure: Exception? = null; var readFailure = false
        override fun observeStaff(): Flow<List<StaffEntity>> = if (readFailure) flow { error("read") } else rows
        override fun observeStaff(id: Long) = observeStaff().map { list -> list.find { it.id == id } }
        override suspend fun addStaff(name: String, employeeId: String): Long {
            addCalls++; failure?.let { throw it }
            val id = rows.value.size.toLong() + 1
            rows.value += StaffEntity(id = id, name = name, employeeId = employeeId)
            return id
        }
        override suspend fun saveFaceEmbedding(staffId: Long, embedding: FloatArray) {
            saveCalls++; failure?.let { throw it }
            rows.value = rows.value.map { if (it.id == staffId) it.copy(faceEmbedding = embedding, faceModelId = "test", faceEnrolledAt = 1) else it }
        }
    }
}
