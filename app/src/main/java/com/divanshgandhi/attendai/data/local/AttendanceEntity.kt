package com.divanshgandhi.attendai.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "attendance", foreignKeys = [ForeignKey(
    entity = StaffEntity::class, parentColumns = ["id"], childColumns = ["staffId"],
    onDelete = ForeignKey.RESTRICT,
)], indices = [Index("staffId"), Index(value = ["selfieFileName"], unique = true)])
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val timestamp: Long,
    val selfieFileName: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)
