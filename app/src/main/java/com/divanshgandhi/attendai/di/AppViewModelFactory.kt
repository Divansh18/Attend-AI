package com.divanshgandhi.attendai.di

import com.divanshgandhi.attendai.ui.attendance.AttendanceViewModel
import com.divanshgandhi.attendai.data.repository.AttendanceRepository
import com.divanshgandhi.attendai.location.LocationProvider
import com.divanshgandhi.attendai.model.UserSession
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.divanshgandhi.attendai.data.repository.AuthRepository
import com.divanshgandhi.attendai.data.repository.StaffRepository
import com.divanshgandhi.attendai.face.FaceRecognitionEngine
import com.divanshgandhi.attendai.navigation.FaceEnrollment
import com.divanshgandhi.attendai.navigation.StaffProfile
import com.divanshgandhi.attendai.ui.SessionViewModel
import com.divanshgandhi.attendai.ui.admin.addstaff.AddStaffViewModel
import com.divanshgandhi.attendai.ui.admin.enrollment.FaceEnrollmentViewModel
import com.divanshgandhi.attendai.ui.admin.stafflist.StaffListViewModel
import com.divanshgandhi.attendai.ui.admin.staffprofile.StaffProfileViewModel
import com.divanshgandhi.attendai.ui.login.LoginViewModel

fun appViewModelFactory(authRepository: AuthRepository, staffRepository: StaffRepository, attendanceRepository: AttendanceRepository, locationProvider: LocationProvider, createFaceEngine: () -> FaceRecognitionEngine) = viewModelFactory {
    initializer {
        val staff = checkNotNull(authRepository.session.value as? UserSession.Staff)
        AttendanceViewModel(staff.staffId, staffRepository, attendanceRepository, createFaceEngine(), locationProvider)
    }
    initializer { SessionViewModel(authRepository) }
    initializer { LoginViewModel(authRepository) }
    initializer { StaffListViewModel(staffRepository) }
    initializer { AddStaffViewModel(staffRepository) }
    initializer { StaffProfileViewModel(createSavedStateHandle().toRoute<StaffProfile>().staffId, staffRepository, attendanceRepository) }
    initializer { FaceEnrollmentViewModel(createSavedStateHandle().toRoute<FaceEnrollment>().staffId, staffRepository, createFaceEngine()) }
}
