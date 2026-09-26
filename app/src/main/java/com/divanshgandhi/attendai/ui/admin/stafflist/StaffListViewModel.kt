package com.divanshgandhi.attendai.ui.admin.stafflist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.data.repository.StaffRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class StaffListUiState(val loading: Boolean = true, val staff: List<StaffEntity> = emptyList(), val error: String? = null)

class StaffListViewModel(private val repository: StaffRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(StaffListUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    init { retry() }

    fun retry() {
        observation?.cancel()
        mutableState.value = StaffListUiState()
        observation = viewModelScope.launch {
            repository.observeStaff()
                .catch { mutableState.value = StaffListUiState(loading = false, error = "Could not load staff. Please retry.") }
                .collect { mutableState.value = StaffListUiState(loading = false, staff = it) }
        }
    }
}
