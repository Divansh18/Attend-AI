package com.divanshgandhi.attendai.di

import com.divanshgandhi.attendai.data.repository.AuthRepository

/** Application-scoped dependencies, shared by the screen ViewModels. */
class AppContainer {
    val authRepository = AuthRepository()
    val viewModelFactory = appViewModelFactory(authRepository)
}
