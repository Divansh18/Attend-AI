package com.divanshgandhi.attendai.ui.admin.staffprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.data.repository.StaffRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class StaffProfileUiState(val loading: Boolean = true, val staff: StaffEntity? = null, val error: String? = null)

class StaffProfileViewModel(private val staffId: Long, private val repository: StaffRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(StaffProfileUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    init { retry() }
    fun retry() {
        observation?.cancel()
        mutableState.value = StaffProfileUiState()
        observation = viewModelScope.launch {
            repository.observeStaff(staffId)
                .catch { mutableState.value = StaffProfileUiState(loading = false, error = "Could not load staff. Please retry.") }
                .collect { mutableState.value = StaffProfileUiState(loading = false, staff = it) }
        }
    }
}
