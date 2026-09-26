package com.divanshgandhi.attendai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.divanshgandhi.attendai.R
import com.divanshgandhi.attendai.model.UserSession
import com.divanshgandhi.attendai.ui.admin.stafflist.StaffListScreen
import com.divanshgandhi.attendai.ui.components.PlaceholderScreen
import com.divanshgandhi.attendai.ui.login.LoginScreen
import com.divanshgandhi.attendai.ui.login.LoginViewModel

@Composable
fun AttendAiNavHost(
    session: UserSession?,
    viewModelFactory: ViewModelProvider.Factory,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val startDestination: Any = when (session) {
        null -> Login
        UserSession.Admin -> AdminStaffList
        is UserSession.Staff -> StaffAttendance
    }

    NavHost(navController = navController, startDestination = startDestination) {
        // Only destinations authorized for the current session enter the graph.
        when (session) {
            null -> composable<Login> {
                val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
                val state by loginViewModel.uiState.collectAsStateWithLifecycle()
                LoginScreen(
                    state = state,
                    onRoleChange = loginViewModel::onRoleChange,
                    onUsernameChange = loginViewModel::onUsernameChange,
                    onPasswordChange = loginViewModel::onPasswordChange,
                    onSignIn = loginViewModel::signIn,
                )
            }
            UserSession.Admin -> {
                composable<AdminStaffList> {
                    StaffListScreen(onSignOut = onSignOut)
                }
                composable<AddStaff> {
                    PlaceholderScreen(
                        title = stringResource(R.string.add_staff),
                        message = stringResource(R.string.feature_coming_soon),
                        actionLabel = stringResource(R.string.back),
                        onAction = { navController.popBackStack() },
                    )
                }
                composable<StaffProfile> { entry ->
                    val route = entry.toRoute<StaffProfile>()
                    PlaceholderScreen(
                        title = stringResource(R.string.staff_profile),
                        message = stringResource(R.string.staff_feature_coming_soon, route.staffId),
                        actionLabel = stringResource(R.string.back),
                        onAction = { navController.popBackStack() },
                    )
                }
                composable<FaceEnrollment> { entry ->
                    val route = entry.toRoute<FaceEnrollment>()
                    PlaceholderScreen(
                        title = stringResource(R.string.face_enrollment),
                        message = stringResource(R.string.staff_feature_coming_soon, route.staffId),
                        actionLabel = stringResource(R.string.back),
                        onAction = { navController.popBackStack() },
                    )
                }
            }
            is UserSession.Staff -> composable<StaffAttendance> {
                PlaceholderScreen(
                    title = stringResource(R.string.attendance),
                    message = stringResource(R.string.feature_coming_soon),
                    actionLabel = stringResource(R.string.sign_out),
                    onAction = onSignOut,
                )
            }
        }
    }
}
