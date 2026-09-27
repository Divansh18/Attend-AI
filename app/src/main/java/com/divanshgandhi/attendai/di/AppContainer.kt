package com.divanshgandhi.attendai.di

import android.content.Context
import androidx.room.Room
import java.io.File
import com.divanshgandhi.attendai.data.local.MIGRATION_1_2
import com.divanshgandhi.attendai.data.repository.RoomAttendanceRepository
import com.divanshgandhi.attendai.location.FusedLocationProvider
import com.divanshgandhi.attendai.storage.PrivateSelfieStorage
import com.divanshgandhi.attendai.data.local.AttendAiDatabase
import com.divanshgandhi.attendai.data.repository.AuthRepository
import com.divanshgandhi.attendai.data.repository.RoomStaffRepository
import com.divanshgandhi.attendai.face.FaceRecognitionEngine

/** Application-scoped database/repositories; face engines are owned by individual ViewModels. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, AttendAiDatabase::class.java, "attendai.db").addMigrations(MIGRATION_1_2).build()
    val staffRepository = RoomStaffRepository(database.staffDao())
    val authRepository = AuthRepository(staffRepository::findByEmployeeId)
    val attendanceRepository = RoomAttendanceRepository(database.attendanceDao(), PrivateSelfieStorage(File(appContext.filesDir, "attendance_selfies")))
    private val locationProvider = FusedLocationProvider(appContext)
    val viewModelFactory = appViewModelFactory(authRepository, staffRepository, attendanceRepository, locationProvider) {
        FaceRecognitionEngine(appContext.assets)
    }
}
