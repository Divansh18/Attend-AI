package com.divanshgandhi.attendai.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divanshgandhi.attendai.camera.CapturedPhoto
import com.divanshgandhi.attendai.data.local.AttendanceEntity
import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.data.repository.AttendanceRepository
import com.divanshgandhi.attendai.data.repository.StaffRepository
import com.divanshgandhi.attendai.face.*
import com.divanshgandhi.attendai.location.LocationFailure
import com.divanshgandhi.attendai.location.LocationProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AttendanceUiState(
    val loading: Boolean = true, val staff: StaffEntity? = null, val loadError: String? = null,
    val busy: Boolean = false, val progress: String? = null, val error: String? = null,
    val mismatch: Boolean = false, val success: AttendanceEntity? = null,
    val latest: AttendanceEntity? = null, val latestError: String? = null,
)

class AttendanceViewModel(
    private val staffId: Long,
    private val staffRepository: StaffRepository,
    private val attendanceRepository: AttendanceRepository,
    private val verifier: AttendanceFaceVerifier,
    private val location: LocationProvider,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AttendanceUiState())
    val state = mutableState.asStateFlow()
    private var staffJob: Job? = null
    private var latestJob: Job? = null
    init { retry() }

    fun retry() {
        if (state.value.busy) return
        staffJob?.cancel(); latestJob?.cancel()
        mutableState.update { it.copy(loading = true, loadError = null, latestError = null) }
        staffJob = viewModelScope.launch {
            staffRepository.observeStaff(staffId)
                .catch { mutableState.update { it.copy(loading = false, loadError = "Could not load your staff profile. Retry or contact Admin.") } }
                .collect { staff -> mutableState.update { it.copy(loading = false, staff = staff) } }
        }
        latestJob = viewModelScope.launch {
            attendanceRepository.observeLatest(staffId)
                .catch { mutableState.update { it.copy(latestError = "Could not load your last attendance. Please retry.") } }
                .collect { row -> mutableState.update { it.copy(latest = row) } }
        }
    }

    fun markAttendance(takePhoto: suspend () -> CapturedPhoto) {
        val current = state.value
        if (current.busy || current.loading || current.loadError != null || current.success != null) return
        if (current.staff?.isFaceEnrolled != true) {
            mutableState.update { it.copy(error = "Face not enrolled. Contact Admin to enroll first.") }
            return
        }
        mutableState.update { it.copy(busy = true, progress = "Verifying your face…", error = null, mismatch = false) }
        viewModelScope.launch {
            try {
                // Reload this authenticated ID immediately before verification, never search other employees.
                val staff = staffRepository.observeStaff(staffId).first()
                check(staff?.isFaceEnrolled == true) { "Face not enrolled. Contact Admin to enroll first." }
                check(staff.faceModelId == TfliteFaceEmbedder.MODEL_SHA256) { "Face model has changed. Contact Admin to re-enroll." }
                val verified = verifier.verify(takePhoto, checkNotNull(staff.faceEmbedding))
                mutableState.update { it.copy(progress = "Getting current location…") }
                val fix = location.currentLocation()
                mutableState.update { it.copy(progress = "Saving attendance…") }
                val row = attendanceRepository.record(staffId, verified.jpeg, fix)
                mutableState.update { it.copy(success = row) }
            } catch (e: TimeoutCancellationException) {
                mutableState.update { it.copy(error = "Camera capture timed out. Please retry.") }
            } catch (e: CancellationException) { throw e
            } catch (e: FaceMismatchException) {
                mutableState.update { it.copy(mismatch = true, error = e.message) }
            } catch (e: FaceProcessingException) {
                mutableState.update { it.copy(error = e.message) }
            } catch (e: LocationFailure) {
                mutableState.update { it.copy(error = e.message) }
            } catch (e: IllegalStateException) {
                mutableState.update { it.copy(error = e.message ?: "Attendance could not be saved. Please retry.") }
            } catch (e: Exception) {
                mutableState.update { it.copy(error = "Attendance could not be saved. Check permissions and storage, then retry.") }
            } finally { mutableState.update { it.copy(busy = false, progress = null) } }
        }
    }

    fun startAnother() {
        if (!state.value.busy) mutableState.update { it.copy(success = null, error = null, mismatch = false) }
    }

    override fun onCleared() {
        CoroutineScope(Dispatchers.Default).launch { verifier.close() }
    }
}
