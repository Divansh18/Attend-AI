package com.divanshgandhi.attendai.ui.admin.addstaff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divanshgandhi.attendai.data.repository.DuplicateEmployeeIdException
import com.divanshgandhi.attendai.data.repository.StaffRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddStaffUiState(
    val name: String = "", val employeeId: String = "",
    val nameError: String? = null, val employeeIdError: String? = null,
    val submitting: Boolean = false, val error: String? = null, val createdId: Long? = null,
)

class AddStaffViewModel(private val repository: StaffRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AddStaffUiState())
    val state = mutableState.asStateFlow()
    fun onNameChange(value: String) {
        if (!state.value.submitting && state.value.createdId == null) mutableState.update { it.copy(name = value, nameError = null, error = null) }
    }
    fun onEmployeeIdChange(value: String) {
        if (!state.value.submitting && state.value.createdId == null) mutableState.update { it.copy(employeeId = value, employeeIdError = null, error = null) }
    }
    fun submit() {
        val form = state.value
        if (form.submitting || form.createdId != null) return
        val nameError = if (form.name.isBlank()) "Enter a name." else null
        val idError = if (form.employeeId.isBlank()) "Enter an employee ID." else null
        mutableState.update { it.copy(nameError = nameError, employeeIdError = idError, error = null) }
        if (nameError != null || idError != null) return
        mutableState.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                val id = repository.addStaff(form.name, form.employeeId)
                mutableState.update { it.copy(createdId = id) }
            } catch (e: CancellationException) { throw e
            } catch (e: DuplicateEmployeeIdException) {
                mutableState.update { it.copy(employeeIdError = e.message) }
            } catch (e: Exception) {
                mutableState.update { it.copy(error = "Could not save staff. Please retry.") }
            } finally { mutableState.update { it.copy(submitting = false) } }
        }
    }
}
