package com.divanshgandhi.attendai.ui

import androidx.lifecycle.ViewModel
import com.divanshgandhi.attendai.data.repository.AuthRepository

class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {
    val session = authRepository.session

    fun signOut() = authRepository.signOut()
}
