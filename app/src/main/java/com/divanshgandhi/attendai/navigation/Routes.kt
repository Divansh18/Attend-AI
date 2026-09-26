package com.divanshgandhi.attendai.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Login

@Serializable
data object AdminStaffList

@Serializable
data object AddStaff

@Serializable
data class StaffProfile(val staffId: Long)

@Serializable
data class FaceEnrollment(val staffId: Long)

@Serializable
data object StaffAttendance

@Serializable
data object FaceProofOfConcept
