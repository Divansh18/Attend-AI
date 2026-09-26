package com.divanshgandhi.attendai.ui.facepoc

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divanshgandhi.attendai.camera.CapturedPhoto
import com.divanshgandhi.attendai.face.FaceComparison
import com.divanshgandhi.attendai.face.FaceMatcher
import com.divanshgandhi.attendai.face.FaceRecognitionEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class FacePocUiState(
    val referenceReady: Boolean = false,
    val busy: Boolean = false,
    val crop: Bitmap? = null,
    val comparison: FaceComparison? = null,
    val elapsedMillis: Long? = null,
    val error: String? = null,
)

class FacePocViewModel(private val engine: FaceRecognitionEngine) : ViewModel() {
    private var reference: FloatArray? = null
    private val mutableState = MutableStateFlow(FacePocUiState())
    val state = mutableState.asStateFlow()

    fun capture(enroll: Boolean, takePhoto: suspend () -> CapturedPhoto) {
        if (state.value.busy || (!enroll && reference == null)) return
        mutableState.update { it.copy(busy = true, comparison = null, error = null, elapsedMillis = null) }
        viewModelScope.launch {
            try {
                val photo = withTimeout(15_000) { takePhoto() }
                val sample = engine.extract(photo)
                val comparison = if (enroll) {
                    reference = sample.embedding
                    null
                } else FaceMatcher.compare(checkNotNull(reference), sample.embedding)
                mutableState.update {
                    it.copy(referenceReady = true, crop = sample.alignedFace, comparison = comparison, elapsedMillis = sample.elapsedMillis)
                }
            } catch (e: TimeoutCancellationException) {
                mutableState.update { it.copy(error = "Camera capture timed out. Reopen this screen and retry.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.update { it.copy(error = e.message ?: "Capture failed. Please retry.") }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun reset() {
        if (state.value.busy) return
        reference = null
        mutableState.value = FacePocUiState()
    }

    override fun onCleared() {
        reference = null
        // Cleanup waits for any in-flight native operation without blocking the UI thread.
        CoroutineScope(Dispatchers.Default).launch { engine.close() }
    }
}
