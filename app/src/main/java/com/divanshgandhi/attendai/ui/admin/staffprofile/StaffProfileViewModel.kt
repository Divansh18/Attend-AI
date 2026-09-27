package com.divanshgandhi.attendai.ui.admin.staffprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divanshgandhi.attendai.data.local.AttendanceEntity
import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.data.repository.AttendanceRepository
import com.divanshgandhi.attendai.data.repository.StaffRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffProfileUiState(
    val loading: Boolean = true,
    val staff: StaffEntity? = null,
    val error: String? = null,
    val historyLoading: Boolean = true,
    val history: List<AttendanceEntity> = emptyList(),
    val historyError: String? = null,
)

class StaffProfileViewModel(
    private val staffId: Long,
    private val repository: StaffRepository,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(StaffProfileUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var historyObservation: Job? = null

    init { retry() }

    fun retry() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, error = null) }
        observation = viewModelScope.launch {
            repository.observeStaff(staffId)
                .catch { mutableState.update { it.copy(loading = false, error = "Could not load staff. Please retry.") } }
                .collect { staff -> mutableState.update { it.copy(loading = false, staff = staff) } }
        }
        retryHistory()
    }

    fun retryHistory() {
        historyObservation?.cancel()
        mutableState.update { it.copy(historyLoading = true, historyError = null) }
        historyObservation = viewModelScope.launch {
            attendanceRepository.observeHistory(staffId)
                .catch { mutableState.update { it.copy(historyLoading = false, historyError = "Could not load attendance history. Please retry.") } }
                .collect { rows -> mutableState.update { it.copy(historyLoading = false, history = rows) } }
        }
    }
}
