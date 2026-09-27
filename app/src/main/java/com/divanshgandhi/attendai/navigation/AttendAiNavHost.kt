package com.divanshgandhi.attendai.navigation

import com.divanshgandhi.attendai.ui.attendance.AttendanceScreen
import com.divanshgandhi.attendai.ui.attendance.AttendanceViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.divanshgandhi.attendai.ui.admin.addstaff.AddStaffScreen
import com.divanshgandhi.attendai.ui.admin.addstaff.AddStaffViewModel
import com.divanshgandhi.attendai.ui.admin.staffprofile.StaffProfileScreen
import com.divanshgandhi.attendai.ui.admin.staffprofile.StaffProfileViewModel
import com.divanshgandhi.attendai.ui.admin.stafflist.StaffListViewModel
import com.divanshgandhi.attendai.ui.admin.enrollment.FaceEnrollmentScreen
import com.divanshgandhi.attendai.ui.admin.enrollment.FaceEnrollmentViewModel
import com.divanshgandhi.attendai.BuildConfig
import com.divanshgandhi.attendai.ui.facepoc.FacePocScreen
import com.divanshgandhi.attendai.ui.facepoc.FacePocViewModel
import com.divanshgandhi.attendai.model.UserSession
import com.divanshgandhi.attendai.ui.admin.stafflist.StaffListScreen
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
                    val vm: StaffListViewModel = viewModel(factory = viewModelFactory)
                    val state by vm.state.collectAsStateWithLifecycle()
                    StaffListScreen(state, onAdd = { navController.navigate(AddStaff) },
                        onStaff = { navController.navigate(StaffProfile(it)) }, onRetry = vm::retry,
                        onSignOut = onSignOut, onFacePoc = { navController.navigate(FaceProofOfConcept) })
                }
                if (BuildConfig.DEBUG) {
                    composable<FaceProofOfConcept> {
                        val faceViewModel: FacePocViewModel = viewModel(factory = viewModelFactory)
                        val state by faceViewModel.state.collectAsStateWithLifecycle()
                        FacePocScreen(
                            state = state,
                            onCapture = faceViewModel::capture,
                            onReset = faceViewModel::reset,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable<AddStaff> {
                    val vm: AddStaffViewModel = viewModel(factory = viewModelFactory)
                    val state by vm.state.collectAsStateWithLifecycle()
                    LaunchedEffect(state.createdId) {
                        state.createdId?.let { id ->
                            navController.navigate(StaffProfile(id)) { popUpTo<AddStaff> { inclusive = true } }
                        }
                    }
                    AddStaffScreen(state, vm::onNameChange, vm::onEmployeeIdChange, vm::submit) { navController.popBackStack() }
                }
                composable<StaffProfile> { entry ->
                    val route = entry.toRoute<StaffProfile>()
                    val vm: StaffProfileViewModel = viewModel(factory = viewModelFactory)
                    val state by vm.state.collectAsStateWithLifecycle()
                    val enrollmentSaved by entry.savedStateHandle.getStateFlow("enrollmentSaved", false).collectAsStateWithLifecycle()
                    StaffProfileScreen(state, enrollmentSaved,
                        onDismissConfirmation = { entry.savedStateHandle["enrollmentSaved"] = false },
                        onEnroll = {
                            entry.savedStateHandle["enrollmentSaved"] = false
                            navController.navigate(FaceEnrollment(route.staffId))
                        },
                        onRetry = vm::retry, onHistoryRetry = vm::retryHistory, onBack = { navController.popBackStack() })
                }
                composable<FaceEnrollment> {
                    val vm: FaceEnrollmentViewModel = viewModel(factory = viewModelFactory)
                    val state by vm.state.collectAsStateWithLifecycle()
                    LaunchedEffect(state.saved) {
                        if (state.saved) {
                            navController.previousBackStackEntry?.savedStateHandle?.set("enrollmentSaved", true)
                            navController.popBackStack()
                        }
                    }
                    FaceEnrollmentScreen(state, vm::enroll, vm::retry) { navController.popBackStack() }
                }
            }
            is UserSession.Staff -> composable<StaffAttendance> {
                val vm: AttendanceViewModel = viewModel(factory = viewModelFactory)
                val state by vm.state.collectAsStateWithLifecycle()
                AttendanceScreen(state, vm::markAttendance, vm::retry, vm::startAnother, onSignOut)
            }
        }
    }
}
