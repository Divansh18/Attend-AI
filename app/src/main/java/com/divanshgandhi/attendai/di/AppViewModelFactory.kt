package com.divanshgandhi.attendai.di

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.divanshgandhi.attendai.data.repository.AuthRepository
import com.divanshgandhi.attendai.ui.SessionViewModel
import com.divanshgandhi.attendai.ui.login.LoginViewModel

fun appViewModelFactory(authRepository: AuthRepository) = viewModelFactory {
    initializer { SessionViewModel(authRepository) }
    initializer { LoginViewModel(authRepository) }
}
