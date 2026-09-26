package com.divanshgandhi.attendai

import android.app.Application
import com.divanshgandhi.attendai.di.AppContainer

class AttendAiApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
