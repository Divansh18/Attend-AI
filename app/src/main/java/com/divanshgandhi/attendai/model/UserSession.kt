package com.divanshgandhi.attendai.model

enum class UserRole { ADMIN, STAFF }

sealed interface UserSession {
    data object Admin : UserSession
    data class Staff(val staffId: Long) : UserSession
}
