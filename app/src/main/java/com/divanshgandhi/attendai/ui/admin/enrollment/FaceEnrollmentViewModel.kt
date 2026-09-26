package com.divanshgandhi.attendai.ui.admin.enrollment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divanshgandhi.attendai.camera.CapturedPhoto
import com.divanshgandhi.attendai.data.local.StaffEntity
import com.divanshgandhi.attendai.data.repository.StaffNotFoundException
import com.divanshgandhi.attendai.data.repository.StaffRepository
import com.divanshgandhi.attendai.face.FaceEnrollmentProcessor
import com.divanshgandhi.attendai.face.FaceProcessingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FaceEnrollmentUiState(
    val loading: Boolean = true, val staff: StaffEntity? = null, val loadError: String? = null,
    val busy: Boolean = false, val error: String? = null, val saved: Boolean = false,
)

class FaceEnrollmentViewModel(
    private val staffId: Long,
    private val repository: StaffRepository,
    private val processor: FaceEnrollmentProcessor,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FaceEnrollmentUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    init { retry() }
    fun retry() {
        if (state.value.busy || state.value.saved) return
        observation?.cancel()
        mutableState.value = FaceEnrollmentUiState()
        observation = viewModelScope.launch {
            repository.observeStaff(staffId)
                .catch { mutableState.update { it.copy(loading = false, loadError = "Could not load staff. Please retry.") } }
                .collect { staff -> mutableState.update { it.copy(loading = false, staff = staff) } }
        }
    }
    fun enroll(takePhoto: suspend () -> CapturedPhoto) {
        val current = state.value
        if (current.busy || current.saved || current.loading || current.staff == null || current.loadError != null) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val embedding = processor.captureEmbedding(takePhoto)
                repository.saveFaceEmbedding(staffId, embedding)
                mutableState.update { it.copy(saved = true) }
            } catch (e: TimeoutCancellationException) {
                mutableState.update { it.copy(error = "Camera capture timed out. Please retry.") }
            } catch (e: CancellationException) { throw e
            } catch (e: StaffNotFoundException) {
                mutableState.update { it.copy(staff = null, error = e.message) }
            } catch (e: FaceProcessingException) {
                mutableState.update { it.copy(error = e.message) }
            } catch (e: Exception) {
                mutableState.update { it.copy(error = "Could not capture or save the face. Please retry.") }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }
    override fun onCleared() {
        CoroutineScope(Dispatchers.Default).launch { processor.close() }
    }
}
