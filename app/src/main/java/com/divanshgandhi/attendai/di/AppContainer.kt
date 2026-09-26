package com.divanshgandhi.attendai.di

import android.content.Context
import androidx.room.Room
import com.divanshgandhi.attendai.data.local.AttendAiDatabase
import com.divanshgandhi.attendai.data.repository.AuthRepository
import com.divanshgandhi.attendai.data.repository.RoomStaffRepository
import com.divanshgandhi.attendai.face.FaceRecognitionEngine

/** Application-scoped database/repositories; face engines are owned by individual ViewModels. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, AttendAiDatabase::class.java, "attendai.db").build()
    val authRepository = AuthRepository()
    val staffRepository = RoomStaffRepository(database.staffDao())
    val viewModelFactory = appViewModelFactory(authRepository, staffRepository) {
        FaceRecognitionEngine(appContext.assets)
    }
}
