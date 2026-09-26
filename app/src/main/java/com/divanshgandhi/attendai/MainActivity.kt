package com.divanshgandhi.attendai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.divanshgandhi.attendai.ui.AttendAiApp
import com.divanshgandhi.attendai.ui.theme.AttendAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AttendAiApplication).container
        setContent {
            AttendAITheme {
                AttendAiApp(container)
            }
        }
    }
}
