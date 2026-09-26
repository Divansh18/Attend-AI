package com.divanshgandhi.attendai.data.repository

import com.divanshgandhi.attendai.data.local.StaffEntity
import kotlinx.coroutines.flow.Flow

interface StaffRepository {
    fun observeStaff(): Flow<List<StaffEntity>>
    fun observeStaff(id: Long): Flow<StaffEntity?>
    suspend fun addStaff(name: String, employeeId: String): Long
    suspend fun saveFaceEmbedding(staffId: Long, embedding: FloatArray)
}

class DuplicateEmployeeIdException : Exception("That employee ID already exists.")
class StaffNotFoundException : Exception("Staff member not found.")
