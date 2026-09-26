package com.divanshgandhi.attendai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :id")
    fun observeById(id: Long): Flow<StaffEntity?>

    @Insert
    suspend fun insert(staff: StaffEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM staff WHERE employeeId = :employeeId)")
    suspend fun employeeIdExists(employeeId: String): Boolean

    @Query("UPDATE staff SET faceEmbedding = :embedding, faceModelId = :modelId, faceEnrolledAt = :enrolledAt WHERE id = :id")
    suspend fun updateEnrollment(id: Long, embedding: ByteArray, modelId: String, enrolledAt: Long): Int
}
