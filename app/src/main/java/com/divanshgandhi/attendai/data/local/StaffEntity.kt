package com.divanshgandhi.attendai.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "staff", indices = [Index(value = ["employeeId"], unique = true)])
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: String,
    val name: String,
    val faceEmbedding: FloatArray? = null,
    val faceModelId: String? = null,
    val faceEnrolledAt: Long? = null,
) {
    val isFaceEnrolled: Boolean get() = faceEmbedding != null && faceModelId != null && faceEnrolledAt != null
}
