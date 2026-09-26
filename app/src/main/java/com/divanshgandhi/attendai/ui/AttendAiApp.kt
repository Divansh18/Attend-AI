package com.divanshgandhi.attendai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divanshgandhi.attendai.di.AppContainer
import com.divanshgandhi.attendai.navigation.AttendAiNavHost

@Composable
fun AttendAiApp(container: AppContainer) {
    val sessionViewModel: SessionViewModel = viewModel(factory = container.viewModelFactory)
    val session by sessionViewModel.session.collectAsStateWithLifecycle()

    // A different session gets a fresh back stack, including after logout.
    key(session) {
        AttendAiNavHost(
            session = session,
            viewModelFactory = container.viewModelFactory,
            onSignOut = sessionViewModel::signOut,
        )
    }
}
